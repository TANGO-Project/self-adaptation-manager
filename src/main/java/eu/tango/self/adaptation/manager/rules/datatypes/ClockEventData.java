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
package eu.tango.self.adaptation.manager.rules.datatypes;

/**
 * This creates an event based upon an internal clock event
 * @author Richard Kavanagh
 */
public class ClockEventData extends EventData {

    public ClockEventData() {
        setSignificantOnOwn(true);
    }  
    
    public ClockEventData(long time, double rawValue, double guranteedValue, Type type, Operator guranteeOperator, String guaranteeid, String agreementTerm) {
        super(time, rawValue, guranteedValue, type, guranteeOperator, guaranteeid, agreementTerm);
        setSignificantOnOwn(true);
    }
    
}
