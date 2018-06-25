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
package eu.tango.self.adaptation.manager.rules.datatypes;

import eu.tango.self.adaptation.manager.actuators.ActuatorInvoker;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a valid response that the self-adaptation manager can
 * use in order to respond to the incoming events.
 *
 * @author Richard Kavanagh
 */
public class Response implements Comparable<Response> {

    private final ActuatorInvoker actuator;
    private EventData cause;
    private AdaptationType actionType;
    private String adaptationDetails;
    private String taskId;
    private boolean performed = false;
    private boolean possibleToAdapt = true;
    //The string to set the adaptation details value to in the event no actuator is found.
    public static final String ADAPTATION_DETAIL_ACTUATOR_NOT_FOUND = "Unable to find actuator.";
    //The string to set the adaptation details value to in the event no task to actuate against is found.
    public static final String ADAPTATION_DETAIL_NO_ACTUATION_TASK = "Could not find a task to actuate against.";
    //Property values for applications and hosts i.e. if the response targets a particular host or application
    public static final String ADAPTATION_DETAIL_APPLICATION = "application";
    public static final String ADAPTATION_DETAIL_HOST = "host";
    private static final Map<String, Response.AdaptationType> ADAPTATION_TYPE_MAPPING
            = new HashMap<>();

    static {
        ADAPTATION_TYPE_MAPPING.put("INCREASE_WALL_TIME", Response.AdaptationType.INCREASE_WALL_TIME);
        ADAPTATION_TYPE_MAPPING.put("REDUCE_WALL_TIME", Response.AdaptationType.REDUCE_WALL_TIME);
        ADAPTATION_TYPE_MAPPING.put("INCREASE_WALL_TIME_SIMILAR_APPS", Response.AdaptationType.INCREASE_WALL_TIME_SIMILAR_APPS);
        ADAPTATION_TYPE_MAPPING.put("REDUCE_WALL_TIME_SIMILAR_APPS", Response.AdaptationType.REDUCE_WALL_TIME_SIMILAR_APPS);        
        ADAPTATION_TYPE_MAPPING.put("MINIMIZE_WALL_TIME_SIMILAR_APPS", Response.AdaptationType.MINIMIZE_WALL_TIME_SIMILAR_APPS);        
        ADAPTATION_TYPE_MAPPING.put("ADD_TASK", Response.AdaptationType.ADD_TASK);
        ADAPTATION_TYPE_MAPPING.put("REMOVE_TASK", Response.AdaptationType.REMOVE_TASK);
        ADAPTATION_TYPE_MAPPING.put("SCALE_TO_N_TASKS", Response.AdaptationType.SCALE_TO_N_TASKS);
        ADAPTATION_TYPE_MAPPING.put("ADD_CPU", Response.AdaptationType.ADD_CPU);
        ADAPTATION_TYPE_MAPPING.put("ADD_MEMORY", Response.AdaptationType.ADD_MEMORY);
        ADAPTATION_TYPE_MAPPING.put("REMOVE_CPU", Response.AdaptationType.REMOVE_CPU);
        ADAPTATION_TYPE_MAPPING.put("PAUSE_APP", Response.AdaptationType.PAUSE_APP);
        ADAPTATION_TYPE_MAPPING.put("UNPAUSE_APP", Response.AdaptationType.UNPAUSE_APP);
        ADAPTATION_TYPE_MAPPING.put("PAUSE_SIMILAR_APPS", Response.AdaptationType.PAUSE_SIMILAR_APPS);
        ADAPTATION_TYPE_MAPPING.put("UNPAUSE_SIMILAR_APPS", Response.AdaptationType.UNPAUSE_SIMILAR_APPS);
        ADAPTATION_TYPE_MAPPING.put("OVERSUBSCRIBE_APP", Response.AdaptationType.OVERSUBSCRIBE_APP);
        ADAPTATION_TYPE_MAPPING.put("EXCLUSIVE_APP", Response.AdaptationType.EXCLUSIVE_APP);
        ADAPTATION_TYPE_MAPPING.put("KILL_APP", Response.AdaptationType.KILL_APP);
        ADAPTATION_TYPE_MAPPING.put("HARD_KILL_APP", Response.AdaptationType.HARD_KILL_APP);
        ADAPTATION_TYPE_MAPPING.put("KILL_SIMILAR_APPS", Response.AdaptationType.KILL_SIMILAR_APPS);
        ADAPTATION_TYPE_MAPPING.put("RESELECT_ACCELERATORS", Response.AdaptationType.RESELECT_ACCELERATORS);
        ADAPTATION_TYPE_MAPPING.put("REDUCE_POWER_CAP", Response.AdaptationType.REDUCE_POWER_CAP);
        ADAPTATION_TYPE_MAPPING.put("INCREASE_POWER_CAP", Response.AdaptationType.INCREASE_POWER_CAP);
        ADAPTATION_TYPE_MAPPING.put("SET_POWER_CAP", Response.AdaptationType.SET_POWER_CAP);
        ADAPTATION_TYPE_MAPPING.put("SHUTDOWN_HOST", Response.AdaptationType.SHUTDOWN_HOST);
        ADAPTATION_TYPE_MAPPING.put("STARTUP_HOST", Response.AdaptationType.STARTUP_HOST);
        ADAPTATION_TYPE_MAPPING.put("SHUTDOWN_N_HOSTS", Response.AdaptationType.SHUTDOWN_N_HOSTS);
        ADAPTATION_TYPE_MAPPING.put("STARTUP_N_HOSTS", Response.AdaptationType.STARTUP_N_HOSTS);
        ADAPTATION_TYPE_MAPPING.put(null, null);
        ADAPTATION_TYPE_MAPPING.put("", null);
    }

