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

import eu.tango.energymodeller.EnergyModeller;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import eu.tango.self.adaptation.manager.actuators.ActuatorInvoker;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.qos.SlaRulesLoader;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.ClockEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_APPLICATION;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_HOST;
import static eu.tango.self.adaptation.manager.rules.datatypes.Response.ADAPTATION_DETAIL_NO_ACTUATION_TASK;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The aim of this class is to decide given an event that has been assessed what
 * the magnitude of an adaptation should be used will be. It may also have to
 * decide to which application or task this adaptation should occur.
 *
 * @author Richard Kavanagh
 */
public abstract class AbstractDecisionEngine implements DecisionEngine {

    /**
     * The actuator is to be used as an information source, with a set of
     * standard questions that can be asked about how adaptation may occur. This
     * avoids the decision engine having to have interface specific code i.e.
     * Rest or ActiveMQ, it also prevents having to maintain multiple
     * connections for different purposes.
     */
    private ActuatorInvoker actuator;
    //Singleton instance helps avoid loading rules in multiple times, i.e. once per decision engine.
    private final SlaRulesLoader loader = SlaRulesLoader.getInstance();
    private final EnergyModeller modeller = EnergyModeller.getInstance();

    public AbstractDecisionEngine() {
    }
    
    @Override
    public void setActuator(ActuatorInvoker actuator) {
        this.actuator = actuator;
    }

    @Override
    public ActuatorInvoker getActuator() {
        return actuator;
    }    
    
    @Override
    public Response decide(Response response) {
        handleClockEvent(response);        
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
                response = selectApplicationToAdapt(response);
                break;
            case SCALE_TO_N_TASKS:
                response = scaleToNTasks(response);
                break;
            case SHUTDOWN_HOST:
            case STARTUP_HOST:
            case SHUTDOWN_N_HOSTS:
            case STARTUP_N_HOSTS:
                handleUnspecifiedHost(response);
                break;
                
        }
        return response;
    }
    
    /**
     * The decision logic for selecting an application to adapt. In this case
     * it gets the "highest ranked" application to adapt in the case of hardware 
     * based events and selects the application to adapt that is seen as the cause
     * of the event in application based cases.
     *
     * @param response The response object to adapt
     * @return The response object with a fully formed decision made on how to
     * adapt.
     */
    public Response selectApplicationToAdapt(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails(ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND);
            response.setPossibleToAdapt(false);
            return response;
        }
        response = handleClockEvent(response);
        if (response.getCause() instanceof HostEventData) {
            HostEventData eventData = (HostEventData) response.getCause();
            //In this case the host is empty
            if (eventData.getAgreementTerm().contains("IDLE")
                    && response.hasAdaptationDetail("application")) {
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
     * Selects a task on the host to perform the actuation against. This method 
     * should decide on which application the adaptation should occur.
     *
     * @param response The original response object to modify
     * @param hostname The hostname to apply the adaptation to
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    protected abstract Response selectTaskOnHost(Response response, String hostname);

    /**
     * Selects a task on any host to perform the actuation against. The aim is
     * to call this method when faced with a host event. This method 
     * should decide on which application the adaptation should occur.
     *
     * @param response The original response object to modify
     * @param application The name of the application to apply the adaptation to
     * @return The response object with a task ID assigned to action against
     * where possible.
     */
    protected abstract Response selectTaskOnAnyHost(Response response, String application);    

    /**
     * Selects a host to perform the adaptation actuation against. The aim is
     * to call this method when faced with a host event with an unspecified host
     * value. This might occur in events with global power cap values (i.e. cluster
     * level).
     *
     * @param response The original response object to modify
     * @return The response object with a host value specified
     */
    protected abstract Response selectHostToAdapt(Response response);     
    
    /**
     * This tests to see if the power consumption limit will be breached or not
     * as well as the task boundaries.
     *
     * @param response The response type to check
     * @param taskType The description of the task to add to
     * @return If the task is permissible to be added.
     */
    public boolean getCanTaskBeAdded(Response response, String taskType) {
        return getCanTaskBeAdded(response, taskType, 1);
    }

    /**
     * This tests to see if the power consumption limit will be breached or not
     * as well as the task's boundaries.
     *
     * @param response The response type to check
     * @param taskType The task type to add to
     * @param count theAmount of tasks to add
     * @return If the task is permissible to add.
     */
    public boolean getCanTaskBeAdded(Response response, String taskType, int count) {
        if (actuator == null) {
            return false;
        }
        //average power of the task type to add
        double averagePower = getAveragePowerUsage(response.getApplicationId(), response.getDeploymentId(), taskType);
        Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.INFO, "Avg power = {0}", averagePower);
        //The current total measured power consumption
        double totalMeasuredPower = getTotalPowerUsage(response.getApplicationId(), response.getDeploymentId());
        Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.INFO, "Total power = {0}", totalMeasuredPower);
        averagePower = averagePower * count;
        List<String> taskTypes = getTaskTypesAvailableToAdd(response.getApplicationId(),
                response.getDeploymentId());
        if (!taskTypes.contains(taskType)) {
            Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.INFO, "Task type {0} is not available to add", taskType);
            for (String type : taskTypes) {
                Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.INFO, "Task type: {0} may be added.", type);
            }
            if (taskTypes.isEmpty()) {
                Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.INFO, "No task types were available to add.");
            }
            return false;
        }
        if (averagePower == 0 || totalMeasuredPower == 0) {
            //Skip if the measured power values don't make any sense.
            Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.WARNING, "Measured Power Fault: Average Power = {0} Total Power = {1}", new Object[]{averagePower, totalMeasuredPower});
            return true;
