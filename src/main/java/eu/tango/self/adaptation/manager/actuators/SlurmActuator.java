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

import eu.tango.energymodeller.EnergyModeller;
import eu.tango.energymodeller.datasourceclient.SlurmDataSourceAdaptor;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This actuator interacts with the Device supervisor SLURM, with the aim of
 * querying for information and invoking adaptation.
 *
 * @author Richard Kavanagh
 */
public class SlurmActuator extends AbstractActuator {

    private final SlurmDataSourceAdaptor datasource = new SlurmDataSourceAdaptor();
    private final EnergyModeller modeller = EnergyModeller.getInstance();

    @Override
    public ApplicationDefinition getApplication(String applicationName, String deploymentId) {
        /**
         * The energy modeller's app id is a number
         */
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        for (ApplicationOnHost app : apps) {
            if ((app.getName().trim().equals(applicationName.trim()))
                    && (app.getId() + "").equals(deploymentId.trim())) {
                ApplicationDefinition answer = new ApplicationDefinition(applicationName, deploymentId);
                return answer;
            }
        }
        return null;
    }

    @Override
    public ApplicationOnHost getTask(String name, String deploymentId, int taskId) {
        /**
         * The energy modeller's app id is a number
         */
        Host host = getHostFromTaskId(taskId);
        List<ApplicationOnHost> tasks = modeller.getApplication(name, Integer.parseInt(deploymentId));
        for (ApplicationOnHost task : tasks) {
            if (task.getAllocatedTo().equals(host)) {
                return task;
            }
        }
        return null;
    }

    @Override
    public double getTotalPowerUsage(String applicationName, String deploymentId) {
        double answer = 0.0;
        List<ApplicationOnHost> tasks = modeller.getApplication(deploymentId, Integer.parseInt(deploymentId));
        for (CurrentUsageRecord record : modeller.getCurrentEnergyForApplication(tasks)) {
            answer = answer + record.getPower();
        }
        return answer;
    }

    /**
     * Converts a task Id into a host
     *
     * @param taskId The task id to convert
     * @return The host
     */
    private Host getHostFromTaskId(int taskId) {
        Collection<Host> hosts = modeller.getHostList();
        for (Host host : hosts) {
            if (host.getId() == taskId) {
                return host;
            }
        }
        return null;
    }

    @Override
    public double getPowerUsageTask(String applicationName, String deploymentId, int taskId) {
        ApplicationOnHost task = getTask(deploymentId, deploymentId, taskId);
        if (task == null) {
            return 0;
    }
        return modeller.getCurrentEnergyForApplication(task).getPower();
    }

