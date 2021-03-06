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

import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * This creates a rest interface for the submission of events from arbritrary
 * sources.
 *
 * @author Richard Kavanagh
 */
public class RestEventMonitor implements EventListener, Runnable {

    // Base URI the Grizzly HTTP server will listen on
    private String baseUri = "http://localhost:8080/sam/";

    private EventAssessor eventAssessor;
    private HttpServer server;
    private static final String CONFIG_FILE = "self-adaptation-manager.properties";
    
    private RestEventMonitor() {
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            baseUri = config.getString("self.adaptation.manager.rest.uri", baseUri);
            config.setProperty("self.adaptation.manager.rest.uri", baseUri);
        } catch (ConfigurationException ex) {
            Logger.getLogger(RestEventMonitor.class.getName()).log(Level.INFO, "Error loading the configuration of the Self adaptation manager", ex);
        }        
    }

    /**
     * SingletonHolder is loaded on the first execution of
     * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
     * not before.
     */
    private static class SingletonHolder {

        private static final RestEventMonitor INSTANCE = new RestEventMonitor();
    }

    /**
     * This creates a new singleton instance of the rest event monitor.
     *
     * @return A singleton instance of a rest event monitor.
     */
    public static RestEventMonitor getInstance() {
        return SingletonHolder.INSTANCE;
    }    

    @Override
    public void setEventAssessor(EventAssessor assessor) {
        eventAssessor = assessor;
    }

    @Override
    public EventAssessor getEventAssessor() {
        return eventAssessor;
    }

    public static void main(String[] args) {
        RestEventMonitor monitor = new RestEventMonitor();
        monitor.startListening();
        try {
            System.in.read();
            monitor.stopListening();
        } catch (IOException ex) {
            Logger.getLogger(RestEventMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }

    public HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in eu.tango.self.adaptation.manager.listeners package
        final ResourceConfig rc = new ResourceConfig().packages("eu.tango.self.adaptation.manager.listeners");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at baseUri
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), rc);
    }

    @Override
    public void startListening() {
        if (server == null) {
            server = startServer();
        }
        Logger.getLogger(RestEventMonitor.class.getName()).log(Level.INFO,
                String.format("Jersey app started with WADL available at "
                        + "%sapplication.wadl", baseUri));
    }

    @Override
    public void stopListening() {
        if (server != null) {
            server.shutdownNow();
        }
        server = null;
    }

    @Override
    public boolean isListening() {
        return server != null;
    }
    
    /**
     * This passes an event on to this monitor's event assessor for evaluation.
     * @param event The event to pass on for processing
     * @return The response object for the event
     */
    public Response assessEvent(EventData event) {
        if (eventAssessor != null) {
            return eventAssessor.assessEvent(event);
        } 
        return null;
    }

    @Override
    public void run() {
        startListening();
        /*
         * No action needed, this thread should just wait for the server instance 
         * to close down.
        */
    }

}
