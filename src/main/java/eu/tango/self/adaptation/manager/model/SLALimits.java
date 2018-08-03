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
package eu.tango.self.adaptation.manager.model;

import eu.ascetic.ioutils.io.ResultsStore;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents the set of all QoS criteria to be monitored.
 *
 * @author Richard Kavanagh
 */
public class SLALimits {

    private static final String APP_POWER = "APP_POWER";
    private static final String TOTAL_POWER = "TOTAL_POWER"; //for the cluster
    private static final String HOST_POWER = "HOST_POWER";
    private final ArrayList<SLATerm> qosCriteria = new ArrayList<>();

    public SLALimits() {
    }

    public ArrayList<SLATerm> getQosCriteria() {
        return qosCriteria;
    }

    public void addQoSCriteria(SLATerm criteria) {
        qosCriteria.add(criteria);
    }
    
    public void addQoSCriteria(Collection<SLATerm> criteria) {
        if (criteria != null) {
            qosCriteria.addAll(criteria);
        }
    }    

    public void removeQoSCriteria(SLATerm criteria) {
        qosCriteria.remove(criteria);
    }

    /**
     * This gets the power limit for individual applications
     * @return the power
     */
    public Double getAppPower() {
        //TODO check semantics of this in use, per application??
        double power = 0;
        for (SLATerm qosCriteria1 : qosCriteria) {
            //TODO consider named application??
            if (qosCriteria1.getAgreementTerm().contains(APP_POWER)) {
                return qosCriteria1.getGuaranteedValue();
            }
        }
        return power;
    }

    /**
     * This gets the power limit for individual physical hosts
     * @return the power
     */
    public Double getHostPower() {
        double power = 0;
        for (SLATerm qosCriteria1 : qosCriteria) {
            //TODO consider named host??
            if (qosCriteria1.getAgreementTerm().contains(HOST_POWER)) {
                return qosCriteria1.getGuaranteedValue();
            }
        }
        return power;
    }    
    
    /**
     * This gets the power limit across all physical hosts
     * @return the power
     */
    public Double getTotalPower() {
        double power = 0;
        for (SLATerm qosCriteria1 : qosCriteria) {
            if (qosCriteria1.getAgreementTerm().equals(TOTAL_POWER)) {
                return qosCriteria1.getGuaranteedValue();
            }
        }
        return power;
    }       

    /**
     * This performs a check to see if the settings file is empty or not. It
     * will write out a blank file if the file is not present.
     *
     * @param rulesFile The list of rules on disk to load
     * @return If the defaults settings have been written out to disk or not.
     */
    public static boolean writeOutDefaults(ResultsStore rulesFile) {
        boolean answer = false;
        //Agreement Term, Guarantee Direction and Response Type
        if (!rulesFile.getResultsFile().exists()) {
            rulesFile.add("Unique Id"); //0
            rulesFile.append("Agreement Term"); //1
            rulesFile.append("Comparator"); //2
            rulesFile.append("Event Type (SLA_BREACH or WARNING)"); //3
            rulesFile.append("Guarantee Value"); //4
            rulesFile.save();
            answer = true;
        }
        return answer;
    }

    /**
     * This loads a set of rules in from disk.
     *
     * @param file The name of the file to load the SLA rules in from disk.
     * @return The list of rules to be used.
     */
    public static SLALimits loadFromDisk(String file) {
        SLALimits answer = new SLALimits();
        Logger.getLogger(SLALimits.class.getName()).log(Level.INFO, "Loading default rules for self adaptation");
        /**
         * Load in from file the following: Agreement Term, Guarantee Direction
         * and Response Type
         */
        ResultsStore rulesFile = new ResultsStore(file);
        writeOutDefaults(rulesFile);
        rulesFile.load();
        Logger.getLogger(SLALimits.class.getName()).log(Level.INFO, "There are {0} to load.", rulesFile.size());
        //ignore the header of the file
        for (int i = 1; i < rulesFile.size(); i++) {
            ArrayList<String> current = rulesFile.getRow(i);
            SLATerm rule = new SLATerm(current.get(0), Double.parseDouble(current.get(4)), EventData.getType(current.get(3)), EventData.Operator.valueOf(current.get(2)), current.get(1));
            String logString = "Unique Id: " + current.get(0) + " Term: " + current.get(1) + " Comparator: " + current.get(2) + " Severity: " + current.get(3) + " Guarantee Value " + current.get(4);
            try {
                if (current.size() >= 4 && !current.get(3).isEmpty()) {
                    rule.setSeverity(EventData.Type.valueOf(current.get(3)));
                    logString = logString + " Type: " + current.get(3);
                }
            } catch (IllegalArgumentException ex) {
                if (current.get(0) != null) {
                    Logger.getLogger(SLALimits.class.getName()).log(Level.WARNING, "Error Parsing Rule: {0}", current.get(0));
                } else {
                    Logger.getLogger(SLALimits.class.getName()).log(Level.WARNING, "Error Parsing a Rule", current.get(0));
                }
                /**
                 * If the event type was not recognised then ignore it. This
                 * therefore leaves this to be an optional value.
                 */
            }
            answer.addQoSCriteria(rule);
            Logger.getLogger(SLALimits.class.getName()).log(Level.WARNING, "Adding Rule from disk: {0}", logString);
        }
        return answer;
    }
    
    /**
     * This obtains all terms for a set of SLA limits that are for a named application.
     * @param toFilter The SLA Limits to filter
     * @param applicationName The application to filter against
     * @return The SLA limits filtered to only include an applications specific rules.
     */
    public static SLALimits filterTerms(SLALimits toFilter, String applicationName) {
        SLALimits answer = new SLALimits();
        for(SLATerm term : toFilter.getQosCriteria()) {
            if (term.getAgreementTerm().contains("APP:") && term.getAgreementTerm().contains(applicationName)) {
                answer.addQoSCriteria(term);
            }
        }
        return answer;           
    }

}
