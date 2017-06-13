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

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
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
     * @param name The application name / identifier
     * @param deploymentId The deployment instance identifier
     * @return The application description that describes a given deployment. 
     * If the application description can't be reported by this actuator 
     * then null is returned instead.
     */
    public abstract ApplicationDefinition getApplication(String name, String deploymentId);
    
    /**
     * This gets a task of a given application, deployment and task id.
     *
     * @param name The application name or identifier
     * @param deployment The deployment instance identifier
     * @param taskId The task id
     * @return The task given the id values specified.
     */
    public ApplicationOnHost getTask(String name, String deployment, int taskId);
    
    /**
     * This gets the power usage of a application.
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     * @return The power usage of the named application. 
     */
    public double getTotalPowerUsage(String applicationName, String deploymentId);      
    
    /**
     * This gets the power usage of a task.
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     * @param taskId The task id
     * @return The power usage of a named task. 
     */
    public double getPowerUsageTask(String applicationName, String deploymentId, int taskId);    
    
    /**
     * This gets the power usage of a task.
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     * @param taskType The id of the task to get the measurement for
     * @return The power usage of a named task. 
     */
    public double getAveragePowerUsage(String applicationName, String deploymentId, String taskType);     

    /**
     * This lists which tasks that can be added to a deployment in order to make it
     * scale.
     *
     * @param applicationName The name of the application
     * @param deploymentId The deployment ID
     * @return The ids that can be used to scale the named deployment
     */
    public abstract List<String> getTaskTypesAvailableToAdd(String applicationName, String deploymentId);
    
    /**
     * This lists which tasks that can be added to a deployment in order to make it
     * scale.
     *
     * @param applicationName The name of the application
     * @param deploymentId The deployment ID
     * @return The task ids that can be used to down size the named deployment
     */
    public abstract List<Integer> getTaskIdsAvailableToRemove(String applicationName, String deploymentId);   
    
    /**
     * This stops the application from running
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     */
    public void hardKillApp(String applicationName, String deploymentId);

    /**
     * This causes the actuator to invoke a given action
     *
     * @param response
     */
    public void actuate(Response response);
    
    /**
     * This adds a task of a given task type to named deployment.
     *
     * @param applicationName The name of the application
     * @param deploymentId The deployment ID
     * @param taskType The task type to instantiate
     */
    public void addTask(String applicationName, String deploymentId, String taskType);

    /**
     * This deletes a task from an application
     *
     * @param applicationName The name of the application
     * @param deployment The id of the deployment instance of the task
     * @param taskID The id of the task to delete
     */
    public void deleteTask(String applicationName, String deployment, String taskID);    
 
    /**
     * This scales a task type to a set amount of tasks
     *
     * @param applicationId The application the task is part of
     * @param deploymentId The id of the deployment instance of the task
     * @param response The response to actuator for
     */
    public void scaleToNTasks(String applicationId, String deploymentId, Response response);    
    
}
