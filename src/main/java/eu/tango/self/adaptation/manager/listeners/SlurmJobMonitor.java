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
    private HostDataSource datasource = new SlurmDataSourceAdaptor();
    private boolean running = true;
    private static final String CONFIG_FILE = "self-adaptation-manager-sla.properties";
    private String workingDir;
    private SLALimits limits;
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

    @Override
    public void stopListening() {
        running = false;
    }

    @Override
    public void run() {
        try {
            // Wait for a message
            while (running) {
                for (EventData event: detectEvent(limits)) {
                    eventAssessor.assessEvent(event);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(EnvironmentMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        ArrayList<EventData> answer = detectRecentIdleHost();
        answer.addAll(detectRecentCompletedApps());
        //Add next test here
        
        //TODO Consider duration host is idle.
        //TODO Consider accelerator becoming free. High importance
        return answer;
    }
    
    /**
     * This takes the list of hosts and detects if one has recently become free.
     * @return An event indicating that a physical host has just become free.
     */
    private ArrayList<EventData> detectRecentIdleHost() {
        ArrayList<EventData> answer = new ArrayList<>();
        HashSet<Host> currentIdle = getIdleHosts();
        HashSet<Host> recentIdle = new HashSet<>(currentIdle);
        recentIdle.removeAll(idleHosts);
        if (!recentIdle.isEmpty()) {
            for (Host idleHost : recentIdle) {
                //return the first recently idle host.
                EventData event = new HostEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), idleHost.getHostName(),
                        0.0,
                        0.0,
                        EventData.Type.OTHER,
                        EventData.Operator.EQ,
                        "HOST_IDLE",
                        "HOST_IDLE");
                idleHosts = currentIdle;
                answer.add(event);
            }

        }
        return answer;
    }
    
    /**
     * This detects recently finished jobs
     * @return 
     */
    private ArrayList<EventData> detectRecentCompletedApps() {
        ArrayList<EventData> answer = new ArrayList<>();
        HashSet<ApplicationOnHost> currentRunning = new HashSet<>(datasource.getHostApplicationList());
        HashSet<ApplicationOnHost> recentFinished = new HashSet<>(runningJobs);
        recentFinished.removeAll(currentRunning);
        if (!recentFinished.isEmpty()) {
            for (ApplicationOnHost finished : recentFinished) {
                //return the first recently idle host.
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
            execCmd(cmd);
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
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String val;
        while (s.hasNextLine()) {
            val = s.next();
            output.add(val);
        }
        return output;
    }

}
