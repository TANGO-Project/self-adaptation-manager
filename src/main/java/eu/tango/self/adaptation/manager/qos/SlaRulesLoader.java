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
    private static final String RULES_FILE = "slarules.csv";
    private final SLALimits limits;

    public SlaRulesLoader() {
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
     * This returns the SLA limits for all terms
     * @return 
     */
    public ArrayList<SLATerm> getSlaTerms() {
        return limits.getQosCriteria();
    }
   
    /**
     * This returns the SLA limits for all terms
     * @return 
     */
    public SLALimits getSlaLimits(String applicationID, String deploymentID) {
        return getLimits();
    }    
    
    
}
