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
package eu.tango.self.adaptation.manager.actuators;

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.util.List;

/**
 * This actuator interacts with the ALDE, with the aim of querying for information
 * and invoking adaptation.
 * @author Richard Kavanagh
 */
public class AldeActuator implements ActuatorInvoker {

    @Override
    public ApplicationDefinition getApplication(String name, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ApplicationOnHost getTask(String name, String deployment, String taskId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getTotalPowerUsage(String applicationName, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getPowerUsageTask(String applicationName, String deploymentId, String taskId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getAveragePowerUsage(String applicationName, String deploymentId, String taskType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getTaskTypesAvailableToAdd(String applicationName, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Integer> getTaskIdsAvailableToRemove(String applicationName, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void hardShutdown(String applicationName, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void actuate(Response response) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addTask(String applicationName, String deploymentId, String taskType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteTask(String applicationName, String deployment, String taskID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}