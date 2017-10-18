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
 *
 * A command to define a testbed is as follows:
 * 
 * 
 * curl -X POST -H'Content-type: application/json' http://127.0.0.1:5000/api/v1/testbeds 
 * -d'{ "name": "nova2", "on_line": true, 
 * "category": "SLURM", "protocol": "SSH", "endpoint": "ns54", 
 * "extra_config": { "enqueue_compss_sc_cfg": "nova.cfg", 
 * "enqueue_env_file": "/home_nfs/home_ejarquej/installations/rc1707/COMPSs/compssenv" }, 
 * "package_formats": [ "SINGULARITY"]}'
 * 
 * An example output of the ALDE is:
 * 
 * {
 * "num_results": 1, 
 * "objects": [
 *  {
 *    "category": "SLURM", 
 *    "endpoint": "ns54", 
 *    "extra_config": "enqueue_compss_sc_cfg": "nova.cfg", "enqueue_env_file": "/home_nfs/home_ejarquej/installations/rc1707/COMPSs/compssenv"
 *    "id": 1, 
 *    "name": "slurm_testbed", 
 *    "nodes": [ns54], 
 *    "on_line": true, 
 *    "package_formats": [], 
 *    "protocol": "SSH"
 *  }
 * ], 
 * "page": 1, 
 * "total_pages": 1
 * }
 * 
 * @author Richard Kavanagh
 */
public class Testbed {

    JSONObject testbedInfo;

    /**
     * No-args constructor
     */    
    public Testbed() {
    }

    /**
     * This wraps the Testbed object around the json object representation of a 
     * testbed.
     * @param testbedInfo 
     */
    public Testbed(JSONObject testbedInfo) {
        this.testbedInfo = testbedInfo;
    }
    
    /**
     * This gets the id of the testbed
     * @return The id of the testbed
     */
    public double getTestbedId() {
        if (testbedInfo.has("id")) {
            return (double) testbedInfo.getInt("id");
        }
        //the default assumption is zero.
        return 0;        
    }    
    
    /**
     * This gets the name of the testbed
     * @return The name of the testbed
     */    
    public String getName() {   
         return getString("name");
    }
    
    /**
     * Indicates if the testbed is online or not
     * @return true if the testbed is online, or the status is unknown. False if
     * on_line field indicates false.
     */
    public boolean isOnline() {
        if (testbedInfo.has("on_line")) {
            return testbedInfo.getBoolean("on_line");
        }
        //the default assumption is true.
        return true;        
    }    
    
    /**
     * This gets the string representation of a given key value
     * @return The string represented by a given key
     */    
    private String getString(String key) {
        if (testbedInfo.has(key)) {
            return testbedInfo.getString(key);
        }
        return null;       
    }    
  
    @Override
    public String toString() {
        return testbedInfo.toString();
    }    
    
    /**
     * This indicates if a key exists within the executable
     * @param key The key to check for its existence.
     * @return True only if the key exists, otherwise false.
     */
    public boolean containsKey(String key) {
        return testbedInfo.has(key);
    }

    /**
     * This returns the testbed's underlying json data.
     * @return 
     */
    public JSONObject getTestbedInfo() {
        return testbedInfo;
    }
    
    /**
     * This gets this configuration as a map.
     * @return The configuration as a map of settings.
     */
    public Map<String, Object> getTestbedAsMap() {
        if (testbedInfo == null) {
            return new LinkedTreeMap<>();
        }
        Gson gson = new Gson();      
        String json = testbedInfo.toString();
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
    }         
    
    
}
