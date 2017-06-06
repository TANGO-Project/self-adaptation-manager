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
 */
package eu.tango.self.adaptation.manager.rules.decisionengine;

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
        if (response.getActionType().equals(Response.AdaptationType.ADD_TASK)) {
            response = addTask(response);
        } else if (response.getActionType().equals(Response.AdaptationType.REMOVE_TASK)) {
            response = deleteTask(response);
        } else if (response.getActionType().equals(Response.AdaptationType.SCALE_TO_N_TASKS)) {
            response = scaleToNTasks(response);
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
