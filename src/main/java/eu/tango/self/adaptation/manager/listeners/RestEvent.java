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
package eu.tango.self.adaptation.manager.listeners;

import javax.xml.bind.annotation.XmlElement;

/**
 * This represents an event pushed to the SAM via the REST interface
 *
 * @author Richard Kavanagh
 */
public class RestEvent {

    @XmlElement
    private String origin; //application, host or clock
    @XmlElement
    private double rawValue; //the metric raw value
    @XmlElement
    private double guaranteedValue; //the guaranteed value
    @XmlElement
    private String type; //breach, warning or other (i.e. informative)
    @XmlElement
    private String guaranteeOperator; // threshold direction
    @XmlElement
    private String agreementTerm;
    @XmlElement
    private String guaranteeid; //sla guarantee id
    @XmlElement
    private String hostname;
    @XmlElement
    private String applicationId;
    @XmlElement
    private String deploymentId;

    /**
     * @return the origin
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * @return the rawValue
     */
    public double getRawValue() {
        return rawValue;
    }

    /**
     * @param rawValue the rawValue to set
     */
    public void setRawValue(double rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * @return the guaranteedValue
     */
    public double getGuaranteedValue() {
        return guaranteedValue;
    }

    /**
     * @param guaranteedValue the guaranteedValue to set
     */
    public void setGuaranteedValue(double guaranteedValue) {
        this.guaranteedValue = guaranteedValue;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the guaranteeOperator
     */
    public String getGuaranteeOperator() {
        return guaranteeOperator;
    }

    /**
     * @param guaranteeOperator the guaranteeOperator to set
     */
    public void setGuaranteeOperator(String guaranteeOperator) {
        this.guaranteeOperator = guaranteeOperator;
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
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return the applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * @param applicationId the applicationId to set
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * @return the deploymentId
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * @param deploymentId the deploymentId to set
     */
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public String toString() {
        String answer = " origin :" + origin
                + " rawValue :" + rawValue
                + " guaranteedValue :" + guaranteedValue
                + " type :" + type
                + " guaranteeOperator :" + guaranteeOperator
                + " agreementTerm :" + agreementTerm
                + " guaranteeid :" + guaranteeid
                + " hostname :" + hostname
                + " applicationId :" + applicationId
                + " deploymentId :" + deploymentId;
        return answer;
    }

}
