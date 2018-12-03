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
import eu.tango.energymodeller.datasourceclient.TangoEnvironmentDataSourceAdaptor;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.model.SLATerm;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Monitors jobs to provide jobs based events for adaptation
 *
 * @author Richard Kavanagh
 */
public class SlurmJobMonitor extends AbstractJobMonitor {

    private final HostDataSource datasource;
    private HashSet<Host> idleHosts = new HashSet<>();
    private HashSet<Host> failingHosts = new HashSet<>();
    private HashSet<Host> drainingHosts = new HashSet<>();
    private double lastPowerCap = Double.NaN;
    private HashSet<ApplicationOnHost> runningJobs = null;
    
    private static final String APP_STARTED = "APP_STARTED";
    private static final String APP_FINISHED = "APP_FINISHED";
    private static final String IDLE_HOST = "IDLE_HOST";
    private static final String POWER_CAP = "POWER_CAP"; 
    private static final String SUSPENDED_JOB = "+SUSPENDED_JOB";
    private static final String PENDING_JOB = "+PENDING_JOB";
    private static final String ACCELERATED = "+ACCELERATED";
    private static final String CLOSE_TO_DEADLINE = "CLOSE_TO_DEADLINE";
    private static final String HOST_DRAIN = "HOST_DRAIN";
    private static final String HOST_FAILURE = "HOST_FAILURE";

    /**
     * no-args constructor, uses a Slurm data source to drive the job monitor
     */
    public SlurmJobMonitor() {
        datasource = new SlurmDataSourceAdaptor();
    }

    /**
     * This creates a slurm job monitor that has a named data source. Thus it
     * allows the job monitor to share the same instance/connection as other
     * monitoring components.
     *
     * @param datasource Either a slurm data source adaptor or tango based
     * adaptor.
     */
    public SlurmJobMonitor(HostDataSource datasource) {
        if (datasource == null) {
            this.datasource = new SlurmDataSourceAdaptor();
            return;
        }
        if (datasource instanceof SlurmDataSourceAdaptor || datasource instanceof TangoEnvironmentDataSourceAdaptor) {
            this.datasource = datasource;
        } else {
            this.datasource = new SlurmDataSourceAdaptor();
        }
    }

