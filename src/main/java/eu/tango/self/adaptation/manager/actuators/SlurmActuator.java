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
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import eu.tango.self.adaptation.manager.listeners.ClockMonitor;
import eu.tango.self.adaptation.manager.qos.SlaRulesLoader;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

/**
 * This actuator interacts with the Device supervisor SLURM, with the aim of
 * querying for information and invoking adaptation.
 *
 * @author Richard Kavanagh
 */
public class SlurmActuator extends AbstractActuator {

    private final SlurmDataSourceAdaptor datasource;
    private final EnergyModeller modeller = EnergyModeller.getInstance();

    public SlurmActuator() {
        datasource = new SlurmDataSourceAdaptor();
    }

    public SlurmActuator(SlurmDataSourceAdaptor datasource) {
        if (datasource == null) {
            this.datasource = new SlurmDataSourceAdaptor();
            return;
        }
        this.datasource = datasource;
    }

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
                answer.setSlaLimits(SlaRulesLoader.getInstance().getSlaLimits(applicationName, deploymentId));
                //TODO Consider the loading of the adaptation criteria as well.
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
        List<ApplicationOnHost> tasks = modeller.getApplication(name, Integer.parseInt(deploymentId));
        for (ApplicationOnHost task : tasks) {
            //TODO Consider how this can be used to get sub tasks?
            if ((task.getName().trim().equals(name.trim()))
                    && (task.getId() + "").equals(deploymentId.trim())) {
                return task;
            }
        }
        return null;
    }

    @Override
    public double getTotalPowerUsage(String applicationName, String deploymentId) {
        double answer = 0.0;
        List<ApplicationOnHost> tasks = modeller.getApplication(applicationName, Integer.parseInt(deploymentId));
        for (CurrentUsageRecord record : modeller.getCurrentEnergyForApplication(tasks)) {
            answer = answer + record.getPower();
        }
        return answer;
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
        List<ApplicationOnHost> tasks = modeller.getApplication(applicationName, Integer.parseInt(deploymentId));
        for (ApplicationOnHost task : tasks) {
            //Treat host id as unique id of task/application on a host
            answer.add(task.getAllocatedTo().getId());
        }
        return answer;
    }

    @Override
    public List<ApplicationOnHost> getTasksOnHost(String host) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        return ApplicationOnHost.filter(apps, datasource.getHostByName(host));
    }

    @Override
    public List<ApplicationOnHost> getTasks() {
        return datasource.getHostApplicationList();
    }

    @Override
    public void hardKillApp(String applicationName, String deploymentId) {
        execCmd("scancel " + deploymentId);
    }

    /**
     * This takes a named application and kills all instances of it.
     * @param applicationName The name of the application to kill
     */
    public void killSimilarApps(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        for (ApplicationOnHost app : apps) {
            execCmd("scancel " + app.getId());
        }
    }

    /**
     * Pauses all jobs with a given name, so that they can be executed later.
     * @param applicationName The name of the application to pause
     */
    public void pauseSimilarJob(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        for (ApplicationOnHost app : apps) {
            pauseJob(applicationName, app.getId() + "");
        }
    }

    /**
     * Un-pauses all jobs with a given name, so that they can be executed later.
     * @param applicationName The name of the application to pause
     */
    public void resumeSimilarJob(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        for (ApplicationOnHost app : apps) {
            resumeJob(applicationName, app.getId() + "");
        }
    }

    /**
     * This increases the wall time of all similar applications
     * @param applicationName The name of the application to change the wall time for
     * @param response  The response object to perform the action for
     */
    public void increaseWallTimeSimilarJob(String applicationName, Response response) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        for (ApplicationOnHost app : apps) {
            increaseWallTime(applicationName, app.getId() + "", response);
        }
    }

    /**
     * This decreases the wall time of all similar applications
     * @param applicationName The name of the application to change the wall time for
     * @param response  The response object to perform the action for
     */
    public void decreaseWallTimeSimilarJob(String applicationName, Response response) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        for (ApplicationOnHost app : apps) {
            decreaseWallTime(applicationName, app.getId() + "", response);
        }
    }

    /**
     * This adjusts the wall time of all similar applications, this is based upon the average
     * wall plus an amount of slack.
     * @param applicationName The name of the application to change the wall time for
     * @param response  The response object to perform the action for
     */
    public void minimizeWallTime(String applicationName, Response response) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        apps = ApplicationOnHost.filter(apps, applicationName, -1);
        Double averageWalltime = getAverageWallTime(applicationName);
        Double slackFactor = 1.10; //10% slack as default
        if (response.hasAdaptationDetail("SLACK_FACTOR")) {
            slackFactor = Double.parseDouble(response.getAdaptationDetail("SLACK_FACTOR"));
        }
        if (averageWalltime > 0) {
            averageWalltime = averageWalltime * slackFactor;
            for (ApplicationOnHost app : apps) {
                execCmd("scontrol update JobID=" + app.getId() + " Timelimit=" + TimeUnit.SECONDS.toMinutes(averageWalltime.intValue()));
            }
        } else {
            response.setAdaptationDetails("Unable to adapt as no recent average job information was available!");
            response.setPerformed(true);
            response.setPossibleToAdapt(false);
        }
    }

    /**
     * This gets the average walltime for a given application.
     * @param applicationName
     * @return the average wall time of all application instances with a given
     * name, over a period of the last day.
     */
    public double getAverageWallTime(String applicationName) {
        double count = 0;
        double totalTime = 0;

        /**
         * This follows the command: 
         * sacct -u kavanagr -S 2017-12-01 -n --delimiter="," -p -o "jobid,jobName,state,ConsumedEnergyRaw,QoS,elapsedraw"
         *
         * which provides data in the format:
         *
         * JobID,JobName,State,ConsumedEnergyRaw,QOS,ElapsedRaw,
         * 4663,GPU-Bench-Test,COMPLETED,,normal,396,
         */
        //Get the last day's worth of runs and find the average time for a given application
        GregorianCalendar date = new GregorianCalendar();
        date.setTimeInMillis(date.getTimeInMillis() - TimeUnit.DAYS.toMillis(1));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateInFormat = formatter.format(date.getTime());
        ArrayList<String> sacctOutput = execCmd("sacct -S " + dateInFormat + " -n --delimiter=\",\" -p -o \"jobid,jobName,state,elapsedraw\"");
        for (String line : sacctOutput) {
            String[] splitLine = line.split(",");
            String jobName = splitLine[1];
            String state = splitLine[2];
            double elapsedraw = Double.parseDouble(splitLine[3]);
            if (jobName.equals(applicationName) && state.equals("COMPLETED")) {
                totalTime = totalTime + elapsedraw;
                count = count + 1;
            }
        }
        if (count == 0) {
            return 0;
        } else {
            return totalTime / count;
        }
    }

    /**
     * Pauses a job, so that it can be executed later.
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     *
     */
    public void pauseJob(String applicationName, String deploymentId) {
        /**
         * "hold" and "suspend" are related commands that pauses a job that is
         * yet to start running or requires elevated privileges.
         */
        execCmd("scancel --signal=STOP " + deploymentId);
    }

    /**
     * un-pauses a job, so that it may resume execution.
     *
     * @param applicationName The application name or identifier
     * @param deploymentId The deployment instance identifier
     */
    public void resumeJob(String applicationName, String deploymentId) {
        execCmd("scancel --signal=CONT " + deploymentId);
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

    /**
     * This prevents a pending task that is to be submitted from running alongside 
     * other applications at the same time.
     * @param applicationName The application to give exclusive node access to
     * @param deploymentId The deployment id of the task to give exclusive access to resources
     */
    public void makeTaskExclusive(String applicationName, String deploymentId) {
        execCmd("scontrol update JobId=" + deploymentId + "OverSubscribe=no");
    }

    /**
     * This makes it possible for a pending task to be submitted so that it can
     * run alongside other applications at the same time.
     * @param applicationName The application to oversubscribe
     * @param deploymentId The deployment id of the task to over subscribe
     */
    public void makeTaskOverSubscribed(String applicationName, String deploymentId) {
        execCmd("scontrol update JobId=" + deploymentId + "OverSubscribe=yes");
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
        ArrayList<String> cpuCount = execCmd("squeue -j " + deploymentId + " -h --format=\"%D\"");
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
        ArrayList<String> cpuCount = execCmd("squeue -j " + deploymentId + " -h --format=\"%C\"");
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

    /**
     * This powers down a host
     *
     * @param hostname The host to power down
     */
    public void shutdownHost(String hostname) {
        //Consider: https://slurm.schedmd.com/power_save.html
        execCmd("scontrol update NodeName=" + hostname + "State=power_down");
        //TODO Consider writing a time wake procedure. i.e. shutdown for X hours etc
    }

    /**
     * This powers up a host
     *
     * @param hostname The host to power up
     */
    public void startupHost(String hostname) {
        execCmd("scontrol update NodeName=" + hostname + "State=power_up");
    }

    /**
     * This obtains the current power cap from SLURM. In the event the value
     * isn't read correctly the value Double.NaN is provided instead.
     *
     * @return
     */
    private double getCurrentPowerCap() {
        ArrayList<String> powerStr = execCmd("scontrol show power"); //using the command
        if (powerStr.isEmpty()) {
            return Double.NaN;
        }
        try {
            String[] values = powerStr.get(0).split(" ");
            for (String value : values) {
                if (value.startsWith("PowerCap")) {
                    return Double.parseDouble(value.split("=")[1]);
                }
            }
        } catch (NumberFormatException ex) {

        }
        return Double.NaN;
    }

    /**
     * This decreases the cluster level power cap on the infrastructure, by a set amount
     * @param response The response object that caused the adaptation to be invoked.
     */
    public void decreasePowerCap(Response response) {
        //scontrol show powercap should be able to read current values       
        //Uses the slurm command: scontrol update powercap=1400000
        //See: https://slurm.schedmd.com/SLUG15/Power_Adaptive_final.pdf
        //See: https://slurm.schedmd.com/SLUG15/Power_mgmt.pdf
        //See: https://slurm.schedmd.com/power_mgmt.html   
        double currentPowerCap = getCurrentPowerCap();
        double incremenet = 10;
        if (response.hasAdaptationDetail("POWER_INCREMENT")) {
            incremenet = Double.parseDouble(response.getAdaptationDetail("POWER_INCREMENT"));
        }
        if (Double.isFinite(currentPowerCap) && currentPowerCap - incremenet > 0) {
            execCmd("scontrol update powercap=" + (currentPowerCap - incremenet));
        }
    }

    /**
     * This increases the cluster level power cap on the infrastructure, by a set amount
     * @param response The response object that caused the adaptation to be invoked.
     */
    public void increasePowerCap(Response response) {
        //scontrol show powercap should be able to read current values       
        //Uses the slurm command: scontrol update powercap=1400000
        //See: https://slurm.schedmd.com/SLUG15/Power_Adaptive_final.pdf
        //See: https://slurm.schedmd.com/SLUG15/Power_mgmt.pdf
        //See: https://slurm.schedmd.com/power_mgmt.html

        double currentPowerCap = getCurrentPowerCap();
        double incremenet = 10;
        if (response.hasAdaptationDetail("POWER_INCREMENT")) {
            incremenet = Double.parseDouble(response.getAdaptationDetail("POWER_INCREMENT"));
        }
        if (Double.isFinite(currentPowerCap)) {
            execCmd("scontrol update powercap=" + (currentPowerCap + incremenet));
        }
    }

    /**
     * This sets the cluster level power cap on the infrastructure
     * @param response The response object that caused the adaptation to be invoked.
     */
    public void setPowerCap(Response response) {
        if (response.hasAdaptationDetail("POWER_CAP")) {
            double powerCap = Double.parseDouble(response.getAdaptationDetail("POWER_CAP"));
            if (Double.isFinite(powerCap) && powerCap > 0) {
                execCmd("scontrol update powercap=" + powerCap);
            }
        } else {
            response.setPerformed(true);
            response.setPossibleToAdapt(false);
            response.setAdaptationDetails("No POWER_CAP value specified");
        }
    }

    public void checkpointAndRequeue() {
        //Checkpoint is not possible: i.e. as per the command: scontrol checkpoint able 3100
        //scontrol_checkpoint error: Requested operation not supported on this system
        //Requeing is also not possible as in: scontrol requeue 3100
        //It says "Requested operation is presently disabled for job 3567"
        //Uses the slurm command: scontrol checkpoint requeue jobid  
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
                addCpu(response.getApplicationId(), getTaskDeploymentId(response));
                break;
            case REMOVE_CPU:
                removeCpu(response.getApplicationId(), getTaskDeploymentId(response));
                break;
            case ADD_TASK:
                addTask(response.getApplicationId(), getTaskDeploymentId(response), response.getAdaptationDetails());
                break;
            case REMOVE_TASK:
                deleteTask(response.getApplicationId(), getTaskDeploymentId(response), response.getTaskId());
                break;
            case SCALE_TO_N_TASKS:
                scaleToNTasks(response.getApplicationId(), getTaskDeploymentId(response), response);
                break;
            case PAUSE_APP:
                pauseJob(response.getApplicationId(), getTaskDeploymentId(response));
                if (response.hasAdaptationDetail("UNPAUSE")) {
                    /**
                     * This requires to have a matching rule to negate the effect of the first.
                     * The matching rule starts with an exclamation! instead.
                     */
                    int unpauseInNseconds = Integer.parseInt(response.getAdaptationDetail("UNPAUSE"));
                    ClockMonitor.getInstance().addEvent("!" + response.getCause().getAgreementTerm(), "application=" + response.getApplicationId() + ";deploymentid=" + getTaskDeploymentId(response), unpauseInNseconds);
                }
                break;
            case UNPAUSE_APP:
                resumeJob(response.getApplicationId(), getTaskDeploymentId(response));
                break;
            case PAUSE_SIMILAR_APPS:
                pauseSimilarJob(response.getApplicationId());
                break;
            case UNPAUSE_SIMILAR_APPS:
                resumeSimilarJob(response.getApplicationId());
                break;
            case OVERSUBSCRIBE_APP:
                makeTaskOverSubscribed(response.getApplicationId(), getTaskDeploymentId(response));
                break;
            case EXCLUSIVE_APP:
                makeTaskExclusive(response.getApplicationId(), getTaskDeploymentId(response));
                break;
            case HARD_KILL_APP:
            case KILL_APP:
                //Note: no soft implementation exists at this time
                hardKillApp(response.getApplicationId(), getTaskDeploymentId(response));
                break;
            case KILL_SIMILAR_APPS:
                killSimilarApps(response.getApplicationId());
                break;
            case INCREASE_WALL_TIME:
                increaseWallTime(response.getApplicationId(), getTaskDeploymentId(response), response);
                break;
            case REDUCE_WALL_TIME:
                decreaseWallTime(response.getApplicationId(), getTaskDeploymentId(response), response);
                break;
            case INCREASE_WALL_TIME_SIMILAR_APPS:
                increaseWallTimeSimilarJob(response.getApplicationId(), response);
                break;
            case REDUCE_WALL_TIME_SIMILAR_APPS:
                decreaseWallTimeSimilarJob(response.getApplicationId(), response);
                break;
            case MINIMIZE_WALL_TIME_SIMILAR_APPS:
                minimizeWallTime(response.getApplicationId(), response);
                break;
            case INCREASE_POWER_CAP:
                increasePowerCap(response);
                break;
            case REDUCE_POWER_CAP:
                decreasePowerCap(response);
                break;
            case SET_POWER_CAP:
                setPowerCap(response);
                break;
            case STARTUP_HOST:
                String host = getHostname(response);
                if (host != null) {
                    startupHost(host);
                }
                break;
            case SHUTDOWN_HOST:
                host = getHostname(response);
                if (host != null) {
                    shutdownHost(host);
                }
                if (response.hasAdaptationDetail("REBOOT")) {
                    int resumeInNseconds = Integer.parseInt(response.getAdaptationDetail("REBOOT"));
                    ClockMonitor.getInstance().addEvent("!" + response.getCause().getAgreementTerm(), "host=" + host, resumeInNseconds);
                }
                break;
            default:
                Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, "The Response type was not recoginised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }

    /**
     * This gets the hostname associated with a response object. This is either
     * derived from the originating event or from the adaptation detail "host".
     * @param response The response object to get the host information for
     * @return The name of the host
     */
    private String getHostname(Response response) {
        if (response.getCause() instanceof HostEventData) {
            return ((HostEventData) response.getCause()).getHost();
        }
        if (response.hasAdaptationDetail("host")) {
            return response.getAdaptationDetail("host");
        }
        return null;
    }

    /**
     * The deployment id and application id originate from the event, thus if a
     * response originates from the host these values are not set. Thus the task
     * Id is the only means to specify which task to perform action against.
     *
     * @param response The response object
     * @return The task/deployment id to be used by slurm to act upon the job.
     */
    private String getTaskDeploymentId(Response response) {
        if (response.getTaskId() != null && !response.getTaskId().isEmpty()) {
            return response.getTaskId();
        }
        /**
         * Information below gained from application based events, it is a backup
         * and uses the originating application as the item to actuate against
         *
         */
        if (response.getDeploymentId() != null && !response.getDeploymentId().isEmpty()) {
            return response.getDeploymentId();
        }
        //This source of deployment information is caused by clock events passing information back
        if (response.hasAdaptationDetail("deploymentid")) {
            return response.getAdaptationDetail("deploymentid");
        }
        return "";
    }

}
