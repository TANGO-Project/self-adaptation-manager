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

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
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
public class ApplicationConfiguration {
 
    JSONObject configurationInformation;

    public ApplicationConfiguration() {
    }
    
    public ApplicationConfiguration(JSONObject configurationInformation) {
        this.configurationInformation = configurationInformation;
    }
  
    /**
     * This gets the id of the configuration
     * @return The id of the configuration
     */
    public int getConfigurationId() {
        if (configurationInformation.has("id")) {
            return (int) configurationInformation.getInt("id");
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
        if (configurationInformation.has("application_id")) {
            return (int) configurationInformation.getInt("application_id");
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
        if (configurationInformation.has("executable_id")) {
            return (double) configurationInformation.getInt("executable_id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the id of the testbed to be used by the configuration
     * @return The id of the testbed to be used by the configuration
     */
    public int getConfigurationsTestbedId() {
        if (configurationInformation.has("testbed_id")) {
            return (int) configurationInformation.getInt("testbed_id");
        }
        //the default assumption is zero.
        return 0;        
    }       
    
    /**
     * This gets the count of nodes needed by this configuration
     * @return The number of nodes needed by the configuration
     */
    public double getNodesNeeded() {
        if (configurationInformation.has("num_nodes")) {
            return (double) configurationInformation.getDouble("num_nodes");
        }
        //the default assumption is zero.
        return 0;   
    }       
    
    /**
     * This gets the count of cpus needed per node by this configuration
     * @return The number of CPUs per node needed by the configuration
     */    
    public double getCpusNeededPerNode() {
        if (configurationInformation.has("num_cpus_per_node")) {
            return (double) configurationInformation.getDouble("num_cpus_per_node");
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
        if (configurationInformation.has("num_gpus_per_node")) {
            return (double) configurationInformation.getDouble("num_gpus_per_node");
        }
        //the default assumption is zero.
        return 0;       
    }
    
    @Override
    public String toString() {
        return configurationInformation.toString();
    }    
    
    /**
     * This indicates if a key exists within the configuration
     * @param key The key to check for its existence.
     * @return True only if the key exists, otherwise false.
     */
    public boolean containsKey(String key) {
        return configurationInformation.has(key);
    }

    /**
     * This returns the application's configuration data's underlying json representation.
     * @return 
     */
    public JSONObject getConfigurationInformation() {
        return configurationInformation;
    }
    
    /**
     * This gets this configuration as a map.
     * @return The configuration as a map of settings.
     */
    public Map<String, Object> getConfigurationAsMap() {
        if (configurationInformation == null) {
            return new LinkedTreeMap<>();
        }
        Gson gson = new Gson();      
        String json = configurationInformation.toString();
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
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
