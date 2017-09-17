/**
 * Copyright 2015 University of Leeds
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
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.util.Collections;
import java.util.List;

/**
 * The aim of this class is to decide given an event that has been assessed what
 * the magnitude of an adaptation should be used will be. It may also have to
 * decide to which task this adaptation should occur.
 *
 * The random decision engine will pick tasks and task types to adapt randomly
 * without any further guidance from outside data sources.
 *
 * @author Richard Kavanagh
 */
public class RandomDecisionEngine extends AbstractDecisionEngine {

    @Override
    public Response decide(Response response) {
        switch (response.getActionType()) {
            case ADD_TASK:
                response = addTask(response);
                break;
            case REMOVE_TASK:
                response = deleteTask(response);
                break;
            case KILL_SIMILAR_APPS:
                killSimilarApps(response);
                break;                
            case KILL_APP: 
            case HARD_KILL_APP:
            case INCREASE_WALL_TIME:
            case REDUCE_WALL_TIME:
            case PAUSE_APP:
            case UNPAUSE_APP:
            case ADD_CPU:
            case REMOVE_CPU:
            case ADD_MEMORY:
            case REMOVE_MEMORY:
                response = randomlySelectApp(response);
                break;
            case SCALE_TO_N_TASKS:
                response = scaleToNTasks(response);
                break;
        }
        return response;
    }
    
    /**
     * The decision logic for adding a task.
     *
     * @param response The response to finalise details for.
     * @return The finalised response object
     */
    public Response randomlySelectApp(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails("Unable to find actuator.");
            response.setPossibleToAdapt(false);
            return response;
        }
        if (response.getCause() instanceof HostEventData) {
            HostEventData eventData = (HostEventData) response.getCause();
            List<ApplicationOnHost> tasks = getActuator().getTasksOnHost(eventData.getHost());
            if (!tasks.isEmpty()) {
                Collections.shuffle(tasks);
                    response.setTaskId(tasks.get(0).getId() + "");
                return response;
            } else {
                response.setAdaptationDetails("Could not find a task to actuate against");
                response.setPossibleToAdapt(false);
            }
        }
        if (!(response.getCause() instanceof ApplicationEventData)) {
            ApplicationEventData cause = (ApplicationEventData) response.getCause();
            if (response.getTaskId() == null || response.getTaskId().isEmpty() || response.getTaskId().equals("*")) {
                if (cause.getDeploymentId() != null && !response.getTaskId().isEmpty()) {
                    response.setTaskId(cause.getDeploymentId());
                } else {
                    response.setAdaptationDetails("Could not find a task to actuate against");
                    response.setPossibleToAdapt(false);
                }
                
            }
        }
        //Note: if the event data was from an application the task id would already be set
        return response;
    }

    /**
     * The decision logic for adding a task.
     *
     * @param response The response to finalise details for.
     * @return The finalised response object
     */
    public Response deleteTask(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails("Unable to find actuator.");
            response.setPossibleToAdapt(false);
            return response;
        }
        List<Integer> taskIds = getActuator().getTaskIdsAvailableToRemove(response.getApplicationId(), response.getDeploymentId());
        if (!taskIds.isEmpty()) {
            Collections.shuffle(taskIds);
            response.setTaskId(taskIds.get(0) + "");
            return response;
        } else {
            response.setAdaptationDetails("Could not find a task to delete");
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
    public Response addTask(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails("Unable to find actuator.");
            response.setPossibleToAdapt(false);
            return response;
        }
        List<String> taskTypes = getActuator().getTaskTypesAvailableToAdd(response.getApplicationId(), response.getDeploymentId());
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
     * This generates the list of tasks to remove
     *
     * @param tasksPossibleToRemove The list of tasks that could be removed
     * @param count The amount of tasks needing to go
     * @return The string for the command to remove the tasks
     */
    @Override
    protected String getTasksToRemove(List<Integer> tasksPossibleToRemove, int count) {
        String answer = "";
        Collections.shuffle(tasksPossibleToRemove);
        for (int i = 0; i < count; i++) {
            Integer taskid = tasksPossibleToRemove.get(0);
            answer = answer + (i == 0 ? "" : ",") + taskid;
            tasksPossibleToRemove.remove(i);
        }
        return answer;
    }

}
