/**
 * Copyright 2018 University of Leeds
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
package eu.tango.self.adaptation.manager.listeners;

import eu.tango.energymodeller.types.energyuser.Host;
import static eu.tango.self.adaptation.manager.io.JsonUtils.readJsonFromFile;
import static eu.tango.self.adaptation.manager.io.JsonUtils.readJsonFromUrl;
import eu.tango.self.adaptation.manager.model.CompssImplementation;
import eu.tango.self.adaptation.manager.model.CompssResource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This acts as the client for the Tango programming model runtime, in order to
 * directly adapt applications that are running. It is aimed to be used in the
 * project's remote processing use case.
 *
 * @author Richard Kavanagh
 */
public class ProgrammingModelClient {

    private String baseUri = "http://localhost:5000/api/v1/";

    /**
     * This provides the client commands through the programming model:
     *
     * https://github.com/TANGO-Project/programming-model-and-runtime When the
     * application is running, the adaptation of the nodes can be performed by
     * means of the adapt_compss_resources command in the following way: $
     * adapt_compss_resources <master_node> <master_job_id> CREATE SLURM-Cluster
     * default <singularity_image>
     * This command will submit another job requesting a new resource of type
     * "default" (the same as the requested in the enqueue_compss) running the
     * COMPSs worker of the singularity_image. $ adapt_compss_resources
     * <master_node> <master_job_id> REMOVE SLURM-Cluster <node_to_delete>
     *
     */
    /**
     * Part of the role of this class will be to parse the output JSON or XML of
     * the programming model runtime in order to provide an application's
     * runtime information.
     */
    /**
     * This gets the compss resources list, indicating which hosts have which
     * worker instances available.
     * @return
     */
    public List<CompssResource> getCompssResources() {
        try {
            //TODO determine how the Json file will be passed
            JSONObject items = readJsonFromUrl(baseUri + "executions", null);
            return CompssResource.getCompssResouce(items);
        } catch (IOException | JSONException ex) {
            Logger.getLogger(ProgrammingModelClient.class.getName()).log(Level.SEVERE, "parse error", ex);
        }
        return new ArrayList<>();
    }
    
    /**
     * Part of the role of this class will be to parse the output JSON or XML of
     * the programming model runtime in order to provide an application's
     * runtime information.
     */
    /**
     * This gets the compss resources list, indicating which hosts have which
     * worker instances available.
     * @return
     */
    public List<Host> getCompssHostList() {
        List<Host> answer = new ArrayList<>();
        try {
            //TODO determine how the Json file will be passed
            JSONObject items = readJsonFromUrl(baseUri + "executions", null);
            List<CompssResource> resourceListing = CompssResource.getCompssResouce(items);
            for (CompssResource resource : resourceListing) {
                answer.add(
                        new Host(Integer.parseInt(
                                resource.getHostname().replaceAll("[^0-9]", "")), 
                                resource.getHostname()));
            }
            
        } catch (IOException | JSONException ex) {
            Logger.getLogger(ProgrammingModelClient.class.getName()).log(Level.SEVERE, "parse error", ex);
        }
        return answer;
    }    

    /**
     * This lists the various different versions of workers that are available.
     * @return 
     */
    public List<CompssImplementation> getCompssImplementation() {
        try {
            //TODO determine how the Json file will be passed
            JSONObject items = readJsonFromUrl(baseUri + "executions", null);
            return CompssImplementation.getCompssImplementation(items);
        } catch (IOException | JSONException ex) {
            Logger.getLogger(ProgrammingModelClient.class.getName()).log(Level.SEVERE, "parse error", ex);
        }
        return new ArrayList<>();
    }

    /**
     * This main method is for testing purposes only and should be removed in time.
     * @param args 
     * TODO remove this method
     */
    public static void main(String[] args) {
        try {
            JSONObject items = readJsonFromFile("C:\\Users\\Richard\\Documents\\University Work\\Research\\Tango\\WP5\\DeltaTec Usecase\\compss jobs\\Post Run\\in_remote.prof");
            ArrayList<CompssImplementation> answer = (ArrayList<CompssImplementation>) CompssImplementation.getCompssImplementation(items);
            for (CompssImplementation answer1 : answer) {
                answer1.toString();
            }
        } catch (IOException ex) {
            Logger.getLogger(ProgrammingModelClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            JSONObject items = readJsonFromFile("C:\\Users\\Richard\\Documents\\University Work\\Research\\Tango\\WP5\\DeltaTec Usecase\\compss jobs\\Post Run\\in_remote.prof");

            ArrayList<CompssResource> answer = (ArrayList<CompssResource>) CompssResource.getCompssResouce(items);
            for (CompssResource answer1 : answer) {
                System.out.println(answer1.getHostname());
                System.out.println(answer1.getImplemenations().size());
                for (CompssImplementation answer2 : answer1.getImplemenations()) {
                    System.out.println(answer2.getName());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ProgrammingModelClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
