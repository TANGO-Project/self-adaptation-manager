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
import eu.tango.self.adaptation.manager.rules.datatypes.FiringCriteria;
import java.util.ArrayList;
import java.util.Map;
import org.json.JSONArray;

/**
 * This class covers the application definition
 * @author Richard Kavanagh
 */
public class ApplicationDefinition {
    
    private String name;
    private int aldeAppId = -1;
    private String deploymentId;
    private SLALimits slaLimits;
    private ArrayList<FiringCriteria> adaptationRules = new ArrayList<>();
    private JSONArray executables;
    private JSONArray configurations;
      
    /**
     * The main constructor for an application definition
     * @param name The name of the application
     * @param deploymentId The deployment id of the application, set to -1 if not currently deployed
     */
    public ApplicationDefinition(String name, String deploymentId) {
        this.name = name;
        this.deploymentId = deploymentId;
    }

    /**
     * A copy constructor for an application definition
     * @param toClone The application definition to clone.
     */
    public ApplicationDefinition (ApplicationDefinition toClone) {
        this.name = toClone.name;
        this.deploymentId = toClone.deploymentId;
        this.slaLimits = toClone.slaLimits;
        this.adaptationRules = toClone.adaptationRules;
        this.executables = toClone.executables;
        this.configurations = toClone.configurations;
    }
    
    /**
     * This gets the applications name
     * @return the name or id of the application
     */
    public String getName() {
        return name;
    }

    /**
     * This sets the applications name
     * @param name the name or id of the application
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * This sets the id that the ALDE knows this application by
     * @param aldeAppId The id value which this application is known by
     */
    public void setAldeAppId(int aldeAppId) {
        this.aldeAppId = aldeAppId;
    }

    /**
     * This gets the id that the ALDE knows this application by
     * @return The id value which this application is known by
     */    
    public int getAldeAppId() {
        return aldeAppId;
    }

    /**
     * This gets the deployment id of the application i.e. describes which instance
     * of the application was run.
     * @return the deploymentId of the application
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * This sets the deployment id of the application i.e. describes which instance
     * of the application was run.
     * @param deploymentId the deploymentId to set
     */
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    /**
     * This gets the list of sla limits associated with an application, if one
     * of these criteria are breached then an SLA event violation occurs.
     * @return The list of sla limits associated with the application
     */
    public SLALimits getSlaLimits() {
        return slaLimits;
    }

    /**
     * This sets the list of sla limits associated with an application, if one
     * of these criteria are breached then an SLA event violation occurs.
     * @param slaLimits The list of sla limits associated with the application 
     */
    public void setSlaLimits(SLALimits slaLimits) {
        this.slaLimits = slaLimits;
    }
    
    /**
     * This sets the list of sla limits associated with an application, if one
     * of these criteria are breached then an SLA event violation occurs.
     * @param slaLimits The list of sla limits associated with the application 
     * @param filter This indicates if the sla limits should be filtered to only
     * rules that mention the application explicitly
     */
    public void setSlaLimits(SLALimits slaLimits, boolean filter) {
        if (filter) {
            setSlaLimits(SLALimits.filterTerms(slaLimits, name));
        } else {
            setSlaLimits(slaLimits);
        }
    }    

    /**
     * This gets the rules that define the mapping between and event and
     * the form of adaptation required to correct the issue.
     * @return the adaptation rules for this application
     */
    public ArrayList<FiringCriteria> getAdaptationRules() {
        return adaptationRules;
    }    

    /**
     * This sets the rules that define the mapping between and event and
     * the adaptation required to correct the issue.
     * @param adaptationRules The adaptation rules to set for this application
     */
    public void setAdaptationRules(ArrayList<FiringCriteria> adaptationRules) {
        this.adaptationRules = adaptationRules;
    }
    
    /**
     * This sets the executables for this application.
     * @param executables The executables as a json object, the internal representation of the ALDE.
     */
    public void setExecutables(JSONArray executables) {
        this.executables = executables;
    }    

