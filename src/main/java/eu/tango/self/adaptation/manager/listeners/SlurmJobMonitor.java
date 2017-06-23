/**
 * Copyright 2017 University of Leeds
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * This is being developed for the TANGO Project: http://tango-project.eu
 *
 */
package eu.tango.self.adaptation.manager.listeners;

import eu.tango.energymodeller.datasourceclient.HostDataSource;
import eu.tango.energymodeller.datasourceclient.SlurmDataSourceAdaptor;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.self.adaptation.manager.actuators.SlurmActuator;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.model.SLATerm;
import eu.tango.self.adaptation.manager.qos.SlaRulesLoader;
import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Monitors jobs to provide jobs based events for adaptation
 *
 * @author Richard Kavanagh
 */
public class SlurmJobMonitor implements EventListener, Runnable {

    private EventAssessor eventAssessor;
    private final HostDataSource datasource = new SlurmDataSourceAdaptor();
    private boolean running = true;
    private final SLALimits limits;
    private HashSet<Host> idleHosts = new HashSet<>();
    private HashSet<ApplicationOnHost> runningJobs = new HashSet<>();

    public SlurmJobMonitor() {
        SlaRulesLoader loader = new SlaRulesLoader();
        limits = loader.getLimits();
    }

    @Override
    public void setEventAssessor(EventAssessor assessor) {
        eventAssessor = assessor;
    }

    @Override
    public EventAssessor getEventAssessor() {
        return eventAssessor;
    }
    
    /**
     * This starts the environment monitor going, in a daemon thread.
     */
    public void startListening() {
        Thread slurmJobMonThread = new Thread(this);
        slurmJobMonThread.setDaemon(true);
        slurmJobMonThread.start();
    }    

    @Override
    public void stopListening() {
        running = false;
    }

