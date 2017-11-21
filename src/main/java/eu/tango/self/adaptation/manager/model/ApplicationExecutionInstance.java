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
import java.util.Map;
import org.json.JSONObject;

/**
 * This represents application's executable that has been launched from the ALDE. 
 * It represents a particular configuration of an application instance that has been
 * launched.
 *
 * Example of executable information:
         * {
         * __tablename__ = 'executions'
         * id = db.Column(db.Integer, primary_key=True)
         * execution_type = db.Column(db.String)
         * status = db.Column(db.String)
         * output = db.Column(db.String)
         * execution_configuration_id = db.Column(db.Integer, db.ForeignKey('execution_configurations.id'))
         * slurm_sbatch_id = db.Column(db.Integer)
         * }
 *
 * @author Richard Kavanagh
 */
public class ApplicationExecutionInstance {

    JSONObject executionInfo;

    /**
     * This takes a JSONObject representation of a running application and converts it
     * into an application execution instance
     * @param executionInfo The original json object representing an application running.
     */
    public ApplicationExecutionInstance(JSONObject executionInfo) {
        this.executionInfo = executionInfo;
    }
    
    /**
     * This gets the id of the execution
     * @return The id of the execution
     */
    public int getExecutionId() {
        if (executionInfo.has("id")) {
            return (int) executionInfo.getInt("id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the id of the executable configuration used to launch the application
     * Note: an application may be comprised of several applications, each of which
     * may be compiled to use different accelerators.
     * @return The id of the executable configuration used to launch the execution
     */
    public int getConfigurationsExecutableId() {
        if (executionInfo.has("execution_configuration_id")) {
            return (int) executionInfo.getInt("execution_configuration_id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the slurm job id of the execution
     * @return The slurm job id of the execution
     */
    public int getSlurmId() {
        if (executionInfo.has("slurm_sbatch_id")) {
            return (int) executionInfo.getInt("slurm_sbatch_id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the executions status
     * @return The status string of the execution
     */    
    public String getStatus() {
        return getString("status");
    }
    
    
    /**
     * This gets the executions type
     * @return The execution type string of the execution
     */    
    public String getExecutionType() {
        return getString("execution_type");
    }  
        
    
    /**
     * This gets the executions output
     * @return The output string of the execution
     */    
    public String getOutput() {
        return getString("output");
    }      
    

    @Override
    public String toString() {
        return executionInfo.toString();
    }
    
    /**
     * This indicates if a key exists within the application's execution instance information
     * @param key The key to check for its existence.
     * @return True only if the key exists, otherwise false.
     */
    public boolean containsKey(String key) {
        return executionInfo.has(key);
    }     
    
    /**
     * This gets the string representation of a given key value
     * @return The string represented by a given key
     */    
    private String getString(String key) {     
        //Tests to see if the excutable_id belongs to a compiled application
        if (executionInfo.has(key) && !executionInfo.isNull(key)) {
            return executionInfo.getString(key);
        }
        return null;       
    }    
    
    /**
     * This gets this execution instance as a map.
     * @return The execution as a map of properties.
     */
    public Map<String, Object> getExecutionInstanceAsMap() {
        if (executionInfo == null) {
            return new LinkedTreeMap<>();
        }
        Gson gson = new Gson();      
        String json = executionInfo.toString();
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
    }       
    
}
