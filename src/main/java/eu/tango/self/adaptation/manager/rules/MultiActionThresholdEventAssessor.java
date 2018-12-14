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
package eu.tango.self.adaptation.manager.rules;

import eu.ascetic.ioutils.io.ResultsStore;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.FiringCriteria;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * This event assessor examines events and as soon as a breach occurs, n times
 * it reacts.
 *
 * @author Richard Kavanagh
 */
public class MultiActionThresholdEventAssessor extends AbstractEventAssessor {

    private int threshold = 2;
    private ArrayList<FiringCriteria> rules = new ArrayList<>();
    private static final String CONFIG_FILE = "self-adaptation-manager-threshold.properties";
    private static final String RULES_FILE = "rules.csv";
    private String workingDir;
    private ResultsStore rulesFile;

    /**
     * This creates a new threshold event assessor. Configuration settings are
     * taken from file.
     */
    public MultiActionThresholdEventAssessor() {
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            threshold = config.getInt("self.adaptation.manager.threshold", threshold);
            config.setProperty("self.adaptation.manager.threshold", threshold);
            workingDir = config.getString("self.adaptation.manager.working.directory", ".");
        } catch (ConfigurationException ex) {
            Logger.getLogger(MultiActionThresholdEventAssessor.class.getName()).log(Level.INFO, "Error loading the configuration of the PaaS Self adaptation manager", ex);
        }
        loadRules();
    }

    /**
     * This performs a check to see if the settings file is empty or not. It
     * will write out a blank file if the file is not present.
     *
     * @param rulesFile The list of rules on disk to load
     * @return If the defaults settings have been written out to disk or not.
     */
    public boolean writeOutDefaults(ResultsStore rulesFile) {
        boolean answer = false;
        //Agreement Term, Guarantee Direction and Response Type
        if (!rulesFile.getResultsFile().exists()) {
            rulesFile.add("Agreement Term");
            rulesFile.append("Comparator");
            rulesFile.append("Response Type");
            rulesFile.append("Event Type (Violation or Warning)");
            rulesFile.append("Lower bound");
            rulesFile.append("Upper bound");
            rulesFile.append("Parameters");
            rulesFile.add("energy_usage_per_app");
            rulesFile.append("GT");
            rulesFile.append("REMOVE_TASK");
            rulesFile.add("power_usage_per_app");
            rulesFile.append("GT");
            rulesFile.append("REMOVE_TASK");
            rulesFile.add("energy_usage_per_app");
            rulesFile.append("GTE");
            rulesFile.append("REMOVE_TASK");
            rulesFile.add("power_usage_per_app");
            rulesFile.append("GTE");
            rulesFile.append("REMOVE_TASK");
            rulesFile.save();
            answer = true;
        }
        return answer;
    }

    /**
     * This loads the rules used by this event assessor in from disk.
     */
    private void loadRules() {
        Logger.getLogger(StackedThresholdEventAssessor.class.getName()).log(Level.INFO, "Loading default rules for self adaptation");
        /**
         * Load in from file the following: Agreement Term, Guarantee Direction
         * and Response Type
         */
        if (!workingDir.endsWith("/")) {
            workingDir = workingDir + "/";
        }
        rulesFile = new ResultsStore(workingDir + RULES_FILE);
        writeOutDefaults(rulesFile);        
        rulesFile.load();
        Logger.getLogger(StackedThresholdEventAssessor.class.getName()).log(Level.INFO, "There are {0} to load.", rulesFile.size());        
        //ignore the header of the file
        for (int i = 1; i < rulesFile.size(); i++) {
            ArrayList<String> current = rulesFile.getRow(i);
            FiringCriteria rule = new FiringCriteria(current.get(0), current.get(1), current.get(2));
            String logString = "Term:" + current.get(0) + " Comparator: " + current.get(1) + " Response: " + current.get(2);
            try {
                if (current.size() >= 4 && !current.get(3).isEmpty()) {
                    rule.setType(EventData.Type.valueOf(current.get(3)));
                    logString = logString + " Type: " + current.get(3);
                }
            } catch (IllegalArgumentException ex) {
                /**
                 * If the event type was not recognised then ignore it.
                 * This therefore leaves this to be an optional value.
                 */
            }
            if (current.size() >= 5) {
                rule.setMinMagnitude(current.get(4));
                logString = logString + " Min: " + current.get(4);
            }
            if (current.size() >= 6) {
                rule.setMaxMagnitude(current.get(5));
                logString = logString + " Max: " + current.get(5);
            }
            if (current.size() >= 7) {
                rule.setParameters(current.get(6));
                logString = logString + " Params: " + current.get(6);
            }
            rules.add(rule);
            Logger.getLogger(MultiActionThresholdEventAssessor.class.getName()).log(Level.WARNING, "Adding Rule: {0}", logString);            
        }
    }
    
    /**
     * This assesses an event and decides if a response is required. If no
     * response is required then null is returned. Calling this is equivalent to
     * calling the method assessEvent(EventData event, List sequence) but in
     * this case the event sequence list is maintained by the event assessor.
     *
     * @param event The SLA event to assess
     * @param eventData The historical list of event data.
     * @return A response object in cases where an adaptive response is
     * required.
     */
    @Override
    protected Response assessEvent(EventData event, List<EventData> eventData) {
        synchronized (this) {
            if (getActuator() != null && event instanceof ApplicationEventData) {
                ApplicationEventData appEvent = (ApplicationEventData) event;
                appEvent.setApplicationDefinition(getActuator().getApplication(appEvent.getApplicationId(), appEvent.getDeploymentId()));
            }
            List<Response> answers = assessAsMultiResponseEvent(event, eventData, adaptations);
            if (answers != null) {
                for (Response answer : answers) {
                    adaptations.addAll(answers);
                    answer = decisionEngine.decide(answer);
                    if (getActuator() != null && answer.isPossibleToAdapt()) {
                        getActuator().actuate(answer);
                        Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, "Actuator - Performing Work");
                        Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, answer.toString());
                    }
                    if (logging) {
                        responseHistoryLogger.printToFile(answer);
                    }
                }
            }
            if (answers != null)
                //usually the return value isn't used, so this is ok.
                return answers.get(0);
            else {
                return null;
            }
        }
    }
    
    @Override
    public Response assessEvent(EventData event, List<EventData> sequence, List<Response> recentAdaptation) {
        Response answer = null;
        List<EventData> previousData = EventDataAggregator.filterEventData(sequence, event.getGuaranteeid(), event.getAgreementTerm());
        if (previousData.size() >= threshold || event.isSignificantOnOwn()) {
            /**
             * The rule should determine the type of response, i.e. scale up
             * down in or out. This will be read in from file.
             *
             * The decision engine should decide where to perform the adaptation
             * and to what scale.
             */
            FiringCriteria rule = getFirstMatchingFiringCriteria(event);
            int previousActionCount = ResponseHistoryAggregator.filterResponseHistory(recentAdaptation, event.getGuaranteeid(), event.getAgreementTerm()).size();
            if (rule != null && previousActionCount == 0) {
                answer = new Response(getActuator(), event, rule.getResponseType());
                answer.setAdaptationDetails(rule.getParameters());
            }
            return answer;
        }
        return answer;
    }    

    /**
     * This assesses an event and decides if a response is required. If no
     * response is required then null is returned.
     *
     * @param event The SLA event to assess
     * @param sequence The sequence of similar events that have been assessed.
     * @param recentAdaptation A list of recent relevant adaptations that have
     * occurred.
     * @return A response object in cases where an adaptive response is
     * required.
     */
    public List<Response> assessAsMultiResponseEvent(EventData event, List<EventData> sequence, List<Response> recentAdaptation) {
        List<Response> listOfAnswers = null;
        List<EventData> previousData = EventDataAggregator.filterEventData(sequence, event.getGuaranteeid(), event.getAgreementTerm());
        if (previousData.size() >= threshold || event.isSignificantOnOwn()) {
            /**
             * The rule should determine the type of response, i.e. scale up
             * down in or out. This will be read in from file.
             *
             * The decision engine should decide where to perform the adaptation
             * and to what scale.
             */
            ArrayList<FiringCriteria> matchingRules = getMatchingFiringCriteria(event);
            for (FiringCriteria rule : matchingRules) {
                int previousActionCount = ResponseHistoryAggregator.filterResponseHistory(recentAdaptation, event.getGuaranteeid(), event.getAgreementTerm()).size();
                if (previousActionCount == 0) {
                    if (listOfAnswers == null) {
                        listOfAnswers = new ArrayList<>();
                        Response answer = new Response(getActuator(), event, rule.getResponseType());
                        answer.setAdaptationDetails(rule.getParameters());
                        listOfAnswers.add(answer);
                    }
                }
            }
            return listOfAnswers;
        }
        return listOfAnswers;
    }
    
    /**
     * This tests an event to see if it matches any of the rules for firing off
     * a response.
     *
     * @param event The event to test
     * @return The first firing criteria that indicated that it has fired, due
     * to the specified event.
     */
    private FiringCriteria getFirstMatchingFiringCriteria(EventData event) {
        for (FiringCriteria rule : rules) {
            if (rule.shouldFire(event)) {
                return rule;
            }
        }
        return null;
    }    

    /**
     * This tests an event to see if it matches any of the rules for firing off
     * a response.
     *
     * @param event The event to test
     * @return The first firing criteria that indicated that it has fired, due
     * to the specified event.
     */
    private ArrayList<FiringCriteria> getMatchingFiringCriteria(EventData event) {
        ArrayList<FiringCriteria> answer = new ArrayList<>();
        for (FiringCriteria rule : rules) {
            if (rule.shouldFire(event)) {
                answer.add(rule);
            }
        }
        return answer;
    }

    /**
     * This gets the amount of events of a given type that need to occur before
     * the event fires.
     *
     * @return the threshold
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * This sets the amount of events of a given type that need to occur before
     * the event fires.
     *
     * @param threshold the threshold to set
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

}
