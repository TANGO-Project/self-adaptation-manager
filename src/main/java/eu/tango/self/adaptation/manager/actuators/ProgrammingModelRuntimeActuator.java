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
package eu.tango.self.adaptation.manager.actuators;

import eu.tango.energymodeller.datasourceclient.CompssDatasourceAdaptor;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import static eu.tango.self.adaptation.manager.io.ExecuteUtils.execCmd;
import eu.tango.self.adaptation.manager.io.HostnameDetection;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * This acts as the actuator for the Tango programming model runtime, in order
 * to directly adapt applications that are running. It is aimed to be used in 
 * the project's remote processing use case.
 * @author Richard Kavanagh
 */
public class ProgrammingModelRuntimeActuator extends AbstractActuator {

    private static final String CONFIG_FILE = "self-adaptation-manager.properties";
    private String compssRuntime = "/home_nfs/home_ejarquej/installations/2.3.1//COMPSs//compssenv";
    private String permittedHosts = "ns51,ns52,ns53,ns54,ns55,ns56,ns57";
    private CompssDatasourceAdaptor client = new CompssDatasourceAdaptor();

    public ProgrammingModelRuntimeActuator() {
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            compssRuntime = config.getString("self.adaptation.manager.compss.runtime", compssRuntime);
            permittedHosts = config.getString("self.adaptation.manager.compss.runtime.permittedhosts", permittedHosts);
            if (!compssRuntime.endsWith("/")) {
                compssRuntime = compssRuntime + "/";
            }
            config.setProperty("self.adaptation.manager.compss.runtime", compssRuntime);
            config.setProperty("self.adaptation.manager.compss.runtime.permittedhosts", permittedHosts);
        } catch (ConfigurationException ex) {
            Logger.getLogger(ProgrammingModelRuntimeActuator.class.getName()).log(Level.INFO, "Error loading the configuration of the Self adaptation manager", ex);
        }
        //Ensure the compss runtime is available
        execCmd("source " + compssRuntime);
    }
    
/*
 * COMPSs jobs are launched using the enqueue compss command.
 * A Sample enqueue_compss command:
   enqueue_compss [queue_system_options] [rucompss_options] application_name application_arguments
   enqueue_compss --num_nodes=3 --cpus-per-node=12 --gpus-per-node=2 \
          --node-memory=32000 –container_image=/path/to/container.img  \
          --lang=c appName appArgs 
 * enqueue_compss --sc_cfg=nova.cfg -d --monitoring=1000 --lang=c --num_nodes=1 --gpus_per_node=2 --cpus_per_node=12 --worker_in_master_cpus=12 --worker_in_master_memory=24000 --container_image=/home_nfs/home_ejarquej/images/91e5d4f3-edb9-4311-9611-b82d2b58ffb9.img --container_compss_path=/opt/TANGO/TANGO_ProgrammingModel/COMPSs/ --exec_time=10 --worker_working_dir=$HOME --appdir=/apps/application/ /apps/application/master/Matmul 2 1024 12.34 $HOME/demo_test/cpu_gpu_run_data/
    App Name: /apps/application/master/Matmul
 */    
    
