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
 */
package eu.tango.self.adaptation.manager.model;

import eu.tango.self.adaptation.manager.rules.datatypes.FiringCriteria;
import java.util.ArrayList;

/**
 * This class covers the application definition
 * @author Richard Kavanagh
 */
public class ApplicationDefinition {
    
    private String applicationId;
    private String deploymentId;    
    private ArrayList<FiringCriteria> qosCriteria = new ArrayList<>();

    public ApplicationDefinition(String applicationId, String deploymentId) {
        this.applicationId = applicationId;
        this.deploymentId = deploymentId;
    }
    
    /**
     * @return the qosCriteria
     */
    public ArrayList<FiringCriteria> getQosCriteria() {
        return qosCriteria;
    }

    /**
     * @param qosCriteria the qosCriteria to set
     */
    public void setQosCriteria(ArrayList<FiringCriteria> qosCriteria) {
        this.qosCriteria = qosCriteria;
    }

    /**
     * @return the applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * @param applicationId the applicationId to set
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * @return the deploymentId
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * @param deploymentId the deploymentId to set
     */
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }
    
}
