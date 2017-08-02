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
 */
package eu.tango.self.adaptation.manager;

import eu.tango.energymodeller.datasourceclient.CollectdDataSourceAdaptor;
import eu.tango.energymodeller.datasourceclient.HostDataSource;
import eu.tango.energymodeller.datasourceclient.WattsUpMeterDataSourceAdaptor;
import eu.tango.self.adaptation.manager.actuators.ActuatorInvoker;
import eu.tango.self.adaptation.manager.actuators.AldeAndSlurmActuator;
import eu.tango.self.adaptation.manager.listeners.EnvironmentMonitor;
import eu.tango.self.adaptation.manager.listeners.EventListener;
import eu.tango.self.adaptation.manager.listeners.SlurmJobMonitor;
import eu.tango.self.adaptation.manager.rules.AbstractEventAssessor;
import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.ThresholdEventAssessor;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * This is the main backbone of the self adaptation manager.
 */
public class SelfAdaptationManager {

    private ArrayList<EventListener> listeners = new ArrayList<>();
    private ActuatorInvoker actuator = null;
    private EventAssessor eventAssessor = null;
    private static final String CONFIG_FILE = "self-adaptation-manager.properties";
    private static final String DEFAULT_EVENT_ASSESSOR_PACKAGE
            = "eu.tango.self.adaptation.manager.rules";
    private static final String DEFAULT_DATA_SOURCE_PACKAGE = "eu.tango.energymodeller.datasourceclient";
    private String eventAssessorName = "ThresholdEventAssessor";
    private HostDataSource datasource;

    /**
     * This creates a new instance of the self-adaptation manager.
     */
    public SelfAdaptationManager() {
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            eventAssessorName = config.getString("self.adaptation.manager.event.assessor", eventAssessorName);
            config.setProperty("self.adaptation.manager.event.assessor", eventAssessorName);
            String datasourceStr = config.getString("self.adaptation.manager.environment.monitor.datasource", "CollectdDataSourceAdaptor");
            config.setProperty("self.adaptation.manager.environment.monitor.datasource", datasourceStr);
            setDataSource(datasourceStr);
        } catch (ConfigurationException ex) {
            Logger.getLogger(SelfAdaptationManager.class.getName()).log(Level.INFO, "Error loading the configuration of the Self adaptation manager", ex);
        }
        setEventAssessor(eventAssessorName);
        EventListener listener;
        listener = new EnvironmentMonitor(datasource);
        listener.setEventAssessor(eventAssessor);
        listener.startListening();        
        listeners.add(listener);
        listener = new SlurmJobMonitor();
        listener.setEventAssessor(eventAssessor);
        listener.startListening();  
        actuator = new AldeAndSlurmActuator();
        eventAssessor.setActuator(actuator);
        eventAssessor.setListeners(listeners);
    }

    /**
     * This allows the event assessor to be set. Event assessors are used to
     * decide the which form of adaptation to take.
     *
     * @param eventAssessorName The name of the algorithm to set
     */
    public final void setEventAssessor(String eventAssessorName) {
        try {
            if (!eventAssessorName.startsWith(DEFAULT_EVENT_ASSESSOR_PACKAGE)) {
                eventAssessorName = DEFAULT_EVENT_ASSESSOR_PACKAGE + "." + eventAssessorName;
            }
            eventAssessor = (EventAssessor) (Class.forName(eventAssessorName).newInstance());
        } catch (ClassNotFoundException ex) {
            if (eventAssessor == null) {
                Logger.getLogger(SelfAdaptationManager.class.getName()).log(Level.SEVERE,
                        "The event assessor class was not found: " + eventAssessorName, ex);
                eventAssessor = new ThresholdEventAssessor();
            }
            Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, "The decision engine specified was not found");
        } catch (InstantiationException | IllegalAccessException ex) {
            if (eventAssessor == null) {
                eventAssessor = new ThresholdEventAssessor();
            }
            Logger.getLogger(AbstractEventAssessor.class.getName()).log(Level.WARNING, "The setting of the decision engine did not work", ex);
        }
    }
    
    /**
     * This allows the energy modellers data source to be set
     *
     * @param dataSource The name of the data source to use for the energy
     * modeller
     */
    private void setDataSource(String dataSource) {
        try {
            if (!dataSource.startsWith(DEFAULT_DATA_SOURCE_PACKAGE)) {
                dataSource = DEFAULT_DATA_SOURCE_PACKAGE + "." + dataSource;
            }
            /**
             * This is a special case that requires it to be loaded under the
             * singleton design pattern.
             */
            String wattsUpMeter = DEFAULT_DATA_SOURCE_PACKAGE + ".WattsUpMeterDataSourceAdaptor";
            if (wattsUpMeter.equals(dataSource)) {
                datasource = WattsUpMeterDataSourceAdaptor.getInstance();
            } else {
                datasource = (HostDataSource) (Class.forName(dataSource).newInstance());
            }
        } catch (ClassNotFoundException ex) {
            if (datasource == null) {
                datasource = new CollectdDataSourceAdaptor();
            }
            Logger.getLogger(SelfAdaptationManager.class.getName()).log(Level.WARNING, "The data source specified was not found");
        } catch (InstantiationException | IllegalAccessException ex) {
            if (datasource == null) {
                datasource = new CollectdDataSourceAdaptor();
            }
            Logger.getLogger(SelfAdaptationManager.class.getName()).log(Level.WARNING, "The data source did not work", ex);
        }
    }    

    /**
     * This creates a new self-adaptation manager and is the main entry point
     * for the program.
     *
     * @param args The args are not used.
     */
    public static void main(String[] args) {
        new SelfAdaptationManager();
    }
}
