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

import eu.tango.energymodeller.datasourceclient.HostDataSource;
import eu.tango.energymodeller.datasourceclient.SlurmDataSourceAdaptor;
import eu.tango.energymodeller.datasourceclient.TangoEnvironmentDataSourceAdaptor;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.self.adaptation.manager.comparator.ConfigurationComparator;
import eu.tango.self.adaptation.manager.comparator.ConfigurationRank;
import eu.tango.self.adaptation.manager.comparator.EnergyComparator;
import eu.tango.self.adaptation.manager.model.ApplicationConfiguration;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.model.ApplicationExecutionInstance;
import eu.tango.self.adaptation.manager.model.Testbed;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This actuator interacts with the ALDE, with the aim of querying for
 * information and invoking adaptation.
 *
 * @author Richard Kavanagh
 */
public class AldeActuator extends AbstractActuator {

    private AldeClient client = new AldeClient();
    private final HostDataSource datasource;    
    private ActuatorInvoker parent = null;

    /**
     * No-args constructor for the alde actuator
     */
    public AldeActuator() {
        datasource = new TangoEnvironmentDataSourceAdaptor();
    }

    public AldeActuator(HostDataSource datasource) {
        if (datasource == null) {
            this.datasource = new TangoEnvironmentDataSourceAdaptor();
            return;
        }
        if (datasource instanceof SlurmDataSourceAdaptor || datasource instanceof TangoEnvironmentDataSourceAdaptor) {
            this.datasource = datasource;
        } else {
            this.datasource = new TangoEnvironmentDataSourceAdaptor();
        }
    }
    /**
     * This sets up a parent actuator for the ALDE. This allows in the case that
     * the ALDE actuator can't perform a particular action to be able to refer 
     * the action to its parent. Thus allowing for a hierarchy of actuators to 
     * be constructed.
     * @param parent The parent actuator of the ALDE actuator.
     */
    public AldeActuator(ActuatorInvoker parent) {
        this.parent = parent;
        datasource = new TangoEnvironmentDataSourceAdaptor();        
    }

    /**
     * This gets the parent actuator of the ALDE if the ALDE actuator is on its 
     * own then this value is null.
     * @return The parent of the ALDE actuator.
     */
    public ActuatorInvoker getParent() {
        return parent;
    }    
    
    /**
     * This sets the parent actuator of the ALDE if the ALDE actuator is on its 
     * own then this value is null.
     * @param parent The parent of the ALDE actuator.
     */
    public void setParent(ActuatorInvoker parent) {
        this.parent = parent;
    } 
    
