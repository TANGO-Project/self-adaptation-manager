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
import eu.tango.self.adaptation.manager.comparator.PowerComparator;
import eu.tango.self.adaptation.manager.comparator.TimeComparator;
import eu.tango.self.adaptation.manager.listeners.ClockMonitor;
import eu.tango.self.adaptation.manager.listeners.EnvironmentMonitor;
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

    public enum RankCriteria {

        ENERGY, TIME, POWER
    }

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
     *
     * @param parent The parent actuator of the ALDE actuator.
     */
    public AldeActuator(ActuatorInvoker parent) {
        this.parent = parent;
        datasource = new TangoEnvironmentDataSourceAdaptor();
    }
 
    /**
     * This gets the parent actuator of the ALDE if the ALDE actuator is on its
     * own then this value is null.
     *
     * @return The parent of the ALDE actuator.
     */
    public ActuatorInvoker getParent() {
        return parent;
    }

    /**
     * This sets the parent actuator of the ALDE if the ALDE actuator is on its
     * own then this value is null.
     *
     * @param parent The parent of the ALDE actuator.
     */
    public void setParent(ActuatorInvoker parent) {
        this.parent = parent;
    }

    /**
     * This executes a given action for a response. Usually it is taken 
     * from the actuator's pending action queue.
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
                addResource(response.getApplicationId(), response.getDeploymentId(), response.getAdaptationDetail("TASK_TYPE"));
                break;
            case REMOVE_TASK:
                removeResource(response.getApplicationId(), response.getDeploymentId(), response.getAdaptationDetail("TASK_TYPE"));
                break;
            case SCALE_TO_N_TASKS:
                scaleToNTasks(response.getApplicationId(), response.getDeploymentId(), response);
                break;
            case RESELECT_ACCELERATORS:
                boolean killPrevious = true;
                if (getTaskDeploymentId(response) == null || getTaskDeploymentId(response).isEmpty()) {
                    response.setPossibleToAdapt(false);
                    Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "No suitable application was found to reselect accelerators for");
                    break;
                }
                if (response.hasAdaptationDetail("KILL_PREVIOUS")) {
                    String killPreviousStr = response.getAdaptationDetail("KILL_PREVIOUS");
                    killPrevious = Boolean.parseBoolean(killPreviousStr);
                }
                RankCriteria rankBy = RankCriteria.ENERGY;
                if (response.hasAdaptationDetail("RANK_BY")) {
                    String rankByStr = response.getAdaptationDetail("RANK_BY");
                    if (RankCriteria.valueOf(rankByStr) != null) {
                        rankBy = RankCriteria.valueOf(rankByStr);
                    }
                }
                ApplicationDefinition appDef = reselectAccelerators(response.getApplicationId(), getTaskDeploymentId(response), killPrevious, rankBy);
                if (appDef == null) {
                    response.setPossibleToAdapt(false);
                    Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "It wasn't possible to adapt, due to a suitable application not being found");
                }
                break;
                case STARTUP_HOST:
                String host = getHostname(response);
                if (host != null) {
                    startupHost(host);
                }
                break;                
                case SHUTDOWN_HOST:
                host = getHostname(response);    
                if (host != null) {
                    shutdownHost(host);
                }
                if (response.hasAdaptationDetail("REBOOT")) {
                    int resumeInNseconds = Integer.parseInt(response.getAdaptationDetail("REBOOT"));
                    ClockMonitor.getInstance().addEvent("!" + response.getCause().getAgreementTerm(), "host=" + host, resumeInNseconds);
                }     
                break;
            default:
                Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "The response type was not recognised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }

    /**
     * This takes the accelerators
     *
     * @param name The name of the application to redeploy
     * @param deploymentId The deployment id of the current application that
     * will be redeployed
     * @param killPreviousApp Indicates if the previous instance should be
     * killed on starting the new instance.
     * @param rankBy This gives a choice of how ranking is performed
     * @return The Application definition of the application
     */
    public ApplicationDefinition reselectAccelerators(String name, String deploymentId, boolean killPreviousApp, RankCriteria rankBy) {
        ApplicationConfiguration selectedConfiguration;
        ApplicationConfiguration currentConfiguration = getCurrentConfigurationInUse(name, deploymentId);
        if (currentConfiguration == null) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "Current running application instance not found");
            return null; //Return without performing any work
        }        
        ApplicationDefinition appDef = client.getApplicationDefinition(currentConfiguration);
        //Find a the list of valid configurations
        ArrayList<ApplicationConfiguration> validConfigurations = getValidConfigurations(appDef, true);
        //and ensure that they haven't been executed as yet
        validConfigurations = removeAlreadyRunningConfigurations(validConfigurations);
        selectedConfiguration = selectConfiguration(validConfigurations, appDef, currentConfiguration, rankBy);
        //Ensure the configuration selected is a change/improvement
        if (selectedConfiguration != null && currentConfiguration != selectedConfiguration) {
            int configId = selectedConfiguration.getConfigurationId();
            try {
                //Delete the current configuration of the application
                if (killPreviousApp) {
                    hardKillApp(name, client.getExecutionInstance(deploymentId).getExecutionId() + "");
                }
                //Launch the best configuration that can be found (fastest/least energy)
                client.executeApplication(configId);
            } catch (IOException ex) {
                Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return appDef;
    }

    /**
     * This takes a application name and deployment id and determines the
     * configuration that was used to launch the application
     *
     * @param name The application name
     * @param deploymentId The deployment id of the application
     * @return The application configuration that was used to launch the
     * application
     */
    private ApplicationConfiguration getCurrentConfigurationInUse(String name, String deploymentId) {
        ApplicationExecutionInstance instance = client.getExecutionInstance(deploymentId);
        if (instance == null) {
            //Sleep and perform a second attempt
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(EnvironmentMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }            
            instance = client.getExecutionInstance(deploymentId);
            if (instance == null) {
                Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "The execution instance with deployment id {0} is not known to the ALDE", deploymentId);
                return null;
            }
        }
        ApplicationDefinition app = client.getApplicationDefinition(instance);
        if (app == null) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "The application named {0} is not known to the ALDE", name);
            return null;
        }
        //In cases where there is only 1 configuration for the application
        if (app.getConfigurations().size() == 1) {
            return app.getConfiguration(0);
        }        
        for (ApplicationConfiguration config : app.getConfigurations()) {
            if (config.getConfigurationId() == instance.getExecutionConfigurationsId()) {
                return config;
            }
        }
        return null;
    }
    
    /**
     * This selects from the list of configurations available one that is valid.
     *
     * @param validConfigurations The list of valid configurations to select
     * from
     * @param currentConfiguration The current configuration in use, this acts
     * as a base line to compare all other cases against.
     * @return The configuration that should be launched, else it returns null
     */
    private ApplicationConfiguration selectConfiguration(ArrayList<ApplicationConfiguration> validConfigurations, ApplicationDefinition appDefintion, ApplicationConfiguration currentConfiguration, RankCriteria rank) {
        ConfigurationComparator comparator = new ConfigurationComparator();
        //The configs to rank against are as follows: valid configs + current config
        ArrayList<ApplicationConfiguration> configsToConsider = (ArrayList<ApplicationConfiguration>) validConfigurations.clone();
        configsToConsider.add(currentConfiguration);
        if (validConfigurations.isEmpty()) {
            return null; //There is no alternative to the current running job
        }
        //If there is more than one configuration then one needs to be picked
        ArrayList<ConfigurationRank> ranked = comparator.compare(appDefintion.getName(), currentConfiguration.getConfigurationId() + "", configsToConsider);        
        //If there is no ranking data just pick one
        if (ranked == null || ranked.isEmpty()) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "No Ranking data of the configuration options was available so one was just picked.");
            return validConfigurations.get(0);
        }
        double referenceScore;
        //Perform ranking
        if (rank.equals(RankCriteria.TIME)) {
            ranked.sort(new TimeComparator());
            referenceScore = ranked.get(0).getDurationVsReference();
        } else if (rank.equals(RankCriteria.POWER)) {
            ranked.sort(new PowerComparator());
            referenceScore = ranked.get(0).getAveragePowerUsageVsReference();
        } else { //The default is by energy consumption
            ranked.sort(new EnergyComparator());
            referenceScore = ranked.get(0).getEnergyUsedVsReference();
        }
        //Ensure the top ranked item isn't the current config
        if (ranked.get(0).getConfigName().equals(currentConfiguration.getConfigurationId() + "")) {
            ranked.remove(0); //Don't pick the configuration that is currently running.
            if (ranked.isEmpty()) {
                return null;
            }
        }
        /**
         * This next line ensures the best option available is better than what is currently running.
         */
        if (comparator.isBetterThanReference(referenceScore)) {
            return ApplicationConfiguration.selectConfigurationById(validConfigurations, Integer.parseInt(ranked.get(0).getConfigName()));
        }       
        //If there is no better solution then return null
        return null;
    }

    /**
     * This filters out applications that are already deployed and running,
     * assuming they can't be caught up with by another instance of the same
     * deployment.
     *
     * @param validConfigurations The list of configurations that are possible
     * to run
     * @param currentlyRunning The configuration/s that are currently running
     * @return The list of configurations that are deployable and have not as
     * yet been deployed.
     */
    private ArrayList<ApplicationConfiguration> removeAlreadyRunningConfigurations(ArrayList<ApplicationConfiguration> validConfigurations) {
        ArrayList<ApplicationExecutionInstance> currentlyRunning = (ArrayList) client.getExecutionInstances();
        ArrayList<ApplicationConfiguration> answer = (ArrayList<ApplicationConfiguration>) validConfigurations.clone();
        for (ApplicationExecutionInstance current : currentlyRunning) {
            for (ApplicationConfiguration config : validConfigurations) {
                //If the configuration is used by a deployment then filter it out
                //deployments doesn't seem to refer directly to the configuration in use
                if (config.getConfigurationId() == current.getExecutionConfigurationsId()) {
                    answer.remove(config);
                }
            }
        }
        return validConfigurations;
    }

    /**
     * Finds the list of valid configurations that can be launched.
     *
     * @param appDef The application definition
     * @param toRunNow Indicates if additional tests should be performed
     * checking to see if the current environment is suitable
     * @return The list of configurations that can be launched
     */
    private ArrayList<ApplicationConfiguration> getValidConfigurations(ApplicationDefinition appDef, boolean toRunNow) {
        //TODO complete the getValidConfigurations method
        ArrayList<ApplicationConfiguration> answer = new ArrayList<>();
        if (appDef == null) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "The application definition was null.");
            return answer;
        }
        for (int i = 0; i < appDef.getConfigurationsCount(); i++) {
            ApplicationConfiguration current = appDef.getConfiguration(i);
            //Check to see if the configuration is compiled, if not ignore it
            if (!appDef.isConfigurationReady(i)) {
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
     *
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
     *
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
     *
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
     *
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
     *
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
     * This gets the count of Nodes that are currently available, with a given
     * cpu count.
     *
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
            if (app.getName() != null && app.getName().equals(name)
                    && (!app.hasDeploymentId() || app.getDeploymentId().equals(deploymentId))) {
                return app;
            }
        }
        return null;
    }

    @Override
    public List<ApplicationOnHost> getTasksOnHost(String host) {
        if (parent != null) {
            return appendQoSInformation(parent.getTasksOnHost(host));
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<ApplicationOnHost> getTasks() {
        if (parent != null) {
            return appendQoSInformation(parent.getTasks());
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * This gets the list of any additional application on host instances associated
     * with a master task.
     * @param master The master task which owns the additional instances
     * @return The list of ApplicationOnHost for the parent application
     */
    public List<ApplicationOnHost> getAdditionalApplicationOnHost(ApplicationOnHost master) {
        List<ApplicationOnHost> answer = new ArrayList<>();
        ApplicationExecutionInstance instance = client.getExecutionInstance(master.getId() + "");
        if (instance.getExtraSlurmId() != null) {
            if (parent != null) {
                for (String appId : instance.getExtraSlurmId().split(" ")) {
                    answer.addAll(appendQoSInformation(parent.getTasks(null, appId)));
                }
            }            
        }
        return answer;
    }
    
    /**
     * This for a given set of applications obtains the QoS information from the ALDE
     * @param applications The list of applications to get information for
     * @return The list of applications with QoS information attached
     */
    public List<ApplicationOnHost> appendQoSInformation(List<ApplicationOnHost> applications) {
        for (ApplicationOnHost application : applications) {
            application.setProperties(client.getApplicationProperties(application.getId()));
        }
        return applications;
    }

    @Override
    public void hardKillApp(String applicationName, String deploymentId) {
        try {
            client.cancelApplication(Integer.parseInt(deploymentId));
        } catch (IOException ex) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
        }     
    }

    @Override
    public void addResource(String applicationName, String deploymentId, String taskType) {
        try {
            int executionId = client.getExecutionInstance(deploymentId).getExecutionId();
            client.addResource(executionId, taskType);
        } catch (IOException ex) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void removeResource(String applicationName, String deployment, String resourceId) {
        try {
            int executionId = client.getExecutionInstance(deployment).getExecutionId();
            client.removeResource(executionId, resourceId);
        } catch (IOException ex) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
        }   
    }
    
    /**
     * This powers down a host
     *
     * @param hostname The host to power down
     */
    public void shutdownHost(String hostname) {
        try {
            client.shutdownHost(hostname);
        } catch (IOException ex) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    /**
     * This powers up a host
     *
     * @param hostname The host to power up
     */
    public void startupHost(String hostname) {
        try {
            client.startHost(hostname);
        } catch (IOException ex) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }    

}