    /**
     * Adaptation types are the forms of adaptation that can be made to occur.
     */
    public enum AdaptationType {

        INCREASE_WALL_TIME, REDUCE_WALL_TIME, 
        INCREASE_WALL_TIME_SIMILAR_APPS, REDUCE_WALL_TIME_SIMILAR_APPS, 
        MINIMIZE_WALL_TIME_SIMILAR_APPS,
        ADD_TASK, REMOVE_TASK, SCALE_TO_N_TASKS, //Application Resources
        ADD_CPU, REMOVE_CPU, ADD_MEMORY, REMOVE_MEMORY,
        PAUSE_APP, UNPAUSE_APP, PAUSE_SIMILAR_APPS, UNPAUSE_SIMILAR_APPS,
        OVERSUBSCRIBE_APP, EXCLUSIVE_APP,        
        KILL_APP, HARD_KILL_APP, KILL_SIMILAR_APPS,
        RESELECT_ACCELERATORS, //Migrate
        REDUCE_POWER_CAP, INCREASE_POWER_CAP, SET_POWER_CAP,
        SHUTDOWN_HOST, STARTUP_HOST, SHUTDOWN_N_HOSTS, STARTUP_N_HOSTS
    }

    /**
     * This creates a standard response object. It indicates which actuator to
     * use and which message to send to it.
     *
     * @param actuator The actuator used to invoke the change.
     * @param cause A copy of the incoming event that caused the response to
     * fire.
     * @param actionType The description of the type of action to take
     */
    public Response(ActuatorInvoker actuator, EventData cause, AdaptationType actionType) {
        this.actuator = actuator;
        this.cause = cause;
        this.actionType = actionType;
    }

    /**
     * This provides the mapping between the string representation of a response
     * type and the adaptation type.
     *
     * @param responseType The name of the rule.
     * @return The Adaptation type required.
     */
    public static Response.AdaptationType getAdaptationType(String responseType) {
        Response.AdaptationType answer = ADAPTATION_TYPE_MAPPING.get(responseType);
        if (answer == null) {
            if (responseType.contains("SCALE_TO_")) {
                return AdaptationType.SCALE_TO_N_TASKS;
            }
        }
        return answer;
    }

    /**
     * This gets the time of arrival of the event that caused the response.
     *
     * @return
     */
    public long getTime() {
        return cause.getTime();
    }

    /**
     * This returns the event that was the original cause of the
     * response to be created.
     *
     * @return The event that caused the response (or at least the last in a
     * sequence of events).
     */
    public EventData getCause() {
        return cause;
    }
    
    /**
     * This sets the event that was the original cause of the response to be created.
     * This allows for cases such as clock based trigger events to be recast into
     * more specific causes.
     *
     * @param cause The event that caused the response (or at least the last in a
     * sequence of events).
     */
    public void setCause(EventData cause) {
        this.cause = cause;
    }    

    @Override
    public int compareTo(Response response) {
        //This sequences responses in cronological order.
        return Long.compare(this.getTime(), response.getTime());
    }

    /**
     * This returns the type of adaptation that the response specifies to give
     * to the event.
     *
     * @return the type of action to perform to respond to the event
     */
    public AdaptationType getActionType() {
        return actionType;
    }

    /**
     * This sets the type of adaptation that the response specifies to give to
     * the event.
     *
     * @param actionType the actionType to set
     */
    public void setActionType(AdaptationType actionType) {
        this.actionType = actionType;
    }

