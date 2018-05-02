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

    private String settings = ""; 
    
    public ClockEventData() {
        setSignificantOnOwn(true);
    }  
    
    /**
     * This creates a new clock event data object.
     *
     * @param time The time of the event (Unix time).
     * @param rawValue The raw value reported by the SLA manager, for the metric
     * that breached its guarantee.
     * @param guaranteedValue The value for the threshold that forms the
     * guarantee placed upon the value that breached.
     * @param type This indicates if the event notifies of a breach or a warning
     * of a potential future breach.
     * @param guaranteeOperator The operator that defines the threshold placed
     * upon the guarantee. e.g. greater_than, less_than ...
     * @param guaranteeid The id of the guarantee that was breached, may be a 
     * reference to a particular term in the SLA.
     * @param agreementTerm The term of the agreement that was breached, such 
     * as a metric's name.
     */  
    public ClockEventData(long time, double rawValue, double guaranteedValue, Type type, Operator guaranteeOperator, String guaranteeid, String agreementTerm) {
        super(time, rawValue, guaranteedValue, type, guaranteeOperator, guaranteeid, agreementTerm);
        setSignificantOnOwn(true);
    }
 
    /**
     * This allows this clock event data to be converted into a application based event data,
     * in cases where the information is contained within the event already.
     * @return The application based event data equivalent of this event 
     */
    public ApplicationEventData castToApplicationEventData() {  
        if (!hasSetting(ClockEventData.SETTING_APPLICATION)) {
            return null;
        }
        String deployment = "*";
        if (hasSetting(ClockEventData.SETTING_DEPLOYMENT_ID)) {
            deployment = getSettingsDetail(ClockEventData.SETTING_DEPLOYMENT_ID);
        }
        return castToApplicationEventData(getSettingsDetail(ClockEventData.SETTING_APPLICATION), deployment);        
             
    }  
    
    /**
     * This allows this clock event data to be converted into a application event data
     * @param applicationId The application id to give to this event
     * @param deploymentId The deployment id to give to this event
     * @return The application based event data equivalent of this event 
     */
    public ApplicationEventData castToApplicationEventData(String applicationId, String deploymentId) {
        ApplicationEventData answer = new ApplicationEventData(this.getTime(), this.getRawValue(), 
                this.getGuaranteedValue(), 
                this.getType(), 
                this.getGuaranteeOperator(), 
                applicationId, 
                deploymentId, 
                this.getGuaranteeid(), 
                this.getAgreementTerm());
        if (hasSetting(ClockEventData.SETTING_APPLICATION)) {
            answer.setApplicationId(applicationId);
        }
        if (hasSetting(ClockEventData.SETTING_DEPLOYMENT_ID)) {
            answer.setDeploymentId(getSettingsDetail(ClockEventData.SETTING_DEPLOYMENT_ID));
        }  
        return answer;
    }
   
    /**
     * This allows this clock event data to be converted into a host event data,
     * in cases where the information is contained within the event already.
     * @return The host based event data equivalent of this event 
     */
    public HostEventData castToHostEventData() {
        if (!hasSetting(ClockEventData.SETTING_HOST)) {
            return null;
        }      
        return castToHostEventData(getSettingsDetail(ClockEventData.SETTING_HOST));
    }    
    
    /**
     * This allows this clock event data to be converted into a host event data
     * @param hostname The name of the host to give to this event
     * @return The host based event data equivalent of this event 
     */
    public HostEventData castToHostEventData(String hostname) {
        HostEventData answer = new HostEventData(this.getTime(), 
                hostname,
                this.getRawValue(), 
                this.getGuaranteedValue(), 
                this.getType(), 
                this.getGuaranteeOperator(),
                this.getGuaranteeid(), 
                this.getAgreementTerm());
        if (hasSetting(ClockEventData.SETTING_HOST)) {
            answer.setHost(getSettingsDetail(ClockEventData.SETTING_HOST));
        }        
        return answer;
    }
    
    /**
     * This returns additional information about the event and means of retaining
     * settings information.
     *
     * @return the settings
     */
    public String getSettings() {
        return settings;
    }

    /**
     * Given the key value of the setting this returns its value.
     *
     * @param key The key name for the settings parameter
     * @return The value of the settings detail else null.
     */
    public String getSettingsDetail(String key) {
        String[] args = settings.split(";");
        for (String arg : args) {
            if (arg.split("=")[0].trim().equalsIgnoreCase(key)) {
                return arg.split("=")[1].trim();
            }
        }
        return null;
    }

    /**
     * This sets additional information about the event, that might be
     * needed.
     *
     * @param settings the settings to set
     */
    public void setSettings(String settings) {
        this.settings = settings;
    }
    
    /**
     * Indicates if this clock event data has a particular setting
     * @param setting The setting to check to see if it is there or not
     * @return If there is a particular setting attached to this clock event data or not
     */
    public boolean hasSetting(String setting) {
        return getSettingsDetail(setting) != null && !getSettingsDetail(setting).isEmpty();
    }
    
    
    
}
