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
package eu.tango.self.adaptation.manager.actuators;

import eu.tango.self.adaptation.manager.model.ApplicationConfiguration;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.model.ApplicationDeployment;
import eu.tango.self.adaptation.manager.model.ApplicationExecutionInstance;
import eu.tango.self.adaptation.manager.model.Gpu;
import eu.tango.self.adaptation.manager.model.Testbed;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;

/**
 * This client directly interfaces with the ALDE to query it
 *
 * @author Richard Kavanagh
 */
public class AldeClient {

    private static final String CONFIG_FILE = "self-adaptation-manager.properties";
    private String baseUri = "http://localhost:5000/api/v1/";

    public AldeClient() {
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            baseUri = config.getString("self.adaptation.manager.alde.rest.uri", baseUri);
            if (!baseUri.endsWith("/")) {
                baseUri = baseUri + "/";
            }
            config.setProperty("self.adaptation.manager.alde.rest.uri", baseUri);
        } catch (ConfigurationException ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.INFO, "Error loading the configuration of the Self adaptation manager", ex);
        }
    }
    
    /**
     * This lists all applications that are deployable by the ALDE
     *
     * @return The list of applications known to the ALDE
     */
    public List<ApplicationDefinition> getApplicationDefinitions() {
        ArrayList<ApplicationDefinition> answer = new ArrayList<>();
        try {
            JSONObject apps = readJsonFromUrl(baseUri + "applications");
            JSONArray objects = apps.getJSONArray("objects");
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                Object next = iterator.next();
                if (next instanceof JSONObject) {
                    JSONObject object = (JSONObject) next;
                    ApplicationDefinition app = new ApplicationDefinition(object.getString("name"), null);
                    app.setAldeAppId(object.getInt("id"));
                    app.setExecutables(object.getJSONArray("executables"));
                    app.setConfigurations(object.getJSONArray("execution_configurations"));
                        answer.add(app);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    /**
     * This gets the application definition of a single application
     *
     * @param name The name of the application to get
     * @return The definition of the named application
     */
    public ApplicationDefinition getApplicationDefinition(String name) {
        List<ApplicationDefinition> allApps = getApplicationDefinitions();
        for (ApplicationDefinition app : allApps) {
            if (app.getName().equals(name)) {
                return app;
            }
        }
        Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, "The application {0} was not found.", name);
        return null;
    }

    /**
     * This gets the application definition of a single application
     *
     * @param deployment The application deployment to get the definition object
     * for
     * @return The definition of the named application
     */
    public ApplicationDefinition getApplicationDefinition(ApplicationDeployment deployment) {
        List<ApplicationDefinition> allApps = getApplicationDefinitions();
        for (ApplicationDefinition app : allApps) {
            if (app.hasExecutable(deployment.getExecutableId())) {
                return app;
            }
        }
        Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, "The application was not found via its deployment.");        
        return null;
    }
    
    /**
     * This gets the application definition from its configuration object
     *
     * @param configuration The application deployment to get the definition object
     * for
     * @return The definition of the named application
     */
    public ApplicationDefinition getApplicationDefinition(ApplicationConfiguration configuration) {
        List<ApplicationDefinition> allApps = getApplicationDefinitions();
        for (ApplicationDefinition app : allApps) {
            if (app.hasConfiguration(configuration.getConfigurationsExecutableId())) {
                return app;
            }
        }
        Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, "The application was not found via its application configuration.");        
        return null;
    }
    
    /**
     * This gets the application definition of a running instance of an application
     *
     * @param instance The application instance to get the definition object
     * for
     * @return The definition of the named application
     */
    public ApplicationDefinition getApplicationDefinition(ApplicationExecutionInstance instance) {
        List<ApplicationDefinition> allApps = getApplicationDefinitions();
        int configId = instance.getExecutionConfigurationsId();
        for (ApplicationDefinition app : allApps) {
            if (app.hasConfiguration(configId)) {
                return app;
            }
            
        }
        Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, "The application was not found via its execution instance."); 
        return null;
    }
    
    /**
     * This lists the application configurations for a given application
     *
     * @param applicationId The application id to get the configuration for
     * @return The list of application configurations known to the ALDE
     */
    public List<ApplicationConfiguration> getConfigurations(int applicationId) {
        ArrayList<ApplicationConfiguration> answer = new ArrayList<>();
        try {
            JSONObject apps = readJsonFromUrl(baseUri + "applications/" + applicationId + "/execution_configurations");
            JSONArray objects = apps.getJSONArray("objects");
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                Object next = iterator.next();
                if (next instanceof JSONObject) {
                    JSONObject object = (JSONObject) next;
                    answer.add(new ApplicationConfiguration(object));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }    

    /**
     * This gets the list of all running execution instances known to the ALDE.
     * @return The list of application execution instances from the ALDE.
     */
    public List<ApplicationExecutionInstance> getExecutionInstances() {
        return getExecutionInstances(true);
    }
       
    /**
     * This gets the list of all running execution instances known to the ALDE.
     * @param runningOnly Indicates if it should be the currently running instances
     * only, if false then all instances that ever existed will be reported.
     * @return The list of application execution instances from the ALDE.
     */
    public List<ApplicationExecutionInstance> getExecutionInstances(boolean runningOnly) {
        //Example curl  http://127.0.0.1:5000/api/v1/executions -G -H'Content-type: application/json' -d'q={"filters":[{"name":"status","op":"like","val":"RUNNING"}]}'
        ArrayList<ApplicationExecutionInstance> answer = new ArrayList<>();
        try {
            JSONObject params = null;
            if (runningOnly) {
                params = new JSONObject();
                JSONArray filter = new JSONArray();
                JSONObject item1 = new JSONObject();
                item1.put("name","status");
                item1.put("op","like");
                item1.put("val","RUNNING");
                filter.put(item1);
                params.put("filters", filter);
            }
            JSONObject apps = readJsonFromUrl(baseUri + "executions", params);
            JSONArray objects = apps.getJSONArray("objects");
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                Object next = iterator.next();
                if (next instanceof JSONObject) {
                    JSONObject object = (JSONObject) next;
                    answer.add(new ApplicationExecutionInstance(object));
                }
            }
        } catch (IOException | JSONException ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, "parse error", ex);
        }
        return answer;
    }     
    
    /**
     * This gets the ALDE json definition for a slurm job.
     * @param deploymentId The deployment id of the slurm job.
     * @return The application execution instance of the slurm job.
     */
    public ApplicationExecutionInstance getExecutionInstance(String deploymentId) {
        /**
         * curl  http://127.0.0.1:5000/api/v1/executions -G -H'Content-type: application/json' -d'q=
         * {"filters":[{"name":"status","op":"like","val":"RUNNING"},
         * {"name":"execution_configuration_id","op":"like","val":1}]}'
         * 
         * The one below is:
         * curl  http://127.0.0.1:5000/api/v1/executions -G -H'Content-type: 
         * application/json' -d'q={"filters":[{"name":"slurm_sbatch_id","op":"like",
         * "val":"4734"}]}'
         */
        JSONObject params = new JSONObject();
        JSONArray filter = new JSONArray();
        JSONObject item1 = new JSONObject();
        item1.put("name","slurm_sbatch_id");
        item1.put("op","like");
        item1.put("val",deploymentId);
        filter.put(item1);       
        params.put("filters", filter);
        try {        
            JSONObject appInstance = readJsonFromUrl(baseUri + "executions", params);
            JSONArray array = appInstance.getJSONArray("objects");
            if (array.length() > 0 && array.getJSONObject(0) != null) {
                return new ApplicationExecutionInstance(appInstance.getJSONArray("objects").getJSONObject(0));
            }
        } catch (IOException | JSONException ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, "Parse Error", ex);
        }
        //A backup exhaustive search in running
        for (ApplicationExecutionInstance instance : getExecutionInstances()) {
            if ((instance.getSlurmId()+"").equals(deploymentId)) {
                Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, "Found via exhaustive search in running set");
                return instance;
            }
        }
        //A backup exhaustive search
        for (ApplicationExecutionInstance instance : getExecutionInstances(false)) {
            if ((instance.getSlurmId()+"").equals(deploymentId)) {
                Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, "Found via exhaustive search");
                return instance;
            }
        }        
        return null;
    }

    /**
     * This lists all applications that are deployable by the ALDE
     *
     * @return The list of applications known to the ALDE
     */
    public List<Testbed> getTestbeds() {
        ArrayList<Testbed> answer = new ArrayList<>();
        try {
            JSONObject apps = readJsonFromUrl(baseUri + "testbeds");
            JSONArray objects = apps.getJSONArray("objects");
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                Object next = iterator.next();
                if (next instanceof JSONObject) {
                    JSONObject object = (JSONObject) next;
                    answer.add(new Testbed(object));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    /**
     * This gets a testbed from its given id value
     *
     * @param testbedId The testbeds id value
     * @return The json object containing properties of the testbed
     */
    public Testbed getTestbed(int testbedId) {
        List<Testbed> testbeds = getTestbeds();
        for (Testbed testbed : testbeds) {
            if (testbed.getTestbedId() == testbedId) {
                return testbed;
            }
        }
        return null;
    }

    /**
     * This lists all deployments of an applications by the ALDE
     *
     * @return The list of application deployments known to the ALDE
     */
    public List<ApplicationDeployment> getDeployments() {
        ArrayList<ApplicationDeployment> answer = new ArrayList<>();
        try {
            /**
             * A deployment holds information such as:
             *
             * {"executable_id":1,
             * "path":"/home_nfs/home_ejarquej/2022-0203-lddk-d4dco.img",
             * "testbed_id":1, "status":"UPLOADED_UPDATED"}
             */
            JSONObject apps = readJsonFromUrl(baseUri + "deployments");
            JSONArray objects = apps.getJSONArray("objects");
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                Object next = iterator.next();
                if (next instanceof JSONObject) {
                    JSONObject object = (JSONObject) next;
                    answer.add(new ApplicationDeployment(object));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }
    
    /**
     * This lists all gpus that are known by the ALDE
     *
     * @return The list of gpus known to the ALDE
     */
    public List<Gpu> getGpus() {
        ArrayList<Gpu> answer = new ArrayList<>();
        try {
            JSONObject apps = readJsonFromUrl(baseUri + "gpus");
            JSONArray objects = apps.getJSONArray("objects");
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                Object next = iterator.next();
                if (next instanceof JSONObject) {
                    JSONObject object = (JSONObject) next;
                    answer.add(new Gpu(object));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }
    
    /**
     * This reads the entire contents from a reader and generates a String
     *
     * @param rd The reader to perform the full read with
     * @return The String representation of the contents of the reader.
     * @throws IOException
     */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * This takes a url and parses the json from it into a Json object.
     *
     * @param url The url to parse
     * @return The json object provided by the named url
     * @throws IOException
     */
    public static JSONObject readJsonFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }
    
    /**
     * This takes a url and parses the json from it into a Json object.
     *
     * @param url The url to parse
     * @param parameters The parameters to add to the query
     * @return The json object provided by the named url
     * @throws IOException
     */
    public JSONObject readJsonFromUrl(String url, JSONObject parameters) throws IOException {
        if (parameters == null) {
            return readJsonFromUrl(url);
        } else {
            return readJsonFromUrl(url + "?q=" + parameters.toString());
        }
    }    

    /**
     * This starts an executable running
     * @param executionId The execution id to start
     * @throws IOException 
     */
    public void cancelApplication(int executionId) throws IOException {
        /**
         * The command that this code replicates: curl -X PATCH -H'Content-type:
         * application/json'
         * http://127.0.0.1:5000/api/v1/executions/197 -d'{"status": CANCEL}'
         */
        JSONObject json = new JSONObject();
        json.put("status", "CANCEL");
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {        
            HttpPatch request = new HttpPatch(baseUri + "executions/" + executionId);
            StringEntity params = new StringEntity(json.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            httpClient.execute(request);
            // handle response here...
        } catch (Exception ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    /**
     * This starts an executable running
     * @param executionId The execution id to start
     * @throws IOException 
     */
    public void executeApplication(int executionId) throws IOException {
        /**
         * The command that this code replicates: curl -X PATCH -H'Content-type:
         * application/json'
         * http://127.0.0.1:5000/api/v1/execution_configurations/1
         * -d'{"launch_execution": true}'
         */
        JSONObject json = new JSONObject();
        json.put("launch_execution", "true");
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPatch request = new HttpPatch(baseUri + "execution_configurations/" + executionId);
            StringEntity params = new StringEntity(json.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            httpClient.execute(request);
            // handle response here...
        } catch (Exception ex) {
            Logger.getLogger(AldeClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}