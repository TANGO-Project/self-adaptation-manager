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
package eu.tango.self.adaptation.manager.model;

import eu.tango.self.adaptation.manager.rules.datatypes.EventData;

/**
 * This represents an SLA term.
 * @author Richard Kavanagh
 */
public class SLATerm {

    private String guaranteeid; //sla gurantee id      
    private double guranteedValue; //the guranteed value
    private EventData.Type severity; //breach, warning or other (i.e. informative)
    private EventData.Operator guranteeOperator; // threshold direction
    private String agreementTerm; //The identifier of the Term i.e. Power, energy etc

    /**
     * @return the guaranteeid
     */
    public String getGuaranteeid() {
        return guaranteeid;
    }

    /**
     * @param guaranteeid the guaranteeid to set
     */
    public void setGuaranteeid(String guaranteeid) {
        this.guaranteeid = guaranteeid;
    }

    /**
     * @return the guranteedValue
     */
    public double getGuranteedValue() {
        return guranteedValue;
    }

    /**
     * @param guranteedValue the guranteedValue to set
     */
    public void setGuranteedValue(double guranteedValue) {
        this.guranteedValue = guranteedValue;
    }

    /**
     * @return the severity
     */
    public EventData.Type getSeverity() {
        return severity;
    }

    /**
     * @param severity the severity to set
     */
    public void setSeverity(EventData.Type severity) {
        this.severity = severity;
    }

    /**
     * @return the guranteeOperator
     */
    public EventData.Operator getGuranteeOperator() {
        return guranteeOperator;
    }

    /**
     * @param guranteeOperator the guranteeOperator to set
     */
    public void setGuranteeOperator(EventData.Operator guranteeOperator) {
        this.guranteeOperator = guranteeOperator;
    }

    /**
     * @return the agreementTerm
     */
    public String getAgreementTerm() {
        return agreementTerm;
    }

    /**
     * @param agreementTerm the agreementTerm to set
     */
    public void setAgreementTerm(String agreementTerm) {
        this.agreementTerm = agreementTerm;
    }

}