    /**
     * This returns additional information about the adaptation.
     *
     * @return the adaptationDetails
     */
    public String getAdaptationDetails() {
        return adaptationDetails;
    }

    /**
     * Given the key value of the adaption detail this returns its value.
     *
     * @param key The key name for the actuation parameter
     * @return The value of the adaptation detail else null.
     */
    public String getAdaptationDetail(String key) {
        String[] args = adaptationDetails.split(";");
        for (String arg : args) {
            if (arg.split("=")[0].trim().equalsIgnoreCase(key)) {
                return arg.split("=")[1].trim();
            }
        }
        return null;
    }
    
    /**
     * Indicates if an adaptation detail with a given key value exists or not
     * @param key The key name for the actuation parameter
     * @return If the adaptation detail is present or not
     */
    public boolean hasAdaptationDetail(String key) {
        return getAdaptationDetail(key) != null && !getAdaptationDetail(key).isEmpty();
    }    

    /**
     * This sets additional information about the adaptation, that might be
     * needed.
     *
     * @param adaptationDetails the adaptationDetails to set
     */
    public void setAdaptationDetails(String adaptationDetails) {
        this.adaptationDetails = adaptationDetails;
    }

    /**
     * This indicates if on deciding to adapt if a possible solution was found.
     *
     * @return the possibleToAdapt
     */
    public boolean isPossibleToAdapt() {
        return possibleToAdapt;
    }

    /**
     * This sets the flag to say if on deciding to adapt if a possible solution
     * was able to be found.
     *
     * @param possibleToAdapt the possibleToAdapt to set
     */
    public void setPossibleToAdapt(boolean possibleToAdapt) {
        this.possibleToAdapt = possibleToAdapt;
    }

    /**
     * This indicates if the action associated with the response has been
     * performed.
     *
     * @return the performed
     */
    public boolean isPerformed() {
        return performed;
    }

    /**
     * This sets the flag to indicate if the action associated with this
     * response has been performed.
     *
     * @param performed the performed to set
     */
    public void setPerformed(boolean performed) {
        this.performed = performed;
    }

    /**
     * This indicates if the action associated with the response has been
     * completed. i.e. its either been performed or is un-actionable.
     *
     * @return the performed
     */
    public boolean isComplete() {
        return performed || !possibleToAdapt;
    }

    /**
     * This returns the deployment id associated with the event that caused the
     * response.
     *
     * @return The name of the application that the response is associated with,
     * blank if the cause is not from an application based event
     */
    public String getApplicationId() {
        if (cause instanceof ApplicationEventData) {
            return ((ApplicationEventData) cause).getApplicationId();
        }
        return "";
    }

    /**
     * This returns the deployment id associated with the event that caused the
     * response.
     *
     * @return The deployment id, if one is set, if not it returns "" for system based events.
     */
    public String getDeploymentId() {
        if (cause instanceof ApplicationEventData) {
            return ((ApplicationEventData) cause).getDeploymentId();
        }
        return "";
    }
    
    /**
     * This indicates if the deployment id is set or not
     * @return true if the deployment id is set, otherwise false
     */
    public boolean hasDeploymentId() {
        return getDeploymentId() != null && !getDeploymentId().isEmpty();
    }

    /**
     * This returns the task id associated with the response. This is the task
     * that is to be adapted. i.e. change size, delete etc
     *
     * @return The task id of the task to be adapted.
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * This sets the Task id associated with the response. This is the task that
     * is to be adapted. i.e. change size, delete etc
     *
     * @param taskId The task id of the task to be adapted.
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * This gets the actuator to be used when the response is executed.
     * @return The actuator that is to be used to execute the response.
     */
    public ActuatorInvoker getActuator() {
        return actuator;
    }

    @Override
    public String toString() {
        String answer = "";
        answer = answer + getTime() + " : ";
        answer = answer + getApplicationId() + " : ";
        answer = answer + getDeploymentId() + " : ";
        answer = answer + getTaskId() + " : ";
        if (getCause() instanceof HostEventData) {
            answer = answer + ((HostEventData)getCause()).getHost() + " : ";
        }
        answer = answer + getActionType().toString() + " : ";
        answer = answer + getAdaptationDetails() + " : ";
        answer = answer + getCause().getAgreementTerm() + " : ";
        answer = answer + getCause().getGuaranteedValue() + " : ";    
        answer = answer + getCause().getRawValue() + " : ";
        answer = answer + getCause().getGuaranteeOperator() + " : ";
        answer = answer + isPossibleToAdapt() + " : ";
        answer = answer + isPerformed();
        return answer;
    }
    
    

}
