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

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import static eu.tango.self.adaptation.manager.io.ExecuteUtils.execCmd;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.io.File;
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
    private String compssRuntime = "/home_nfs/home_ejarquej/installations/2.2.5//COMPSs//compssenv";

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
            if (!compssRuntime.endsWith("/")) {
                compssRuntime = compssRuntime + "/";
            }
            config.setProperty("self.adaptation.manager.compss.runtime", compssRuntime);
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
                return;
            }
        }
        switch (response.getActionType()) {
            case ADD_TASK:
                addTask(response.getApplicationId(), getTaskDeploymentId(response), response.getAdaptationDetails());
                break;
            case REMOVE_TASK:
                deleteTask(response.getApplicationId(), getTaskDeploymentId(response), response.getTaskId());
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
    public ApplicationDefinition getApplication(String name, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public void hardKillApp(String applicationName, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addTask(String applicationName, String deploymentId, String taskType) {
        //Command: "adapt_compss_resources <master_node> <master_job_id> CREATE SLURM-Cluster default <singularity_image>"
        String masterNode = ""; 
        String masterJobId = "";
        String singularityImage = "";
        execCmd("adapt_compss_resources " + masterNode + " " + masterJobId + " CREATE SLURM-Cluster default " + singularityImage);
    }

    @Override
    public void deleteTask(String applicationName, String deployment, String taskID) {
        //Command: "adapt_compss_resources <master_node> <master_job_id> REMOVE SLURM-Cluster <node_to_delete>"
        String masterNode = ""; 
        String masterJobId = "";
        String nodeToDelete = "";
        execCmd("adapt_compss_resources " + masterNode + " " + masterJobId + " REMOVE SLURM-Cluster " + nodeToDelete);
    }
    
}
