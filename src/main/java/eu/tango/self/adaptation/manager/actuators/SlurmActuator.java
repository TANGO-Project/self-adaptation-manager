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

import eu.tango.energymodeller.datasourceclient.SlurmDataSourceAdaptor;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import static eu.tango.self.adaptation.manager.io.ExecuteUtils.execCmd;
import eu.tango.self.adaptation.manager.model.ApplicationDefinition;
import eu.tango.self.adaptation.manager.qos.SlaRulesLoader;
import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This actuator interacts with the Device supervisor SLURM, with the aim of
 * querying for information and invoking adaptation.
 *
 * @author Richard Kavanagh
 */
public class SlurmActuator extends AbstractActuator {

    private final SlurmDataSourceAdaptor datasource;

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
     * @param applicationName The name of the application to kill, or series of
     * applications split by the & symbol
     */
    public void killSimilarApps(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        for (String subAppName : applicationName.split("&")) {
            List<ApplicationOnHost> killApps = ApplicationOnHost.filter(apps, subAppName.trim(), -1);
            for (ApplicationOnHost app : killApps) {
                execCmd("scancel " + app.getId());
            }
        }
    }

    /**
     * Pauses all jobs with a given name, so that they can be executed later.
     * @param applicationName The name of the application to pause, or series of
     * applications split by the & symbol
     */
    public void pauseSimilarJob(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        for (String subAppName : applicationName.split("&")) {
            List<ApplicationOnHost> pauseApps = ApplicationOnHost.filter(apps, subAppName.trim(), -1);
            for (ApplicationOnHost app : pauseApps) {
                pauseJob(subAppName.trim(), app.getId() + "");
            }
        }
    }

    /**
     * Un-pauses all jobs with a given name, so that they can be executed later.
     * @param applicationName The name of the application to pause, or series of
     * applications split by the & symbol
     */
    public void resumeSimilarJob(String applicationName) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        for (String subAppName : applicationName.split("&")) {        
            List<ApplicationOnHost> resumeApps = ApplicationOnHost.filter(apps, subAppName.trim(), -1);
            for (ApplicationOnHost app : resumeApps) {
                resumeJob(subAppName.trim(), app.getId() + "");
            }
        }
    }

    /**
     * This increases the wall time of all similar applications
     * @param applicationName The name of the application to change the wall time for
     * @param response The response object to perform the action for, or series of
     * applications split by the & symbol
     */
    public void increaseWallTimeSimilarJob(String applicationName, Response response) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        for (String subAppName : applicationName.split("&")) {         
            List<ApplicationOnHost> walltimeApps = ApplicationOnHost.filter(apps, subAppName.trim(), -1);
            for (ApplicationOnHost app : walltimeApps) {
                increaseWallTime(subAppName.trim(), app.getId() + "", response);
            }
        }
    }

    /**
     * This decreases the wall time of all similar applications
     * @param applicationName The name of the application to change the wall time for
     * @param response The response object to perform the action for, or series of
     * applications split by the & symbol
     */
    public void decreaseWallTimeSimilarJob(String applicationName, Response response) {
        List<ApplicationOnHost> apps = datasource.getHostApplicationList();
        for (String subAppName : applicationName.split("&")) {           
            List<ApplicationOnHost> walltimeApps = ApplicationOnHost.filter(apps, subAppName.trim(), -1);
            for (ApplicationOnHost app : walltimeApps) {
                decreaseWallTime(subAppName.trim(), app.getId() + "", response);
            }
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
     * This increases the walltime of a job
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
    public void addResource(String applicationName, String deploymentId, String taskType) {
        Logger.getLogger(SlurmActuator.class.getName()).log(Level.INFO, "Executing a SLURM add resource action");  
        int oldCount = getNodeCount(deploymentId);
        if (oldCount > 0) { //checks to make sure the count of nodes was detected correctly
            execCmd("scontrol update JobId=" + deploymentId + "NumNodes=" + (oldCount + 1));
        }
    }

    @Override
    public void removeResource(String applicationName, String deploymentId, String taskID) {
        Logger.getLogger(SlurmActuator.class.getName()).log(Level.INFO, "Executing a SLURM remove resource action");          
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
        if (hostname.contains(",")) {
            for (String host : hostname.split(",")) {
                execCmd("scontrol update NodeName=" + host.trim() + "State=power_down");
            }
        } else {
            execCmd("scontrol update NodeName=" + hostname.trim() + "State=power_down");
        }
    }

    /**
     * This powers up a host
     *
     * @param hostname The host to power up
     */
    public void startupHost(String hostname) {
        //Starts all hosts
        if (hostname.equals("ALL")) {
            for(Host host : datasource.getHostList()) {
                if (!host.isAvailable()) {
                    execCmd("scontrol update NodeName=" + host.getHostName().trim() + "State=power_up");
                }
            }
            return;
        }        
        if (hostname.contains(",")) {
            for (String host : hostname.split(",")) {
                execCmd("scontrol update NodeName=" + host.trim() + "State=power_up");
            }
        } else {
            execCmd("scontrol update NodeName=" + hostname.trim() + "State=power_up");
        }
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
        double currentPowerCap = SlurmDataSourceAdaptor.getCurrentPowerCap();
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

        double currentPowerCap = SlurmDataSourceAdaptor.getCurrentPowerCap();
        double incremenet = 10;
        if (response.hasAdaptationDetail("POWER_INCREMENT")) {
            incremenet = Double.parseDouble(response.getAdaptationDetail("POWER_INCREMENT"));
        }
        if (Double.isFinite(currentPowerCap)) {
            execCmd("scontrol update powercap=" + (currentPowerCap + incremenet));
        }
        if (response.hasAdaptationDetail("RESTORE_HOSTS")) {
            startupHost("ALL");
        }        
    }

    /**
     * This sets the cluster level power cap on the infrastructure
     *
     * @param response The response object that caused the adaptation to be
     * invoked.
     */
    public void setPowerCap(Response response) {
        if (response.hasAdaptationDetail("POWER_CAP")) {
            double powerCap = Double.parseDouble(response.getAdaptationDetail("POWER_CAP"));
            if (Double.isFinite(powerCap) && powerCap > 0) {
                execCmd("scontrol update powercap=" + powerCap);
            }
            if (response.hasAdaptationDetail("RESTORE_HOSTS")) {
                startupHost("ALL");
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
     * This executes a given action for a response. Usually it is taken 
     * from the actuator's pending action queue.
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
                addResource(response.getApplicationId(), getTaskDeploymentId(response), response.getAdaptationDetails());
                generateReverseApplicationAction(response);                
                break;
            case REMOVE_TASK:
                removeResource(response.getApplicationId(), getTaskDeploymentId(response), response.getTaskId());
                generateReverseApplicationAction(response);
                break;
            case SCALE_TO_N_TASKS:
                scaleToNTasks(response.getApplicationId(), getTaskDeploymentId(response), response);
                break;
            case PAUSE_APP:
                pauseJob(response.getApplicationId(), getTaskDeploymentId(response));
                generateReverseApplicationAction(response); //requires UNPAUSE keyword
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
                generateReverseHostAction(response);
                break;
            default:
                Logger.getLogger(SlurmActuator.class.getName()).log(Level.SEVERE, "The Response type was not recognised by this adaptor");
                break;
        }
        response.setPerformed(true);
    }

}
