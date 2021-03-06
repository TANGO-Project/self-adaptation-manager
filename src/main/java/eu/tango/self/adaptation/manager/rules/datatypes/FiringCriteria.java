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
package eu.tango.self.adaptation.manager.rules.datatypes;

import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the firing criteria for a threshold based event assessor. It contains
 * the mappings between an an agreement term and the rule that is required to be
 * fired.
 *
 * @author Richard Kavanagh
 */
public class FiringCriteria {

    private String agreementTerm;
    private EventData.Operator operator;
    private EventData.Type type;
    private Response.AdaptationType responseType;
    private Double minMagnitude = null;
    private Double maxMagnitude = null;
    private String parameters = "";

    public FiringCriteria() {
    }

    /**
     * This create a new firing criteria.
     *
     * @param agreementTerm The type of guarantee used.
     * @param operator If it is above or below the threshold value
     * @param responseType The type of response to give
     */
    public FiringCriteria(String agreementTerm, EventData.Operator operator, Response.AdaptationType responseType) {
        this.agreementTerm = agreementTerm;
        this.operator = operator;
        this.responseType = responseType;
    }

    /**
     * This create a new firing criteria.
     *
     * @param agreementTerm The type of guarantee used.
     * @param responseType The type of response to give
     * @param operator If it is above or below the threshold value
     */
    public FiringCriteria(String agreementTerm, String operator, String responseType) {
        this.agreementTerm = agreementTerm;
        this.operator = EventData.getOperator(operator);
        this.responseType = Response.getAdaptationType(responseType);
    }

    /**
     * This indicates if the specified event would meet the firing criteria.
     *
     * @param event The event to test to see if it would meet the firing
     * criteria
     * @return true only if the criteria are met.
     */
    public boolean shouldFire(EventData event) {
        if (minMagnitude != null && event.getDeviationBetweenRawAndGuarantee() < minMagnitude) {
            return false;
        }
        if (maxMagnitude != null && event.getDeviationBetweenRawAndGuarantee() > maxMagnitude) {
            return false;
        }
        if (type != null && !event.getType().equals(type)) {
            return false;
        }
        //Apply tests for start and end times for rules, should the parameter exist
        if (getStartTime() != null && LocalTime.now().isBefore(getStartTime())) {
            return false;
        }
        if (getEndTime() != null && LocalTime.now().isAfter(getEndTime())) {
            return false;
        }
        if (getDoWString()!= null && !isTodayInDayOfWeekString(getDoWString())) {
            return false;
        }        
        if (event instanceof ApplicationEventData) {
            /**
             * This ensures rules can be targeted at specific applications only.
             */
            ApplicationEventData appEvent = (ApplicationEventData) event;
            String appName = appEvent.getApplicationId();
            if (hasParameter("application")) {
                if (!getParameter("application").equals(appName)) {
                    return false;
                }
            }
        }
        return (agreementTerm.equals(event.getAgreementTerm())
                && operator.equals(event.getGuaranteeOperator()));
    }

    /**
     * The term in the SLA that is caused the breach, must match this in order
     * for the rule to fire.
     *
     * @return the agreementTerm The SLA agreement term that must be matched for
     * the rule to be applied.
     */
    public String getAgreementTerm() {
        return agreementTerm;
    }

    /**
     * The term in the SLA that is caused the breach, must match this in order
     * for the rule to fire.
     *
     * @param agreementTerm The SLA agreement term that must be matched for the
     * rule to be applied.
     */
    public void setAgreementTerm(String agreementTerm) {
        this.agreementTerm = agreementTerm;
    }

    /**
     * The form of response that is associated with the firing of this rule.
     *
     * @return the response to execute given the firing of this rule.
     */
    public Response.AdaptationType getResponseType() {
        return responseType;
    }

    /**
     * This sets the form of response that is associated with the firing of this
     * rule.
     *
     * @param responseType the response to execute given the firing of this
     * rule.
     */
    public void setResponseType(Response.AdaptationType responseType) {
        this.responseType = responseType;
    }

    /**
     * This returns the operator that is used as part of the test to see if the
     * measured value is "greater than"/"less than"/"equal" to the guaranteed
     * value.
     *
     * @return the operator either LT, LTE, EQ, GTE, GT
     */
    public EventData.Operator getOperator() {
        return operator;
    }

