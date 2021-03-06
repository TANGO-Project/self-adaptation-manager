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

import eu.tango.self.adaptation.manager.rules.datatypes.FiringCriteria;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class covers the application definition
 * @author Richard Kavanagh
 */
public class ApplicationDefinition {
    
    private String name;
    private int aldeAppId = -1;
    private String deploymentId;
    private ApplicationType applicationType;
    private int priority;
    private SLALimits slaLimits;
    private JSONObject properties; //helps better describe the application
    private ArrayList<FiringCriteria> adaptationRules = new ArrayList<>();
    private JSONArray executables;
    private JSONArray configurations;
       
    public enum ApplicationType { RIGID, MOULDABLE, CHECKPOINTABLE, MALLEABLE}
    
    /**
     * The main constructor for an application definition
     * @param name The name of the application
     * @param deploymentId The deployment id of the application, set to -1 if not currently deployed
     */
    public ApplicationDefinition(String name, String deploymentId) {
        this.name = name;
        this.deploymentId = deploymentId;
    }

    /**
     * A copy constructor for an application definition
     * @param toClone The application definition to clone.
     */
    public ApplicationDefinition (ApplicationDefinition toClone) {
        this.name = toClone.name;
        this.deploymentId = toClone.deploymentId;
        this.slaLimits = toClone.slaLimits;
        this.adaptationRules = toClone.adaptationRules;
        this.properties = toClone.properties;
        this.executables = toClone.executables;
        this.configurations = toClone.configurations;
    }
    
    /**
     * This gets the applications name
     * @return the name or id of the application
     */
    public String getName() {
        return name;
    }

    /**
     * This sets the applications name
     * @param name the name or id of the application
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * This sets the id that the ALDE knows this application by
     * @param aldeAppId The id value which this application is known by
     */
    public void setAldeAppId(int aldeAppId) {
        this.aldeAppId = aldeAppId;
    }

    /**
     * This gets the id that the ALDE knows this application by
     * @return The id value which this application is known by
     */    
    public int getAldeAppId() {
        return aldeAppId;
    }

