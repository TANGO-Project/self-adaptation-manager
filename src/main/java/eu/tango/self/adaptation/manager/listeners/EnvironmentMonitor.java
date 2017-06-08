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
package eu.tango.self.adaptation.manager.listeners;

import eu.tango.energymodeller.datasourceclient.CollectDNotificationHandler;
import eu.tango.energymodeller.datasourceclient.CollectdDataSourceAdaptor;
import eu.tango.energymodeller.datasourceclient.HostDataSource;
import eu.tango.energymodeller.datasourceclient.HostMeasurement;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.model.SLATerm;
import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.glassfish.jersey.Severity;
import org.jcollectd.agent.api.Notification;

/**
 * This creates an environment monitor that connects directly into CollectD via
 * its network plug-in.
 *
 * @author Richard Kavanagh
 */
public class EnvironmentMonitor implements EventListener, Runnable, CollectDNotificationHandler {

    private EventAssessor eventAssessor;
    private HostDataSource datasource = new CollectdDataSourceAdaptor();
    private boolean running = true;
    private static final String RULES_FILE = "slarules.csv";
    private static final String CONFIG_FILE = "self-adaptation-manager-sla.properties";
    private String workingDir;

    private static final Map<String, EventData.Operator> OPERATOR_MAPPING
            = new HashMap<>();

    static {
        OPERATOR_MAPPING.put("less_than", EventData.Operator.LT);
        OPERATOR_MAPPING.put("LESS", EventData.Operator.LT);
        OPERATOR_MAPPING.put("less_than_or_equals", EventData.Operator.LTE);
        OPERATOR_MAPPING.put("LESS_EQUAL", EventData.Operator.LTE);
        OPERATOR_MAPPING.put("equals", EventData.Operator.EQ);
        OPERATOR_MAPPING.put("EQUALS", EventData.Operator.EQ);
        OPERATOR_MAPPING.put("greater_than", EventData.Operator.GT);
        OPERATOR_MAPPING.put("GREATER", EventData.Operator.GT);
        OPERATOR_MAPPING.put("greater_than_or_equals", EventData.Operator.GTE);
        OPERATOR_MAPPING.put("GREATER_EQUAL", EventData.Operator.GTE);
        OPERATOR_MAPPING.put(null, null);
        OPERATOR_MAPPING.put("", null);
    }