/**
 * This actuator class provides commands through the programming model:
 * 
 * https://github.com/TANGO-Project/programming-model-and-runtime
 * 
 * In order for the Self-Adaptation Manager to take the responsibility for 
 * scale up/down the resources, the application must be submitted with the 
 * enable_external_adaptation option of the enqueue_compss command as indicated 
 * in the example below.
 * $ enqueue_compss --num_nodes=3 --cpus-per-node=12 --gpus-per-node=2 \
       --node-memory=32000 --elasticity=2 --enable_external_adaptation=true \
       --container_image=/path/to/container.img --lang=c appName appArgs
 * 
 * When the application is running, the adaptation of the nodes can be performed 
 * by means of the adapt_compss_resources command in the following way:
 * $ adapt_compss_resources <master_node> <master_job_id> CREATE SLURM-Cluster default <singularity_image>
 * This command will submit another job requesting a new resource of type 
 * "default" (the same as the requested in the enqueue_compss) running the COMPSs 
 * worker of the singularity_image.
 * $ adapt_compss_resources <master_node> <master_job_id> REMOVE SLURM-Cluster <node_to_delete>
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
                Logger.getLogger(ProgrammingModelRuntimeActuator.class.getName()).log(Level.INFO, "DeploymentID not set correctly!");
                return;
            }
        }
        switch (response.getActionType()) {
            case ADD_TASK:
                addResource(response.getApplicationId(), getTaskDeploymentId(response), response.getAdaptationDetails());
                break;
            case REMOVE_TASK:
                removeResource(response.getApplicationId(), getTaskDeploymentId(response), response.getTaskId());
                break;
            case SCALE_TO_N_TASKS:
                scaleToNTasks(response.getApplicationId(), getTaskDeploymentId(response), response);
                break;
            case HARD_KILL_APP:
            case KILL_APP:
                //Note: no soft implementation exists at this time
                hardKillApp(response.getApplicationId(), getTaskDeploymentId(response));
                break;
            default:
                Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, "The Response type was not recoginised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }

    @Override
    public ApplicationDefinition getApplication(String name, String masterJobId) {
        ApplicationDefinition answer = new ApplicationDefinition(name, masterJobId);
        /**
         * Maybe set
            SLALimits slaLimits;
            ArrayList<FiringCriteria> adaptationRules
         */
        return answer;
    }

    @Override
    public List<ApplicationOnHost> getTasksOnHost(String host) {
        List<ApplicationOnHost> answer = new ArrayList<>();
        List<ApplicationOnHost> allTasks = getTasks();
        for (ApplicationOnHost task : allTasks) {
            if (task.getAllocatedTo().getHostName().equals(host)) {
                answer.add(task);
            }
        }
        return answer;
    }

    @Override
    public List<ApplicationOnHost> getTasks() {
        return client.getHostApplicationList();
    }
    
    @Override
    public List<ApplicationOnHost> getTasks(String applicationName, String deploymentId) {
        List<ApplicationOnHost> unFilteredAnswer = getTasks();
        List<ApplicationOnHost> answer = new ArrayList<>();
        int deployid = Integer.parseInt(deploymentId);
        for (ApplicationOnHost application : unFilteredAnswer) {
            if ((application.getName() == null || application.getName().equals(applicationName)) &&
                    application.getId() == deployid) {
                answer.add(application);
            }
        }        
        return answer;
    }    

    @Override
    public void hardKillApp(String applicationName, String masterJobId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * This adds a task/resource of a given type to named deployment.
     *
     * @param applicationName The name of the application
     * @param masterJobId The deployment id/job id. In compss the identifier of a job
     * is called the masterJobId
     * @param taskParams additional task parameters such as task type
     */
    @Override
    public void addResource(String applicationName, String masterJobId, String taskParams) {
        //Command: "adapt_compss_resources <master_node> <master_job_id> CREATE SLURM-Cluster default <singularity_image>"
        /**
         * Example command: adapt_compss_resources ns54 EmulateRemote_01 CREATE Direct ns51 default
         */
        masterJobId = client.getCurrentMonitoringJobId();
        String masterNode = getMasterNode();
        String nodeToAdd = "";
        HashSet<String> activeHosts = new HashSet<>();
        for (Host host : client.getHostList()) {
            //Obtain the first idle host already added to COMPSS
            if (host.getState().equals("IDLE")) {
                nodeToAdd = (nodeToAdd.isEmpty() ? host.getHostName() : nodeToAdd);
            } else {
                activeHosts.add(nodeToAdd);
            }
        }
        //If a host isn't available pick one from the none active hosts not added to COMPSS already
        if (nodeToAdd.isEmpty()) {
            for(String host : permittedHosts.split(",")) {
                if (!activeHosts.contains(host)) {
                    nodeToAdd = host;
                    break;
                }
            }
        }
        System.out.println("RUNNING COMMAND: adapt_compss_resources " + masterNode + " " + masterJobId + " CREATE Direct " + nodeToAdd + " default");
        execCmd("adapt_compss_resources " + masterNode + " " + masterJobId + " CREATE Direct " + nodeToAdd + " default");
    }
    
    /**
     * This gets the master node for a given job
     * @param applicationName The application name
     * @return The master node of the named job
     */
    private String getMasterNode() {
        return HostnameDetection.getHostname();
    }

    @Override
    public void removeResource(String applicationName, String masterJobId, String taskId) {
        //Command: "adapt_compss_resources <master_node> <master_job_id> REMOVE SLURM-Cluster <node_to_delete>"
        /**
         * Example Command: adapt_compss_resources ns54 EmulateRemote_01 REMOVE Direct ns51
         */
        masterJobId = client.getCurrentMonitoringJobId();
        String masterNode = getMasterNode();
        String nodeToDelete = getNodeToDelete(taskId); //task id == host id
        System.out.println("RUNNING COMMAND: adapt_compss_resources " + masterNode + " " + masterJobId + " REMOVE Direct " + nodeToDelete);
        execCmd("adapt_compss_resources " + masterNode + " " + masterJobId + " REMOVE Direct " + nodeToDelete);
    }
    
    /**
     * This translates task id into hostname
     * @param taskId The task id to delete
     * @return The hostname to remove
     */
    private String getNodeToDelete(String taskId) {
        for (Host host : client.getHostList()) {
            if((host.getId() + "").equals(taskId)) {
                return host.getHostName();
            }
        }
        return "";
    }
    
}
