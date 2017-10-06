/**
 * Copyright 2016 University of Leeds
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
import java.util.Collections;
import java.util.List;

/**
 * This ranks the tasks to create destroy etc based upon power consumption.
 *
 * @author Richard Kavanagh
 */
public class PowerRankedDecisionEngine extends AbstractDecisionEngine {

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
            case PAUSE_SIMILAR_APPS:
            case UNPAUSE_SIMILAR_APPS:
                actOnAlllSimilarApps(response);
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
                response = getHighestPowerConsumingApp(response);
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
    public Response getHighestPowerConsumingApp(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails("Unable to find actuator.");
            response.setPossibleToAdapt(false);
            return response;
        }
        response = handleClockEvent(response);
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
        return response;
    }
    
    /**
     * This modifies the response object's cause in the case that it is a clock event
     * into either an application or a host based event.
     * @param response The response object to modify
     * @return The altered response object, no changes are made if the cause is 
     * not a clock event
     */
    private Response handleClockEvent(Response response) {
        if (response.getCause() instanceof ClockEventData) {
            ClockEventData cause = (ClockEventData) response.getCause(); 
            //The next two if statements deal with call backs, where the original event has settings data attached.
            if (cause.hasSetting("application")) {
                response.setCause(cause.castToApplicationEventData());
                return response;
            }
            if (cause.hasSetting("host")) {
                response.setCause(cause.castToHostEventData());
                return response;
            }       
            //The next two if statements deal with cases where the decision rules have information attached.
            if (response.hasAdaptationDetail("host")) {
                response.setCause(cause.castToHostEventData(response.getAdaptationDetail("host")));
                return response;
            } 
            if (response.hasAdaptationDetail("application")) {
                response = selectPowerHungryTask(response, response.getAdaptationDetail("application"));
                response.setAdaptationDetails(response.getAdaptationDetails() + ";origin=clock");
                response.setCause(cause.castToApplicationEventData(response.getAdaptationDetail("application"), "*"));
                return response;
            }
        }
        return response;
    }      

    /**
     * Selects a task on the host to perform the actuation against.
     * @param response The original response object to modify
     * @param hostname The hostname to apply the adaptation to
     * @return The response object with a task ID assigned to action against where possible.
     */
    private Response selectTaskOnHost(Response response, String hostname) {
        List<ApplicationOnHost> tasks = getActuator().getTasksOnHost(hostname);
        if (!tasks.isEmpty()) {
            double power = 0;
            for (ApplicationOnHost task : tasks) {
                double currentPower = getActuator().getTotalPowerUsage(task.getName(), task.getId() + "");
                //Select the most power consuming task
                if (currentPower > power || response.getTaskId().isEmpty()) {
                    response.setTaskId(task.getId() + "");
                    power = currentPower;
                }
            }
            return response;
        } else {
            response.setAdaptationDetails("Could not find a task to act upon");
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
    private Response selectPowerHungryTask(Response response, String applicationName) {
        List<ApplicationOnHost> tasks = getActuator().getTasks();
        tasks = ApplicationOnHost.filter(tasks, applicationName, -1);
        if (!tasks.isEmpty()) {
            double power = 0;
            for (ApplicationOnHost task : tasks) {
                double currentPower = getActuator().getTotalPowerUsage(task.getName(), task.getId() + "");
                //Select the most power consuming task
                if (currentPower > power || response.getTaskId().isEmpty()) {
                    response.setTaskId(task.getId() + "");
                    power = currentPower;
                }
            }
            return response;
        } else {
            response.setAdaptationDetails("Could not find a task to act upon");
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
            System.out.println("Internal Error list of deleteable task Ids equals null.");
            response.setAdaptationDetails("Unable find a task to delete.");
            response.setPossibleToAdapt(false);
            return response;
        }
        if (!taskIds.isEmpty()) {
            //Remove the highest powered task from the list of possible tasks
            response.setTaskId(getHighestPoweredTask(response, taskIds) + "");
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
        if (taskTypes.isEmpty()) {
            response.setAdaptationDetails("Could not find a task type to add");
            response.setPossibleToAdapt(false);
            return response;
        }
        Collections.shuffle(taskTypes);
        //Give preference to any task type specified in the rule.
        String taskTypePreference = response.getAdaptationDetail("TASK_TYPE");
        String taskTypeToAdd;
        //Check that the preferential type can be added
        if (taskTypePreference != null && taskTypes.contains(taskTypePreference)) {
            taskTypeToAdd = taskTypePreference;
        } else { //If no preference is given then pick the best alternative
            taskTypeToAdd = pickLowestAveragePower(response, taskTypes);
        }
        response.setAdaptationDetails(taskTypeToAdd);
        if (getCanTaskBeAdded(response, taskTypeToAdd)) {
            return response;
        } else {
            response.setAdaptationDetails("Adding a task would breach SLA criteria");
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
