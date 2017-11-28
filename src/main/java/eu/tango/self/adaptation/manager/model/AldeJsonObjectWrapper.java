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
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class acts as a baseline for all json based representations of information
 * from the ALDE.
 * @author Richard Kavanagh
 */
public abstract class AldeJsonObjectWrapper {

    protected JSONObject json;

    public AldeJsonObjectWrapper(JSONObject json) {
        this.json = json;
    }
    
    /**
     * This gets the string representation of a given key value
     * @param key The key to obtain from the json object
     * @return The string represented by a given key
     */    
    protected String getString(String key) {
        if (json.has(key) && !json.isNull(key)) {
            return json.getString(key);
        }
        return null;       
    }
    
    /**
     * This gets the double representation of a given key value
     * @param key The key to obtain from the json object
     * @return The double represented by a given key
     */      
    public double getDouble(String key) {
        try {
            if (json.has(key) && !json.isNull(key)) {
                return (double) json.getDouble(key);
            }
        }catch (JSONException ex) {
            //This happens in cases where the integer doesn't parse correctly
        }        
        //the default assumption is zero.
        return 0;       
    }
    
    /**
     * This gets the int representation of a given key value
     * @param key The key to obtain from the json object
     * @return The double represented by a given key
     */     
    public int getInt(String key) {
        try {
            //Tests to see if the excutable_id belongs to a compiled application
            if (json.has(key) && !json.isNull(key)) {
                return (int) json.getInt(key);
            }
        }catch (JSONException ex) {
            //This happens in cases where the integer doesn't parse correctly
        }
        //the default assumption is zero.
        return 0;       
    }        
  
    @Override
    public String toString() {
        return json.toString();
    }    
    
    /**
     * This indicates if a key exists within the json representation
     * @param key The key to check for its existence.
     * @return True only if the key exists, otherwise false.
     */
    public boolean containsKey(String key) {
        return json.has(key);
    }

    /**
     * This returns the underlying json data.
     * @return 
     */
    public JSONObject getJsonObject() {
        return json;
    }
    
    /**
     * This gets this json wrapper as a map.
     * @return The wrapper object as a map of properties.
     */
    public Map<String, Object> getJsonAsMap() {
        if (json == null) {
            return new LinkedTreeMap<>();
        }
        Gson gson = new Gson();      
        String jsonStr = this.json.toString();
        Map<String, Object> map = gson.fromJson(jsonStr, new TypeToken<Map<String, Object>>(){}.getType());
        return map;
    }      
    
}
