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
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This wraps around the programming model's json output data, for a list of
 * resources that are running
 * @author Richard Kavanagh
 */
public class CompssResource extends AldeJsonObjectWrapper {

    String hostname = "";
    
    public CompssResource(JSONObject json) {
        super(json);
    }
    
    public CompssResource(String hostname, JSONObject json) {
        super(json);
        this.hostname = hostname;
    }    
    
    public List<CompssImplementation> getImplemenations() {
        return CompssImplementation.getCompssImplementation(json);
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    /**
     * This parses a json object for a list of compss resource objects
     * @param items The json object with items in it
     * @return The list of implementation objects from the json
     */
    public static List<CompssResource> getCompssResouce(JSONObject items) {
        ArrayList<CompssResource> answer = new ArrayList<>();
        /**
         * The code below parse a compss resource from an json originating source
         */        
        if (items.has("resources")) {
            JSONObject resources = items.getJSONObject("resources");
            for (Iterator iterator = resources.keys(); iterator.hasNext();) {
                Object key = iterator.next();
                if (key instanceof String && resources.getJSONObject((String) key) instanceof JSONObject) {
                    JSONObject compssResource = resources.getJSONObject((String) key);
                    answer.add(new CompssResource((String) key, compssResource));
                }
            }
            return answer;
        }
        /**
         * The code below parse a compss resource from an xml originating source
         */
        if (items.has("Resource")) {
            if (items.get("Resource") instanceof JSONArray) {
                JSONArray resources = items.getJSONArray("Resource");
                for (int i = 0; i < resources.length();i++) {
                    if (resources.getJSONObject(i) instanceof JSONObject) {
                        JSONObject compssResource = resources.getJSONObject(i);
                        answer.add(new CompssResource(compssResource.getString("id"), compssResource));
                    }
                }
            }
            JSONObject compssResource = items.getJSONObject("Resource");
            answer.add(new CompssResource(compssResource.getString("id"), compssResource));
            return answer;
        }
        return answer;
    }
    
    public String getState() {
        if (json.has("Status")) {
            return json.getString("Status");
        }           
        return "";
    }

    public int getCoreCount() {
        if (json.has("TotalCPUComputingUnits")) {
            return json.getInt("TotalCPUComputingUnits");
        }           
        return 0;
    }
    
    public int getGpuCount() {
        if (json.has("TotalGPUComputingUnits")) {
            return json.getInt("TotalGPUComputingUnits");
        }           
        return 0;
    }
    
    public int getFpgaCount() {
        if (json.has("TotalFPGAComputingUnits")) {
            return json.getInt("TotalFPGAComputingUnits");
        }           
        return 0;
    }
    
    public double getMemorySize() {
        if (json.has("Memory")) {
            return json.getDouble("Memory");
        }           
        return 0;
    }
    
    public double getDiskSize() {
        if (json.has("Disk")) {
            return json.getDouble("Disk");
        }           
        return 0;
    }
    
}
