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
package eu.tango.self.adaptation.manager.qos;

import eu.tango.self.adaptation.manager.listeners.EnvironmentMonitor;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.model.SLATerm;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * This loads the SLA Rules from disk and applies them to a given application
 * @author Richard Kavanagh
 */
public class SlaRulesLoader {

    private String workingDir;
    private static final String CONFIG_FILE = "self-adaptation-manager.properties";
    private static final String RULES_FILE_START = "QoSEventCriteria";
    private static final String RULES_FILE_END = ".csv";
    private static final String RULES_FILE = RULES_FILE_START + RULES_FILE_END;    
    private SLALimits limits;
    private HashMap<String, SLALimits> appSpecificLimits = new HashMap<>();

    /**
     * SingletonHolder is loaded on the first execution of
     * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
     * not before.
     */
    private static class SingletonHolder {

        private static final SlaRulesLoader INSTANCE = new SlaRulesLoader();
    }

    /**
     * This creates a new singleton instance of the sla rules loader.
     *
     * @return A singleton instance of a sla rules loader.
     */
    public static SlaRulesLoader getInstance() {
        return SingletonHolder.INSTANCE;
    }    
    
    private SlaRulesLoader() {
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            workingDir = config.getString("self.adaptation.manager.working.directory", ".");
            if (!workingDir.endsWith("/")) {
                workingDir = workingDir + "/";
            }
            config.save();
        } catch (ConfigurationException ex) {
            Logger.getLogger(EnvironmentMonitor.class.getName()).log(Level.INFO, "Error loading the configuration of the Self adaptation manager", ex);
        }
        limits = SLALimits.loadFromDisk(workingDir + RULES_FILE);
    }

    /**
     * This returns the SLA limits for all terms
     * @return 
     */
    public SLALimits getLimits() {
        return limits;
    }
    
    /**
     * This adds application specific limits to the QoS goals
     * @param applicationId The application to add the limits to
     * @param limits The extra application specific limits
     */
    public void addApplicationSpecificLimits(String applicationId, SLALimits limits) {
        appSpecificLimits.put(applicationId, limits);
    }
    
    /**
     * This adds application specific limits to the QoS goals
     * @param applicationId The application to add the limits to
     * @param limits The extra application specific limits
     */
    public void removeApplicationSpecificLimits(String applicationId, SLALimits limits) {
        appSpecificLimits.remove(applicationId);
    }
        
    
    /**
     * This returns the SLA limits for all terms
     * @param reload provides the opportunity to reload the QoS terms. 
     * @return 
     */
    public SLALimits getLimits(boolean reload) {
        if (reload) {
        limits = SLALimits.loadFromDisk(workingDir + RULES_FILE);    
        }
        return limits;
    }    
    
    /**
     * This returns the SLA limits for all terms
     * @return 
     */
    public ArrayList<SLATerm> getSlaTerms() {
        return limits.getQosCriteria();
    }
   
    /**
     * This returns the SLA limits for all terms
     * @param applicationID The application id to get the rules for
     * @param deploymentID The deployment id to get the rules for
     * @return The complete set of QoS rules that should be applied to a given 
     * application
     */
    public SLALimits getSlaLimits(String applicationID, String deploymentID) {
        /**
         * This loads rules in from disk and allows for the application definition
         * to have additional rules as well.
         */
        //Creating a new SLA Limits, prevents unintended changes when merging the two rulesets.
        SLALimits answer = new SLALimits();
        //Load these application specific rules in from disk
        String appRulesFile = workingDir + RULES_FILE_START + applicationID + RULES_FILE_END;
        if (new File(appRulesFile).exists() && !appSpecificLimits.containsKey(applicationID)) {
            appSpecificLimits.put(applicationID, SLALimits.loadFromDisk(appRulesFile));
        }
        SLALimits appAnswer = appSpecificLimits.get(applicationID);
        if (appSpecificLimits.containsKey(applicationID)) {
            answer.addQoSCriteria(appAnswer.getQosCriteria());
        }
        answer.addQoSCriteria(getLimits().getQosCriteria());
        return getLimits();
    }    
    
    
}