    /**
     * This takes a list of measurements and determines if an SLA breach has
     * occurred by comparing them to the QoS limits.
     *
     * @param limits The QoS goal limits.
     * @return The first SLA breach event. Null if none found.
     */
    @Override
    protected ArrayList<EventData> detectEvent(SLALimits limits) {
        ArrayList<EventData> answer = new ArrayList<>();
        if (containsTerm(limits, IDLE_HOST)) {
            answer.addAll(detectRecentIdleHost());
        }
        if (containsTerm(limits, APP_STARTED) || containsTerm(limits, APP_FINISHED)) {
            answer.addAll(detectAppStartAndEnd(containsTerm(limits, APP_STARTED), containsTerm(limits, APP_FINISHED)));
        }
        if (containsTerm(limits, IDLE_HOST + SUSPENDED_JOB)) {
            answer.addAll(detectIdleHostsWithSuspendedJobs());
        }
        if (containsTerm(limits, IDLE_HOST + PENDING_JOB)) {
            answer.addAll(detectIdleHostsWithPendingJobs());
        }
        if (containsTerm(limits, CLOSE_TO_DEADLINE, true)) {
            answer.addAll(detectCloseToDeadlineJobs(limits));
        }
        if (containsTerm(limits, POWER_CAP)) {
            answer.addAll(detectPowerCapChange(limits));
        }   
        if (containsTerm(limits, HOST_FAILURE)) {
            answer.addAll(detectHostFailure(true));
        }
        if (containsTerm(limits, HOST_DRAIN)) {
            answer.addAll(detectHostDrain());
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
                        EventData.Type.WARNING,
                        EventData.Operator.EQ,
                        IDLE_HOST + (idleHost.hasAccelerator() ? ACCELERATED : ""),
                        IDLE_HOST + (idleHost.hasAccelerator() ? ACCELERATED : ""));
                event.setSignificantOnOwn(true);
                answer.add(event);
            }
        }
        idleHosts = currentIdle;
        return answer;
    }
    
    /**
     * This detects changes to SLURMs power cap
     * @return An event indicating the change in the current power cap
     */
    private ArrayList<EventData> detectPowerCapChange(SLALimits limits) {   
        ArrayList<EventData> answer = new ArrayList<>();
        double currentPowerCap = SlurmDataSourceAdaptor.getCurrentPowerCap();
        if (currentPowerCap != lastPowerCap && Double.isFinite(currentPowerCap)) {
            for (SLATerm term : limits.getQosCriteria()) {
                if (term.getAgreementTerm().equals(POWER_CAP) && term.isBreached(currentPowerCap)) {
                    EventData event = new HostEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                        "*",
                        currentPowerCap,
                        term.getGuaranteedValue(),
                        EventData.Type.WARNING,
                        term.getGuaranteeOperator(),
                        POWER_CAP,
                        POWER_CAP);
                    event.setSignificantOnOwn(true);
                    answer.add(event);
                }
            }
        }
        lastPowerCap = currentPowerCap;
        return answer;
    }

    /**
     * This takes the list of hosts and detects if one has recently been set to
     * a failure state
     *
     * @return An event indicating that a physical host has just become free.
     */
    private ArrayList<EventData> detectHostFailure(boolean includeAboutToFail) {
        ArrayList<EventData> answer = new ArrayList<>();
        HashSet<Host> failed = getHostInState("failed");
            HashSet<Host> down = getHostInState("down");
            failed.addAll(down);
        if (includeAboutToFail) {
            HashSet<Host> failing = getHostInState("failing");
            failed.addAll(failing);
        }
        HashSet<Host> recentFailed = new HashSet<>(failed);
        recentFailed.removeAll(this.failingHosts);
        if (!recentFailed.isEmpty()) {
            for (Host failedHost : recentFailed) {
                //return the list of recently failing hosts.
                EventData event;
                event = new HostEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), failedHost.getHostName(),
                        0.0,
                        0.0,
                        EventData.Type.WARNING,
                        EventData.Operator.EQ,
                        HOST_FAILURE + (failedHost.hasAccelerator() ? ACCELERATED : ""),
                        HOST_FAILURE + (failedHost.hasAccelerator() ? ACCELERATED : ""));
                event.setSignificantOnOwn(true);
                answer.add(event);
            }
        }
        this.failingHosts = failed;        
        return answer;
    }

    /**
     * This takes the list of hosts and detects if one has recently been set to
     * drain.
     *
     * @return An event indicating that a physical host has just started to
     * drain.
     */
    private ArrayList<EventData> detectHostDrain() {
        ArrayList<EventData> answer = new ArrayList<>();
        HashSet<Host> draining = getHostInState("draining");
        HashSet<Host> drain = getHostInState("drain");
        draining.addAll(drain);
        HashSet<Host> recentDrain = new HashSet<>(draining);
        recentDrain.removeAll(this.drainingHosts);
        if (!recentDrain.isEmpty()) {
            for (Host drainHost : recentDrain) {
                //return the list of recently draining hosts.
                EventData event;
                event = new HostEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), drainHost.getHostName(),
                        0.0,
                        0.0,
                        EventData.Type.WARNING,
                        EventData.Operator.EQ,
                        HOST_DRAIN + (drainHost.hasAccelerator() ? ACCELERATED : ""),
                        HOST_DRAIN + (drainHost.hasAccelerator() ? ACCELERATED : ""));
                event.setSignificantOnOwn(true);
                answer.add(event);
            }
        }
        this.drainingHosts = draining;        
        return answer;
    }

    /**
     * This detects recently finished jobs
     *
     * @return The list of events indicating which jobs had finished.
     */
    private ArrayList<EventData> detectAppStartAndEnd(boolean startedJobs, boolean finishedJobs) {
        ArrayList<EventData> eventsList = new ArrayList<>();
        if (runningJobs == null) {
            /**
             * This checks the startup case, where detection doesn't want to act
             * just because the SAM started.
             */
            runningJobs = new HashSet<>(datasource.getHostApplicationList(ApplicationOnHost.JOB_STATUS.RUNNING));
            return eventsList;
        }
        //The job status, prevents jobs that have just started and not been allocated creating a starting event 
        List<ApplicationOnHost> currentRoundAppList = datasource.getHostApplicationList(ApplicationOnHost.JOB_STATUS.RUNNING);
        HashSet<ApplicationOnHost> firstRound = new HashSet<>(runningJobs);
        HashSet<ApplicationOnHost> secondRound = new HashSet<>(currentRoundAppList);
        HashSet<ApplicationOnHost> recentStarted = new HashSet<>(secondRound);
        recentStarted.removeAll(firstRound); //Remove all jobs that are already running
        HashSet<ApplicationOnHost> recentFinished = new HashSet<>(firstRound);
        recentFinished.removeAll(secondRound); //Remove all jobs that are still running
        //Ensure the sets are disjoint, this helps protect against any errors
        recentFinished.removeAll(recentStarted);
        recentStarted.removeAll(recentFinished);
        if (finishedJobs) {
            for (ApplicationOnHost finished : recentFinished) {
                //return the recently finished applications.
                EventData event = new ApplicationEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                        0.0,
                        0.0,
                        EventData.Type.WARNING,
                        EventData.Operator.EQ,
                        finished.getName(),
                        finished.getId() + "",
                        APP_FINISHED,
                        APP_FINISHED);
                event.setSignificantOnOwn(true);
                eventsList.add(event);                
            }
        }
        if (startedJobs) {
            for (ApplicationOnHost started : recentStarted) {
                //return the recently finished applications.
                EventData event = new ApplicationEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                        0.0,
                        0.0,
                        EventData.Type.WARNING,
                        EventData.Operator.EQ,
                        started.getName(),
                        started.getId() + "",
                        APP_STARTED,
                        APP_STARTED);
                event.setSignificantOnOwn(true);
                eventsList.add(event);
            }

        }
        //Ensures the list of currently running jobs is updated.
        runningJobs = new HashSet<>(currentRoundAppList);
        return eventsList;
    }