    public EnvironmentMonitor() {
        if (datasource instanceof CollectdDataSourceAdaptor) {
            ((CollectdDataSourceAdaptor) datasource).setNotificationHandler(this);
        }

        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            workingDir = config.getString("self.adaptation.manager.working.directory", ".");
            if (!workingDir.endsWith("/")) {
                workingDir = workingDir + "/";
            }
            config.save();
        } catch (ConfigurationException ex) {
            Logger.getLogger(EnvironmentMonitor.class.getName()).log(Level.INFO, "Error loading the configuration of the Self adaptation manager", ex);
        }
        SLALimits.loadFromDisk(workingDir + RULES_FILE);

    }

    @Override
    public void setEventAssessor(EventAssessor assessor) {
        eventAssessor = assessor;
    }

    @Override
    public EventAssessor getEventAssessor() {
        return eventAssessor;
    }

    @Override
    public void stopListening() {
        running = false;
    }

    /**
     * This starts the environment monitor going, in a daemon thread.
     */
    public void startListening() {
        Thread envMonThread = new Thread(this);
        envMonThread.setDaemon(true);
        envMonThread.start();
    }

    @Override
    public void run() {
        try {
            // Wait for a message
            while (running) {
                //Obtains the list of qos parameters to monitor
                SLALimits limits = SLALimits.loadFromDisk(RULES_FILE);
                EventData event = detectBreach(limits);
                eventAssessor.assessEvent(event);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(EnvironmentMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This takes a list of measurements and determines if an SLA breach has
     * occurred by comparing them to the QoS limits.
     *
     * @param measurements The list of measurements
     * @param limits The QoS goal limits.
     * @return The first SLA breach event. Null if none found.
     */
    private EventData detectBreach(SLALimits limits) {
        ArrayList<SLATerm> criteria = limits.getQosCriteria();
        for (SLATerm term : criteria) {
            String[] termStr = term.getSplitAgreementTerm();
            String agreementTerm = termStr[1];
            Host host = datasource.getHostByName(termStr[0]);
            HostMeasurement measurement = datasource.getHostData(host);
            double currentValue = measurement.getMetric(agreementTerm).getValue();
            if (term.isBreached(currentValue)) {
                return new HostEventData(measurement.getClock(), host.getHostName(),
                        currentValue, term.getGuranteedValue(),
                        EventData.Type.SLA_BREACH,
                        term.getGuranteeOperator(),
                        term.getGuaranteeid(),
                        term.getAgreementTerm());
            }
        }
        return null;
    }

    /**
     * @param notification The incoming event to convert into the
     * self-adaptation managers internal representation
     * @return The converted internal representation of events
     * @see eu.ascetic.paas.slam.pac.events.ViolationMessage
     */
    public static EventData convertEventData(Notification notification) {
        /**
         * This is an example of the output of the notification element. Host:
         * VM10-10-1-13 Severity: FAILURE Data: Host VM10-10-1-13, plugin
         * aggregation (instance cpu-average) type cpu (instance idle): Data
         * source "value" is currently nan. That is within the failure region of
         * 0.000000 and 12000.000000. Message: Host VM10-10-1-13, plugin
         * aggregation (instance cpu-average) type cpu (instance idle): Data
         * source "value" is currently nan. That is within the failure region of
         * 0.000000 and 12000.000000. Plugin: aggregation Plugin Instance:
         * cpu-average Source: VM10-10-1-13/aggregation/cpu-average/cpu/idle
         * Type: cpu Type Instance: idle
         */
        HostEventData answer = new HostEventData();
        answer.setHost(notification.getHost());

        answer.setTime(notification.getTime() >> 30);
        long now = System.currentTimeMillis();
        if (answer.getTime() > now) {
            answer.setTime(now);
        }
        /**
         * Options available: FAILURE(1), WARNING(2), UNKNOWN(3), OKAY(4)
         */
        if (notification.getSeverity().equals(Severity.FATAL)) {
            answer.setType(EventData.Type.SLA_BREACH);
        } else if (notification.getSeverity().equals(Severity.WARNING)) {
            answer.setType(EventData.Type.WARNING);
        } else {
            answer.setType(EventData.Type.OTHER);
        }
        answer = setHostEventGuaranteeValues(notification, answer);

        answer.setGuaranteeid(notification.getSource());
        answer.setAgreementTerm(notification.getMessage());
        return answer;

    }

    /**
     * This reads a notification and alters the host event data to match. The
     * focus is to set values for the: Raw measured value The value that was to
     * be guaranteed and the comparison operator for the guarantee i.e. LT, EQ
     * GT
     *
     * @param notification The event type to convert.
     * @param event The host event to alter
     * @return The altered host event
     */
    public static HostEventData setHostEventGuaranteeValues(Notification notification, HostEventData event) {
        /**
         * The string to parse the data from is: Host VM10-10-1-13, plugin
         * aggregation (instance cpu-average) type cpu (instance idle): Data
         * source "value" is currently nan. That is within the failure region of
         * 0.000000 and 12000.000000.
         */
        String data = notification.getData();
        String toParse = data.split(":")[1];
        /**
         * ToParse in the example case is:
         *
         * Data source "value" is currently nan. That is within the failure
         * region of 0.000000 and 12000.000000.
         */
        Scanner scanner = new Scanner(toParse).useDelimiter("[^\\d]+");
        //Indicates if the region is giving a bounds where the value cannot go or not.
        boolean reversed = data.contains("within the failure region");
        double current = scanner.nextDouble();
        event.setRawValue(current); //Sets the measured current value
        double firstBound = scanner.nextDouble();
        if (scanner.hasNextInt()) { //A second number means an upper bound.
            double upperbound = scanner.nextDouble();
            if (reversed) { //case where an exclusion zone is given
                //TODO something clever by considering distance from boundary conditions
                event.setGuranteeOperator(EventData.Operator.GT); //GT Lower bound but also LT upper bound
                event.setGuranteedValue(firstBound);
                return event;
            }
            //Case where a green good zone is given instead
            if (current <= firstBound) {
                event.setGuranteeOperator(EventData.Operator.LT); //LT first bound
                event.setGuranteedValue(firstBound);
                return event;
            } else if (current >= upperbound) {
                event.setGuranteeOperator(EventData.Operator.GT); //GT second bound
                event.setGuranteedValue(upperbound);
                return event;
            }
        } else { //dealing with the simple case of one bound.
            //current/raw value is already set, so setting the boundary condition.
            event.setGuranteedValue(firstBound);
            if (reversed) {
                //Flips values around ensuring correct answer is given
                double temp = current;
                current = firstBound;
                firstBound = temp;
            }

            /**
             * The bounds of LE or LTE are difficult to test, or LTE and EQ thus
             * only LT, EQ and GT can be inferred from a breach with any
             * certainty.
             */
            if (current == firstBound) {
                event.setGuranteeOperator(EventData.Operator.EQ);
                return event;
            } else if (current > firstBound) {
                //current value higher than bound caused breach
                event.setGuranteeOperator(EventData.Operator.LT); //so current value should normally be less than
                return event;
            } else {
                event.setGuranteeOperator(EventData.Operator.GT);
                return event;
            }

        }
        event.setGuranteeOperator(EventData.Operator.LT); //default e.g. current power < guaranteed value
        return event;
    }

    @Override
    public void dispatch(Notification ntfctn) {
        EventData data = convertEventData(ntfctn);
        eventAssessor.assessEvent(data);

    }

}