    /**
     * This sets the operator that is used as part of the test to see if the
     * measured value is "greater than"/"less than"/"equal" to the guaranteed
     * value.
     *
     * @param operator the operator either LT, LTE, EQ, GTE, GT
     */
    public void setOperator(EventData.Operator operator) {
        this.operator = operator;
    }

    /**
     * This gets the optional parameter of the Event type, i.e. if the rule
     * should fire only because of warnings or because of actual violation
     * notifications.
     *
     * @return The event violation type. either SLA_BREACH or WARNING
     */
    public EventData.Type getType() {
        return type;
    }

    /**
     * This sets the optional parameter of the Event type, i.e. if the rule
     * should fire only because of warnings or because of actual violation
     * notifications.
     *
     * @param type The event violation type to set, either SLA_BREACH or WARNING
     */
    public void setType(EventData.Type type) {
        this.type = type;
    }

    /**
     * This indicates the minimum magnitude for the rule to fire, for the
     * absolute difference between the guaranteed value and the measured value.
     * This value is optional.
     *
     * @return the minimum magnitude before this rule fires (inclusive).
     */
    public double getMinMagnitude() {
        return minMagnitude;
    }

    /**
     * This sets the minimum magnitude for the rule to fire, for the absolute
     * difference between the guaranteed value and the measured value. This
     * value is optional.
     *
     * @param minMagnitude the minimum magnitude before this rule fires
     * (inclusive).
     */
    public void setMinMagnitude(double minMagnitude) {
        this.minMagnitude = minMagnitude;
    }
    
    /**
     * This sets the minimum magnitude for the rule to fire, for the absolute
     * difference between the guaranteed value and the measured value. This
     * value is optional. This method parses the string safely and if the string
     * can not be interpreted as a double then the value is not set.
     *
     * @param minMagnitude the minimum magnitude before this rule fires
     * (inclusive).
     */
    public void setMinMagnitude(String minMagnitude) {
        try {
            if (minMagnitude == null) {
                this.minMagnitude = null;
            }
            this.minMagnitude = Double.parseDouble(minMagnitude);
        } catch (NumberFormatException ex) {
            //Ignoring parse errors and leaving the original value in place.
        }
    }    
    
    /**
     * This indicates the minimum magnitude for the rule to fire, for the
     * absolute difference between the guaranteed value and the measured value.
     * This value is optional.
     *
     * @return the maximum magnitude before this rule fires (inclusive).
     */
    public double getMaxMagnitude() {
        return maxMagnitude;
    }

    /**
     * This sets the maximum magnitude for the rule to fire, for the absolute
     * difference between the guaranteed value and the measured value. This
     * value is optional.
     *
     * @param maxMagnitude the maximum magnitude before this rule fires
     * (inclusive).
     */
    public void setMaxMagnitude(double maxMagnitude) {
        this.maxMagnitude = maxMagnitude;
        if (minMagnitude == null) {
            minMagnitude = 0.0;
        }
    }

    /**
     * This sets the maximum magnitude for the rule to fire, for the absolute
     * difference between the guaranteed value and the measured value. This
     * value is optional. This method parses the string safely and if the string
     * can not be interpreted as a double then the value is not set.
     *
     * @param maxMagnitude the maximum magnitude before this rule fires
     * (inclusive).
     */
    public void setMaxMagnitude(String maxMagnitude) {
        try {
            if (maxMagnitude == null) {
                this.maxMagnitude = null;
            }            
            this.maxMagnitude = Double.parseDouble(maxMagnitude);
            if (minMagnitude == null) {
                minMagnitude = 0.0;
            }
        } catch (NumberFormatException ex) {
            //Ignoring parse errors and leaving the original value in place.
        }        
    }    
    
    /**
     * This examines the application definition for rules associated with the 
     * application. It extracts the rules and returns them.
     *
     * @param applicationDefinition The OVF to extract the firing criteria from
     * @return The firing criteria that are from the application.
     */
    public static ArrayList<FiringCriteria> getFiringCriteriaFromApplication(ApplicationDefinition applicationDefinition) {
        ArrayList<FiringCriteria> answer = new ArrayList<>();
        if (applicationDefinition == null) {
            return answer;
        }
        //This ensures rules that are gloabal are collected correctly
        answer.addAll(applicationDefinition.getAdaptationRules());
        return answer;
    }