//    private SLALimits getNearBoundaryLimit(String applicaitonName) {
//        limits.getSlaLimits("App", null); //filter by specific limit name
//    }
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
                    EventData.Type.WARNING,
                    EventData.Operator.EQ,
                    IDLE_HOST + (stuckHost.hasAccelerator() ? ACCELERATED : "") + PENDING_JOB,
                    IDLE_HOST + (stuckHost.hasAccelerator() ? ACCELERATED : "") + PENDING_JOB);
            event.setSignificantOnOwn(true);
            answer.add(event);
        }
        return answer;
    }

    /**
     * This detects hosts that have jobs stuck on them with pending resource
     * requirements.
     *
     * @return The list of events indicating which hosts have stuck jobs.
     */
    private ArrayList<EventData> detectIdleHostsWithSuspendedJobs() {
        ArrayList<EventData> answer = new ArrayList<>();
        List<Host> stuckHosts = getIdleHostsWithSuspendedJobs();
        EventData event;
        for (Host stuckHost : stuckHosts) {
            event = new HostEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), stuckHost.getHostName(),
                    0.0,
                    0.0,
                    EventData.Type.WARNING,
                    EventData.Operator.EQ,
                    IDLE_HOST + (stuckHost.hasAccelerator() ? ACCELERATED : "") + SUSPENDED_JOB,
                    IDLE_HOST + (stuckHost.hasAccelerator() ? ACCELERATED : "") + SUSPENDED_JOB);
            event.setSignificantOnOwn(true);
            answer.add(event);
        }
        return answer;
    }

    /**
     * This detects jobs that are nearing their deadline.
     *
     * @return The list of events indicating which jobs are nearing their
     * deadline, which would cause them to terminate.
     */
    private ArrayList<EventData> detectCloseToDeadlineJobs(SLALimits limits) {
        ArrayList<EventData> answer = new ArrayList<>();
        HashSet<ApplicationOnHost> currentRunning = new HashSet<>(datasource.getHostApplicationList());
        for (ApplicationOnHost job : currentRunning) {
            double boundary = getApplicationDeadline(limits, job);
            if (job.getProgress() > boundary) {
                EventData event = new ApplicationEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                        0.0,
                        0.0,
                        EventData.Type.WARNING,
                        EventData.Operator.EQ,
                        job.getName(),
                        job.getId() + "",
                        CLOSE_TO_DEADLINE,
                        CLOSE_TO_DEADLINE);
                event.setSignificantOnOwn(true);
                answer.add(event);
            }
        }
        return answer;
    }
    
    /**
     * This reads the limits loaded and considers if any specify an application 
     * deadline limit for the application.
     * @param limits the list of SLA limits
     * @param application The application to find the SLA time limit
     * @return The deadline as percentage of time available to the application
     */
    private double getApplicationDeadline(SLALimits limits, ApplicationOnHost application) {
        double answer = 95.0;
        SLALimits filtered = SLALimits.filterTerms(limits, application.getName());
        for (SLATerm term : filtered.getQosCriteria()) {
            if (term.getAgreementTerm().contains(CLOSE_TO_DEADLINE)) {
                String[] split = term.getSplitAgreementTerm();
                if (split.length == 2) {
                    return Double.parseDouble(split[1]);
                } else if(split.length == 3) {
                    if (application.getName().equals(split[1])) {
                        return Double.parseDouble(split[2]);
                    }
                }
            }
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
        List<ApplicationOnHost> pendingJobs = datasource.getHostApplicationList(ApplicationOnHost.JOB_STATUS.PENDING);
        for (ApplicationOnHost pendingJob : pendingJobs) {
            if (currentIdle.contains(pendingJob.getAllocatedTo())) {
                answer.add(pendingJob.getAllocatedTo());
            }
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
    private List<Host> getIdleHostsWithSuspendedJobs() {
        List<Host> answer = new ArrayList<>();
        HashSet<Host> currentIdle = getIdleHosts();
        List<ApplicationOnHost> suspendedJobs = datasource.getHostApplicationList(ApplicationOnHost.JOB_STATUS.SUSPENDED);
        for (ApplicationOnHost suspendedJob : suspendedJobs) {
            if (currentIdle.contains(suspendedJob.getAllocatedTo())) {
                answer.add(suspendedJob.getAllocatedTo());
            }
        }
        return answer;
    }

    /**
     * This lists the hosts that are in a specified state.
     *
     * @return The list of hosts that are in the specified state
     */
    private HashSet<Host> getHostInState(String state) {
        HashSet<Host> answer = new HashSet<>();
        List<Host> hosts = datasource.getHostList();
        for (Host host : hosts) {
            if (host.getState().trim().equalsIgnoreCase(state)
                    || host.getState().toLowerCase().trim().contains(state.toLowerCase()) ) {
                answer.add(host);
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
        HashSet<Host> answer = new HashSet<>();
        List<Host> hosts = datasource.getHostList();
        for (Host item : hosts) {
            if (item.getState().trim().equalsIgnoreCase("IDLE")) {
                answer.add(item);
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
        List<ApplicationOnHost> pendingJobs = datasource.getHostApplicationList(ApplicationOnHost.JOB_STATUS.PENDING);
        for (ApplicationOnHost pendingJob : pendingJobs) {
            //TODO add the notion of long pending here
            answer.add(pendingJob.getAllocatedTo());
        }
        return answer;
    }

}
