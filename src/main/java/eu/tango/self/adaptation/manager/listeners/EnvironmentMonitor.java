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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.Severity;
import org.jcollectd.agent.api.Notification;
import org.jcollectd.agent.api.Values;
import org.jcollectd.agent.protocol.Dispatcher;

/**
 * This creates an environment monitor that connects directly into CollectD via
 * its network plugin.
 *
 * @author Richard Kavanagh
 */
public class EnvironmentMonitor implements EventListener, Dispatcher, Runnable, CollectDNotificationHandler {

    private EventAssessor eventAssessor;
    private HostDataSource datasource = new CollectdDataSourceAdaptor();
    private boolean running = true;

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
                //TODO Implement here
                //GET LIST OF Terms to monitor here
                SLALimits limits = null;
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
     * This takes a list of measurements and determines if an SLA breach has occured
     * by comparing them to the QoS limits.
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
                return new EventData(measurement.getClock(), currentValue, term.getGuranteedValue(), EventData.Type.SLA_BREACH, term.getGuranteeOperator(), "", "", term.getGuaranteeid(), term.getAgreementTerm());
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
        EventData answer = new EventData();
        /**
         * TODO Fix commented out code sections Any new event may be either
         * system orientated or application oriented this will depend on the
         * source of the event
         */
//        answer.setApplicationId(measurement.getAppId());
//        answer.setDeploymentId(measurement.getDeploymentId());

        answer.setTime(notification.getTime() >> 30);
        long now = System.currentTimeMillis();
        if (answer.getTime() > now) {
            answer.setTime(now);
        }
        //TODO May not be the 0th SLA Term FIX HERE
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
        answer.setRawValue(Double.parseDouble(notification.getData()));
//        answer.setGuranteedValue(notification.getAlert().getSlaGuaranteedState().getGuaranteedValue());
        answer.setGuranteeOperator(getOperator(notification));

//        answer.setSlaUuid(notification.getAlert().getSlaUUID());
//        answer.setGuaranteeid(notification.getAlert().getSlaGuaranteedState().getGuaranteedId());
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
    public void dispatch(Values values) {
        /**
         * The main element of this is to listen for notifications of CollectD
         * threshold limits. Therefore this is not listened to and values
         * arriving are ignored.
         * 
         * Consider route i.e. through adaptor or through this mechanism here. 
         */
    }

    @Override
    public void dispatch(Notification ntfctn) {
        EventData data = convertEventData(ntfctn);
        eventAssessor.assessEvent(data);

    }

}
