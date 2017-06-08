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
         * This is an example of the output of the notification element.
         * Host: VM10-10-1-13
         * Severity: FAILURE
         * Data: Host VM10-10-1-13, plugin aggregation (instance cpu-average) 
         *          type cpu (instance idle): Data source "value" is currently nan. 
         *          That is within the failure region of 0.000000 and 12000.000000.
         * Message: Host VM10-10-1-13, plugin aggregation 
         *          (instance cpu-average) type cpu (instance idle): Data source 
         *          "value" is currently nan. That is within the failure 
         *          region of 0.000000 and 12000.000000.
         * Plugin: aggregation
         * Plugin Instance: cpu-average
         * Source: VM10-10-1-13/aggregation/cpu-average/cpu/idle
         * Type: cpu
         * Type Instance: idle
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
        //TODO Set these values here, need to parse the data element.
        double rawValue = 0.0;
        double guranteedValue = 0.0;
        answer.setRawValue(rawValue);
        answer.setGuranteedValue(guranteedValue);
        answer.setGuranteeOperator(getOperator(notification));

        answer.setGuaranteeid(notification.getSource());
        answer.setAgreementTerm(notification.getMessage());
        return answer;

    }

    /**
     * This converts a violation messages guarantee state's operator into an
     * event type operator.
     *
     * @param notification The event type to convert.
     * @return The enumerated type for the operator
     */
    public static EventData.Operator getOperator(Notification notification) {
        //TODO find the terms that setup the notification event
//        return OPERATOR_MAPPING.get(notification.getOperator());
        return EventData.Operator.LT;
    }

    @Override
    public void dispatch(Notification ntfctn) {
        EventData data = convertEventData(ntfctn);
        eventAssessor.assessEvent(data);

    }

}
