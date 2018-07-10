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

import eu.tango.self.adaptation.manager.model.CompssImplementation;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.model.SLATerm;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The aim of this class is to integrate with the programming model runtime so
 * that it can run in a compss job only environment without SLURM or the ALDE.
 * @author Richard Kavanagh
 */
public class CompssJobMonitor extends AbstractJobMonitor {

    private final ProgrammingModelClient datasource = new ProgrammingModelClient();
    private static final String IDLE_HOST = "IDLE_HOST";
    private static final String HOST_FAILURE = "HOST_FAILURE";    
    private static final String FRAME_RATE = "FRAME_RATE";    
    private static final String TASK_COMPLETION_RATE = "TASK_COMPLETION_RATE";    

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
        ArrayList<SLATerm> criteria = limits.getQosCriteria();
        List<CompssImplementation> jobs = datasource.getCompssImplementation();
        for (SLATerm term : criteria) {
            answer.addAll(detectEvent(term, jobs));   
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
                    job.getName(), //application name
                    job.getName(), //TODO application id
                    term.getAgreementTerm(), // guaranteed id
                    term.getAgreementTerm() //agreement term
                );
                answer.add(event);
            }
        }
        return answer;
    }   
    
}
