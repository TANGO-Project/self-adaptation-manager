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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * This encapsulates a json object representing gpus in the system.
 * @author Richard Kavanagh
 */
public class Gpu {

/**
 * An example of the output this object encapsulates is:
 * 
 * [kavanagr@ns54 self-adaptation-manager]$  curl http://127.0.0.1:5000/api/v1/gpus
{
  "num_results": 4, 
  "objects": [
    {
      "id": 29445, 
      "model_name": "K20 GPU Accelerator", 
      "node": {
        "disabled": false, 
        "id": 3, 
        "information_retrieved": true, 
        "name": "ns50", 
        "state": "IDLE", 
        "testbed_id": 1
      }, 
      "node_id": 3, 
      "vendor_id": "Nvidia"
    }, 
    {
      "id": 29446, 
      "model_name": "K20 GPU Accelerator", 
      "node": {
        "disabled": false, 
        "id": 3, 
        "information_retrieved": true, 
        "name": "ns50", 
        "state": "IDLE", 
        "testbed_id": 1
      }, 
      "node_id": 3, 
      "vendor_id": "Nvidia"
    }, 
    {
      "id": 29447, 
      "model_name": "K20 GPU Accelerator", 
      "node": {
        "disabled": false, 
        "id": 5, 
        "information_retrieved": true, 
        "name": "ns51", 
        "state": "DOWN*", 
        "testbed_id": 1
      }, 
      "node_id": 5, 
      "vendor_id": "Nvidia"
    }, 
    {
      "id": 29448, 
      "model_name": "K20 GPU Accelerator", 
      "node": {
        "disabled": false, 
        "id": 5, 
        "information_retrieved": true, 
        "name": "ns51", 
        "state": "DOWN*", 
        "testbed_id": 1
      }, 
      "node_id": 5, 
      "vendor_id": "Nvidia"
    }
  ], 
  "page": 1, 
  "total_pages": 1
}
 */    
  
    JSONObject gpuInfo;    

    /**
     * This wraps the gpu object around the json object representation of a gpu.
     * @param gpuInfo The gpu object to wrap around
     */
    public Gpu(JSONObject gpuInfo) {
        this.gpuInfo = gpuInfo;
    }
    
    /**
     * This gets the model name of the gpu
     * @return The model name of the gpu
     */    
    public String getModelName() {   
         return getString("model_name");
    }
    
    /**
     * This gets the id of the gpu
     * @return The id of the gpu
     */
    public int getId() {
        if (gpuInfo.has("id")) {
            return (int) gpuInfo.getInt("id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the id of the gpu
     * @return The id of the gpu
     */
    public JSONObject getNodeObject() {
        if (gpuInfo.has("node")) {
            return gpuInfo.getJSONObject("node");
        }
        //the default is null.
        return null;        
    }
    
    /**
     * This gets the id of the gpu
     * @return The id of the gpu
     */
    public Node getNode() {
        if (gpuInfo.has("node")) {
            return new Node(gpuInfo.getJSONObject("node"));
        }
        //the default is null.
        return null;        
    }    
        
    
    /**
     * This gets the string representation of a given key value
     * @return The string represented by a given key
     */    
    private String getString(String key) {
        if (gpuInfo.has(key) && !gpuInfo.isNull(key)) {
            return gpuInfo.getString(key);
        }
        return null;       
    }    
  
    @Override
    public String toString() {
        return gpuInfo.toString();
    }    
    
    /**
     * This indicates if a key exists within the gpu
     * @param key The key to check for its existence.
     * @return True only if the key exists, otherwise false.
     */
    public boolean containsKey(String key) {
        return gpuInfo.has(key);
    }

    /**
     * This returns the gpus's underlying json data.
     * @return 
     */    
    public JSONObject getGpuInfo() {
        return gpuInfo;
    }

    /**
     * This gets this node as a map.
     * @return The node as a map of properties.
     */
    public Map<String, Object> getGpuAsMap() {
        if (gpuInfo == null) {
            return new LinkedTreeMap<>();
        }
        Gson gson = new Gson();      
        String json = gpuInfo.toString();
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
    }
    
    /**
     * This filters a list of GPUs by the hostname to which it is attached.
     * @param gpuList The list of gpus to filter 
     * @param hostname The hostname to get the gpus available for
     * @return The list of GPUS attached to the named host
     */
    public static List<Gpu> filterByHostName(List<Gpu> gpuList, String hostname) {
        ArrayList<Gpu> answer = new ArrayList<>();
        for (Gpu current : gpuList) {
            if (current.getNode().getName().equals(hostname)) {
                answer.add(current);
            }
        }
        return answer;
    }
    
    /**
     * This filters a list of GPUs by the hostname to which it is attached.
     * @param gpuList The list of gpus to filter 
     * @param idleOnly Indicates if the host must be idle hosts to be included in the list
     * @return The list of GPUS attached to the named host
     */
    public static List<Gpu> filterOutUnavailable(List<Gpu> gpuList, boolean idleOnly) {
        ArrayList<Gpu> answer = new ArrayList<>();
        for (Gpu current : gpuList) {
            if (current.getNode().isDisabled()) {
                continue;
            }
            if (idleOnly && !current.getNode().getState().equals("IDLE")) {
                continue;
            }
            answer.add(current);
        }
        return answer;
    }    
    
}
