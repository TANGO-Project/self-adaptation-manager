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

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.model.SLATerm;
import eu.tango.self.adaptation.manager.qos.SlaRulesLoader;
import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The aim of this class is to integrate with the programming model runtime so
 * that it can run in a compss job only environment without SLURM or the ALDE.
 * @author Richard Kavanagh
 */
public class CompssJobMonitor extends AbstractJobMonitor {

    private HashSet<Host> idleHosts = new HashSet<>();
    private HashSet<Host> failingHosts = new HashSet<>();
    private HashSet<Host> drainingHosts = new HashSet<>();
    private double lastPowerCap = Double.NaN;
    private HashSet<ApplicationOnHost> runningJobs = null;
    private ProgrammingModelClient datasource = new ProgrammingModelClient();
    private final SlaRulesLoader limits = SlaRulesLoader.getInstance();
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

    public CompssJobMonitor() { 
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
            //answer.addAll(detectRecentIdleHost());
        }
        if (containsTerm(limits, APP_STARTED) || containsTerm(limits, APP_FINISHED)) {
            //answer.addAll(detectAppStartAndEnd(containsTerm(limits, APP_STARTED), containsTerm(limits, APP_FINISHED)));
        }
        if (containsTerm(limits, IDLE_HOST + PENDING_JOB)) {
            //answer.addAll(detectIdleHostsWithPendingJobs());
        }
        if (containsTerm(limits, CLOSE_TO_DEADLINE)) {
            //answer.addAll(detectCloseToDeadlineJobs());
        }
        if (containsTerm(limits, HOST_FAILURE)) {
            //answer.addAll(detectHostFailure(true));
        }        
        return answer;
    }    
    
}
