/**
 * Copyright 2015 University of Leeds
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
 */
package eu.tango.self.adaptation.manager.rules.loggers;

import eu.ascetic.ioutils.io.GenericLogger;
import eu.ascetic.ioutils.io.ResultsStore;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import java.io.File;

/**
 *
 * This logs out a history of event arrivals to disk.
 * @author Richard Kavanagh
 */
public class EventHistoryLogger extends GenericLogger<EventData> {

    /**
     * This creates a new logger that is used to write out to disk event data.
     * @param file The file to write the log out to
     * @param overwrite If the file should be overwritten on starting the logger
     */
    public EventHistoryLogger(File file, boolean overwrite) {
        super(file, overwrite);
    }    
    
    @Override
    public void writeHeader(ResultsStore store) {
        store.add("Time");
        store.append("Application ID");
        store.append("Deployment ID");
        store.append("Host");
        store.append("Agreement Term");
        store.append("Guarantee Value");          
        store.append("Raw Value");   
        store.append("Guarantee Operator");   
    }

    @Override
    public void writebody(EventData eventData, ResultsStore store) {
        store.add(eventData.getTime());
        if (eventData instanceof ApplicationEventData) {
        store.append(((ApplicationEventData)eventData).getApplicationId());
        store.append(((ApplicationEventData)eventData).getDeploymentId());
        } else {
            store.append("");
            store.append("");
        }
        if (eventData instanceof HostEventData) {
            store.append(((HostEventData)eventData).getHost());
        } else {
            store.append("");
        }
        store.append(eventData.getAgreementTerm());
        store.append(eventData.getGuranteedValue());          
        store.append(eventData.getRawValue());   
        store.append(eventData.getGuranteeOperator()); 
    }
    
}
