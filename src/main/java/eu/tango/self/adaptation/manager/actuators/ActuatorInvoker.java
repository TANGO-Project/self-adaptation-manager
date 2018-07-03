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
    public ApplicationDefinition getApplication(String name, String deploymentId);
    
    /**
     * This gets the list of tasks on a given host
     * @param host The host to get the list of tasks for
     * @return The list of tasks on a given host
     */
    public List<ApplicationOnHost> getTasksOnHost(String host);
    
    /**
     * This gets the list of tasks on all hosts
     * @return The list of tasks on a given host
     */    
    public List<ApplicationOnHost> getTasks();   

    /**
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     * @return The list of tasks on a given host
     */    
    public List<ApplicationOnHost> getTasks(String applicationName, String deploymentId);     
    
    /**
     * This stops the application from running
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     */
    public void hardKillApp(String applicationName, String deploymentId);

    /**
     * This causes the actuator to invoke a given action. Usually the action is
     * placed in a queue and executed soon after.
     *
     * @param response The response to an event that needs to be executed.
     */
    public void actuate(Response response);
    
    /**
     * This adds a task/resource of a given type to named deployment.
     *
     * @param applicationName The name of the application
     * @param deploymentId The deployment ID
     * @param taskParams additional task parameters such as task type
     */
    public void addResource(String applicationName, String deploymentId, String taskParams);

    /**
     * This deletes a task/resource from an application
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the task
     * @param resourceId The id of the task/resource to delete
     */
    public void deleteResource(String applicationName, String deploymentId, String resourceId);    
 
    /**
     * This scales a task type to a set amount of tasks
     *
     * @param applicationId The application the task is part of
     * @param deploymentId The id of the deployment instance of the task
     * @param response The response to actuator for
     */
    public void scaleToNTasks(String applicationId, String deploymentId, Response response);    
    
}
