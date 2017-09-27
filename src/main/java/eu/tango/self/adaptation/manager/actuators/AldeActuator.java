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
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 * This actuator interacts with the ALDE, with the aim of querying for
 * information and invoking adaptation.
 *
 * @author Richard Kavanagh
 */
public class AldeActuator extends AbstractActuator {

    AldeClient client = new AldeClient();

    /**
     * This executes a given action for a response that has been placed in the
     * actuator's queue for deployment.
     *
     * @param response The response object to launch the action for
     */
    @Override
    protected void launchAction(Response response) {
        if (response.getCause() instanceof ApplicationEventData) {
            /**
             * This checks to see if application based events have the necessary
             * information to perform the adaptation.
             */
            if (response.getDeploymentId() == null || response.getDeploymentId().isEmpty()) {
                response.setPerformed(true);
                response.setPossibleToAdapt(false);
                return;
            }
        }
        switch (response.getActionType()) {
            case ADD_TASK:
                addTask(response.getApplicationId(), response.getDeploymentId(), response.getAdaptationDetails());
                break;
            case REMOVE_TASK:
                deleteTask(response.getApplicationId(), response.getDeploymentId(), response.getTaskId());
                break;
            case SCALE_TO_N_TASKS:
                scaleToNTasks(response.getApplicationId(), response.getDeploymentId(), response);
                break;
            case RESELECT_ACCELERATORS:
                reselectAccelerators(response.getApplicationId(), response.getDeploymentId());
                break;
            default:
                Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, "The Response type was not recoginised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }
    
    public ApplicationDefinition reselectAccelerators(String name, String deploymentId) {
        ApplicationDefinition appDef = client.getApplicationDefintion(name);
        ArrayList<JSONObject> currentlyDeployed = client.getDeployments();
        //Need to find a valid configuration
        ArrayList<Map<String,Object>> validConfigurations = getValidConfigurations(appDef, true);
        //and ensure that they haven't been executed as yet
        validConfigurations = removeAlreadyRunningConfigurations(validConfigurations, currentlyDeployed);
        //TODO Launch one of the valid configurations
        //Needs to also delete the current application once the new selection has been made
        //or does it as a proof of completing quickly enough??
        return appDef;
    }
    
    /**
     * This filters out applications that are already deployed and running, assuming 
     * they can't be caught up with by another instance of the same deployment.
     * @param validConfigurations
     * @param currentlyDeployed
     * @return 
     */
    private ArrayList<Map<String,Object>> removeAlreadyRunningConfigurations(ArrayList<Map<String,Object>> validConfigurations, ArrayList<JSONObject> currentlyDeployed) {
        return validConfigurations;
    }

    /**
     * Finds the list of valid configurations that can be launched.
     * @param appDef The application definition
     * @param toRunNow Indicates if additional tests should be performed checking
     * to see if the current environment is suitable
     * @return The list of configurations that can be launched
     */
    private ArrayList<Map<String,Object>> getValidConfigurations(ApplicationDefinition appDef, boolean toRunNow) {
        ArrayList<Map<String,Object>> answer = new ArrayList();
        for (int i = 0; i < appDef.getConfigurationsCount(); i++) {
            //Check to see if the configuration is compiled, if not ignore it
            if (appDef.isConfigurationReady(i)) {
                continue;
            }
            if (!toRunNow) { //check if further tests for current environment are valid to run
                answer.add(appDef.getConfiguration(i));
                continue;
            }
            if (appDef.getNodesNeededByConfiguration(i) > 0) {
            //Test to see if we have enough nodes available
            }
            //Test to see if it needs GPU acceleration
            if (appDef.getCpusNeededPerNodeByConfiguration(i) > 0) {
                //Test to see if nodes have enough cpus for the instance
            }            
            //Test to see if it needs GPU acceleration
            if (appDef.getCpusNeededPerNodeByConfiguration(i) > 0) {
                //Test candidate has GPUs available to run
                //Retest these nodes to see if they have enough cpus as well
            }
            answer.add(appDef.getConfiguration(i));
        }
        return answer;
    }

    @Override
    public ApplicationDefinition getApplication(String name, String deploymentId) {
        ArrayList<ApplicationDefinition> allApps = client.getApplicationDefinitions();
        for (ApplicationDefinition app : allApps) {
            if (app.getName().equals(name)
                    && (app.getDeploymentId().equals(deploymentId)
                    || deploymentId == null || deploymentId.isEmpty())) {
                return app;
            }
        }
        return null;
    }

    @Override
    public List<ApplicationOnHost> getTasksOnHost(String host) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ApplicationOnHost> getTasks() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ApplicationOnHost getTask(String name, String deployment, int taskId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getTotalPowerUsage(String applicationName, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getPowerUsageTask(String applicationName, String deploymentId, int taskId) {
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
    public void hardKillApp(String applicationName, String deploymentId) {
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