    @Override
    public void run() {
        try {
            // Wait for a message
            while (running) {
                for (EventData event : detectEvent(limits)) {
                    eventAssessor.assessEvent(event);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SlurmJobMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SlurmJobMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Checks to see if the SLA rules includes a check for a given condition
     *
     * @param limits The SLA terms
     * @param termName The name of the SLA term
     * @return If the term is contained or not within the SLA limits set
     */
    private boolean containsTerm(SLALimits limits, String termName) {
        for (SLATerm slaTerm : limits.getQosCriteria()) {
            if (slaTerm.getAgreementTerm().equals(termName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This takes a list of measurements and determines if an SLA breach has
     * occurred by comparing them to the QoS limits.
     *
     * @param measurements The list of measurements
     * @param limits The QoS goal limits.
     * @return The first SLA breach event. Null if none found.
     */
    private ArrayList<EventData> detectEvent(SLALimits limits) {
        ArrayList<EventData> answer = new ArrayList<>();
        if (containsTerm(limits, "IDLE_HOST")) {
            answer.addAll(detectRecentIdleHost());
        }
        if (containsTerm(limits, "APP_FINISHED")) {
            answer.addAll(detectRecentCompletedApps());
        }
        if (containsTerm(limits, "IDLE_HOST+PENDING_JOB")) {
            answer.addAll(detectIdleHostsWithPendingJobs());
        }
        //Add next test here

        //TODO Consider duration host is idle.
        //TODO Consider size of queue for given host
        /**
         * Commands to consider when extending this:
         *
         * List all running jobs for a user: squeue -u <username> -t RUNNING
         * List all pending jobs for a user: squeue -u <username> -t PENDING
         * List priority order of jobs for the current user (you) in a given
         * partition: showq-slurm -o -u -q <partition>
         */
        return answer;
    }

    /**
     * This takes the list of hosts and detects if one has recently become free.
     *
     * @return An event indicating that a physical host has just become free.
     */
    private ArrayList<EventData> detectRecentIdleHost() {
        ArrayList<EventData> answer = new ArrayList<>();
        HashSet<Host> currentIdle = getIdleHosts();
        HashSet<Host> recentIdle = new HashSet<>(currentIdle);
        recentIdle.removeAll(idleHosts);
        if (!recentIdle.isEmpty()) {
            for (Host idleHost : recentIdle) {
                //return the list of recently idle hosts.
                EventData event;
                event = new HostEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), idleHost.getHostName(),
                        0.0,
                        0.0,
                        EventData.Type.OTHER,
                        EventData.Operator.EQ,
                        "IDLE_HOST" + (idleHost.hasAccelerator() ? "+ACCELERATED" : ""),
                        "IDLE_HOST" + (idleHost.hasAccelerator() ? "+ACCELERATED" : ""));
                answer.add(event);
                idleHosts = currentIdle;
            }

        }
        return answer;
    }

    /**
     * This detects recently finished jobs
     *
     * @return The list of events indicating which jobs had finished.
     */
    private ArrayList<EventData> detectRecentCompletedApps() {
        ArrayList<EventData> answer = new ArrayList<>();
        HashSet<ApplicationOnHost> currentRunning = new HashSet<>(datasource.getHostApplicationList());
        HashSet<ApplicationOnHost> recentFinished = new HashSet<>(runningJobs);
        recentFinished.removeAll(currentRunning);
        if (!recentFinished.isEmpty()) {
            for (ApplicationOnHost finished : recentFinished) {
                //return the recently finished applications.
                EventData event = new ApplicationEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                        0.0,
                        0.0,
                        EventData.Type.OTHER,
                        EventData.Operator.EQ,
                        finished.getName(),
                        finished.getId() + "",
                        "APP_FINISHED",
                        "APP_FINISHED");
                runningJobs = currentRunning;
                answer.add(event);
            }

        }
        return answer;
    }

    /**
     * This detects hosts that have jobs stuck on them with pending resource
     * requirements.
     *
     * @return The list of events indicating which hosts have stuck jobs.
     */
    private ArrayList<EventData> detectIdleHostsWithPendingJobs() {
        ArrayList<EventData> answer = new ArrayList<>();
        List<Host> stuckHosts = getIdleHostsWithPendingJobs();
        EventData event;
        for (Host stuckHost : stuckHosts) {
            event = new HostEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), stuckHost.getHostName(),
                    0.0,
                    0.0,
                    EventData.Type.OTHER,
                    EventData.Operator.EQ,
                    "IDLE_HOST" + (stuckHost.hasAccelerator() ? "+ACCELERATED" : "") + "+PENDING_JOB",
                    "IDLE_HOST" + (stuckHost.hasAccelerator() ? "+ACCELERATED" : "") + "+PENDING_JOB");
            answer.add(event);
        }
        return answer;
    }

    /**
     * This lists the pending jobs on an idle host. This means the SAM has the
     * possibility of detecting this and therefore responding to it. e.g. it
     * might get the ALDE to recompile so it can place the job elsewhere.
     *
     * @return The list of idle hosts with pending jobs (i.e. blocked for
     * another reason, such as not all resources were obtainable)
     */
    private List<Host> getIdleHostsWithPendingJobs() {
        List<Host> answer = new ArrayList<>();
        HashSet<Host> currentIdle = getIdleHosts();
        List<ApplicationOnHost> pendingJobs = datasource.getHostApplicationList(HostDataSource.JOB_STATUS.PENDING);
        for (ApplicationOnHost pendingJob : pendingJobs) {
            if (currentIdle.contains(pendingJob.getAllocatedTo())) {
                answer.add(pendingJob.getAllocatedTo());
            }
        }
        return answer;
    }

    /**
     * This lists the hosts that are idle
     *
     * @return The list of hosts that are currently idle
     */
    private HashSet<Host> getIdleHosts() {
        HashSet<Host> answer = new HashSet(datasource.getHostList());
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        for (ApplicationOnHost app : apps) {
            if (answer.contains(app.getAllocatedTo())) {
                answer.remove(app.getAllocatedTo());
            }
        }
        return answer;
    }
    
    /**
     * This lists the long pending jobs in the queue. 
     *
     * @return The list of pending jobs, that have been in the queue for a long
     * time.
     */
    private List<Host> getLongPendingJobsInQueue() {
        List<Host> answer = new ArrayList<>();
        //TODO Consider if this is a deployment time issue, not runtime??
        //Is it only runtime if the resources available change
        List<ApplicationOnHost> pendingJobs = datasource.getHostApplicationList(HostDataSource.JOB_STATUS.PENDING);
        for (ApplicationOnHost pendingJob : pendingJobs) {
            //TODO add the notion of long pending here
                answer.add(pendingJob.getAllocatedTo());
        }
        return answer;
    }    

    /**
     * This executes a command and returns the output as a line of strings.
     *
     * @param cmd The command to execute
     * @return A list of output broken down by line
     * @throws java.io.IOException
     */
    private static ArrayList<String> execCmd(String mainCmd) {
        String cmd[] = {"/bin/sh",
            "-c",
            mainCmd};
        try {
            return execCmd(cmd);
        } catch (IOException ex) {
            Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    /**
     * This executes a command and returns the output as a line of strings.
     *
     * @param cmd The command to execute
     * @return A list of output broken down by line
     * @throws java.io.IOException
     */
    private static ArrayList<String> execCmd(String[] cmd) throws java.io.IOException {
        ArrayList<String> output = new ArrayList<>();
        Process proc = Runtime.getRuntime().exec(cmd);
        java.io.InputStream is = proc.getInputStream();
        java.util.Scanner s = new java.util.Scanner(is);
        String outputLine;
        while (s.hasNextLine()) {
            outputLine = s.nextLine();
            output.add(outputLine);
        }
        return output;
    }

}
