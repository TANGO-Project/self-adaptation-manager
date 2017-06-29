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
 * @TODO Add calls to ALDE when the alde develops further.
 * @author Richard Kavanagh
 */
public class AldeAndSlurmActuator implements ActuatorInvoker, Runnable {

    private final AldeActuator alde = new AldeActuator();
    private final SlurmActuator slurm = new SlurmActuator();

    @Override
    public ApplicationDefinition getApplication(String name, String deploymentId) {
        return slurm.getApplication(name, deploymentId);
    }

    @Override
    public ApplicationOnHost getTask(String name, String deployment, int taskId) {
        return slurm.getTask(name, deployment, taskId);
    }

    @Override
    public double getTotalPowerUsage(String applicationName, String deploymentId) {
        return slurm.getTotalPowerUsage(applicationName, deploymentId);
    }

    @Override
    public double getPowerUsageTask(String applicationName, String deploymentId, int taskId) {
        return slurm.getPowerUsageTask(applicationName, deploymentId, taskId);
    }

    @Override
    public double getAveragePowerUsage(String applicationName, String deploymentId, String taskType) {
        return slurm.getAveragePowerUsage(applicationName, deploymentId, taskType);
    }

    @Override
    public List<String> getTaskTypesAvailableToAdd(String applicationName, String deploymentId) {
        return slurm.getTaskTypesAvailableToAdd(applicationName, deploymentId);
    }

    @Override
    public List<Integer> getTaskIdsAvailableToRemove(String applicationName, String deploymentId) {
        return slurm.getTaskIdsAvailableToRemove(applicationName, deploymentId);
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
            case ADD_TASK:
            case REMOVE_TASK:
            case SCALE_TO_N_TASKS:
            case PAUSE_APP:
            case UNPAUSE_APP:
            case HARD_KILL_APP:
            case INCREASE_WALL_TIME:
            case REDUCE_WALL_TIME:
                slurm.actuate(response);
                break;
            case RESELECT_ACCELERATORS:
                alde.actuate(response);
            default:
                Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, "The Response type was not recoginised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }

    @Override
    public void addTask(String applicationName, String deploymentId, String taskType) {
        slurm.addTask(applicationName, deploymentId, taskType);
    }

    @Override
    public void deleteTask(String applicationName, String deployment, String taskID) {
        slurm.deleteTask(applicationName, deployment, taskID);
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
