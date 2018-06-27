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

import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.Response;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This actuator creates the basics for an actuator that utilises a queueing
 * mechanism to avoid long delays in the calling agent.
 *
 * @author Richard Kavanagh
 */
public abstract class AbstractActuator implements ActuatorInvoker, Runnable {

    private final LinkedBlockingDeque<Response> queue = new LinkedBlockingDeque<>();
    private boolean stop = false;

    /**
     * This stops this actuator from running.
     */
    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        while (!stop || !queue.isEmpty()) {
            try {
                Response currentItem = queue.poll(30, TimeUnit.SECONDS);
                if (currentItem != null) {
                    ArrayList<Response> actions = new ArrayList<>();
                    actions.add(currentItem);
                    int draincount = queue.drainTo(actions);
                    Logger.getLogger(AbstractActuator.class.getName()).log(Level.FINEST, "The actuator just processed {0} actions.", draincount);
                    for (Response action : actions) {
                        try {
                            launchAction(action);
                        } catch (Exception ex) {
                            /**
                             * This prevents exceptions when messaging the
                             * server from propagating and stopping the thread
                             * from running.
                             */
                            Logger.getLogger(AbstractActuator.class.getName()).log(Level.SEVERE, null, ex);
                            action.setPerformed(true);
                            action.setPossibleToAdapt(false);
                        }
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(AbstractActuator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void actuate(Response response) {
        queue.add(response);
    }

    /**
     * This executes a given action for a response. Usually it is taken 
     * from the actuator's pending action queue.
     *
     * @param response The response object to launch the action for
     */    
    protected abstract void launchAction(Response response);
    
    @Override
    public void scaleToNTasks(String applicationId, String deploymentId, Response response) {
        String taskType = response.getAdaptationDetail("TASK_TYPE");
        String tasksToRemove = response.getAdaptationDetail("TASKS_TO_REMOVE");
        if (tasksToRemove == null) { //Add Tasks
            int count = Integer.parseInt(response.getAdaptationDetail("TASK_COUNT"));
            for (int i = 0; i < count; i++) {
                addResource(applicationId, deploymentId, taskType);
            }
        } else { //Remove tasks
            for (String taskId : tasksToRemove.split(",")) {
                deleteResource(applicationId, deploymentId, taskId.trim());
            }
        }
    }
    
    /**
     * The deployment id and application id originate from the event, thus if a
     * response originates from the host these values are not set. Thus the task
     * id is the only means to specify which task to perform action against.
     *
     * @param response The response object
     * @return The task/deployment id to be used by to act upon the job.
     */
    protected String getTaskDeploymentId(Response response) {
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
    
    /**
     * This gets the hostname associated with a response object. This is either
     * derived from the originating event or from the adaptation detail "host".
     * @param response The response object to get the host information for
     * @return The name of the host
     */
    protected String getHostname(Response response) {
        if (response.getCause() instanceof HostEventData) {
            return ((HostEventData) response.getCause()).getHost();
        }
        if (response.hasAdaptationDetail("host")) {
            return response.getAdaptationDetail("host");
        }
        return null;
    }    

}
