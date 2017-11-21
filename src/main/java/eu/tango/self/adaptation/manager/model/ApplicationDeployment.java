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
 * This represents application deployment information from the ALDE. It reports
 * the mapping between an executable and the testbed in which it was launched.
 *
 * A command that defines a deployment in the ALDE would be: curl -X POST
 * -H'Content-type: application/json' http://127.0.0.1:5000/api/v1/deployments
 * -d'{"executable_id": 1, "testbed_id": 1}'
 *
 * A deployment holds information such as:
 *
 * {"executable_id":1,
 * "path":"/home_nfs/home_ejarquej/2022-0203-lddk-d4dco.img", 
 * "testbed_id":1,
 * "status":"UPLOADED_UPDATED"}
 *
 *
 * @author Richard Kavanagh
 */
public class ApplicationDeployment {

    JSONObject deploymentInfo;

    /**
     * This takes a JSONObject representation of a deployment and converts it
     * into an application deployment
     * @param deploymentInfo The original json object representing a deployment.
     */
    public ApplicationDeployment(JSONObject deploymentInfo) {
        this.deploymentInfo = deploymentInfo;
    }

    /**
     *
     * @return
     */
    public int getExecutableId() {
        if (deploymentInfo.has("executable_id")) {
            return (int) deploymentInfo.getInt("executable_id");
        }
        //the default assumption is zero.
        return 0;        
    }

    /**
     *
     * @return
     */
    public int getTestbedId() {
        if (deploymentInfo.has("testbed_id")) {
            return (int) deploymentInfo.getInt("testbed_id");
        }
        //the default assumption is zero.
        return 0;
    }
    
    /**
     * This gets the status of the deployment
     * @return 
     */
    public String getStatus() {
        return getString("status");
    }
    
    /**
     * This gets the string representation of a given key value
     * @return The string represented by a given key
     */    
    private String getString(String key) {     
        //Tests to see if the excutable_id belongs to a compiled application
        if (deploymentInfo.has(key) && !deploymentInfo.isNull(key)) {
            return deploymentInfo.getString(key);
        }
        return null;       
    }        

    @Override
    public String toString() {
        return deploymentInfo.toString();
    }
    
    /**
     * This indicates if a key exists within the deployment information
     * @param key The key to check for its existence.
     * @return True only if the key exists, otherwise false.
     */
    public boolean containsKey(String key) {
        return deploymentInfo.has(key);
    }    
    
    /**
     * This gets this deployment as a map.
     * @return The deployment as a map of settings.
     */
    public Map<String, Object> getDeploymentAsMap() {
        if (deploymentInfo == null) {
            return new LinkedTreeMap<>();
        }
        Gson gson = new Gson();      
        String json = deploymentInfo.toString();
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
    }     

}
