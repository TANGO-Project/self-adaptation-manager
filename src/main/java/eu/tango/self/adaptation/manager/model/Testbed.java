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
import org.json.JSONArray;
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
public class Testbed extends AldeJsonObjectWrapper {

    /**
     * This wraps the Testbed object around the json object representation of a 
     * testbed.
     * @param testbedInfo 
     */
    public Testbed(JSONObject testbedInfo) {
        super(testbedInfo);
    }
    
    /**
     * This gets the id of the testbed
     * @return The id of the testbed
     */
    public int getTestbedId() {
        if (json.has("id")) {
            return json.getInt("id");
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
        if (json.has("on_line")) {
            return json.getBoolean("on_line");
        }
        //the default assumption is true.
        return true;        
    }
    
    /**
     * This lists the nodes that are available in the testbed.
     * @return The list of nodes available in the testbed.
     */
    public ArrayList<Node> getNodes() {
        ArrayList<Node> answer = new ArrayList<>();
        if (json.has("nodes")) {
            JSONArray array = json.getJSONArray("nodes");
            for(int i = 0; i < array.length(); i++) {
                answer.add(new Node(array.getJSONObject(i)));
            }
        }
        return answer;  
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Testbed) {
            Testbed other = (Testbed) obj;
            return this.getTestbedId() == other.getTestbedId() && this.getName().equals(other.getName());
        }
        return false;
    }       
  
}