    /**
     * This gets the deployment id of the application i.e. describes which instance
     * of the application was run.
     * @return the deploymentId of the application
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * This sets the deployment id of the application i.e. describes which instance
     * of the application was run.
     * @param deploymentId the deploymentId to set
     */
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }
    
    /**
     * This indicates if the deployment id of the application is set. 
     * This id describes a particular instance of the application that was run.
     * @return if the deployment id was set or not, i.e. must not be null or empty
     */
    public boolean hasDeploymentId() {
        return deploymentId != null && !deploymentId.isEmpty();
    }    

    /**
     * This gets the applications type, either not defined i.e. null or
     * RIGID, MOULDABLE, CHECKPOINTABLE, MALLEABLE. It defines how the application
     * may be adapted.
     * @return the application type
     */
    public ApplicationType getApplicationType() {
        return applicationType;
    }

    /**
     * This sets the applications type, either not defined i.e. null or
     * RIGID, MOULDABLE, CHECKPOINTABLE, MALLEABLE. It defines how the application
     * may be adapted.
     * @param applicationType the applicationType to set
     */
    public void setApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    /**
     * This gets the priority value for the application
     * @return The priority of the application
     */
    public int getPriority() {
        return priority;
    }

    /**
     * This sets a job priority value for the application
     * @param priority The priority value to set
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    /**
     * @return the properties
     */
    public JSONObject getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(JSONObject properties) {
        this.properties = properties;
    }
    
    /**
     * Adds a string based property to the application definition. It allows 
     * properties to be added which can then change how any decision is made 
     * when performing adaptation. i.e. QoS parameters
     * @param key The key for the property
     * @param value The value for the property
     */
    public void addProperty(String key, String value) {
        if (value == null) {
            return;
        }      
        if (properties == null) {
            properties = new JSONObject();
        } 
        properties.put(key, value);
    }
    
     /**
     * Adds a string based property to the application definition. It allows 
     * properties to be added which can then change how any decision is made 
     * when performing adaptation. i.e. QoS parameters
     * @param key The key for the property
     * @param value The value for the property
     */
    public void addProperty(String key, Object value) {
        if (value == null) {
            return;
        }
        if (properties == null) {
            properties = new JSONObject();
        }
        properties.put(key, value);
    }   
    
    /**
     * This gets the string representation of a named property
     * @param key The key value of the property to return
     * @return The string representation of a named property
     */
    public String getPropertyAsString(String key) {
        if (properties == null) {
            return null;
        }
        if (properties.get(key) instanceof String) {
            return properties.getString(key);
        }
        //Backup option to use toString method
        return properties.get(key).toString();
    }
 
    /**
     * This gets the integer representation of a named property
     * @param key The key value of the property to return
     * @return The integer representation of a named property
     */
    public Integer getPropertyAsInt(String key) {
        if (properties == null) {
            return null;
        }
        if (properties.get(key) instanceof String) {
            return properties.getInt(key);
        }
        //Backup option to use toString method
        return properties.getInt(key);
    }       
    
    /**
     * This gets the double representation of a named property
     * @param key The key value of the property to return
     * @return The double representation of a named property
     */
    public Double getPropertyAsDouble(String key) {
        if (properties == null) {
            return null;
        }
        if (properties.get(key) instanceof String) {
            return properties.getDouble(key);
        }
        //Backup option to use toString method
        return properties.getDouble(key);
    }    
    
    /**
     * This gets the object based representation of a named property
     * @param key The key value of the property to return
     * @return The string representation of a named property. Null if no properties
     * are set
     */
    public Object getProperty(String key) {
        if (properties == null) {
            return null;
        }        
        return properties.get(key);
    }
    
    /**
     * This indicates if this application on a host has a particular key value
     * pair stored
     * @param key The key to search for
     * @return If a value is stored for a given property.
     */
    public boolean hasProperty(String key) {
        if (properties == null) {
            return false;
        }
        return properties.has(key);
    }    
    
    /**
     * This gets the list of sla limits associated with an application, if one
     * of these criteria are breached then an SLA event violation occurs.
     * @return The list of sla limits associated with the application
     */
    public SLALimits getSlaLimits() {
        return slaLimits;
    }
    
    /**
     * This adds an term to the application's SLA constraints
     * @param term The term to add to the application, if null no item will be added.
     * If getSLALimits returns null a new SLALimits object will be created.
     */
    public void addSlaLimit(SLATerm term) {
        if (term == null) {
            return;
        }
        if (slaLimits == null) {
            slaLimits = new SLALimits();
        }
        slaLimits.addQoSCriteria(term);
    }

    /**
     * This sets the list of sla limits associated with an application, if one
     * of these criteria are breached then an SLA event violation occurs.
     * @param slaLimits The list of sla limits associated with the application 
     */
    public void setSlaLimits(SLALimits slaLimits) {
        this.slaLimits = slaLimits;
    }
    
    /**
     * This sets the list of sla limits associated with an application, if one
     * of these criteria are breached then an SLA event violation occurs.
     * @param slaLimits The list of sla limits associated with the application 
     * @param filter This indicates if the sla limits should be filtered to only
     * rules that mention the application explicitly
     */
    public void setSlaLimits(SLALimits slaLimits, boolean filter) {
        if (filter) {
            setSlaLimits(SLALimits.filterTerms(slaLimits, name));
        } else {
            setSlaLimits(slaLimits);
        }
    }    

    /**
     * This gets the rules that define the mapping between and event and
     * the form of adaptation required to correct the issue.
     * @return the adaptation rules for this application
     */
    public ArrayList<FiringCriteria> getAdaptationRules() {
        return adaptationRules;
    }    

    /**
     * This sets the rules that define the mapping between and event and
     * the adaptation required to correct the issue.
     * @param adaptationRules The adaptation rules to set for this application
     */
    public void setAdaptationRules(ArrayList<FiringCriteria> adaptationRules) {
        this.adaptationRules = adaptationRules;
    }
    
    /**
     * This sets the executables for this application.
     * @param executables The executables as a json object, the internal representation of the ALDE.
     */
    public void setExecutables(JSONArray executables) {
        this.executables = executables;
    }
    
    /**
     * This lists the executables for this application.
     * @return The executables as a json object, the internal representation of the ALDE.
     */
    public JSONArray getExecutablesAsJson() {
        return executables;
    }
    
    /**
     * This lists the executables for this application.
     * @return The executables as a list of objects wrapped around the ALDEs json representation.
     */
    public ArrayList<ApplicationExecutable> getExecutables() {
        ArrayList<ApplicationExecutable> answer = new ArrayList<>();
        for (int i = 0; i < getExecutablesCount(); i++) {
            answer.add(getExecutable(i));
        }
        return answer;
    }    
       
    /**
     * This returns the count of executables that are available for this application
     * @return The count of executables available for this application.
     */
    public int getExecutablesCount() {
        if (executables == null) {
            return 0;
        }
        return executables.length();
    }
    
    /**
     * This indicates if this application definition has a given executable id
     * @param executableId Indicates if this application has a particular application id present
     * @return true if and only if the definition object contains an executable
     * with the named id.
     */
    public boolean hasExecutable(double executableId) {
        ArrayList<ApplicationExecutable> executablesMap = getExecutables();
        for (ApplicationExecutable executable : executablesMap) {
            if (executable.getExecutableId() == executableId) {
                return true;
            }
        }
        return false;
    }        
    
    /**
     * This gets a specific execution.
     * @param index The index value of the execution, 0 is the first in the index.
     * @return The executable with a given index.
     */
    public ApplicationExecutable getExecutable(int index) {
        /**
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
         */
        if (executables == null || index >= executables.length()) {
            Logger.getLogger(ApplicationDefinition.class.getName()).log(Level.SEVERE, 
                    "The executable at index {0} was not found. The count of executables found was: {1}.", 
                    new Object[]{index, executables == null ? "N/A " : executables.length()});
            return null;
        }           
        return new ApplicationExecutable(executables.getJSONObject(index));
    }
    
    /**
     * This gets a specific execution.
     * @param id The id value for the executable
     * @return The executable with a given id.
     */
    public ApplicationExecutable getExecutableById(double id) {
        for (ApplicationExecutable exectuable : getExecutables()) {
            if (exectuable.getExecutableId() == id) {
                return exectuable;
            }
        }
        Logger.getLogger(ApplicationDefinition.class.getName()).log(Level.SEVERE, "The executable with id {0} was not found. The count of executables found was: {1}.",
                new Object[]{id, executables == null ? "N/A " : getExecutables().size()});     
        return null;
    }    
    
    /**
     * This indicates if the executable with a given index is ready.
     * @param index The index of executable
     * @return If the executable is in a state that is suitable for running
     */
    public boolean isExecutableReady(double index) {
        ApplicationExecutable executable = getExecutable((int) index);
        //If the status is set, then the application must be compiled.
        if (executable.containsKey("status")) {
            return executable.getStatus().equals("COMPILED");
        }
        //the default assumption is that it isn't ready.
        return false;
    }
    
    /**
     * This indicates if the executable with a given index is ready.
     * @param executable The executable to test if it is ready or not
     * @return If the executable is in a state that is suitable for running
     */
    public boolean isExecutableReady(ApplicationExecutable executable) {
        //If the status is set, then the application must be compiled.
        if (executable == null) {
            return false;
        }
        if (executable.containsKey("status")) {
            return executable.getStatus().equals("COMPILED");
        }
        //the default assumption is that it isn't ready.
        return false;
    }    

    /**
     * This sets the application configurations for this application.
     * @param configurations The application configurations as a json object, 
     * the internal representation of the ALDE.
     */
    public void setConfigurations(JSONArray configurations) {
        this.configurations = configurations;
    }
    
    /**
     * This gets the application configurations for this application.
     * @return configurations The application configurations as a json array, 
     * the internal representation of the ALDE.
     */
    public JSONArray getConfigurationsAsJson() {
        return configurations;
    }
    
    /**
     * This gets the application configurations for this application.
     * @return The list of configurations.
     */
    public ArrayList<ApplicationConfiguration> getConfigurations() {
        ArrayList<ApplicationConfiguration> answer = new ArrayList<>();
        for (int i = 0; i < getConfigurationsCount(); i++) {
            answer.add(getConfiguration(i));
        }
        return answer;
    }
    
    /**
     * This indicates if this application definition has a given configuration id
     * @param configurationId Indicates if this application has a particular configuration id present
     * @return true if and only if the definition object contains a configuration with the named id.
     */
    public boolean hasConfiguration(double configurationId) {
        ArrayList<ApplicationConfiguration> configs = getConfigurations();
        for (ApplicationConfiguration configuration : configs) {
            if (configuration.getConfigurationId() == configurationId) {
                return true;
            }
        }
        return false;
    }     
    
    /**
     * This gets the list of application executions for this application.
     * @return The list of executions of this application.
     */
    public ArrayList<ApplicationExecutionInstance> getExecutionInstances() {
        ArrayList<ApplicationExecutionInstance> answer = new ArrayList<>();
        for (ApplicationConfiguration config : getConfigurations()) {
            answer.addAll(config.getExecutionInstances());
        }
        return answer;
    }
    
    /**
     * This gets the list of application executions for this application.
     * @param onlyRunning only the ones running, filtering out all instances 
     * that have finished.
     * @return The list of executions of this application.
     */
    public List<ApplicationExecutionInstance> getExecutionInstances(boolean onlyRunning) {
        List<ApplicationExecutionInstance> answer = getExecutionInstances();
        if (onlyRunning) {
            answer = ApplicationExecutionInstance.filterBasedUponStatus(answer, ApplicationExecutionInstance.Status.RUNNING);
        }
        return answer;
    }     
    
    /**
     * This gets the list of application executions for this application.
     * @param slurmId This gets an execution instance from the application
     * @return The list of executions of this application.
     */
    public ApplicationExecutionInstance getExecutionInstance(int slurmId) {
        for (int i = 0; i < getConfigurationsCount(); i++) {
            ApplicationExecutionInstance answer = getConfiguration(i).getExecutionInstance(slurmId);
            if (answer != null) {
                return answer;
            }
        }
        return null;
    }     

    /**
     * This gets the configuration information for a given execution, that was
     * executed using this application definition
     * @param instance The execution instance to get the configuration for
     * @return The application configuration for the given application instance.
     */
    public ApplicationConfiguration getExecutionInstanceConfig(ApplicationExecutionInstance instance) {
        for (int i = 0; i < getConfigurationsCount(); i++) {
            if (getConfiguration(i).hasExecutionInstance(instance)) {
                return getConfiguration(i);
            }
        }
        return null;
    }    
    
    /**
     * This returns the count of configurations that are available for this application
     * @return The count of configurations available for this application.
     */
    public int getConfigurationsCount() {
        if (configurations == null) {
            return 0;
        }
        return configurations.length();
    }    
    
    /**
     * This gets a specific configuration of an application.
     * @param index The index value of the configuration, 0 is the first in the index.
     * @return The configuration of an application.
     */
    public ApplicationConfiguration getConfiguration(int index) {
        if (configurations == null || index >= configurations.length()) {
            Logger.getLogger(ApplicationDefinition.class.getName()).log(Level.SEVERE, "The configuration at index {0} was not found. The count of configurations was: {1}.", 
                new Object[]{index, configurations == null ? "N/A " : configurations.length()});               
            return null;
        }
        return new ApplicationConfiguration(configurations.getJSONObject(index));
    }
    
    /**
     * This indicates if the executable with a given index is ready.
     * @param index The index of executable
     * @return If the executable is in a state that is suitable for running
     */
    public boolean isConfigurationReady(int index) {
        ApplicationConfiguration configuration = getConfiguration(index);
        //Tests to see if the excutable_id belongs to a compiled application
        if (configuration.containsKey("executable_id")) {
            return isExecutableReady(getExecutableById(configuration.getConfigurationsExecutableId()));
        }
        //the default assumption is that it isn't ready.
        return false;
    } 
    
    /**
     * Indicates if the definition of the application represents and application
     * that has been deployed or not.
     * @return True if the application has been deployed otherwise false.
     */
    public boolean isRealisedInstance() {
        /**
         * The deployment id should be -1 if not deployed. i.e. defined only by
         * the ALDE for example, ready to deploy.
         */
        return !deploymentId.equals("-1");
    }

    @Override
    public String toString() {
        return "Name: " + name + " ID: " + aldeAppId + " Deployment ID: " + deploymentId
                + " Application Type: " + applicationType
                + " Priority: " + priority
                + " hasLimits: " + (slaLimits != null)
                + " Rules Size: " + adaptationRules.size()
                + " Executables Count: " + getExecutablesCount()
                + " Configurations Count: " + getConfigurationsCount();
    }
    
}
