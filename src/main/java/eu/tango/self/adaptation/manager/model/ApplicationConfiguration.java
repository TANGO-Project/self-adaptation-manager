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
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This represents application configuration information from the ALDE. It represents 
 * the settings that may be used to launch an application.
 *
 * Example of configuration information:
 * executable_id : 1.0
 * num_cpus_per_node : 12.0
 * testbed_id : 1.0
 * exec_time : 10.0
 * num_gpus_per_node : 2.0
 * compss_config : --worker_in_master_cpus=12 --worker_in_master_memory=24000 --worker_working_dir=/home_nfs/home_garciad --lang=c --monitoring=1000 -d
 * execution_type : SINGULARITY:PM
 * id : 1.0
 * application_id : 1.0
 * command : /apps/application/master/Matmul 2 1024 12.34 /home_nfs/home_garciad/demo_test/cpu_gpu_run_data
 * num_nodes : 1.0
 *
 * @author Richard Kavanagh
 */
public class ApplicationConfiguration extends AldeJsonObjectWrapper {
    
    public ApplicationConfiguration(JSONObject configurationInformation) {
        super(configurationInformation);
    }
  
    /**
     * This gets the id of the configuration
     * @return The id of the configuration
     */
    public int getConfigurationId() {
        if (json.has("id")) {
            return (int) json.getInt("id");
        }
        //the default assumption is zero.
        return 0;        
    }    
    
    /**
     * This gets the id of the application to be used in the configuration.
     * Note: an application may be comprised of several applications, each of which
     * may be compiled to use different accelerators.
     * @return The id of the application to be used in the configuration
     */
    public int getConfigurationsApplicationId() {
        if (json.has("application_id")) {
            return (int) json.getInt("application_id");
        }
        //the default assumption is zero.
        return 0;        
    }    
    
    /**
     * This gets the id of the executable to be used in the configuration
     * Note: an application may be comprised of several applications, each of which
     * may be compiled to use different accelerators.
     * @return The id of the executable to be used in the configuration
     */
    public double getConfigurationsExecutableId() {
        if (json.has("executable_id")) {
            return (double) json.getInt("executable_id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the id of the testbed to be used by the configuration
     * @return The id of the testbed to be used by the configuration
     */
    public int getConfigurationsTestbedId() {
        if (json.has("testbed_id")) {
            return (int) json.getInt("testbed_id");
        }
        //the default assumption is zero.
        return 0;        
    }       
    
    /**
     * This gets the count of nodes needed by this configuration
     * @return The number of nodes needed by the configuration
     */
    public double getNodesNeeded() {
        if (json.has("num_nodes")) {
            return (double) json.getDouble("num_nodes");
        }
        //the default assumption is zero.
        return 0;   
    }       
    
    /**
     * This gets the count of cpus needed per node by this configuration
     * @return The number of CPUs per node needed by the configuration
     */    
    public double getCpusNeededPerNode() {
        if (json.has("num_cpus_per_node")) {
            return (double) json.getDouble("num_cpus_per_node");
        }
        //the default assumption is zero.
        return 0;        
    }    

    /**
     * This gets the count of GPUs needed per node by this configuration
     * @return The number of GPUs per node needed by the configuration
     */    
    public double getGpusNeededPerNode() {
        //Tests to see if the excutable_id belongs to a compiled application
        if (json.has("num_gpus_per_node")) {
            return (double) json.getDouble("num_gpus_per_node");
        }
        //the default assumption is zero.
        return 0;       
    }

    /**
     * This gets the executions of this configuration running on the testbed
     * @return The array of executions running on the testbed
     */    
    public JSONArray getExecutionInstancesAsJson() {
        //Tests to see if the excutable_id belongs to a compiled application
        if (json.has("executions")) {
            return json.getJSONArray("executions");
        }
        return null;       
    }
    
    /**
     * This gets the executions of this configuration running on the testbed
     * @return The array of executions running on the testbed
     */    
    public List<ApplicationExecutionInstance> getExecutionInstances() {
        List<ApplicationExecutionInstance> answer = new ArrayList<>();
        //Tests to see if the excutable_id belongs to a compiled application
        if (json.has("executions")) {
            JSONArray objects = json.getJSONArray("executions");
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                answer.add(new ApplicationExecutionInstance((JSONObject) iterator.next()));
            }          
        }
        return answer;       
    }
    
    /**
     * This checks to see if an application execution instance exist or not, for this configuration.
     * @param instance The instance to check to see if it exists or not
     * @return True only if this configuration of an application has a given execution instance.
     */
    public boolean hasExecutionInstance(ApplicationExecutionInstance instance) {
        if (!json.has("executions")) {
            return false;
        }
        return instance.getExecutionConfigurationsId() == this.getConfigurationId();
    }
    
    /**
     * This selects from a list of configurations the configuration indicated by its id
     * @param configurations The list of configurations to search through
     * @param configId The configuration id to select from the list
     * @return The first configuration found as indicated by its id, otherwise null.
     */
    public static ApplicationConfiguration selectConfigurationById(List<ApplicationConfiguration> configurations, int configId) {
        for (ApplicationConfiguration current : configurations) {
            if (current.getConfigurationId() == configId) {
                return current;
            }
        }
        return null;
        
    }
   
}
