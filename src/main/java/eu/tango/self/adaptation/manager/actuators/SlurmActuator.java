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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This actuator interacts with the Device supervisor SLURM, with the aim of
 * querying for information and invoking adaptation.
 *
 * @author Richard Kavanagh
 */
public class SlurmActuator implements ActuatorInvoker, Runnable {

    SlurmDataSourceAdaptor datasource = new SlurmDataSourceAdaptor();
    EnergyModeller modeller = EnergyModeller.getInstance();
    private final LinkedBlockingDeque<Response> queue = new LinkedBlockingDeque<>();
    private boolean stop = false;

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
     * @param taskId The task id to convert
     * @return The host
     */
    private Host getHostFromTaskId(int taskId) {
        Collection<Host> hosts = modeller.getHostList();
        for (Host host : hosts) {
            if (host.getId() == taskId)
                return host;
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
     * @param applicationName
     * @param deploymentId
     */
    public void pauseJob(String applicationName, String deploymentId) {
        execCmd("scontrol hold " + deploymentId);
    }

    /**
     * un-pauses a job, so that it may resume execution.
     *
     * @param applicationName
     * @param deploymentId
     */
    public void unPauseJob(String applicationName, String deploymentId) {
        execCmd("scontrol resume " + deploymentId);
    }

    @Override
    public void actuate(Response response) {
        queue.add(response);
    }

    @Override
    public void addTask(String applicationName, String deploymentId, String taskType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteTask(String applicationName, String deployment, String taskID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void scaleToNTasks(String applicationId, String deploymentId, Response response) {
        String taskType = response.getAdaptationDetail("TASK_TYPE");
        String tasksToRemove = response.getAdaptationDetail("TASKS_TO_REMOVE");
        if (tasksToRemove == null) { //Add Tasks
            int count = Integer.parseInt(response.getAdaptationDetail("TASK_COUNT"));
            for (int i = 0; i < count; i++) {
                addTask(applicationId, deploymentId, taskType);
            }
        } else { //Remove tasks
            for (String taskId : tasksToRemove.split(",")) {
                deleteTask(applicationId, deploymentId, taskId.trim());
            }
        }
    }

    @Override
    public void run() {
        while (!stop || !queue.isEmpty()) {
            try {
                Response currentItem = queue.poll(30, TimeUnit.SECONDS);
                if (currentItem != null) {
                    ArrayList<Response> actions = new ArrayList<>();
                    actions.add(currentItem);
                    queue.drainTo(actions);
                    for (Response action : actions) {
                        try {
                            launchAction(action);
                        } catch (Exception ex) {
                            /**
                             * This prevents exceptions when messaging the
                             * server from propagating and stopping the thread
                             * from running.
                             */
                            Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, null, ex);
                            action.setPerformed(true);
                            action.setPossibleToAdapt(false);
                        }
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * This executes a given action for a response that has been placed in the
     * actuator's queue for deployment.
     *
     * @param response The response object to launch the action for
     */
    private void launchAction(Response response) {
        switch (response.getActionType()) {
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
                pauseJob(response.getApplicationId(), response.getDeploymentId());
                break;
            case HARD_KILL_APP:
                hardKillApp(response.getApplicationId(), response.getDeploymentId());
                break;
            default:
                Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, "The Response type was not recoginised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }

    /**
     * This executes a command and returns the output as a line of strings.
     *
     * @param cmd The command to execute
     * @return A list of output broken down by line
     * @throws java.io.IOException
     */
    private static ArrayList<String> execCmd(String mainCmd) {
        String cmd[] = {"/bin/sh",
            "-c",
            mainCmd};
        try {
            execCmd(cmd);
        } catch (IOException ex) {
            Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    /**
     * This executes a command and returns the output as a line of strings.
     *
     * @param cmd The command to execute
     * @return A list of output broken down by line
     * @throws java.io.IOException
     */
    private static ArrayList<String> execCmd(String[] cmd) throws java.io.IOException {
        ArrayList<String> output = new ArrayList<>();
        Process proc = Runtime.getRuntime().exec(cmd);
        java.io.InputStream is = proc.getInputStream();
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String val = "";
        while (s.hasNextLine()) {
            val = s.next();
            output.add(val);
        }
        return output;
    }

}
