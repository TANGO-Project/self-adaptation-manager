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
 * This encapsulates a json object representing a node.
 * @author Richard Kavanagh
 */
public class Node {

    /**
     * An example of a node is:
     * 
     "nodes":[
      {
         "testbed_id":1,
         "name":"ns55",
         "disabled":false,
         "id":1,
         "state":"IDLE",
         "information_retrieved":true
      },
      ...
    */

    JSONObject nodeInfo;    

    /**
     * This wraps the Node object around the json object representation of a 
     * node.
     * @param nodeInfo The node object to wrap around
     */    
    public Node(JSONObject nodeInfo) {
        this.nodeInfo = nodeInfo;
    }
    
    /**
     * This gets the name of the node
     * @return The name of the node
     */    
    public String getName() {   
         return getString("name");
    }
    
    /**
     * This gets the id of the node
     * @return The id of the node
     */
    public int getId() {
        if (nodeInfo.has("id")) {
            return (int) nodeInfo.getInt("id");
        }
        //the default assumption is zero.
        return 0;        
    }    
    
    /**
     * This gets the id of the testbed the node is attached to
     * @return The id of the testbed
     */
    public int getTestbedId() {
        if (nodeInfo.has("testbed_id")) {
            return nodeInfo.getInt("testbed_id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the state of the node
     * @return The state of the node
     */    
    public String getState() {   
         return getString("state");
    }
    
    /**
     * Indicates if the information for the node has been retrieved or not
     * @return true if the information has been retrieved. False if
     * information_retrieved field indicates false.
     */
    public boolean isInformationRetrieved() {
        if (nodeInfo.has("information_retrieved")) {
            return nodeInfo.getBoolean("information_retrieved");
        }
        //the default assumption is false.
        return false;        
    }
    
    /**
     * Indicates if the node is disabled or not
     * @return true if the node is disabled, or the status is unknown. False if
     * disabled field indicates false.
     */
    public boolean isDisabled() {
        if (nodeInfo.has("disabled")) {
            return nodeInfo.getBoolean("disabled");
        }
        //the default assumption is true.
        return true;        
    }    
    
    /**
     * This gets the string representation of a given key value
     * @return The string represented by a given key
     */    
    private String getString(String key) {
        if (nodeInfo.has(key) && !nodeInfo.isNull(key)) {
            return nodeInfo.getString(key);
        }
        return null;       
    }    
  
    @Override
    public String toString() {
        return nodeInfo.toString();
    }    
    
    /**
     * This indicates if a key exists within the node
     * @param key The key to check for its existence.
     * @return True only if the key exists, otherwise false.
     */
    public boolean containsKey(String key) {
        return nodeInfo.has(key);
    }

    /**
     * This returns the nodes's underlying json data.
     * @return 
     */    
    public JSONObject getNodeInfo() {
        return nodeInfo;
    }

    /**
     * This gets this node as a map.
     * @return The node as a map of properties.
     */
    public Map<String, Object> getNodeAsMap() {
        if (nodeInfo == null) {
            return new LinkedTreeMap<>();
        }
        Gson gson = new Gson();      
        String json = nodeInfo.toString();
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
    }
    
    /**
     * This gets a node from a list of hostnames
     * @param hostList The list of hosts to filter 
     * @param hostname The hostname of the host to get from the list
     * @return The node that represents the host, else null
     */
    public static Node getHostFromList(List<Node> hostList, String hostname) {
        for (Node current : hostList) {
            if (current.getName().equals(hostname)) {
                return current;
            }
        }
        return null;
    } 
    
    /**
     * This filters a list of Nodes by if the host is available or not
     * @param nodeList The list of nodes to filter 
     * @param idleOnly Indicates if the host must be idle to be included in the list
     * @return The list of Nodes, removing all disabled hosts and optionally all non-idle hosts as well
     */
    public static List<Node> filterOutUnavailable(List<Node> nodeList, boolean idleOnly) {
        ArrayList<Node> answer = new ArrayList<>();
        for (Node current : nodeList) {
            if (current.isDisabled()) {
                continue;
            }
            if (idleOnly && !current.getState().equals("IDLE")) {
                continue;
            }
            answer.add(current);
        }
        return answer;
    }     
    
}
