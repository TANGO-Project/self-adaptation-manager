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
 * This represents application executable information from the ALDE. It represents 
 * a particular implementation of an application. The application has a list of
 * executables and this object represents one of them.
 *
 * Example of executable information:
         * {
         * "executables": [
         * {
         *    "application_id": 1,
         *    "compilation_script": "compilation.sh",
         *    "compilation_type": "singularity:pm",
         *    "executable_file": null,
         *    "id": 1,
         *    "source_code_file": "f5a8e16b-6c36-4092-97cb-6081374d9b29.zip",
         *    "status": "NOT_COMPILED"
         }
         ],
         "execution_scripts": [],
         "id": 1,
         "name": "my_app"
         }
 *
 * @author Richard Kavanagh
 */
public class ApplicationExecutable extends AldeJsonObjectWrapper {

    /**
     * This wraps the ApplicationExecutable object around the json object
     * @param executableInfo 
     */
    public ApplicationExecutable(JSONObject executableInfo) {
        super(executableInfo);
    }
    
    /**
     * This gets the id of the executable to be used in the configuration
     * Note: an application may be comprised of several executables, each of which
     * may be compiled to use different accelerators.
     * @return The id of the executable to be used in the configuration
     */
    public double getExecutableId() {
        if (json.has("id")) {
            return (double) json.getInt("id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the id of the application.
     * Note: an application may be comprised of several executables, each of which
     * may be compiled to use different accelerators.
     * @return The id of the application to be used in the configuration
     */
    public int getConfigurationsApplicationId() {
        if (json.has("application_id")) {
            return (int) json.getInt("application_id");
        }
        //the default assumption is zero.
        return 0;        
    }
    
    /**
     * This gets the compilation script
     * @return The compilation script
     */    
    public String getCompilationScript() {   
        return getString("compilation_script");   
    }
 
    /**
     * This gets the compilation type
     * @return The compilation type
     */    
    public String getCompilationType() {   
        return getString("compilation_type");   
    }    

    /**
     * This gets the executable file
     * @return The executable file
     */    
    public String getExecutableFile() {
        return getString("executable_file");
    }      
    
    /**
     * This gets the source code file
     * @return The source code file
     */    
    public String getSourceCodeFile() {   
         return getString("source_code_file");
    }
    
    /**
     * This indicates if the executable is compiled.
     * @return If the executable is in a state that is suitable for running
     */
    public boolean isExecutableReady() {
        //the status must be set and it must be compiled.
        return (getStatus() != null && getStatus().equals("COMPILED"));
    }
    
    /**
     * This gets the applications status
     * @return The status string of the application 
     */    
    public String getStatus() {
        //Tests to see if the excutable_id belongs to a compiled application
        return getString("status");     
    }
    
}
