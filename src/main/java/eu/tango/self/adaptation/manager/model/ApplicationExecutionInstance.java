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
        if (json.has("id")) {
            return (int) json.getInt("id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the id of the executable configuration used to launch the application
     * Note: an application may be comprised of several applications, each of which
     * may be compiled to use different accelerators.
     * @return The id of the executable configuration used to launch the execution
     */
    public int getConfigurationsExecutableId() {
        if (json.has("execution_configuration_id")) {
            return (int) json.getInt("execution_configuration_id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the slurm job id of the execution
     * @return The slurm job id of the execution
     */
    public int getSlurmId() {
        if (json.has("slurm_sbatch_id")) {
            return (int) json.getInt("slurm_sbatch_id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the executions status
     * @return The status string of the execution
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
    
}
