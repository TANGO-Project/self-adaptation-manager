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

import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.model.ApplicationDeployment;
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
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * This client directly interfaces with the ALDE to query it
 *
 * @author Richard Kavanagh
 */
public class AldeClient {

    private static final String CONFIG_FILE = "self-adaptation-manager.properties";
    private static String baseUri = "http://localhost:5000/api/v1/";

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
    public ArrayList<ApplicationDefinition> getApplicationDefinitions() {
        ArrayList<ApplicationDefinition> answer = new ArrayList<>();
        try {
            JSONObject apps = readJsonFromUrl(baseUri + "applications");
            JSONArray objects = apps.getJSONArray("objects");
            for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
                Object next = iterator.next();
                if (next instanceof JSONObject) {
                    JSONObject object = (JSONObject) next;
                    ApplicationDefinition app = new ApplicationDefinition(object.getString("name"), "-1");
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
    public ApplicationDefinition getApplicationDefintion(String name) {
        ArrayList<ApplicationDefinition> allApps = getApplicationDefinitions();
        for (ApplicationDefinition app : allApps) {
            if (app.getName().equals(name)) {
                return app;
            }
        }
        return null;
    }

    /**
     * This gets the application definition of a single application
     *
     * @param deployment The application deployment to get the definition object
     * for
     * @return The definition of the named application
     */
    public ApplicationDefinition getApplicationDefintion(ApplicationDeployment deployment) {
        ArrayList<ApplicationDefinition> allApps = getApplicationDefinitions();
        for (ApplicationDefinition app : allApps) {
            if (app.hasExecutable(deployment.getExecutableId())) {
                return app;
            }
        }
        return null;
    }

    /**
     * This lists all applications that are deployable by the ALDE
     *
     * @return The list of applications known to the ALDE
     */
    public ArrayList<Testbed> getTestbeds() {
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
     * @param testbedId The testbeds id value
     * @return The json object containing properties of the testbed
     */
    public Testbed getTestbed(int testbedId) {
        ArrayList<Testbed> testbeds = getTestbeds();
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
    public ArrayList<ApplicationDeployment> getDeployments() {
        ArrayList<ApplicationDeployment> answer = new ArrayList<>();
        try {
            /**
             * A deployment holds information such as:
             *
             * {"executable_id":1,
             *  "path":"/home_nfs/home_ejarquej/2022-0203-lddk-d4dco.img",
             *  "testbed_id":1,
             *  "status":"UPLOADED_UPDATED"}
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
            JSONObject json = new JSONObject(jsonText);
            return json;
        }
    }

    public void executeApplication(double executionId) throws IOException {
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
            // handle exception here
        }
    }
}
