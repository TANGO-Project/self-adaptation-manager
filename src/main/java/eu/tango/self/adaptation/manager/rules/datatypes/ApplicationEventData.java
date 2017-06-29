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

import eu.tango.self.adaptation.manager.model.ApplicationDefinition;

/**
 * This class represents an event that arrives at self-adaptation manager for
 * assessment, based upon an event from an application.
 * @author Richard Kavanagh
 */
public class ApplicationEventData extends EventData {

    //augments information about the event, with the original deployment information
    private String applicationId;
    private String deploymentId;
    private ApplicationDefinition application;

    /**
     * This creates a new event data object.
     *
     * @param time The time of the event (Unix time).
     * @param rawValue The raw value reported by the SLA manager, for the metric
     * that breached its guarantee.
     * @param guranteedValue The value for the threshold that forms the
     * guarantee placed upon the value that breached.
     * @param type This indicates if the event notifies of a breach or a warning
     * of a potential future breach.
     * @param guranteeOperator The operator that defines the threshold placed
     * upon the guarantee. e.g. greater_than, less_than ...
     * @param applicationId The id of the application that caused the breach
     * @param deploymentId The id of the specific deployment that caused the
     * breach
     * @param guaranteeid The id of the guarantee that was breached
     * @param agreementTerm The type of guarantee that was breached.
     */    
    public ApplicationEventData(long time, double rawValue, double guranteedValue, Type type, Operator guranteeOperator, String applicationId, String deploymentId, String guaranteeid, String agreementTerm) {
        super(time, rawValue, guranteedValue, type, guranteeOperator, guaranteeid, agreementTerm);
        this.applicationId = applicationId;
        this.deploymentId = deploymentId;
    }
    
    /**
     * This gets the application id associated with the origin of the event.
     *
     * @return the applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * This sets the application id associated with the origin of the event.
     *
     * @param applicationId the applicationId to set
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * This gets the deployment id associated with the origin of the event.
     *
     * @return the deploymentId
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * This sets the deployment id associated with the origin of the event.
     *
     * @param deploymentId the deploymentId to set
     */
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    /**
     * This gets the definition of the original application deployment.
     *
     * @return the definition of the original deployment
     */
    public ApplicationDefinition getApplicationDefinition() {
        return application;
    }

    /**
     * This sets the definition of the original application deployment.
     *
     * @param application the definition of the application deployment to set.
     */
    public void setApplicationDefinition(ApplicationDefinition application) {
        this.application = application;
    }    
    
}
