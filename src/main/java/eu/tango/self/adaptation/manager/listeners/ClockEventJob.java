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

import eu.tango.self.adaptation.manager.rules.datatypes.ClockEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This represents an executable job by the Quartz scheduler. It invokes actions
 * via the Clock Monitor class that started the action.
 * @author Richard Kavanagh
 */
public class ClockEventJob implements Job {

        public ClockEventJob() {
        }
        
        @Override
        public void execute(JobExecutionContext jec) throws JobExecutionException {
            for (EventData event : detectEvent(jec)) {
                Logger.getLogger(ClockEventJob.class.getName()).log(Level.INFO, "Clock Based Event Detected: {0}", jec.toString());
                ClockMonitor.getInstance().assessEvent(event);
            }
        }
        
    /**
     * This indicates when the clock has met a given time interval.
     *
     * @return A fully constructed event data for the clock based event.
     */
    private ArrayList<EventData> detectEvent(JobExecutionContext jec) {
        ArrayList<EventData> answer = new ArrayList<>();
        Logger.getLogger(ClockEventJob.class.getName()).log(Level.INFO, "Sending clock trigger event.");
        ClockEventData event = new ClockEventData(TimeUnit.MILLISECONDS.toSeconds(jec.getFireTime().getTime()),
                0.0,
                0.0,
                EventData.Type.WARNING,
                EventData.Operator.EQ,
                "CLOCK_TRIGGER",
                jec.getTrigger().getKey().getName());
        if (jec.getTrigger().getDescription() != null) {
            event.setSettings(jec.getTrigger().getDescription());
        }
        answer.add(event);
        return answer;
    }        
    
}
