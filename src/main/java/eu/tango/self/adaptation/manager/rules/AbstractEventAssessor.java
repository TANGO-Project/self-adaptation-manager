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
package eu.tango.self.adaptation.manager.rules;

import eu.tango.self.adaptation.manager.actuators.ActuatorInvoker;
import eu.tango.self.adaptation.manager.listeners.EventListener;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import eu.tango.self.adaptation.manager.rules.decisionengine.DecisionEngine;
import eu.tango.self.adaptation.manager.rules.decisionengine.JobPriorityDecisionEngine;
import eu.tango.self.adaptation.manager.rules.decisionengine.RandomDecisionEngine;
import eu.tango.self.adaptation.manager.rules.loggers.EventHistoryLogger;
import eu.tango.self.adaptation.manager.rules.loggers.ResponseHistoryLogger;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * The abstract class event assessor provides the generic routines for
 * assessment of adaptation events. These events are assessed by concrete
 * implementations of this class. Adaption is then implemented based upon the
 * outcome of rules associated with the assessor.
 *
 * @author Richard Kavanagh
 */
public abstract class AbstractEventAssessor implements EventAssessor {

    private ArrayList<EventListener> listeners = new ArrayList<>();
    private ActuatorInvoker actuator = null;
    private List<EventData> eventHistory = new ArrayList<>();
    protected DecisionEngine decisionEngine;
    private String decisionEngineName = "RandomDecisionEngine";
    protected boolean logging = true;
    protected ResponseHistoryLogger responseHistoryLogger = null;
    private Thread responseHistoryLoggerThread = null;
    private EventHistoryLogger eventHistoryLogger = null;
    private Thread eventHistoryLoggerThread = null;
    protected List<Response> adaptations = new ArrayList<>();
    //duration a history item can stay alive
    protected int historyLengthSeconds = (int) TimeUnit.MINUTES.toSeconds(5);
    //The rate at how often history items are checked to be still in date
    private int pollInterval = 5;
    private Thread historyClearerThread = null;
    private HistoryClearer historyClearer = null;
    private static final String CONFIG_FILE = "self-adaptation-manager.properties";
    private static final String DEFAULT_DECISION_ENGINE_PACKAGE
            = "eu.tango.self.adaptation.manager.rules.decisionengine";

