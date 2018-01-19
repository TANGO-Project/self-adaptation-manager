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
import java.util.Collections;
import java.util.List;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_NO_ACTUATION_TASK;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_APPLICATION;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_HOST;

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
            case PAUSE_SIMILAR_APPS:
            case UNPAUSE_SIMILAR_APPS:
            case INCREASE_WALL_TIME_SIMILAR_APPS:
            case REDUCE_WALL_TIME_SIMILAR_APPS:
            case MINIMIZE_WALL_TIME_SIMILAR_APPS:
                handleClockEvent(response);                
                actOnAllSimilarApps(response);
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
            case RESELECT_ACCELERATORS:
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
            response.setAdaptationDetails(ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND);
            response.setPossibleToAdapt(false);
            return response;
        }
        response = handleClockEvent(response);
        if (response.getCause() instanceof HostEventData) {
            HostEventData eventData = (HostEventData) response.getCause();
            //In this case the host is empty
            if (eventData.getAgreementTerm().contains("IDLE") && 
                    response.hasAdaptationDetail("application")) {
                response = selectTaskOnAnyHost(response, response.getAdaptationDetail("application"));
            } else {
                response = selectTaskOnHost(response, eventData.getHost());
            }
        }
        if (response.getCause() instanceof ApplicationEventData) {
            ApplicationEventData cause = (ApplicationEventData) response.getCause();
            if (response.getTaskId() == null || response.getTaskId().isEmpty() || response.getTaskId().equals("*")) {
                if (cause.getDeploymentId() != null) {
                    response.setTaskId(cause.getDeploymentId());
                } else {
                    response.setAdaptationDetails(ADAPTATION_DETAIL_NO_ACTUATION_TASK);
                    response.setPossibleToAdapt(false);
                }

            }
        }
        //Note: if the event data was from an application the task id would already be set
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
            if (cause.hasSetting(ADAPTATION_DETAIL_APPLICATION)) {             
                response.setCause(cause.castToApplicationEventData());
                return response;
            }
            if (cause.hasSetting(ClockEventData.SETTING_HOST)) {
                response.setCause(cause.castToHostEventData());
                return response;
            }       
            //The next two if statements deal with cases where the decision rules have information attached.
            if (response.hasAdaptationDetail(ADAPTATION_DETAIL_HOST)) {
                response.setCause(cause.castToHostEventData(response.getAdaptationDetail(ADAPTATION_DETAIL_HOST)));
                return response;
            } 
            if (response.hasAdaptationDetail(ADAPTATION_DETAIL_APPLICATION)) {
                response = selectRandomTask(response, response.getAdaptationDetail(ADAPTATION_DETAIL_APPLICATION));
                response.setAdaptationDetails(response.getAdaptationDetails() + ";origin=clock");
                response.setCause(cause.castToApplicationEventData(response.getAdaptationDetail(ADAPTATION_DETAIL_APPLICATION), "*"));
                return response;
            }
        }
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
        List<ApplicationOnHost> tasks = getActuator().getTasksOnHost(hostname);
        if (!tasks.isEmpty()) {
            Collections.shuffle(tasks);
            response.setTaskId(tasks.get(0).getId() + "");
            return response;
        } else {
            response.setAdaptationDetails(ADAPTATION_DETAIL_NO_ACTUATION_TASK);
            response.setPossibleToAdapt(false);
        }
        return response;
    }
    
    /**
     * Selects a task on the host to perform the actuation against.
     *
     * @param response The original response object to modify
     * @param application The name of application to apply the adaptation to
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    private Response selectTaskOnAnyHost(Response response, String application) {
        List<ApplicationOnHost> tasks = ApplicationOnHost.filter(getActuator().getTasks(),application, -1);
        if (!tasks.isEmpty()) {
            Collections.shuffle(tasks);
            response.setTaskId(tasks.get(0).getId() + "");
            return response;
        } else {
            response.setAdaptationDetails(ADAPTATION_DETAIL_NO_ACTUATION_TASK);
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
    private Response selectRandomTask(Response response, String applicationName) {
        List<ApplicationOnHost> tasks = getActuator().getTasks();
        tasks = ApplicationOnHost.filter(tasks, applicationName, -1);
        if (!tasks.isEmpty()) {
            Collections.shuffle(tasks);
            response.setTaskId(tasks.get(0).getId() + "");
            return response;
        } else {
            response.setAdaptationDetails(ADAPTATION_DETAIL_NO_ACTUATION_TASK);
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
    public Response deleteTask(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails(ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND);
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
            response.setAdaptationDetails(ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND);
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
