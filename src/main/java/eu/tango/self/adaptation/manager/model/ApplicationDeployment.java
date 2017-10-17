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

    int executableId;
    int testbedId;
    String status;
    JSONObject deploymentInfo;

    /**
     * This takes a JSONObject representation of a deployment and converts it
     * into an application deployment
     * @param deploymentInfo The original json object representing a deployment.
     */
    public ApplicationDeployment(JSONObject deploymentInfo) {
        this.deploymentInfo = deploymentInfo;
        executableId = deploymentInfo.getInt("executable_id");
        testbedId = deploymentInfo.getInt("testbed_id");
        if (deploymentInfo.has("status") && !deploymentInfo.isNull("status")) {
            status = deploymentInfo.getString("status");
        } else {
            status = null;
        }
    } 
    
    /**
     * The no-args constructor
     */
    public ApplicationDeployment() {
    }

    /**
     *
     * @param executableId
     * @param testbedId
     */
    public ApplicationDeployment(int executableId, int testbedId) {
        this.executableId = executableId;
        this.testbedId = testbedId;
    }

    /**
     *
     * @return
     */
    public int getExecutableId() {
        return executableId;
    }

    /**
     *
     * @param executableId
     */
    public void setExecutableId(int executableId) {
        this.executableId = executableId;
    }

    /**
     *
     * @return
     */
    public int getTestbedId() {
        return testbedId;
    }

    /**
     *
     * @param testbedId
     */
    public void setTestbedId(int testbedId) {
        this.testbedId = testbedId;
    }

    /**
     * 
     * @return 
     */
    public String getStatus() {
        return status;
    }

    /**
     * 
     * @param status 
     */
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return deploymentInfo.toString();
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