    /**
     * This launches a new event assessor.
     */
    public AbstractEventAssessor() {
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            historyLengthSeconds = config.getInt("self.adaptation.manager.history.length", historyLengthSeconds);
            config.setProperty("self.adaptation.manager.history.length", historyLengthSeconds);
            pollInterval = config.getInt("self.adaptation.manager.history.poll.interval", pollInterval);
            config.setProperty("self.adaptation.manager.history.poll.interval", pollInterval);
            decisionEngineName = config.getString("self.adaptation.manager.decision.engine", decisionEngineName);
            config.setProperty("self.adaptation.manager.decision.engine", decisionEngineName);
            setDecisionEngine(decisionEngineName);
            if (decisionEngine instanceof JobPriorityDecisionEngine) {
                if (config.containsKey("self.adaptation.manager.decision.engine.ranking.jobs")) {
                    ((JobPriorityDecisionEngine)decisionEngine).setJobRanking(config.getString("self.adaptation.manager.decision.engine.ranking.jobs", "JobTypeAndPriority"));             
                } else {
                    config.setProperty("self.adaptation.manager.decision.engine.ranking.jobs", "JobTypeAndPriority");
                }
                if (config.containsKey("self.adaptation.manager.decision.engine.ranking.hosts")) {
                    ((JobPriorityDecisionEngine)decisionEngine).setHostRanking(config.getString("self.adaptation.manager.decision.engine.ranking.hosts", "HostIdlePower"));             
                } else {
                    config.setProperty("self.adaptation.manager.decision.engine.ranking.hosts", "HostIdlePower");
                }
            }
            logging = config.getBoolean("self.adaptation.manager.logging", logging);
            config.setProperty("self.adaptation.manager.logging", logging);
            if (logging) {
                responseHistoryLogger = new ResponseHistoryLogger(new File("ResponseLog.csv"), true);
                responseHistoryLoggerThread = new Thread(responseHistoryLogger);
                responseHistoryLoggerThread.setDaemon(true);
                responseHistoryLoggerThread.start();
                eventHistoryLogger = new EventHistoryLogger(new File("EventLog.csv"), true);
                eventHistoryLoggerThread = new Thread(eventHistoryLogger);
                eventHistoryLoggerThread.setDaemon(true);
                eventHistoryLoggerThread.start();
            }
            start();
        } catch (ConfigurationException ex) {
            Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.INFO, "Error loading the configuration of the PaaS Self adaptation manager", ex);
        }
    }

    /**
     * This allows the decision engine to be set Decision engines are used to
     * decide the scale and location of an adaptation.
     *
     * @param decisionEngineName The name of the algorithm to set
     */
    public final void setDecisionEngine(String decisionEngineName) {
        try {
            if (!decisionEngineName.startsWith(DEFAULT_DECISION_ENGINE_PACKAGE)) {
                decisionEngineName = DEFAULT_DECISION_ENGINE_PACKAGE + "." + decisionEngineName;
            }
            decisionEngine = (DecisionEngine) (Class.forName(decisionEngineName).newInstance());
        } catch (ClassNotFoundException ex) {
            if (decisionEngine == null) {
                decisionEngine = new RandomDecisionEngine();
            }
            Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, "The decision engine specified was not found");
        } catch (InstantiationException | IllegalAccessException ex) {
            if (decisionEngine == null) {
                decisionEngine = new RandomDecisionEngine();
            }
            Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, "The setting of the decision engine did not work", ex);
        }
        decisionEngine.setActuator(actuator);
    }

    @Override
    public Response assessEvent(EventData event) {
        //Add the current event into the sequence of all events.
        List<EventData> eventHistoryToAssess = new ArrayList<>();
        synchronized (this) { 
            eventHistory.add(event);
            eventHistoryToAssess.addAll(eventHistory);
        }
        if (logging) {
            eventHistoryLogger.printToFile(event);
        }
        //filter event sequence for only relevent data    
        List<EventData> eventData = EventDataAggregator.filterEventData(eventHistoryToAssess, event.getGuaranteeid(), event.getAgreementTerm());
        //Purge old event map data
        eventData = EventDataAggregator.filterEventDataByTime(eventData, historyLengthSeconds);
        return assessEvent(event, eventData);
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
    protected Response assessEvent(EventData event, List<EventData> eventData) {
        synchronized (this) {
            if (actuator != null && event instanceof ApplicationEventData) {
                ApplicationEventData appEvent = (ApplicationEventData) event;
                appEvent.setApplicationDefinition(actuator.getApplication(appEvent.getApplicationId(), appEvent.getDeploymentId()));
            }          
            Response answer = assessEvent(event, eventData, adaptations);
            if (answer != null) {
                adaptations.add(answer);
                answer = decisionEngine.decide(answer);
                if (actuator != null && answer.isPossibleToAdapt()) {
                    actuator.actuate(answer);
                    Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, "Actuator - Performing Work");
                    Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, answer.toString());
                }
                if (logging) {
                    responseHistoryLogger.printToFile(answer);
                }
            }
            /**
             * This causes a looping behaviour when the action is not possible
             * to carry out. The test at the end ensures that if the history is
             * wiped out the loop is not infinite.
             */
            if (answer != null && !answer.isPossibleToAdapt() && historyLengthSeconds > 30) {
                Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, "Performing another try at assessing the event.");
                assessEvent(event, eventData);
            }
            return answer;
        }
    }

    /**
     * This allows the ability to record adaptations that haven't been performed
     * by this event assessor. It thus prevents the event assessor overturning a
     * change made by another soon after the change has occurred.
     *
     * @param response The response to add into the modeller's history.
     */
    @Override
    public synchronized void addRemoteAdaptationEvent(Response response) {
        adaptations.add(response);
        Collections.sort(adaptations);
    }

    /**
     * This gets the event assessors internal list of event listeners
     *
     * @return the list of event listeners the event assessor uses
     */
    @Override
    public ArrayList<EventListener> getListeners() {
        return listeners;
    }

    /**
     * This sets the event assessors internal list of event listeners
     *
     * @param listeners the listeners to set
     */
    @Override
    public void setListeners(ArrayList<EventListener> listeners) {
        this.listeners = listeners;
        for (EventListener listener : listeners) {
            listener.setEventAssessor(this);
            if (listener instanceof Runnable && !listener.isListening()) {
                Thread thread = new Thread((Runnable) listener);
                thread.start();
            }
        }
    }

    /**
     * This adds a listener to the event assessors internal list of event
     * listeners
     *
     * @param listener The listener to add
     */
    @Override
    public void addListeners(EventListener listener) {
        listeners.add(listener);
        listener.setEventAssessor(this);
        if (listener instanceof Runnable) {
            Thread thread = new Thread((Runnable) listener);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * This clears the event assessors internal list of event listeners
     */
    @Override
    public void clearListeners() {
        for (EventListener listener : listeners) {
            listener.stopListening();
        }
        listeners.clear();
    }

    /**
     * This gets the event assessors actuator
     *
     * @return the actuator the event assessor uses
     */
    @Override
    public ActuatorInvoker getActuator() {
        return actuator;
    }

    /**
     * This sets the event assessors actuator
     *
     * @param actuator the actuators to set
     */
    @Override
    public void setActuator(ActuatorInvoker actuator) {
        this.actuator = actuator;
        decisionEngine.setActuator(actuator);
        if (this.actuator instanceof Runnable) {
            Thread actuatorThread = new Thread((Runnable) this.actuator);
            actuatorThread.setDaemon(true);
            actuatorThread.start();
        }
    }

    /**
     * This deletes the event assessors actuator
     */
    @Override
    public void deleteActuator() {
        actuator = null;
    }

    /**
     * This starts the event history maintenance routines in the event assessor.
     */
    @Override
    public void start() {
        historyClearer = new HistoryClearer();
        historyClearerThread = new Thread(historyClearer);
        historyClearerThread.setDaemon(true);
        historyClearerThread.start();
    }

    /**
     * This stops the event history maintenance routines in the event assessor.
     */
    @Override
    public void stop() {
        if (historyClearer != null) {
            historyClearer.stop();
            historyClearer = null;
            historyClearerThread = null;
        }
    }

    /**
     * The history clearer prunes the sequence of events of old redundant data.
     */
    private class HistoryClearer implements Runnable {

        private boolean running = true;

        /**
         * This makes the history clearer go through the historic event list and
         * prune values as required.
         */
        @Override
        @SuppressWarnings("SleepWhileInLoop")
        public void run() {
            while (running) {
                if (!eventHistory.isEmpty()) {
                    eventHistory = EventDataAggregator.filterEventDataByTime(eventHistory, historyLengthSeconds);
                }
                if (!adaptations.isEmpty()) {
                    synchronized (this) {
                        adaptations = filterAdaptationHistory();
                    }
                }
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(pollInterval));
                } catch (InterruptedException ex) {
                    Logger.getLogger(HistoryClearer.class.getName()).log(Level.WARNING, "History Cleaner: InterruptedException", ex);
                }
            } //While not stopped
        }

        /**
         * This filters the current adaptation history and return a new list
         * that has all of the old entries removed.
         *
         * @return The list of recent adaptations made by the event assessor.
         */
        private List<Response> filterAdaptationHistory() {
            ArrayList<Response> answer = new ArrayList<>();
            if (historyLengthSeconds == 0) {
                //Ensure automatic removal of all previous history records, without further testing.
                return answer;
            }
            long now = System.currentTimeMillis();
            now = now / 1000;
            long filterTime = now - historyLengthSeconds;
            synchronized (this) {
                for (Response response : adaptations) {
                    if (response.getTime() >= filterTime) {
                        answer.add(response);
                    }
                }
                if (answer.size() != adaptations.size()) {
                    Logger.getLogger(HistoryClearer.class.getName()).log(Level.INFO, "Cleaning History Performed: Old size: {0} New Size: {1}", new Object[]{adaptations.size(), answer.size()});
                }
            }
            return answer;
        }

        /**
         * This stops the history clearer thread.
         */
        private void stop() {
            running = false;
        }
    }
}
