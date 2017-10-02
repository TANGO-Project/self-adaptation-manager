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
import eu.tango.self.adaptation.manager.rules.datatypes.ClockEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The aim of this class is to decide given an event that has been assessed what
 * the magnitude of an adaptation should be used will be. It may also have to
 * decide where this adaptation should occur.
 *
 * The last task created decision engine will pick tasks and task types to adapt
 * randomly and when deleting tasks it will pick the task which was created
 * last. It uses no outside data source to guide this decision.
 *
 * @author Richard Kavanagh
 */
public class LastTaskCreatedDecisionEngine extends AbstractDecisionEngine {

    @Override
    public Response decide(Response response) {
        switch (response.getActionType()) {
            case ADD_TASK:
                response = addTask(response);
                break;
            case REMOVE_TASK:
                response = deleteTask(response);
                break;
            case SCALE_TO_N_TASKS:
                response = scaleToNTasks(response);
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
            case OVERSUBSCRIBE_APP:
            case EXCLUSIVE_APP:
            case ADD_CPU:
            case REMOVE_CPU:
            case ADD_MEMORY:
            case REMOVE_MEMORY:
                response = getLastApp(response);
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
    public Response getLastApp(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails("Unable to find actuator.");
            response.setPossibleToAdapt(false);
            return response;
        }
        if (response.getCause() instanceof ClockEventData) {
            if (response.getAdaptationDetail("host") != null && !response.getAdaptationDetail("host").isEmpty()) {
                ClockEventData cause = (ClockEventData) response.getCause();
                response.setCause(cause.castToHostEventData(response.getAdaptationDetail("host")));
            }
            if (response.getAdaptationDetail("application") != null && !response.getAdaptationDetail("application").isEmpty()) {
                ClockEventData cause = (ClockEventData) response.getCause();
                response = selectLastTask(response, response.getAdaptationDetail("application"));
                response.setAdaptationDetails(response.getAdaptationDetails() + ";origin=clock");
                response.setCause(cause.castToApplicationEventData(response.getAdaptationDetail("application"), "*"));
            }            
        }
        if (response.getCause() instanceof HostEventData) {
            HostEventData eventData = (HostEventData) response.getCause();
            response = selectTaskOnHost(response, eventData.getHost());
        }
        if (response.getCause() instanceof ApplicationEventData) {
            ApplicationEventData cause = (ApplicationEventData) response.getCause();
            if (response.getTaskId() == null || response.getTaskId().isEmpty() || response.getTaskId().equals("*")) {
                if (cause.getDeploymentId() != null) {
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
     * Selects a task on the host to perform the actuation against.
     *
     * @param response The original response object to modify
     * @param hostname The hostname to apply the adaptation to
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    private Response selectTaskOnHost(Response response, String hostname) {
        ArrayList<Integer> ids = new ArrayList<>();
        List<ApplicationOnHost> tasks = getActuator().getTasksOnHost(hostname);
        for (ApplicationOnHost task : tasks) {
            ids.add(task.getId());
        }
        if (!ids.isEmpty()) {
            Collections.sort(ids);
            Collections.reverse(ids);
            response.setTaskId(ids.get(0) + "");
            return response;
        } else {
            response.setAdaptationDetails("Could not find a task to actuate against");
            response.setPossibleToAdapt(false);
        }
        return response;
    }
    
    /**
     * Selects a task on the to perform the actuation against.
     *
     * @param response The original response object to modify
     * @param applicationName The application id/name
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    private Response selectLastTask(Response response, String applicationName) {
        ArrayList<Integer> ids = new ArrayList<>();
        List<ApplicationOnHost> tasks = getActuator().getTasks();
        tasks = ApplicationOnHost.filter(tasks, applicationName, -1);
        for (ApplicationOnHost task : tasks) {
            ids.add(task.getId());
        }
        if (!ids.isEmpty()) {
            Collections.sort(ids);
            Collections.reverse(ids);
            response.setTaskId(ids.get(0) + "");
            return response;
        } else {
            response.setAdaptationDetails("Could not find a task to actuate against");
            response.setPossibleToAdapt(false);
        }
        return response;
    }    

    /**
     * The decision logic for deleting a task. It removes the last task to be
     * created (i.e. highest task ID first).
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
        if (taskIds == null) {
            System.out.println("Internal Error list of deleteable tasks Ids equals null.");
            response.setPossibleToAdapt(false);
            return response;
        }
        if (!taskIds.isEmpty()) {
            Collections.sort(taskIds);
            Collections.reverse(taskIds);
            //Remove the last task to be created from the list of possible Tasks
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
