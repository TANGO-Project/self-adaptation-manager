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
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This represents application's executable that has been launched from the ALDE. 
 * It represents a particular configuration of an application instance that has been
 * launched.
 *
 * Example of executable information:
         * {
         * __tablename__ = 'executions'
         * id = db.Column(db.Integer, primary_key=True)
         * execution_type = db.Column(db.String)
         * status = db.Column(db.String)
         * output = db.Column(db.String)
         * execution_configuration_id = db.Column(db.Integer, db.ForeignKey('execution_configurations.id'))
         * slurm_sbatch_id = db.Column(db.Integer)
         * }
 *
 * @author Richard Kavanagh
 */
public class ApplicationExecutionInstance extends AldeJsonObjectWrapper {
    
    public enum Status {
        RUNNING,COMPLETED,FAILED,UNKNOWN;
    }

    /**
     * This takes a JSONObject representation of a running application and converts it
     * into an application execution instance
     * @param executionInfo The original json object representing an application running.
     */
    public ApplicationExecutionInstance(JSONObject executionInfo) {
        super(executionInfo);
    }
    
    /**
     * This gets the id of the execution
     * @return The id of the execution
     */
    public int getExecutionId() {
        return getInt("id");    
    }
    
    /**
     * This gets the id of the executable configuration used to launch the application
     * Note: an application may be comprised of several executables, each of which
     * may be compiled to use different accelerators.
     * @return The id of the executable configuration used to launch the execution
     */
    public int getExecutionConfigurationsId() {
        return getInt("execution_configuration_id");      
    }
    
    /**
     * This gets the slurm job id of the execution
     * @return The slurm job id of the execution
     */
    public int getSlurmId() {
        return getInt("slurm_sbatch_id");   
    }
    
    /**
     * This gets the slurm job id of any additional execution caused by scaling
     * @return The slurm job id of any additional execution
     */
    public String getExtraSlurmId() {
        /**
         * The style follows the pattern:
         * "children": [
            {
              "execution_configuration_id": null,
              "execution_type": "SINGULARITY:PM",
              "id": 377,
              "output": null,
              "parent_id": 376,
              "slurm_sbatch_id": "",
              "status": "RUNNING"
            },
            {
              "execution_configuration_id": null,
              "execution_type": "SINGULARITY:PM",
              "id": 378,
              "output": null,
              "parent_id": 376,
              "slurm_sbatch_id": "",
              "status": "RUNNING"
            }
          ],
         */
        if (containsKey("children")) {
            String answer = "";
            for (ApplicationExecutionInstance app : getChildInstances()) {
                answer = answer + (answer.isEmpty() ? app.getSlurmId() : ":" + app.getSlurmId());
            }
            return answer;
        }
        return ""; //Not expected to reach this statement
    }
    
    /**
     * This gets any additional execution instances that have been used in order
     * to expand the current execution.
     * @return 
     */
    public ArrayList<ApplicationExecutionInstance> getChildInstances() {
        ArrayList<ApplicationExecutionInstance> answer = new ArrayList<>();
        if (getKeyType("children").equals(JSONArray.class)) {
            //The list case
            JSONArray array = getJsonArray("children");
            if (array != null) {
                for(int i = 0; i < array.length();i++) {
                    answer.add(new ApplicationExecutionInstance(array.getJSONObject(i)));
                }
            }
        } else if (getKeyType("children").equals(JSONObject.class)) {        
            //The single item case
            JSONObject item = getJsonObject("children");
            if (item != null) {
                answer.add(new ApplicationExecutionInstance(item));
            }
            return answer;
        }
        return answer;
    }
    
    /**
     * This gets the executions Status
     * @return The Status string of the execution
     */    
    public String getStatus() {
        return getString("status");
    }
    
    
    /**
     * This gets the executions type
     * @return The execution type string of the execution
     */    
    public String getExecutionType() {
        return getString("execution_type");
    }  
        
    
    /**
     * This gets the executions output
     * @return The output string of the execution
     */    
    public String getOutput() {
        return getString("output");
    }
    
    /**
     * This gets the energy consumption of the execution instance
     * @return The energy consumed while running this execution instance
     */
    public int getEnergy() {
        return getInt("energy_output");
    }
    
    /**
     * This gets the duration of a completed execution instance
     * @return The completion time of an execution instance
     */
    public int getDuration() {
        return getInt("runtime_output");
    }

    /**
     * This filters a list of application execution instances by their Status
     * @param appList The list of application executions to filter 
     * @param status Indicates the Status of the application
     * @return  The list of execution instances with the given Status
     */
    public static List<ApplicationExecutionInstance> filterBasedUponStatus(List<ApplicationExecutionInstance> appList, Status status) {
        return filterBasedUponStatus(appList, status.toString());
    }    
    
    /**
     * This filters a list of application execution instances by their status
     * @param appList The list of application executions to filter 
     * @param status Indicates the status of the application
     * @return  The list of execution instances with the given status
     */
    public static List<ApplicationExecutionInstance> filterBasedUponStatus(List<ApplicationExecutionInstance> appList, String status) {
        ArrayList<ApplicationExecutionInstance> answer = new ArrayList<>();
        for (ApplicationExecutionInstance current : appList) {
            if (current.getStatus().equals(status)) {
                answer.add(current);
            }
        }
        return answer;
    }
    
    /**
     * This filters a list of application execution instances by their status
     * @param appList The list of application executions to filter 
     * @param slurmJobId The slurm job id, to find from the list of executions
     * @return  The execution instance with the named slurm job id
     */
    public static ApplicationExecutionInstance getExecutionInstance(List<ApplicationExecutionInstance> appList, int slurmJobId) {
        for (ApplicationExecutionInstance current : appList) {
            if (current.getSlurmId() == slurmJobId) {
                return current;
            }
        }
        return null;
    }
    
}
