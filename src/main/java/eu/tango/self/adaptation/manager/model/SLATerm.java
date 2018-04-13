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

import eu.tango.self.adaptation.manager.rules.datatypes.EventData;

/**
 * This represents an SLA term.
 *
 * @author Richard Kavanagh
 */
public class SLATerm {

    private String guaranteeid; //sla gurantee id      
    private double guaranteedValue; //the guranteed value
    private EventData.Type severity; //breach, warning or other (i.e. informative)
    private EventData.Operator guaranteeOperator; // threshold direction
    private String agreementTerm; //The identifier of the Term i.e. Power, energy etc

    /**
     * @return the guaranteeid
     */
    public String getGuaranteeid() {
        return guaranteeid;
    }

    public SLATerm(String guaranteeid, double guranteedValue, EventData.Type severity, EventData.Operator guaranteeOperator, String agreementTerm) {
        this.guaranteeid = guaranteeid;
        this.guaranteedValue = guranteedValue;
        this.severity = severity;
        this.guaranteeOperator = guaranteeOperator;
        this.agreementTerm = agreementTerm;
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
    public double getGuaranteedValue() {
        return guaranteedValue;
    }

    /**
     * @param guaranteedValue the guranteedValue to set
     */
    public void setGuaranteedValue(double guaranteedValue) {
        this.guaranteedValue = guaranteedValue;
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
     * @return the guaranteeOperator
     */
    public EventData.Operator getGuaranteeOperator() {
        return guaranteeOperator;
    }

    /**
     * @param guaranteeOperator the guaranteeOperator to set
     */
    public void setGuaranteeOperator(EventData.Operator guaranteeOperator) {
        this.guaranteeOperator = guaranteeOperator;
    }

    /**
     * @return the agreementTerm
     */
    public String getAgreementTerm() {
        return agreementTerm;
    }

    /**
     * This splits the agreement term into sections based upon the delimiter :
     *
     * @return the agreementTerm split into sub components
     */
    public String[] getSplitAgreementTerm() {
        return agreementTerm.split(":");
    }

    /**
     * @param agreementTerm the agreementTerm to set
     */
    public void setAgreementTerm(String agreementTerm) {
        this.agreementTerm = agreementTerm;
    }

    /**
     * This indicates if this SLA term has been breached or not.
     *
     * @param currentValue The current measured value for this term.
     * @return If an SLA breach has occurred or not.
     */
    public boolean isBreached(double currentValue) {
        switch (guaranteeOperator) {
            case EQ:
                return currentValue == guaranteedValue;
            case GT:
                return currentValue > guaranteedValue;
            case GTE:
                return currentValue >= guaranteedValue;
            case LT:
                return currentValue < guaranteedValue;
            case LTE:
                return currentValue <= guaranteedValue;
        }
        return false;
    }

}
