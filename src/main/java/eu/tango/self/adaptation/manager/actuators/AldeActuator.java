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
import eu.tango.self.adaptation.manager.model.Node;
import eu.tango.self.adaptation.manager.model.Testbed;
import eu.tango.self.adaptation.manager.qos.SlaRulesLoader;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private AldeClient client = new AldeClient();
    private final HostDataSource datasource;
    private ActuatorInvoker parent = null;

    public enum RankCriteria {

        ENERGY, TIME, POWER
    }
    
    //This is the property that is used to rank how flexible a host workload is to change.
    private static final String APPLICATION_TYPE = "application_type";
    
    public enum AppType {

        RIGID, MOULDABLE, CHECKPOINTABLE, MALLEABLE
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
            case PAUSE_APP:
                pauseJob(response.getApplicationId(), getTaskDeploymentId(response));
                if (response.hasAdaptationDetail("UNPAUSE")) {
                    /**
                     * This requires to have a matching rule to negate the effect of the first.
                     * The matching rule starts with an exclamation! instead.
                     */
                    int unpauseInNseconds = Integer.parseInt(response.getAdaptationDetail("UNPAUSE"));
                    ClockMonitor.getInstance().addEvent("!" + response.getCause().getAgreementTerm(), "application=" + response.getApplicationId() + ";deploymentid=" + getTaskDeploymentId(response), unpauseInNseconds);
                }
                break;
            case UNPAUSE_APP:
                resumeJob(response.getApplicationId(), getTaskDeploymentId(response));
                break;
            case PAUSE_SIMILAR_APPS:
                pauseSimilarJob(response.getApplicationId());
                break;
            case UNPAUSE_SIMILAR_APPS:
                resumeSimilarJob(response.getApplicationId());
                break;
            case KILL_SIMILAR_APPS:
                killSimilarApps(response.getApplicationId());
                break;                
            case KILL_APP:
            case HARD_KILL_APP:
                hardKillApp(response.getApplicationId(), getTaskDeploymentId(response));
                break;            
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
            case INCREASE_POWER_CAP:
                increasePowerCap(response);
                break;
            case REDUCE_POWER_CAP:
                decreasePowerCap(response);
                break;
            case SET_POWER_CAP:
                setPowerCap(response);                
            break;
            case STARTUP_HOST:
                String host = getHostname(response);
                if (host != null) {
                    startupHost(host);
                } else {
                    response.setPossibleToAdapt(false);
                }
            break;                
            case SHUTDOWN_HOST:
                host = getHostname(response);    
                if (host != null) {
                    preShutdownHost(host, response);
                    //This bit peforms the main operation to shutdown the host
                    shutdownHost(host);
                } else {
                    response.setPossibleToAdapt(false);
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
        selectedConfiguration = selectConfiguration(validConfigurations, appDef, currentConfiguration, rankBy, isExecutionInstanceOnLiveHosts(deploymentId));
        //Ensure the configuration selected is a change/improvement
        if (selectedConfiguration != null && (currentConfiguration != selectedConfiguration || 
                !isExecutionInstanceOnLiveHosts(deploymentId))) {
            int configId = selectedConfiguration.getConfigurationId();
            try {
                //Delete the current configuration of the application
                if (killPreviousApp) {
                    hardKillApp(name, deploymentId + "");
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
     * @param hasToBeImprovement indicates if it must be an improvement on the current
     * condition configuration
     * @return The configuration that should be launched, else it returns null
     */
    private ApplicationConfiguration selectConfiguration(ArrayList<ApplicationConfiguration> validConfigurations, ApplicationDefinition appDefintion, ApplicationConfiguration currentConfiguration, RankCriteria rank, boolean hasToBeImprovement) {
        ConfigurationComparator comparator = new ConfigurationComparator();
        //The configs to rank against are as follows: valid configs + current config
        ArrayList<ApplicationConfiguration> configsToConsider = (ArrayList<ApplicationConfiguration>) validConfigurations.clone();
        configsToConsider.add(currentConfiguration);
        if (validConfigurations.isEmpty()) {
            return null; //There is no alternative to the current running job
        }
        //If there is more than one configuration then one needs to be picked
        ArrayList<ApplicationExecutionInstance> pastRunData = new ArrayList<>();
        for (ApplicationConfiguration config : configsToConsider) {
            //Ensure it constains completed run data only
            List<ApplicationExecutionInstance> completedRuns = client.getExecutionInstances(config.getConfigurationId());
            pastRunData.addAll(ApplicationExecutionInstance.filterBasedUponStatus(completedRuns, ApplicationExecutionInstance.Status.COMPLETED));
        }
        ArrayList<ConfigurationRank> ranked = comparator.compare(currentConfiguration.getConfigurationId() + "", configsToConsider, pastRunData);
        //This echos out to the console the reason for the adaptation decision
        System.out.println("The relative ranking of different configurations is as follows: ");
        for (ConfigurationRank rankedItem : ranked) {
            System.out.println(rankedItem);
        }
        //If the ALDE doesn't have ranking data, use a fall back to using a file on disk.
        if (ranked == null || ranked.isEmpty()) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.WARNING, "No Ranking data was available in the ALDE falling back to reading ranking data from file.");
            ranked = comparator.compare(appDefintion.getName(), currentConfiguration.getConfigurationId() + "", configsToConsider);
        }
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
        if (ranked.get(0).getConfigName().equals(currentConfiguration.getConfigurationId() + "") && hasToBeImprovement) {
            ranked.remove(0); //Don't pick the configuration that is currently running.
            if (ranked.isEmpty()) {
                return null;
            }
        }
        /**
         * This next line ensures the best option available is better than what is currently running.
         */
        if (comparator.isBetterThanReference(referenceScore) || !hasToBeImprovement) {
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
            if (isExecutionInstanceOnLiveHosts(current)) { //Only consider it running if the host is not down or draining
                for (ApplicationConfiguration config : validConfigurations) {
                    //If the configuration is used by a deployment then filter it out
                    //deployments doesn't seem to refer directly to the configuration in use
                    if (config.getConfigurationId() == current.getExecutionConfigurationsId()) {
                        answer.remove(config);
                    }
                }
            }
        }
        return validConfigurations;
    }
    /**
     * This indicates if a deployment is on a live host or not
     * @param deploymentId The deployment to test to see if the host is ok
     * @return If the host is live or not
     */
    private boolean isExecutionInstanceOnLiveHosts(String deploymentId) {
        return isExecutionInstanceOnLiveHosts(client.getExecutionInstance(deploymentId));
    }
    
    /**
     * This indicates if an execution instance is on a host that has failed or not
     * @param application The application to test
     * @return If any host the application is running on has failed this will flag
     * the failure. The idea is then that a migration should take place no matter what.
     */
    private boolean isExecutionInstanceOnLiveHosts(ApplicationExecutionInstance application) {
        for (Node node : application.getNodes()) {
            Node updatedNode = client.getNode(node.getName());
            if (updatedNode.isDisabled()
                    || updatedNode.getState().trim().toLowerCase().contains("down")
                    || updatedNode.getState().trim().toLowerCase().contains("drain")) {
                return false;
            }
        }
        return true;
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
    
    /**
     * This takes a named application and kills all instances of it.
     * @param applicationName The name of the application to kill
     */
    public void killSimilarApps(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        for (ApplicationOnHost app : apps) {
            hardKillApp(app.getName(), app.getId() + "");
        }
    }

    /**
     * Pauses all jobs with a given name, so that they can be executed later.
     * @param applicationName The name of the application to pause
     */
    public void pauseSimilarJob(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        for (ApplicationOnHost app : apps) {
            pauseJob(applicationName, app.getId() + "");
        }
    }
    
    /**
     * Un-pauses all jobs with a given name, so that they can be executed later.
     * @param applicationName The name of the application to pause
     */
    public void resumeSimilarJob(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        for (ApplicationOnHost app : apps) {
            resumeJob(applicationName, app.getId() + "");
        }
    }
        
    /**
     * Pauses a job, so that it can be executed later.
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     *
     */
    public void pauseJob(String applicationName, String deploymentId) {
        try {
            client.pauseJob(client.getExecutionInstance(deploymentId).getExecutionId());
        } catch (IOException ex) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
        }  
    }

    /**
     * un-pauses a job, so that it may resume execution.
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     */
    public void resumeJob(String applicationName, String deploymentId) {
        try {
            client.resumeJob(client.getExecutionInstance(deploymentId).getExecutionId());
        } catch (IOException ex) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
        }            
    }    

    @Override
    public void hardKillApp(String applicationName, String deploymentId) {
        try {
            client.cancelApplication(client.getExecutionInstance(deploymentId).getExecutionId());
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
     * This is intended as an extension to the shutdown call. It cancels existing 
     * applications on the host, instead of just performing a host drain operation.
     * @param host The host to perform the cancelling of work against
     * @param response The response object that stated the host shutdown.
     */
    private void preShutdownHost(String host, Response response) {
        //TODO add a softer cancelling here!!! Thus meeting Jorge's reqs.
        if (response.hasAdaptationDetail("CANCEL_APPS")) {
            /**
             * Hard cancel is default cancelling the app if the cancel apps flag is set.
             * The flag needs to be set to soft to graciously migrate/checkpoint/resize apps away.
             */
            boolean hardCancel = !response.getAdaptationDetail("CANCEL_APPS").equalsIgnoreCase("soft");
            for(ApplicationExecutionInstance app : client.getExecutionInstances(true) ) {
                try {
                    Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "Cancelling Apps on host: {0}", host);
                    JSONObject appProperties = client.getApplicationProperties(app.getSlurmId());
                    String applicationType = AppType.RIGID.toString();
                    if (appProperties.has(APPLICATION_TYPE)) {
                        applicationType = appProperties.getString(APPLICATION_TYPE);
                    }
                    if (hardCancel || applicationType.equalsIgnoreCase(AppType.RIGID.toString())
                            || applicationType.equalsIgnoreCase(AppType.MALLEABLE.toString())
                            || applicationType.equalsIgnoreCase(AppType.CHECKPOINTABLE.toString())) {
                        /**
                         * Cancel RIGID and MALLEABLE jobs or all jobs if hard cancel is set
                         * Malleable is like rigid at runtime, and mouldable pre starting
                         * Currently just cancel CHECKPOINTABLE
                         * TODO perform checkpoint and pause/cancel instead
                         */                     
                        for (Node node : app.getNodes()) {
                            if (node.getName().equals(host)) {
                                Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "Cancelling with id: {0}", app.getExecutionId());
                                client.cancelApplication(app.getExecutionId());
                            }
                        } // MOULDABLE so can remove tasks at runtime
                    } else if (applicationType.equalsIgnoreCase(AppType.MOULDABLE.toString())) {
                        client.removeResource(app.getExecutionId(), host);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, "Could not cancel application with execution id: {0}", app.getExecutionId());
                }
            }
        }        
    }
    
    /**
     * This powers down a host
     *
     * @param hostname The host to power down
     */
    public void shutdownHost(String hostname) {
        try {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.INFO, "Performing shutdown of host {0}", hostname);
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
            //Starts all hosts
            if (hostname.equals("ALL")) {
                for(Host host : datasource.getHostList()) {
                    if (!host.isAvailable()) {
                        client.startHost(host.getHostName());
                         Logger.getLogger(AldeActuator.class.getName()).log(Level.INFO, "Performing startup of host {0}", host.getHostName());
                    }
                }
                return;
            }
            Logger.getLogger(AldeActuator.class.getName()).log(Level.INFO, "Performing startup of host {0}", hostname);
            client.startHost(hostname);
        } catch (IOException ex) {
            Logger.getLogger(AldeActuator.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }    

    /**
     * This decreases the cluster level power cap on the infrastructure, by a set amount
     * @param response The response object that caused the adaptation to be invoked.
     */
    public void decreasePowerCap(Response response) {
        //This uses the internal power capping system
        double currentPowerCap = SlaRulesLoader.getInstance().getPowerCap();
        double incremenet = 10;
        if (response.hasAdaptationDetail("POWER_INCREMENT")) {
            incremenet = Double.parseDouble(response.getAdaptationDetail("POWER_INCREMENT"));
        }
        if (Double.isFinite(currentPowerCap) && currentPowerCap - incremenet > 0) {
            SlaRulesLoader.getInstance().setPowerCap(currentPowerCap - incremenet);
        }
    }

    /**
     * This increases the cluster level power cap on the infrastructure, by a set amount
     * @param response The response object that caused the adaptation to be invoked.
     */
    public void increasePowerCap(Response response) {
        //This uses the internal power capping system
        double currentPowerCap = SlaRulesLoader.getInstance().getPowerCap();
        double incremenet = 10;
        if (response.hasAdaptationDetail("POWER_INCREMENT")) {
            incremenet = Double.parseDouble(response.getAdaptationDetail("POWER_INCREMENT"));
        }
        if (response.hasAdaptationDetail("RESTORE_HOSTS")) {
            startupHost("ALL");
        }        
        if (Double.isFinite(currentPowerCap)) {
            SlaRulesLoader.getInstance().modifySlaTerm("HOST:ALL:power", null, currentPowerCap + incremenet);
        }
    }

    /**
     * This sets the cluster level power cap on the infrastructure
     *
     * @param response The response object that caused the adaptation to be
     * invoked.
     */
    public void setPowerCap(Response response) {
        //This uses the internal power capping system        
        if (response.hasAdaptationDetail("POWER_CAP")) {
            double powerCap = Double.parseDouble(response.getAdaptationDetail("POWER_CAP"));
            if (Double.isFinite(powerCap) && powerCap > 0) {
                SlaRulesLoader.getInstance().modifySlaTerm("HOST:ALL:power", null, powerCap);
            }
            if (response.hasAdaptationDetail("RESTORE_HOSTS")) {
                startupHost("ALL");
            }
        } else {
            response.setPerformed(true);
            response.setPossibleToAdapt(false);
            response.setAdaptationDetails("No POWER_CAP value specified");
        }
    }     
    
}