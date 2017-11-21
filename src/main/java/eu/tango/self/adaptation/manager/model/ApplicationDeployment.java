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
public class ApplicationDeployment extends AldeJsonObjectWrapper {

    /**
     * This takes a JSONObject representation of a deployment and converts it
     * into an application deployment
     * @param deploymentInfo The original json object representing a deployment.
     */
    public ApplicationDeployment(JSONObject deploymentInfo) {
        super(deploymentInfo);
    }

    /**
     *
     * @return
     */
    public int getExecutableId() {
        if (json.has("executable_id")) {
            return (int) json.getInt("executable_id");
        }
        //the default assumption is zero.
        return 0;        
    }

    /**
     *
     * @return
     */
    public int getTestbedId() {
        if (json.has("testbed_id")) {
            return (int) json.getInt("testbed_id");
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

}
