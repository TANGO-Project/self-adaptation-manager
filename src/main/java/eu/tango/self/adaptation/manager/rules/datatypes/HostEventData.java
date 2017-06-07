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
 * This stores events based upon physical hosts
 * @author Richard Kavanagh
 */
public class HostEventData extends EventData {
  
    private String host;    
    
    public HostEventData() {
    }
    
    /**
     * This creates a new event data object.
     *
     * @param time The time of the event (Unix time).
     * @param host The host from where the event originated
     * @param rawValue The raw value reported by the SLA manager, for the metric
     * that breached its guarantee.
     * @param guranteedValue The value for the threshold that forms the
     * guarantee placed upon the value that breached.
     * @param type This indicates if the event notifies of a breach or a warning
     * of a potential future breach.
     * @param guranteeOperator The operator that defines the threshold placed
     * upon the guarantee. e.g. greater_than, less_than ...
     * @param guaranteeid The id of the guarantee that was breached
     * @param agreementTerm The type of guarantee that was breached.
     */    
    public HostEventData(long time, String host, double rawValue, double guranteedValue, Type type, Operator guranteeOperator, String guaranteeid, String agreementTerm) {
        super(time, rawValue, guranteedValue, type, guranteeOperator, guaranteeid, agreementTerm);
        this.host = host;
    }

    /**
     * This sets the name of the host from where the event originated.
     * @param host 
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * This returns the name of the host from where the event originated.
     * @return 
     */
    public String getHost() {
        return host;
    }
    
}
