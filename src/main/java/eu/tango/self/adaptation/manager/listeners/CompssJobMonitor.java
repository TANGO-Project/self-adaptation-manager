/**
 * Copyright 2018 University of Leeds
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

import eu.tango.energymodeller.datasourceclient.CompssDatasourceAdaptor;
import eu.tango.energymodeller.datasourceclient.compsstype.CompssImplementation;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.model.SLATerm;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The aim of this class is to integrate with the programming model runtime so
 * that it can run in a compss job only environment without SLURM or the ALDE.
 * @author Richard Kavanagh
 */
public class CompssJobMonitor extends AbstractJobMonitor {

    private final CompssDatasourceAdaptor datasource = new CompssDatasourceAdaptor();
    private static final String APP_STARTED = "APP_STARTED";
    private static final String APP_FINISHED = "APP_FINISHED";    
    private static final String IDLE_HOST = "IDLE_HOST";
    private static final String HOST_FAILURE = "HOST_FAILURE";    
    private static final String ACCELERATED = "+ACCELERATED";    
    private static final String FRAME_RATE = "FRAME_RATE";    
    private static final String TASK_COMPLETION_RATE = "TASK_COMPLETION_RATE";
    private HashSet<Host> idleHosts = new HashSet<>();
    private HashSet<Host> failingHosts = new HashSet<>();
    private HashSet<ApplicationOnHost> runningJobs = null;
    /**
     * The next two hashmaps are used to find the average jobs completed, over
     * a short window. The Compss calculation works for all of time as it is 
     * un-windowed.
     */
    HashMap<String, CompssImplementation> previous = new HashMap<>();
    HashMap<String, Long> previousTime = new HashMap<>();    

    public CompssJobMonitor() { 
    }
    
    /**
     * This returns the age of the monitoring file. It can therefore be used
     * to test to see if the information is stale
     * @return The age in seconds of the monitoring file
     */
    private long getMonitoringFileAge() {
        long monitoringFileDate = datasource.getMonitoringFileLastModifiedDate();
        long currentDate = new GregorianCalendar().getTimeInMillis();
        return TimeUnit.MILLISECONDS.toSeconds(currentDate - monitoringFileDate);
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
        ArrayList<SLATerm> criteria = limits.getQosCriteria();
        List<CompssImplementation> jobs = datasource.getCompssImplementation();
        if (datasource.getRunningTaskCount() == 0 || getMonitoringFileAge() > 15) {
            //No applications are running so there can't be any application based events
            return answer;
        }
        for (SLATerm term : criteria) {
            answer.addAll(detectEvent(term, jobs));   
        }
        if (containsTerm(limits, IDLE_HOST)) {
            answer.addAll(detectRecentIdleHost());
        }
        if (containsTerm(limits, HOST_FAILURE)) {
            answer.addAll(detectHostFailure());
        }
        if (containsTerm(limits, APP_STARTED) || containsTerm(limits, APP_FINISHED)) {
            answer.addAll(detectAppStartAndEnd(containsTerm(limits, APP_STARTED), containsTerm(limits, APP_FINISHED)));
        }
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
     * This takes the list of hosts and detects if one has recently been set to
     * a failure state
     *
     * @return An event indicating that a physical host has just become free.
     */
    private ArrayList<EventData> detectHostFailure() {
        ArrayList<EventData> answer = new ArrayList<>();
        HashSet<Host> failed = getHostInState("failed");
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
     * This lists the hosts that are in a specified state.
     *
     * @return The list of hosts that are in the specified state
     */
    private HashSet<Host> getHostInState(String state) {
        HashSet<Host> answer = new HashSet<>();
        List<Host> hosts = datasource.getHostList();
        for (Host host : hosts) {
            if (host.getState().equals(state)) {
                answer.add(host);
            }
        }
        return answer;
    }    
    
    /**
     * This detects changes in processing rates to generate events, such as framerate
     * @param term The term to compare
     * @return The list of events within the system, such as framerate drops
     */
    private ArrayList<EventData> detectEvent(SLATerm term, List<CompssImplementation> jobs) {
        ArrayList<EventData> answer = new ArrayList<>();
        EventData event;
        double currentValue;       
        for (CompssImplementation job : jobs) {
            switch (term.getAgreementTerm()) {
                case FRAME_RATE:
                case TASK_COMPLETION_RATE:
                case "MeanExecutionTime":                    
                    currentValue = job.getAverageTime();                    
                break;
                case "MinExecutionTime":
                    currentValue = job.getMinTime();                      
                break;
                case "MaxExecutionTime":
                    currentValue = job.getMaxTime();                      
                break;
		case "ExecutedCount":
                    currentValue = job.getExecutionCount();                      
                break;
                case "ActionCount":
                    currentValue = datasource.getHostApplicationList().size();
                break;
                case "RollingAverage": //Job's completion rate since last record
                    long time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                    currentValue = getSpotRunningTime(job, time);
                break;
                default: //Unrecognised term, so continue
                    continue;
            }           
            if (term.isBreached(currentValue)) {
                event = new ApplicationEventData(
                    TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                    currentValue, //measured value
                    term.getGuaranteedValue(), //guaranteed value 
                    term.getSeverity(), //breach type
                    term.getGuaranteeOperator(), //operator
                    datasource.getCurrentMonitoringJobId().replaceAll("[_0-9]", ""), //application name
                    datasource.getCurrentMonitoringJobId().replaceAll("[^0-9]", ""), //application id
                    term.getAgreementTerm(), // guaranteed id
                    term.getAgreementTerm() //agreement term
                    /**
                     * TODO consider if compss implementation data should be added 
                     * to the agreement term.
                     * i.e. job.getName() identifies the Compss implementation 
                     * at fault, not the job itself.
                     */
                );
                answer.add(event);
            }
        }
        return answer;
    }
    
    /**
     * This gets the spot average runtime of an application - compss reports the
     * average runtime for all time. This method instead generates the average since 
     * the last record was seen, thus uses a short window length, to aid responsiveness.
     * @param item The current compss implementation record undergoing writing to disk
     * @param time The current time, in milliseconds
     * @return The rate at which jobs have been processed
     */
    private double getSpotRunningTime(CompssImplementation item, long time) {
        double answer = 0;
        long currentTime = time / 1000;       
        Long lastTimeValue = previousTime.get(item.getName());
        CompssImplementation previousItem = previous.get(item.getName());
        if (lastTimeValue != null && previousItem != null) {
            int changeInExecutionCount = item.getExecutionCount() - previousItem.getExecutionCount();
            long changeInTime = currentTime - lastTimeValue;
            answer = ((double) changeInExecutionCount) / changeInTime;
        }
        //make a record of the previous item seen
        previousTime.put(item.getName(), currentTime);
        previous.put(item.getName(), item);
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
    
}
