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
    
}
