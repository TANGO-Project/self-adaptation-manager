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

package eu.tango.self.adaptation.manager.actuators;

import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.util.List;

/**
 * This is the actuator interface for the self adaption manager.
 *
 * @author Richard Kavanagh
 */
public interface ActuatorInvoker {

    /**
     * This gets the ovf of a given deployment.
     *
     * @param applicationId The application ID
     * @param deploymentId The deployment ID
     * @return The application description that describes a given deployment. 
     * If the application description can't be reported by this actuator 
     * then null is returned instead.
     */
    public abstract ApplicationDefinition getApplication(String applicationId, String deploymentId);
    
    /**
     * This gets a VM given its application, deployment and VM ids.
     *
     * @param application The application ID
     * @param deployment The deployment ID
     * @param vmID The VM id
     * @return The VM given the id values specified.
     */
    public ApplicationDefinition getApplication(String application, String deployment, String vmID);
    
    /**
     * This gets the power usage of a VM.
     *
     * @param applicationId The application the VM is part of
     * @param deploymentId The id of the deployment instance of the VM
     * @return The power usage of the named application. 
     */
    public double getTotalPowerUsage(String applicationId, String deploymentId);      
    
    /**
     *  This obtains information regarding the SLA limits of an application
     * that is to be actuated against
     * @param applicationId The application id
     * @param deploymentId The deployment id
     * @return 
     */
    public SLALimits getSlaLimits(String applicationId, String deploymentId);
    
    /**
     * This gets the power usage of a task.
     *
     * @param applicationId The application the VM is part of
     * @param deploymentId The id of the deployment instance of the VM
     * @param taskId The id of the task to get the measurement for
     * @return The power usage of a named task. 
     */
    public double getPowerUsageTask(String applicationId, String deploymentId, String taskId);    
    
    /**
     * This gets the power usage of a task.
     *
     * @param applicationId The application the VM is part of
     * @param deploymentId The id of the deployment instance of the VM
     * @param taskType The id of the task to get the measurement for
     * @return The power usage of a named task. 
     */
    public double getAveragePowerUsage(String applicationId, String deploymentId, String taskType);     

    /**
     * This lists which tasks that can be added to a deployment in order to make it
     * scale.
     *
     * @param applicationId The application ID
     * @param deploymentId The deployment ID
     * @return The ids that can be used to scale the named deployment
     */
    public abstract List<String> getTaskTypesAvailableToAdd(String applicationId, String deploymentId);
    
    /**
     * This lists which tasks that can be added to a deployment in order to make it
     * scale.
     *
     * @param applicationId The application ID
     * @param deploymentId The deployment ID
     * @return The task ids that can be used to down size the named deployment
     */
    public abstract List<Integer> getTaskIdsAvailableToRemove(String applicationId, String deploymentId);   
    
    /**
     * This stops the application from running
     *
     * @param applicationId The application the VM is part of
     * @param deploymentId The id of the deployment instance of the VM
     */
    public void hardShutdown(String applicationId, String deploymentId);

    /**
     * This causes the actuator to invoke a given action
     *
     * @param response
     */
    public void actuate(Response response);
    
    /**
     * This adds a task of a given task type to named deployment.
     *
     * @param applicationId The application ID
     * @param deploymentId The deployment ID
     * @param taskType The task type to instantiate
     */
    public void addTask(String applicationId, String deploymentId, String taskType);

    /**
     * This deletes a task from an application
     *
     * @param application The application the task is part of
     * @param deployment The id of the deployment instance of the task
     * @param taskID The id of the task to delete
     */
    public void deleteTask(String application, String deployment, String taskID);    
    
}
