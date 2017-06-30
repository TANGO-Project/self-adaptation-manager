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
import eu.tango.self.adaptation.manager.qos.SlaRulesLoader;
import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import eu.ascetic.ioutils.io.ResultsStore;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcollectd.agent.api.Notification.Severity;
import org.jcollectd.agent.api.Notification;

/**
 * This creates an environment monitor that connects directly into CollectD via
 * its network plug-in.
 *
 * @author Richard Kavanagh
 */
public class EnvironmentMonitor implements EventListener, Runnable, CollectDNotificationHandler {

    private EventAssessor eventAssessor;
    private final HostDataSource datasource;
    private boolean running = true;
    private SLALimits limits;

    /**
     * Instantiates the Environment monitor with the default CollectD data
     * source.
     */
    public EnvironmentMonitor() {
        datasource = new CollectdDataSourceAdaptor();
        initialise();
    }

    /**
     * Instantiates with a different data source to CollectD.
     *
     * @param datasource
     */
    public EnvironmentMonitor(HostDataSource datasource) {
        this.datasource = datasource;
        initialise();
    }

    /**
     * Initialises the environment monitor, in particular if a collectd data
     * source is used it allows for the listening to events from its monitoring
     * infrastructure.
     */
    private void initialise() {
        if (datasource instanceof CollectdDataSourceAdaptor) {
            ((CollectdDataSourceAdaptor) datasource).setNotificationHandler(this);
        }
        SlaRulesLoader loader = new SlaRulesLoader();
        limits = loader.getLimits();
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
     * This reloads the SLA criteria held in the environment monitor.
     */
    public void reloadLimits() {
        SlaRulesLoader loader = new SlaRulesLoader();
        limits = loader.getLimits();
    }

    /**
     * This starts the environment monitor going, in a daemon thread.
     */
    @Override
    public void startListening() {
        Thread envMonThread = new Thread(this);
        envMonThread.setDaemon(true);
        envMonThread.start();
    }

    @Override
    public void run() {
        try {
            printRecognisedTerms();//This provides guidance on how to create detection rules.
            // Wait for a message
            while (running) {
                for (EventData event : detectEvent(limits)) {
                    eventAssessor.assessEvent(event);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(EnvironmentMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(EnvironmentMonitor.class.getName()).log(Level.SEVERE, null, ex);
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
    private ArrayList<EventData> detectEvent(SLALimits limits) {
        ArrayList<EventData> answer = new ArrayList<>();
        ArrayList<SLATerm> criteria = limits.getQosCriteria();
        for (SLATerm term : criteria) {
            /**
             * Structure assumed to be: HOST:ns32:power or HOST:ALL:power
             * Otherwise it simply matches against the agreement term for all
             * hosts
             */
            if (term.getAgreementTerm().contains("HOST:")) {
                String[] termStr = term.getSplitAgreementTerm();
                if (termStr.length != 3) {
                    Logger.getLogger(EnvironmentMonitor.class.getName()).log(Level.SEVERE, "A Rule parse error occured");
                    continue;
                }
                String agreementTerm = termStr[2];
                Host host = datasource.getHostByName(termStr[1]);
                EventData event = detectEvent(term, agreementTerm, host);
                if (event != null) {
                    answer.add(event);
                }
            } else {
                answer.addAll(detectEvent(term, datasource.getHostList()));
            }
        }
        return answer;
    }

    /**
     * Detects any QoS term breaches
     * @param term The sla term to check against
     * @param hosts The list of hosts to test for a breach of SLA criteria
     * @return  The list of SLA breach events, the empty list is returned if
     * no breach occurs.
     */
    private ArrayList<EventData> detectEvent(SLATerm term, List<Host> hosts) {
        ArrayList<EventData> answer = new ArrayList<>();
        for (Host host : hosts) {
            EventData event = detectEvent(term, term.getAgreementTerm(), host);
            if (event != null) {
                answer.add(event);
            }
        }
        return answer;
    }

    /**
     * Detects any QoS term breaches
     * @param term The sla term to check against
     * @param agreementTerm The parsed string for the agreement term
     * @param host The host to test for a breach of SLA criteria
     * @return The SLA breach events if it occurs, otherwise null
     */
    private HostEventData detectEvent(SLATerm term, String agreementTerm, Host host) {
        if (host == null) {
            return null;
        }
        HostMeasurement measurement = datasource.getHostData(host);

        if (measurement.getMetric(agreementTerm) == null) {
            /**
             * Check the metric term exists, it may be that another monitor
             * reads the file and uses special terms such as: IDLE_HOST"
             * "APP_FINISHED" "IDLE_HOST+PENDING_JOB" CLOSE_TO_DEADLINE" or
             * simply the data source isn't providing the information needed.
             */
            return null;
        }
        double currentValue = measurement.getMetric(agreementTerm).getValue();
        if (term.isBreached(currentValue)) {
            return new HostEventData(measurement.getClock(), host.getHostName(),
                    currentValue, term.getGuranteedValue(),
                    term.getSeverity(),
                    term.getGuranteeOperator(),
                    term.getGuaranteeid(),
                    term.getAgreementTerm());
        }
        return null;
    }

    /**
     * This prints out a list of hosts and metric terms, it therefore aids the
     * user in writing a file that lists the detection and adaptation rules.
     */
    private void printRecognisedTerms() {
        //Wait for the environment to catch up before printing.
        try {
            Thread.sleep(1000);

        } catch (InterruptedException ex) {
            Logger.getLogger(EnvironmentMonitor.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        HashSet<String> namesList = new HashSet<>();
        ResultsStore store = new ResultsStore("RecognisedTerms.csv");
        List<HostMeasurement> data = datasource.getHostData();
        for (HostMeasurement item : data) {
            namesList.addAll(item.getMetricNameList());
        }

        store.add("This lists all recognised agreement terms, thus helping "
                + "to list rules for detecting term breach events.");
        store.add("The agreement terms for a specific host begin in the format HOST:<name>:<term>");
        store.add("");
        store.add("A list of rules for detection criteria look like:");
        store.add("Unique Id,Agreement Term,Comparator,Event Type (SLA_BREACH or WARNING),Guarantee Value");
        store.add("1,A term below in the provided format,EQ,SLA_BREACH,0");
        store.add("");
        store.add("The is the list of known hosts. This gets added HOST:<To be added here>:<term>");
        for (Host host : datasource.getHostList()) {
            store.add(host.getHostName());
        }
        store.add("");
        store.add("This is the list of detected metrics from the host: HOST:<hostname>:<To be added here>");
        for (String termname : namesList) {
            store.add(termname);
        }
        store.save();
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
        /**
         * Second Example:
         *
         * org.jcollectd.agent.api.Notification@264ab70a [FAILURE] Host
         * VM10-10-1-13, plugin aggregation (instance cpu-average) type cpu
         * (instance user): Data source "value" is currently 0.500090. That is
         * above the failure threshold of 0.000000. Host: VM10-10-1-13 Severity:
         * FAILURE Data: Host VM10-10-1-13, plugin aggregation (instance
         * cpu-average) type cpu (instance user): Data source "value" is
         * currently 0.500090. That is above the failure threshold of 0.000000.
         * Message: Host VM10-10-1-13, plugin aggregation (instance cpu-average)
         * type cpu (instance user): Data source "value" is currently 0.500090.
         * That is above the failure threshold of 0.000000. Plugin: aggregation
         * Plugin Instance: cpu-average Source:
         * VM10-10-1-13/aggregation/cpu-average/cpu/user Type: cpu Type
         * Instance: user
         *
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
        if (notification.getSeverity().equals(Severity.FAILURE)) {
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
