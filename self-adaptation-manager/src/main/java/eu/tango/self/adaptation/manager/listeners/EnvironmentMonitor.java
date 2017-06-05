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
 */
package eu.tango.self.adaptation.manager.listeners;

import eu.tango.energymodeller.datasourceclient.CollectdDataSourceAdaptor;
import eu.tango.energymodeller.datasourceclient.HostDataSource;
import eu.tango.energymodeller.datasourceclient.HostMeasurement;
import eu.tango.energymodeller.datasourceclient.Measurement;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import java.util.HashMap;
import java.util.List;
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
public class EnvironmentMonitor implements EventListener, Dispatcher, Runnable {

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
                //Compare them to the data source
                //Invoke adapation as needed.
                List<HostMeasurement> measurements = datasource.getHostData();
                eventAssessor.assessEvent(null);
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
     * @param measurement The incoming event to convert into the self-adaptation
     * managers internal representation
     * @return The converted internal representation of events
     * @see eu.ascetic.paas.slam.pac.events.ViolationMessage
     */
    public static EventData convertEventData(Measurement measurement, SLALimits criteria) {
        EventData answer = new EventData();
        /**
         * TODO Fix the measurement contains no concept of breach, this already needs to exist
         * for a conversion method to work.
         */
        /**
         * Any new event may be either system orientated or application oriented
         * this will depend on the source of the event
         */
//        answer.setApplicationId(measurement.getAppId());
//        answer.setDeploymentId(measurement.getDeploymentId());
        
        answer.setTime(measurement.getClock());
        long now = System.currentTimeMillis();
        now = now / 1000;
        if (answer.getTime() > now) {
            answer.setTime(now);
        }
        //TODO May not be the 0th SLA Term FIX HERE
        if (criteria.getQosCriteria().get(0).getSeverity().equals("violation")) {
            answer.setType(EventData.Type.SLA_BREACH);
        } else if (criteria.getQosCriteria().get(0).getSeverity().equals("warning")) {
            answer.setType(EventData.Type.WARNING);
        } else {
            answer.setType(EventData.Type.OTHER);
        }
//        answer.setRawValue(Double.parseDouble(measurement.getValue().getTextValue()));
//        answer.setGuranteedValue(measurement.getAlert().getSlaGuaranteedState().getGuaranteedValue());
//        answer.setGuranteeOperator(getOperator(measurement));
//
//        answer.setSlaUuid(measurement.getAlert().getSlaUUID());
//        answer.setGuaranteeid(measurement.getAlert().getSlaGuaranteedState().getGuaranteedId());
//        answer.setAgreementTerm(measurement.getAlert().getSlaAgreementTerm());
        return answer;

    }
    
    /**
     * @param notification The incoming event to convert into the self-adaptation
     * managers internal representation
     * @return The converted internal representation of events
     * @see eu.ascetic.paas.slam.pac.events.ViolationMessage
     */
    public static EventData convertEventData(Notification notification, SLALimits criteria) {
        EventData answer = new EventData();
        /**
         * TODO Fix the measurement contains no concept of breach, this already needs to exist
         * for a conversion method to work.
         */
        /**
         * Any new event may be either system orientated or application oriented
         * this will depend on the source of the event
         */
//        answer.setApplicationId(measurement.getAppId());
//        answer.setDeploymentId(measurement.getDeploymentId());
        
        answer.setTime(notification.getTime());
        long now = System.currentTimeMillis();
        now = now / 1000;
        if (answer.getTime() > now) {
            answer.setTime(now);
        }
        //TODO May not be the 0th SLA Term FIX HERE
        /**
         *Options available@ FAILURE(1), WARNING(2), UNKNOWN(3), OKAY(4);
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
        throw new UnsupportedOperationException("Not supported yet.");
        /**
         * The main element of this is to listen for notifications of CollectD threshold limits.
         */
    }

    @Override
    public void dispatch(Notification ntfctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
