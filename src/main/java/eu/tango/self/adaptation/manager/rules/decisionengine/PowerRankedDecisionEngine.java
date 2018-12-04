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
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.comparators.HostIdlePower;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_NO_ACTUATION_TASK;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This ranks the tasks to create destroy etc based upon power consumption.
 *
 * @author Richard Kavanagh
 */
public class PowerRankedDecisionEngine extends AbstractDecisionEngine {

    /**
     * The decision logic for selecting an application to adapt. In this case
     * it gets the highest power application to adapt in the case of hardware based
     * events and selects the application to adapt that is seen as the cause
     * of the event in other cases.
     *
     * @param response The response object to adapt
     * @return The response object with a fully formed decision made on how to
     * adapt.
     */
    @Override
    public Response selectApplicationToAdapt(Response response) {
        /**
         * The call to the super method utilises abstract methods that are 
         * implemented here. These change things such as sort orders of applications
         * providing a different outcome of the overall call to similar classes 
         * derived from the AbstractDecisionEngine.
         */        
        return super.selectApplicationToAdapt(response);
    }

    /**
     * Selects a task on the host to perform the actuation against. In this case
     * it selects the most power hungry task.
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
     * Selects a task on any host to perform the actuation against.  In this case
     * it selects the most power hungry task.
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
     * Selects a task from a list to perform the actuation against. In this case
     * it selects the most power hungry task.
     *
     * @param response The original response object to modify
     * @param application The name of the application to apply the adaptation to
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    private Response selectTaskFromList(Response response, List<ApplicationOnHost> tasks) {      
        if (!tasks.isEmpty()) {
            double power = 0;
            for (ApplicationOnHost task : tasks) {
                double currentPower = getTotalPowerUsage(task.getName(), task.getId() + "");
                //Select the most power consuming application
                if (currentPower > power || response.getTaskId().isEmpty()) {
                    response.setTaskId(task.getId() + "");
                    power = currentPower;
                }
            }
            return response;
        } else {
            response.setAdaptationDetails(ADAPTATION_DETAIL_NO_ACTUATION_TASK);
            response.setPossibleToAdapt(false);
        }
        return response;       
    }
    
    @Override
    protected Response selectHostToAdapt(Response response) {
        //Could also rank by Max Power or flops per Watt
        return selectHostToAdapt(response, getHostRanking());
    }
    
    /**
     * This provides the ranking mechanism by which this decisions engine ranks
     * hosts. i.e. by host idle power.
     * @return The list of hosts to rank against 
     */
    @Override    
    public Comparator<Host> getHostRanking() {
        return new HostIdlePower();
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
            //Remove the highest powered task from the list of possible tasks
            response.setTaskId(getHighestPoweredTask(response, taskIds) + "");
            return response;
        } else {
            response.setAdaptationDetails("Could not find a task to delete.");
            response.setPossibleToAdapt(false);
        }
        return response;
    }

    /**
     * The decision logic for adding a task. It adds a new task of a type that 
     * has the lowest power consumption possible, so long as there is room to 
     * add another one of that type. Unless a parameterised specific task type is
     * to be added. In this case the adaptation detail should have a value for 
     * TASK_TYPE.
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