//            return enoughSpaceForVM(response, vmOvfType);
        }
        String applicationID = response.getApplicationId();
        String deploymentID = response.getDeploymentId();
        SLALimits limits = loader.getSlaLimits(applicationID, deploymentID);
        if (limits != null && limits.getPower() != null) {
            Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.INFO, "New power = {0}", totalMeasuredPower + averagePower);
            Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.INFO, "Limit of power = {0}", limits.getPower());
            if (totalMeasuredPower + averagePower > limits.getPower()) {
                return false;
            }
        }
        //TODO compare any further standard guarantees here that make sense
        //TODO cost??
        return true;
//        return enoughSpaceForVM(response, vmOvfType);
    }

    /**
     * The decision logic for horizontal scaling to a given target value.
     *
     * @param response The response to finalise details for.
     * @return The finalised response object
     */
    public Response scaleToNTasks(Response response) {
//        if (getActuator() == null) {
//            response.setAdaptationDetails("Unable to find actuator.");
//            response.setPossibleToAdapt(false);
//            return response;
//        }
//        String appId = response.getApplicationId();
//        String deploymentId = response.getDeploymentId();
//        String vmType = response.getAdaptationDetail("VM_TYPE");
//        int currentVmCount = getActuator().getVmCountOfGivenType(appId, deploymentId, vmType);
//        Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.WARNING, "Adaptation Details {0}", response.getAdaptationDetails());
//        Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.WARNING, "VM Type: {0} VM Count: {1}", new Object[]{vmType, response.getAdaptationDetail("VM_COUNT")});
//        int targetCount = Integer.parseInt(response.getAdaptationDetail("VM_COUNT"));
//        int difference = targetCount - currentVmCount;
//        ApplicationDefinition ovf = response.getCause().getApplicationDefinition();
//        ProductSection details = OVFUtils.getProductionSectionFromOvfType(ovf, vmType);
//        if (difference == 0) {
//            response.setPerformed(true);
//            response.setPossibleToAdapt(false);
//            response.setAdaptationDetails("Unable to adapt, the VM count is already at the target value");
//            return response;
//        }
//        if (ovf != null && details != null) {
//            if (targetCount < details.getLowerBound() || targetCount > details.getUpperBound()) {
//                response.setPerformed(true);
//                response.setPossibleToAdapt(false);
//                response.setAdaptationDetails("Unable to adapt, the target was out of acceptable bounds");
//                return response;
//            }
//        }
//        if (difference > 0) { //add VMs
//            response.setAdaptationDetails("VM_TYPE=" + vmType + ";VM_COUNT=" + difference);
//        } else { //less that zero so remove VMs
//            List<Integer> vmsPossibleToRemove = getActuator().getTaskIdsAvailableToRemove(appId, deploymentId);
//            //Note: the 0 - difference is intended to make the number positive
//            response.setAdaptationDetails("VM_TYPE=" + vmType + ";VMs_TO_REMOVE=" + getTasksToRemove(vmsPossibleToRemove, 0 - difference));
//        }
        return response;
    }
    
    /**
     * The decision logic for adding a task.
     *
     * @param response The response to finalise details for.
     * @return The finalised response object
     */
    public abstract Response deleteTask(Response response);

    /**
     * The decision logic for adding a task.
     *
     * @param response The response to finalise details for.
     * @return The finalised response object
     */
    public  abstract Response addTask(Response response);    

    /**
     * This modifies the response object to make a decision in cases where a host
     * level event doesn't specify the host. i.e. where the event might for
     * example relate to a cluster level power cap "POWER_CAP", with a host specified
     * as "*".
     *
     * @param response The response object to modify
     * @return The altered response object, no changes are made if the cause is
     * not a host event
     */
    protected Response handleUnspecifiedHost(Response response) {
        if (response.getCause() instanceof HostEventData) {
            HostEventData event = (HostEventData) response.getCause();
            if (event.getHost().equals("*") || event.getHost().isEmpty()) {
                response = selectHostToAdapt(response);
            }
        }
        return response;
    }    
    /**
     * This modifies the response object's cause in the case that it is a clock
     * event into either an application or a host based event.
     *
     * @param response The response object to modify
     * @return The altered response object, no changes are made if the cause is
     * not a clock event
     */
    protected Response handleClockEvent(Response response) {
        if (response.getCause() instanceof ClockEventData) {
            ClockEventData cause = (ClockEventData) response.getCause();
            //The next two if statements deal with call backs, where the original event has settings data attached.
            if (cause.hasSetting(ClockEventData.SETTING_APPLICATION)) {
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
                response = selectTaskOnAnyHost(response, response.getAdaptationDetail(ADAPTATION_DETAIL_APPLICATION));
                response.setAdaptationDetails(response.getAdaptationDetails() + ";origin=clock");
                response.setCause(cause.castToApplicationEventData(response.getAdaptationDetail(ADAPTATION_DETAIL_APPLICATION), "*"));
                return response;
            }
        }
        return response;
    }

    /**
     * This takes a response caused by an application and kills all other instances
     * of that application.
     * @param response The response object caused by an application based event.
     * @return The response object with indication of if the adaptation action is 
     * possible or not.
     */
    public Response actOnAllSimilarApps(Response response) {
        if (getActuator() == null) {
            response.setAdaptationDetails(ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND);
            response.setPossibleToAdapt(false);
            return response;
        }    
        if (!(response.getCause() instanceof ApplicationEventData)) {
            response.setAdaptationDetails("Wrong type of event cause, not an application.");
            response.setPossibleToAdapt(false);
            return response; 
        }
        ApplicationEventData cause = (ApplicationEventData)response.getCause();
        response.setTaskId(cause.getDeploymentId());
        List<ApplicationOnHost> tasks = getActuator().getTasks();
        tasks = ApplicationOnHost.filter(tasks, cause.getApplicationId(), -1);
        if (tasks == null || tasks.isEmpty()) {
            response.setAdaptationDetails("There were no other instances of the application to act upon.");
            response.setPossibleToAdapt(false);
            return response;
        }
        return response;        
    }    

    /**
     * This generates the list of Tasks to remove
     *
     * @param tasksPossibleToRemove The list of Tasks that could be removed
     * @param count The amount of Tasks needing to go
     * @return The string for the command to remove the tasks
     */
    protected abstract String getTasksToRemove(List<Integer> tasksPossibleToRemove, int count);

    /**
     * General Utility functions
     */

    /**
     * This lists which tasks that can be added to a deployment in order to make it
     * scale.
     *
     * @param applicationName The name of the application
     * @param deploymentId The deployment ID
     * @return The ids that can be used to scale the named deployment
     */
    public List<String> getTaskTypesAvailableToAdd(String applicationName, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * This lists which tasks that can be removed from a deployment in order to make it
     * scale.
     *
     * @param applicationName The name of the application
     * @param deploymentId The deployment ID
     * @return The task ids that can be used to down size the named deployment
     */
    protected List<Integer> getTaskIdsAvailableToRemove(String applicationName, String deploymentId) {
        List<Integer> answer = new ArrayList<>();
        List<ApplicationOnHost> tasks = modeller.getApplication(applicationName, Integer.parseInt(deploymentId));
        for (ApplicationOnHost task : tasks) {
            //Treat host id as unique id of task/application on a host
            answer.add(task.getAllocatedTo().getId());
        }
        return answer;
    }    
    
    /**
     * This gets a task of a given application, deployment and task id.
     *
     * @param name The application name or identifier
     * @param deploymentId The deployment instance identifier
     * @param taskId The task id
     * @return The task given the id values specified.
     */    
    protected ApplicationOnHost getTask(String name, String deploymentId, int taskId) {
        /**
         * The energy modeller's app id is a number
         */
        List<ApplicationOnHost> tasks = modeller.getApplication(name, Integer.parseInt(deploymentId));
        for (ApplicationOnHost task : tasks) {
            if ((task.getName().trim().equals(name.trim()))
                    && (task.getId() + "").equals(deploymentId.trim())) {
                return task;
            }
        }
        return null;
    }
    
    /**
     * This gets the power usage of a application.
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     * @return The power usage of the named application. 
     */
    protected double getTotalPowerUsage(String applicationName, String deploymentId) {
        double answer = 0.0;
        List<ApplicationOnHost> tasks = modeller.getApplication(applicationName, Integer.parseInt(deploymentId));
        for (CurrentUsageRecord record : modeller.getCurrentEnergyForApplication(tasks)) {
            answer = answer + record.getPower();
        }
        return answer;
    }

    /**
     * This gets the power usage of a task.
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     * @param taskId The task id
     * @return The power usage of a named task. 
     */
    protected double getPowerUsageTask(String applicationName, String deploymentId, int taskId) {
        ApplicationOnHost task = getTask(deploymentId, deploymentId, taskId);
        if (task == null) {
            return 0;
        }
        return modeller.getCurrentEnergyForApplication(task).getPower();
    }

    /**
     * This gets the power usage of a task.
     *
     * @param applicationName The name of the application
     * @param deploymentId The id of the deployment instance of the application
     * @param taskType The id of the task to get the measurement for
     * @return The power usage of a named task. 
     */
    protected double getAveragePowerUsage(String applicationName, String deploymentId, String taskType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * This gets the lowest power consuming task type it may for example be used
     * to add another instance of this type.
     *
     * @param response The response type to get the task type for.
     * @param taskType The list of task types of to search through (i.e.
     * available task types to add).
     * @return The task type with the lowest average power consumption
     */
    protected String pickLowestAveragePower(Response response, List<String> taskType) {
        response.setAdaptationDetails(taskType.get(0));
        if (taskType.isEmpty()) {
            return "";
        }
        String lowestAvgPowerType = taskType.get(0);
        double lowestAvgPower = Double.MAX_VALUE;
        for (String currentTaskType : taskType) {
            double currentTypesAvgPower = getAveragePowerUsage(response.getApplicationId(), response.getDeploymentId(),
                    currentTaskType);
            if (currentTypesAvgPower == 0) {
                Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.INFO,
                        "The calculation of the lowest average power of a task type saw a zero value for the type: {0}", currentTaskType);
            }
            if ((currentTypesAvgPower < lowestAvgPower) && currentTypesAvgPower > 0) {
                lowestAvgPowerType = currentTaskType;
                lowestAvgPower = currentTypesAvgPower;
            }
        }
        return lowestAvgPowerType;
    }

    /**
     * This gets the highest powered task to remove from the application
     * deployment.
     *
     * @param response The response object to perform the test for.
     * @param taskIds The TaskIds that are to be tested (i.e. ones that could be
     * removed for example).
     * @return The VmId to remove.
     */
    protected Integer getHighestPoweredTask(Response response, List<Integer> taskIds) {
        Integer answer = null;
        double answerPower = 0;
        String taskType = response.getAdaptationDetail("TASK_TYPE");
        if (taskIds.isEmpty()) {
            Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.WARNING,
                    "No Tasks were able to be deleted");
            return answer;
        }
        for (Integer taskId : taskIds) {
            double currentValue = getPowerUsageTask(response.getApplicationId(), response.getDeploymentId(), taskId);
            if (currentValue == 0) {
                Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.WARNING,
                        "The calculation of the highest powered Task saw a zero value for Task: {0}", taskId);
            }
            //ApplicationOnHost taskDef = getTask(response.getApplicationId(), response.getDeploymentId(), taskId);
            if (currentValue > answerPower && (taskType == null)) {  // || taskDef.getOvfId().equals(vmType))) {
                answer = taskId;
                answerPower = currentValue;
            }
        }
        if (answer == null) {
            Logger.getLogger(AbstractDecisionEngine.class.getName()).log(Level.WARNING,
                    "No task had the highest power thus defaulting to the first in the list");            
            taskIds.get(0);
        }
        return answer;
    }
    
    /**
     * This applies a sort and then picks the first N relevant hosts to perform 
     * the adaptation for.
     * @param response The response object to set a host name for
     * @param sort The sort to apply, if null will shuffle hosts instead, i.e. random.
     * @return The modified response object with a host value set.
     */
    protected Response selectHostToAdapt(Response response, Comparator<Host> sort) {
        if (response.getCause() instanceof HostEventData) {
            List<Host> hosts = getHostList(sort);
            if (sort == null) {
                Collections.shuffle(hosts);
            }
            HostEventData event = (HostEventData) response.getCause();
            //An indicator of how big the breach is.
            double scaleFactor = event.getDeviationBetweenRawAndGuarantee();
            //An indicator of how much the current seleected change will help
            double currentChangeFactor = 0.0;
            if (response.getActionType().equals(Response.AdaptationType.SHUTDOWN_HOST) || response.getActionType().equals(Response.AdaptationType.SHUTDOWN_N_HOSTS)) {
                Collections.reverse(hosts); //largest first (i.e. power consumer)               
                for (Host host : hosts) {
                    if (host.isAvailable()) {
                        event.setHost(generateHostString(event.getHost(), host.getHostName()));
                        currentChangeFactor = currentChangeFactor + modeller.getCurrentEnergyForHost(host).getPower();
                        if (currentChangeFactor >= scaleFactor || response.getActionType().equals(Response.AdaptationType.SHUTDOWN_HOST)) {
                            return response;
                        }
                    }
                }           
            } else if (response.getActionType().equals(Response.AdaptationType.STARTUP_HOST)  || response.getActionType().equals(Response.AdaptationType.STARTUP_N_HOSTS)) {
                for (Host host : hosts) { //smallest first (i.e. power consumer)
                    if (!host.isAvailable()) {
                        event.setHost(generateHostString(event.getHost(), host.getHostName()));
                        currentChangeFactor = currentChangeFactor + modeller.getCurrentEnergyForHost(host).getPower();
                        if (currentChangeFactor >= scaleFactor || response.getActionType().equals(Response.AdaptationType.STARTUP_HOST)) {
                            return response;
                        }
                    }
                }                
            }
        }
        return response;
    }
    
    /**
     * This method concatenates hosts into a host string ready for either shutting down
     * or starting up a set of hosts.
     * @param original The original host string
     * @param toAdd The host to add to the action list
     * @return The new concatenated hosts list
     */
    private String generateHostString(String original, String toAdd) {
        if (original == null || original.isEmpty()) {
            if (toAdd == null) {
                return "";
            }
            return toAdd;
        }
        if (toAdd == null) {
            return original;
        }
        return original + "," + toAdd;
    }
    
    /**
     * This creates a list of hosts as defined by a sorted order
     * @param sort The sort order to apply, null = natural sort
     * @return The list of hosts available
     */
    protected List<Host> getHostList(Comparator<Host> sort) {
        if (sort == null)
            return new ArrayList<>(modeller.getHostList());
        else {
            return modeller.getHostList(sort);            
        }
    }

    /**
     * This gets the list of all task objects from a given taskId and provides the
     * power consumption, ready for ranking.
     *
     * @param response The response object to perform the test for.
     * @param taskIds The Taskids that are to be tested (i.e. ones that could be
     * removed for example).
     * @return The list of Tasks and there power consumption
     */
    protected ArrayList<PowerApplicationMapping> getApplicationPowerList(Response response, List<Integer> taskIds) {
        ArrayList<PowerApplicationMapping> answer = new ArrayList<>();
        for (Integer taskId : taskIds) {
            double power = getPowerUsageTask(response.getApplicationId(), response.getDeploymentId(), taskId);
            ApplicationOnHost task = getTask(response.getApplicationId(), response.getDeploymentId(), taskId);
            answer.add(new PowerApplicationMapping(power, task));
        }
        Collections.sort(answer);
        return answer;
    }

    /**
     * This maps a power measurement to a Task.
     */
    public class PowerApplicationMapping implements Comparable<PowerApplicationMapping> {

        private final Double power;
        private final ApplicationOnHost task;

        public PowerApplicationMapping(double power, ApplicationOnHost task) {
            this.power = power;
            this.task = task;
        }

        public Double getPower() {
            return power;
        }

        public ApplicationOnHost getApplicationDefinition() {
            return task;
        }

        @Override
        public int compareTo(PowerApplicationMapping o) {
            return this.power.compareTo(o.power);
        }

    }

}