    /**
     * This lists the executables for this application.
     * @return The executables as a json object, the internal representation of the ALDE.
     */
    public JSONArray getExecutables() {
        return executables;
    }
    
    /**
     * This lists the executables for this application.
     * @return The executables as a json object, the internal representation of the ALDE.
     */
    public ArrayList<Map<String, Object>> getExecutablesAsMap() {
        ArrayList<Map<String, Object>> answer = new ArrayList<>();
        for (int i = 0; i < getExecutablesCount(); i++) {
            answer.add(getExecutable(i));
        }
        return answer;
    }    
       
    /**
     * This returns the count of executables that are available for this application
     * @return The count of executables available for this application.
     */
    public int getExecutablesCount() {
        if (executables == null) {
            return 0;
        }
        return executables.length();
    }
    
    /**
     * This gets a specific execution as a map.
     * @param index The index value of the execution, 0 is the first in the index.
     * @return The executables as a map of settings.
     */
    public Map<String, Object> getExecutable(int index) {
        /**
         * {
         * "executables": [
         * {
         *    "application_id": 1,
         *    "compilation_script": "compilation.sh",
         *    "compilation_type": "singularity:pm",
         *    "executable_file": null,
         *    "id": 1,
         *    "source_code_file": "f5a8e16b-6c36-4092-97cb-6081374d9b29.zip",
         *    "status": "NOT_COMPILED"
         }
         ],
         "execution_scripts": [],
         "id": 1,
         "name": "my_app"
         }
         */
        if (executables == null || index >= executables.length()) {
            return new LinkedTreeMap<>();
        }        
        Gson gson = new Gson();       
        String json = executables.get(index).toString();
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
    }
    
    /**
     * This indicates if the executable with a given index is ready.
     * @param index The index of executable
     * @return If the executable is in a state that is suitable for running
     */
    public boolean isExecutableReady(int index) {
        Map<String, Object> properties = getExecutable(index);
        //If the status is set, then the application must be compiled.
        if (properties.containsKey("status")) {
            return properties.get("status").equals("COMPILED");
        }
        //the default assumption is that it isn't ready.
        return false;
    }

    /**
     * This sets the application configurations for this application.
     * @param configurations The application configurations as a json object, 
     * the internal representation of the ALDE.
     */
    public void setConfigurations(JSONArray configurations) {
        this.configurations = configurations;
    }
    
    /**
     * This gets the application configurations for this application.
     * @return configurations The application configurations as a json object, 
     * the internal representation of the ALDE.
     */
    public JSONArray getConfigurations() {
        return configurations;
    }
    
    /**
     * This gets the application configurations for this application.
     * @return The list of configurations as a map of settings.
     */
    public ArrayList<Map<String, Object>> getConfigurationsAsMap() {
        ArrayList<Map<String, Object>> answer = new ArrayList<>();
        for (int i = 0; i < getConfigurationsCount(); i++) {
            answer.add(getConfiguration(i));
        }
        return answer;
    }       
    
    /**
     * This returns the count of configurations that are available for this application
     * @return The count of configurations available for this application.
     */
    public int getConfigurationsCount() {
        if (configurations == null) {
            return 0;
        }
        return configurations.length();
    }    
    
    /**
     * This gets a specific configuration as a map.
     * @param index The index value of the configuration, 0 is the first in the index.
     * @return The configuration as a map of settings.
     */
    public Map<String, Object> getConfiguration(int index) {
        if (configurations == null || index >= configurations.length()) {
            return new LinkedTreeMap<>();
        }
        /**
         * Example Output:
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
         */
        Gson gson = new Gson();      
        String json = configurations.get(index).toString();
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
    }
    
    /**
     * Indicates if the definition of the application represents and application
     * that has been deployed or not.
     * @return True if the application has been deployed otherwise false.
     */
    public boolean isRealisedInstance() {
        /**
         * The deployment id should be -1 if not deployed. i.e. defined only by
         * the ALDE for example, ready to deploy.
         */
        return deploymentId.equals("-1");
    }
    
}
