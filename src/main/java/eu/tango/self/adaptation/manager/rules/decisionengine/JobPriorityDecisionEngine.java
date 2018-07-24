/**
 * Copyright 2018 University of Leeds
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
package eu.tango.self.adaptation.manager.rules.decisionengine;

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.comparators.HostIdlePower;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_NO_ACTUATION_TASK;
import eu.tango.self.adaptation.manager.rules.decisionengine.comparators.JobPriority;
import java.util.Collections;
import java.util.List;

/**
 * This ranks jobs by their priority value and then by the last task created.
 * @author Richard Kavanagh
 */
public class JobPriorityDecisionEngine extends AbstractDecisionEngine {

    /**
     * Selects a task on the host to perform the actuation against. This selects the
     * task that was last launched to act against.
     *
     * @param response The original response object to modify
     * @param hostname The hostname to apply the adaptation to
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    @Override    
    protected Response selectTaskOnHost(Response response, String hostname) {
        List<ApplicationOnHost> tasks = getActuator().getTasksOnHost(hostname);
        return selectTaskFromList(response, tasks);
    }

    /**
     * Selects a task on any host to perform the actuation against. This selects the
     * task based upon its priority then age to act against.
     *
     * @param response The original response object to modify
     * @param application The name of the application to apply the adaptation to
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    @Override
    protected Response selectTaskOnAnyHost(Response response, String application) {
        List<ApplicationOnHost> tasks = ApplicationOnHost.filter(getActuator().getTasks(), application, -1);
        return selectTaskFromList(response, tasks);
    }
    
    /**
     * Selects a task to perform the actuation against. This selects the
     * task based upon priority then age to act against.
     * @param response The original response object to modify
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    private Response selectTaskFromList(Response response, List<ApplicationOnHost> tasks) {      
        //Error checking
        if (tasks.isEmpty()) {
            response.setAdaptationDetails(ADAPTATION_DETAIL_NO_ACTUATION_TASK);
            response.setPossibleToAdapt(false);
            return response;
        }
        //General case
        Collections.sort(tasks, new JobPriority());
        Collections.reverse(tasks);
        response.setTaskId(tasks.get(0).getId() + "");
        return response;        
    }
    
    @Override
    protected Response selectHostToAdapt(Response response) {
        //Could also rank by Max Power or flops per Watt
        return selectHostToAdapt(response, new HostIdlePower());
    }  

    /**
     * The decision logic for deleting a task. It removes the last task to be
     * created (i.e. highest task ID first).
     *
     * @param response The response to finalise details for.
     * @return The finalised response object
     */
    @Override
    public Response deleteTask(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails(ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND);
            response.setPossibleToAdapt(false);
            return response;
        }
        List<Integer> taskIds = getTaskIdsAvailableToRemove(response.getApplicationId(), response.getDeploymentId());
        if (taskIds == null) {
            System.out.println("Internal Error list of deleteable task Ids equals null.");
            response.setAdaptationDetails("Could not find a task to delete.");
            response.setPossibleToAdapt(false);
            return response;
        }
        if (!taskIds.isEmpty()) {
            Collections.sort(taskIds);
            Collections.reverse(taskIds);
            response.setTaskId(taskIds.get(0) + "");
            return response;
        } else {
            response.setAdaptationDetails("Could not find a task to delete.");
            response.setPossibleToAdapt(false);
        }
        return response;
    }

    /**
     * The decision logic for adding a task.
     *
     * @param response The response to finalise details for.
     * @return The finalised response object
     */
    @Override
    public Response addTask(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails(ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND);
            response.setPossibleToAdapt(false);
            return response;
        }
        List<String> taskTypes = getTaskTypesAvailableToAdd(response.getApplicationId(), response.getDeploymentId());
        if (!taskTypes.isEmpty()) {
            Collections.shuffle(taskTypes);
            response.setAdaptationDetails(taskTypes.get(0));
            if (getCanTaskBeAdded(response, taskTypes.get(0))) {
                return response;
            } else {
                response.setAdaptationDetails("Adding a task would breach SLA criteria");
                response.setPossibleToAdapt(false);
                return response;
            }
        } else {
            response.setAdaptationDetails("Could not find a task type to add");
            response.setPossibleToAdapt(false);
            return response;
        }
    }

    /**
     * This generates the list of tasks to remove. They are removed in the order
     * in which they are created.
     *
     * @param tasksPossibleToRemove The list of tasks that could be removed
     * @param count The amount of tasks needing to go
     * @return The string for the command to remove the tasks
     */
    @Override
    protected String getTasksToRemove(List<Integer> tasksPossibleToRemove, int count) {
        String answer = "";
        Collections.sort(tasksPossibleToRemove);
        Collections.reverse(tasksPossibleToRemove);
        //Remove the last task to be created from the list of possible tasks
        for (int i = 0; i < count; i++) {
            Integer taskid = tasksPossibleToRemove.get(i);
            answer = answer + (i == 0 ? "" : ",") + taskid;
        }
        return answer;
    }
    
}