    /**
     * This executes a given action for a response that has been placed in the
     * actuator's queue for deployment.
     *
     * @param response The response object to launch the action for
     */
    @Override
    protected void launchAction(Response response) {
        if (response.getCause() instanceof ApplicationEventData && !response.hasDeploymentId()) {
            /**
             * This checks to see if application based events have the necessary
             * information to perform the adaptation.
             */
            response.setPerformed(true);
            response.setPossibleToAdapt(false);
            return;
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
    
    /**
     * This takes the accelerators 
     * @param name The name of the application to redeploy
     * @param deploymentId The deployment id of the current application that will be redeployed
     * @return The Application definition of the application
     */
    public ApplicationDefinition reselectAccelerators(String name, String deploymentId) {
        ApplicationConfiguration selectedConfiguration;
        ApplicationConfiguration currentConfiguration = getCurrentConfigurationInUse(name, deploymentId);
        ApplicationDefinition appDef = client.getApplicationDefinition(name);
        if (currentConfiguration == null) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "Current running application instance not found");
            return appDef; //Return without performing any work
        }
        ArrayList<ApplicationExecutionInstance> currentlyRunning = (ArrayList) client.getExecutionInstances();
        //Find a the list of valid configurations
        ArrayList<ApplicationConfiguration> validConfigurations = getValidConfigurations(appDef, true);
        //and ensure that they haven't been executed as yet
        validConfigurations = removeAlreadyRunningConfigurations(validConfigurations, currentlyRunning);
        selectedConfiguration = selectConfiguration(validConfigurations, appDef, currentConfiguration);
        //Ensure the configuration selected is a change/improvement
        if (selectedConfiguration != null && currentConfiguration != selectedConfiguration) {
            Double executionId = (Double) selectedConfiguration.getConfigurationsExecutableId();
            try {
                //Delete the current configuration of the application
                hardKillApp(name, deploymentId); //or does it as a proof of completing quickly enough?
                //Launch the best configuration that can be found (fastest/least energy)
                client.executeApplication(executionId);
            } catch (IOException ex) {
                Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return appDef;
    }
    
    /**
     * This takes a application name and deployment id and determines the 
     * configuration that was used to launch the application
     * @param name The application name
     * @param deploymentId The deployment id of the application
     * @return The application configuration that was used to launch the application
     */
    private ApplicationConfiguration getCurrentConfigurationInUse(String name, String deploymentId) {
        ApplicationDefinition app = client.getApplicationDefinition(name);
        //In cases where there is only 1 configuration for the application
        if (app.getConfigurations().size() == 1) {
            return app.getConfiguration(0);
        }
        for(ApplicationConfiguration config : app.getConfigurations()) {
            if(config.hasExecutionInstance(Integer.parseInt(deploymentId.trim()))) {
                return config;
            }
        }
        return null;
    }
    
    /**
     * This selects from the list of configurations available one that is valid.
     * @param validConfigurations The list of valid configurations to select from
     * @param currentConfiguration The current configuration in use, this acts
     * as a base line to compare all other cases against.
     * @return The configuration that should be launched, else it returns null
     */
    private ApplicationConfiguration selectConfiguration(ArrayList<ApplicationConfiguration> validConfigurations, ApplicationDefinition appDefintion, ApplicationConfiguration currentConfiguration) {
        ConfigurationComparator comparator = new ConfigurationComparator();
        if (validConfigurations.isEmpty()) {
            return null;
        }
        //It there is only one valid configuration return that option
        if (validConfigurations.size() == 1) {
            return validConfigurations.get(0);
        }
        //If there is more than one then select one
        ApplicationConfiguration answer = null;
        ArrayList<ConfigurationRank> ranked = comparator.compare(appDefintion.getName(), currentConfiguration.getConfigurationId() + "");
        ranked.sort(new EnergyComparator()); //Todo Consider making it switch out based upon time instead??
        if (comparator.isBetterThanReference(ranked.get(0).getEnergyUsedVsReference())) {
            return ApplicationConfiguration.selectConfigurationById(validConfigurations, Integer.parseInt(ranked.get(0).getConfigName()));
        }
        //If there is no better solution then return null
        return answer;
    }
    
    /**
     * This filters out applications that are already deployed and running, assuming 
     * they can't be caught up with by another instance of the same deployment.
     * @param validConfigurations The list of configurations that are possible to run
     * @param currentlyRunning The configuration/s that are currently running
     * @return The list of configurations that are deployable and have not as yet
     * been deployed.
     */
    private ArrayList<ApplicationConfiguration> removeAlreadyRunningConfigurations(ArrayList<ApplicationConfiguration> validConfigurations, ArrayList<ApplicationExecutionInstance> currentlyRunning) {
        ArrayList<ApplicationConfiguration> answer = validConfigurations;
        for (ApplicationExecutionInstance current : currentlyRunning) {
            for(ApplicationConfiguration config : validConfigurations) {
                //If the configuration is used by a deployment then filter it out
                //deployments doesn't seem to refer directly to the configuration in use
                if (config.getConfigurationsExecutableId() == current.getExecutionConfigurationsId()) {
                    answer.remove(config);
                }
            }
        }
        return validConfigurations;
    }

    /**
     * Finds the list of valid configurations that can be launched.
     * @param appDef The application definition
     * @param toRunNow Indicates if additional tests should be performed checking
     * to see if the current environment is suitable
     * @return The list of configurations that can be launched
     */
    private ArrayList<ApplicationConfiguration> getValidConfigurations(ApplicationDefinition appDef, boolean toRunNow) {
        //TODO complete the getValidConfigurations method
        ArrayList<ApplicationConfiguration> answer = new ArrayList();
        for (int i = 0; i < appDef.getConfigurationsCount(); i++) {
            ApplicationConfiguration current = appDef.getConfiguration(i);
            //Check to see if the configuration is compiled, if not ignore it
            if (appDef.isConfigurationReady(i)) {
                continue;
            }
            if (!toRunNow) { //check if further tests for current environment are valid to run
                answer.add(current);
                continue;
            }
            //check the testbed is online
            Testbed testbed = client.getTestbed(current.getConfigurationsTestbedId());
            if (!testbed.isOnline()) {
                continue;
            }
            //Test to see if the nodes are available
            if (current.getNodesNeeded() > 0 && getNodeCount() < current.getNodesNeeded()) {
                continue;
            }
            //Test to see if it a particular amount of cpus is needed.
            if (current.getCpusNeededPerNode() > 0 && getCoreNodeCount((int) current.getCpusNeededPerNode()) < current.getCpusNeededPerNode()) {
                continue;
            }            
            //Test to see if it needs GPU acceleration
            if (current.getGpusNeededPerNode() > 0 && getGpuNodeCount((int) current.getGpusNeededPerNode()) < current.getNodesNeeded()) {
                continue;
                //Retest these nodes to see if they have enough cpus as well
            }
            answer.add(current);
        }
        return answer;
    }
    
    /**
     * This gets the count of GPUs that are currently available.
     * @return 
     */
    protected int getAvailableGpuCount() {
        int answer = 0;
        for (Host host : datasource.getHostList()) {
            if (host.isAvailable()) {
                answer = answer + host.getGpuCount();
            }
        }
        return answer;
    }

    /**
     * This gets the count of GPUs that are currently available.
     * @param minGpus the minimum amount of gpus that are available
     * @return 
     */
    protected int getGpuNodeCount(int minGpus) {
        int answer = 0;
        for (Host host : datasource.getHostList()) {
            if (host.isAvailable() && host.getGpuCount() > minGpus) {
                answer = answer + host.getGpuCount();
            }
        }
        return answer;
    }    
    
    /**
     * This gets the count of Intel Mics that are currently available.
     * @return 
     */
    protected int getAvailableMicCount() {
        int answer = 0;
        for (Host host : datasource.getHostList()) {
            if (host.isAvailable()) {
                answer = answer + host.getMicCount();
            }
        }
        return answer;
    }
    
    /**
     * This gets the count of Mics that are currently available.
     * @param minMics the minimum amount of mics that are available
     * @return 
     */
    protected int getMicNodeCount(int minMics) {
        int answer = 0;
        for (Host host : datasource.getHostList()) {
            if (host.isAvailable() && host.getMicCount() > minMics) {
                answer = answer + host.getMicCount();
            }
        }
        return answer;
    }
    
    /**
     * This gets the count of nodes that are currently available.
     * @return 
     */
    protected int getNodeCount() {
        int answer = 0;
        for (Host host : datasource.getHostList()) {
            if (host.isAvailable()) {
                answer = answer + 1;
            }
        }
        return answer;
    }    

    /**
     * This gets the count of Nodes that are currently available, with a given cpu count.
     * @param minCpus the amount of cpus needed
     * @return 
     */
    protected int getCoreNodeCount(int minCpus) {
        int answer = 0;
        for (Host host : datasource.getHostList()) {
            if (host.isAvailable() && host.getCoreCount() > minCpus) {
                answer = answer + host.getCoreCount();
            }
        }
        return answer;
    }       
    
    @Override
    public ApplicationDefinition getApplication(String name, String deploymentId) {
        List<ApplicationDefinition> allApps = client.getApplicationDefinitions();
        for (ApplicationDefinition app : allApps) {
            if (app.getName().equals(name)
                    && (app.getDeploymentId().equals(deploymentId) || !app.hasDeploymentId())) {
                return app;
            }
        }
        return null;
    }

    @Override
    public List<ApplicationOnHost> getTasksOnHost(String host) {
        if (parent != null) {
            return parent.getTasksOnHost(host);
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<ApplicationOnHost> getTasks() {
        if (parent != null) {
            return parent.getTasks();
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ApplicationOnHost getTask(String name, String deployment, int taskId) {
        if (parent != null) {
            return parent.getTask(name, deployment, taskId);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getTotalPowerUsage(String applicationName, String deploymentId) {
        if (parent != null) {
            return parent.getTotalPowerUsage(applicationName, deploymentId);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getPowerUsageTask(String applicationName, String deploymentId, int taskId) {
        if (parent != null) {
            return parent.getPowerUsageTask(applicationName, deploymentId, taskId);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getAveragePowerUsage(String applicationName, String deploymentId, String taskType) {
        if (parent != null) {
            return parent.getAveragePowerUsage(applicationName, deploymentId, taskType);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getTaskTypesAvailableToAdd(String applicationName, String deploymentId) {
        if (parent != null) {
            return parent.getTaskTypesAvailableToAdd(applicationName, deploymentId);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Integer> getTaskIdsAvailableToRemove(String applicationName, String deploymentId) {
        if (parent != null) {
            return parent.getTaskIdsAvailableToRemove(applicationName, deploymentId);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void hardKillApp(String applicationName, String deploymentId) {
        if (parent != null) {
            parent.hardKillApp(applicationName, deploymentId);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addTask(String applicationName, String deploymentId, String taskType) {
        if (parent != null) {
            parent.addTask(applicationName, deploymentId, taskType);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteTask(String applicationName, String deployment, String taskID) {
        if (parent != null) {
            parent.deleteTask(applicationName, deployment, taskID);
        }        
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