    @Override
    public double getAveragePowerUsage(String applicationName, String deploymentId, String taskType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getTaskTypesAvailableToAdd(String applicationName, String deploymentId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Integer> getTaskIdsAvailableToRemove(String applicationName, String deploymentId) {
        List<Integer> answer = new ArrayList<>();
        List<ApplicationOnHost> tasks = modeller.getApplication(deploymentId, Integer.parseInt(deploymentId));
        for (ApplicationOnHost task : tasks) {
            //Treat host id as unique id of task/application on a host
            answer.add(task.getAllocatedTo().getId());
        }
        return answer;
    }

    @Override
    public void hardKillApp(String applicationName, String deploymentId) {
        execCmd("scancel " + deploymentId);
    }

    /**
     * Pauses a job, so that it can be executed later.
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     *
     */
    public void pauseJob(String applicationName, String deploymentId) {
        execCmd("scontrol hold " + deploymentId);
    }

    /**
     * un-pauses a job, so that it may resume execution.
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     */
    public void resumeJob(String applicationName, String deploymentId) {
        execCmd("scontrol resume " + deploymentId);
    }

    /**
     * This increases the walltime of a job
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     * @param response The response object to perform the action for
     */
    public void decreaseWallTime(String applicationName, String deploymentId, Response response) {
        //Example: "scontrol update JobID=" + deploymentId + " Timelimit=-30:00"
        String walltimeIncrement = response.getAdaptationDetail("WALL_TIME_INCREMENT");
        if (walltimeIncrement == null || walltimeIncrement.isEmpty()) {
            walltimeIncrement = "30:00";
        }
        execCmd("scontrol update JobID=" + deploymentId + " Timelimit=-" + walltimeIncrement);
    }

    /**
     * This decreases the walltime of a job
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     * @param response The response object to perform the action for
     */
    public void increaseWallTime(String applicationName, String deploymentId, Response response) {
        String walltimeIncrement = response.getAdaptationDetail("WALL_TIME_INCREMENT");
        if (walltimeIncrement == null || walltimeIncrement.isEmpty()) {
            walltimeIncrement = "30:00";
        }
        //Example: "scontrol update JobID=" + deploymentId + " Timelimit=+30:00"
        execCmd("scontrol update JobID=" + deploymentId + " Timelimit=+" + walltimeIncrement);
    }

    @Override
    public void addTask(String applicationName, String deploymentId, String taskType) {
        int oldCount = getNodeCount(deploymentId);
        if (oldCount > 0) { //checks to make sure the count of cpus was detected correctly
            execCmd("scontrol update JobId=" + deploymentId + "NumNodes=" + (oldCount + 1));
        }
    }

    @Override
    public void deleteTask(String applicationName, String deploymentId, String taskID) {
        int oldCount = getNodeCount(deploymentId);
        if (oldCount > 2) {
            execCmd("scontrol update JobId=" + deploymentId + "NumNodes=" + (oldCount - 1));
        }
    }

    /**
     * Returns the amount of nodes allocated to a given deployment
     *
     * @param deploymentId the id of the job to get the node count for
     * @return the node count for a given deployment, -1 in the event of error
     */
    private int getNodeCount(String deploymentId) {
        int answer = -1;
        ArrayList<String> cpuCount = execCmd("squeue -j " + deploymentId + " --format=\"%D\"");
        if (!cpuCount.isEmpty()) {
            return Integer.parseInt(cpuCount.get(0));
        }
        return answer;
    }

    /**
     * Returns the minimum cpu count for a given deployment
     *
     * @param deploymentId the id of the job to get the cpu count for
     * @return the cpu count for a given deployment, -1 in the event of error
     */
    private int getCpuCount(String deploymentId) {
        int answer = -1;
        ArrayList<String> cpuCount = execCmd("squeue -j " + deploymentId + " --format=\"%C\"");
        if (!cpuCount.isEmpty()) {
            return Integer.parseInt(cpuCount.get(0));
        }
        return answer;
    }

    /**
     * Adds a cpu to an applications deployment
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     */
    public void addCpu(String applicationName, String deploymentId) {
        int oldCount = getCpuCount(deploymentId);
        if (oldCount > 0) { //checks to make sure the count of cpus was detected correctly
            execCmd("scontrol update JobId=" + deploymentId + "NumCPUs=" + (oldCount + 1));
        }
    }

    /**
     * Removes a cpu from an applications deployment
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     */
    public void removeCpu(String applicationName, String deploymentId) {
        int oldCount = getCpuCount(deploymentId);
        if (oldCount > 2) {
            execCmd("scontrol update JobId=" + deploymentId + "NumCPUs=" + (oldCount - 1));
        }
    }

    public void shutdownHost(Host host) {
        //Consider: https://slurm.schedmd.com/power_save.html
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void startupHost(Host host) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void decreasePowerCap() {
        //scontrol show powercap should be able to read current values        
        //Usses the slurm command: scontrol update powercap=1400000
        //TODO this feature is disabled on the testbed so cannot be tested/developed as yet
        //See: https://slurm.schedmd.com/SLUG15/Power_Adaptive_final.pdf
        //See: https://slurm.schedmd.com/SLUG15/Power_mgmt.pdf
        //See: https://slurm.schedmd.com/power_mgmt.html
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.        
    }
    
    public void increasePowerCap() {
        //scontrol show powercap should be able to read current values        
        //Usses the slurm command: scontrol update powercap=1400000
        //TODO this feature is disabled on the testbed so cannot be tested/developed as yet
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.        
    }

    /**
     * This executes a given action for a response that has been placed in the
     * actuator's queue for deployment.
     *
     * @param response The response object to launch the action for
     */
    @Override
    protected void launchAction(Response response) {
        if (response.getCause() instanceof ApplicationEventData) {
            /**
             * This checks to see if application based events have the necessary 
             * information to perform the adaptation.
             */
            if (response.getDeploymentId() == null || response.getDeploymentId().isEmpty()) {
                response.setPerformed(true);
                response.setPossibleToAdapt(false);
                return;
            }
        }
        switch (response.getActionType()) {
            case ADD_CPU:
                addCpu(response.getApplicationId(), response.getDeploymentId());
                break;
            case REMOVE_CPU:
                removeCpu(response.getApplicationId(), response.getDeploymentId());
                break;
            case ADD_TASK:
                addTask(response.getApplicationId(), response.getDeploymentId(), response.getAdaptationDetails());
                break;
            case REMOVE_TASK:
                deleteTask(response.getApplicationId(), response.getDeploymentId(), response.getTaskId());
                break;
            case SCALE_TO_N_TASKS:
                scaleToNTasks(response.getApplicationId(), response.getDeploymentId(), response);
                break;
            case PAUSE_APP:
                pauseJob(response.getApplicationId(), response.getDeploymentId());
                break;
            case UNPAUSE_APP:
                resumeJob(response.getApplicationId(), response.getDeploymentId());
                break;
            case HARD_KILL_APP:
                hardKillApp(response.getApplicationId(), response.getDeploymentId());
                break;
            case INCREASE_WALL_TIME:
                increaseWallTime(response.getApplicationId(), response.getDeploymentId(), response);
                break;
            case REDUCE_WALL_TIME:
                decreaseWallTime(response.getApplicationId(), response.getDeploymentId(), response);
                break;
            case INCREASE_POWER_CAP:
                increasePowerCap();
                break;
            case REDUCE_POWER_CAP:
                decreasePowerCap();
                break;
            default:
                Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, "The Response type was not recoginised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }

}
