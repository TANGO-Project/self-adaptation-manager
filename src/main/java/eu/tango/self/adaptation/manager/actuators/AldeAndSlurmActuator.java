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

import eu.tango.energymodeller.datasourceclient.HostDataSource;
import eu.tango.energymodeller.datasourceclient.SlurmDataSourceAdaptor;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An actuator that merges the usage of SLURM and the ALDE. The actuator most
 * appropriate to perform a given type of actuation is used.
 *
 * @author Richard Kavanagh
 */
public class AldeAndSlurmActuator implements ActuatorInvoker, Runnable {

    private final AldeActuator alde;
    private final SlurmActuator slurm;

    /**
     * No-args constructor
     */
    public AldeAndSlurmActuator() {
        alde = new AldeActuator();
        slurm = new SlurmActuator();
    }
    
    /**
     * This constructor can be used to share a datasource among actuators.
     * @param datasource
     */
    public AldeAndSlurmActuator(HostDataSource datasource) {
        if (datasource instanceof SlurmDataSourceAdaptor) {
            slurm = new SlurmActuator((SlurmDataSourceAdaptor) datasource);
        } else {
            slurm = new SlurmActuator();
        }
        alde = new AldeActuator(datasource);
    }    
    
    
    @Override
    public ApplicationDefinition getApplication(String name, String deploymentId) {
        ApplicationDefinition answer = slurm.getApplication(name, deploymentId);
        ApplicationDefinition aldeAnswer = alde.getApplication(name, deploymentId);
        if (answer != null && aldeAnswer != null) {
            answer.setConfigurations(aldeAnswer.getConfigurationsAsJson());
            answer.setExecutables(aldeAnswer.getExecutablesAsJson());
            answer.setAldeAppId(aldeAnswer.getAldeAppId());
        }
        return answer;
    }

    @Override
    public List<ApplicationOnHost> getTasksOnHost(String host) {
        return alde.appendQoSInformation(slurm.getTasksOnHost(host));
    }

    @Override
    public List<ApplicationOnHost> getTasks() {
        return alde.appendQoSInformation(slurm.getTasks());
    }

    @Override
    public List<ApplicationOnHost> getTasks(String applicationName, String deploymentId) {
        return alde.appendQoSInformation(slurm.getTasks(applicationName, deploymentId));
    }    

    @Override
    public void hardKillApp(String applicationName, String deploymentId) {
        slurm.hardKillApp(applicationName, deploymentId);
    }

    @Override
    public void actuate(Response response) {
        switch (response.getActionType()) {
            case ADD_CPU:
            case REMOVE_CPU:
            case PAUSE_APP:
            case UNPAUSE_APP:
            case PAUSE_SIMILAR_APPS:
            case UNPAUSE_SIMILAR_APPS:                  
            case OVERSUBSCRIBE_APP:
            case EXCLUSIVE_APP:
            case KILL_SIMILAR_APPS:
            case KILL_APP:
            case HARD_KILL_APP:
            case INCREASE_WALL_TIME:
            case REDUCE_WALL_TIME:
            case SET_POWER_CAP:
            case INCREASE_POWER_CAP:
            case REDUCE_POWER_CAP:
            case INCREASE_WALL_TIME_SIMILAR_APPS:
            case REDUCE_WALL_TIME_SIMILAR_APPS:
            case MINIMIZE_WALL_TIME_SIMILAR_APPS:
            case STARTUP_HOST:
            case SHUTDOWN_HOST:
                slurm.actuate(response);
                break;
            case SCALE_TO_N_TASKS:
            case ADD_TASK:
            case REMOVE_TASK:
                /**
                 * Task type information indicates that it is an ALDE based adaptation
                 * otherwise slurm adds more nodes to the job. 
                 */
                if (response.hasAdaptationDetail("TASK_TYPE")) {
                    //This sends the compss command through the ALDE
                    alde.actuate(response);
                } else { //Generically add more nodes to the job
                    alde.actuate(response);
                }
                break;
            case RESELECT_ACCELERATORS:
                alde.setParent(this);
                alde.actuate(response);
                break;
            default:
                Logger.getLogger(AldeAndSlurmActuator.class.getName()).log(Level.SEVERE, "The Response type was not recoginised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }

    @Override
    public void addResource(String applicationName, String deploymentId, String taskType) {
        alde.addResource(applicationName, deploymentId, taskType);
    }

    @Override
    public void removeResource(String applicationName, String deployment, String taskID) {
        alde.removeResource(applicationName, deployment, taskID);
    }

    @Override
    public void scaleToNTasks(String applicationId, String deploymentId, Response response) {
        slurm.scaleToNTasks(applicationId, deploymentId, response);
    }

    @Override
    public void run() {
        Thread aldeActuatorThread = new Thread((Runnable) alde);
        aldeActuatorThread.setDaemon(true);
        aldeActuatorThread.start();
        Thread slurmActuatorThread = new Thread((Runnable) slurm);
        slurmActuatorThread.setDaemon(true);
        slurmActuatorThread.start();
    }

}