    /**
     * Actuation rules sometimes need additional information in order to fire
     * correctly, such as how many VMs are needed, the type of VM to scale etc.
     * This provides a means in the rule sets to provide this extra parameter
     * information.
     *
     * @return the parameters The list of all parameters, unparsed in the format
     * "X=test;Y=testing"
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * Given the key value of the parameter this returns its value.
     *
     * @param key The key name for the actuation parameter
     * @return The value of the parameter else null.
     */
    public String getParameter(String key) {
        String[] args = parameters.split(";");
        for (String arg : args) {
            if (arg.split("=")[0].equals(key)) {
                return arg.split("=")[1].trim();
            }
        }
        return null;
    }
    
    /**
     * Indicates if this firing criteria has a particular parameter or not
     * @param key The parameter to check to see if it is there or not
     * @return If there are settings attached to this clock event data or not
     */
    public boolean hasParameter(String key) {
        return getParameter(key) != null && !getParameter(key).isEmpty();
    }

    /**
     * Actuation rules sometimes need additional information in order to fire
     * correctly, such as how many VMs are needed, the type of VM to scale etc.
     * This provides a means in the rule sets to provide this extra parameter
     * information.
     *
     * @param parameters the parameters to set as a semi-colon delimeter based
     * list of key value pairs. i.e. argument=value;argument2=three
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    
    /**
     * This returns the firing criteria's start time, in a format such as: "12:30:18"
     * @return The start time of this firing criteria
     */
    public LocalTime getStartTime() {
        try {
            if (hasParameter("START_TIME")) {
                return LocalTime.parse(getParameter("START_TIME"));
            }
        } catch (DateTimeParseException ex) {
            Logger.getLogger(FiringCriteria.class.getName()).log(Level.SEVERE, "The start time did not parse correctly");
        }
        return null;
    }
    
    /**
     * This returns the firing criteria's end time, in a format such as: "14:30:00"
     * @return The end time of this firing criteria
     */
    public LocalTime getEndTime() {
        try {
        if (hasParameter("END_TIME")) {
            return LocalTime.parse(getParameter("END_TIME"));
            }
        } catch (DateTimeParseException ex) {
            Logger.getLogger(FiringCriteria.class.getName()).log(Level.SEVERE, "The end time did not parse correctly");
        }
        return null;
    }
    
    /**
     * This compares a day of the week string, such as "1000000", meaning Monday, 
     * or "1010000" meaning Monday and Wednesday etc.
     * @param dowString The day of the week string, representing a bit mask for the day's
     * of the week a rule should fire.
     * @return If the rule should fire or not based upon the day of the week.
     */
    private boolean isTodayInDayOfWeekString(String dowString) {
        byte[] array = dowString.getBytes(StandardCharsets.UTF_8);
        byte[] today = {0,0,0,0,0,0,0};
        //Note: -2 ensures it fits in the array's range of 0..6 and that monday is the start of the week
        today[new GregorianCalendar().get(Calendar.DAY_OF_WEEK) - 2] = 1;
        for (int i = 0; i < 7; i++) {
            array[i] = (byte) (array[i] - 48);
        }
        for (int i = 0; i < 7; i++) {
            array[i] = (byte) (array[i] & today[i]);             
        }
        for (byte u : array) {
            if (u >= 1) {
                return true;
            }
        }
        return false;
    }    
    
    /**
     * This returns the firing criteria's day of the week string, such as "1111100"
     * indicating it should fire from Monday through to Friday. 
     * @return The day of the week string for this rule.
     */
    public String getDoWString() {
        try {
        if (hasParameter("DAY_OF_WEEK")) {
            return getParameter("DAY_OF_WEEK");
            }
        } catch (DateTimeParseException ex) {
            Logger.getLogger(FiringCriteria.class.getName()).log(Level.SEVERE, "The end time did not parse correctly");
        }
        return null;
    }     
    
    @Override
    public String toString() {
        return "Rule: "
                + agreementTerm + ":"
                + operator + ":"
                + type + ":"
                + responseType + ":"
                + parameters;
    }

}
