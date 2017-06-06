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

import java.util.ArrayList;

/**
 * This class represents the set of all QoS criteria to be monitored.
 * @author Richard Kavanagh
 */
public class SLALimits {
    
    private static String POWER = "POWER";
    private ArrayList<SLATerm> qosCriteria = new ArrayList<>();

    public SLALimits() {
    }

    public ArrayList<SLATerm> getQosCriteria() {
        return qosCriteria;
    }
    
    public void addQoSCriteria(SLATerm criteria) {
        qosCriteria.add(criteria);
    }
    
    public void removeQoSCriteria(SLATerm criteria) {
        qosCriteria.remove(criteria);
    }
    
    /**
     * @return the power
     */
    public Double getPower() {
        double power = 0;
        for (SLATerm qosCriteria1 : qosCriteria) {
            if (qosCriteria1.getAgreementTerm().equals(POWER)) {
                return qosCriteria1.getGuranteedValue();
            }
        }
        return power;
    }

    /**
     * @param power the power to set
     */
    public void setPower(double power) {
        /**
         * TODO note: this only changes current power qos goal, it can't be used
         * to set the goal outright.
         */
        for (SLATerm qosCriteria1 : qosCriteria) {
            if (qosCriteria1.getAgreementTerm().equals(POWER)) {
                qosCriteria1.setGuranteedValue(power);
            }
        }
    }
    
}
