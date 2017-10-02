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
package eu.tango.self.adaptation.manager.listeners;

import eu.ascetic.ioutils.io.ResultsStore;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.datatypes.ClockEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.JobDetail;
import org.quartz.CronScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;

/**
 * This class produces events based upon the system clock
 *
 * @author Richard Kavanagh
 */
public class ClockMonitor implements EventListener, Runnable, Job {

    /**
     * All clock monitors must share the same event assessor, to that when
     * the job executes they know where to send the job processing request to.
     */
    private static EventAssessor eventAssessor;
    private Scheduler scheduler;
    private static final String CONFIG_FILE = "CronEvents.csv";

    private ClockMonitor() {
    }
    
    /**
     * SingletonHolder is loaded on the first execution of
     * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
     * not before.
     */
    private static class SingletonHolder {

        private static final ClockMonitor INSTANCE = new ClockMonitor();
    }

    /**
     * This creates a new singleton instance of the clock monitor.
     *
     * @return A singleton instance of a clock monitor.
     */
    public static ClockMonitor getInstance() {
        return SingletonHolder.INSTANCE;
    }    

    @Override
    public void setEventAssessor(EventAssessor assessor) {
        if (assessor != null) {
            eventAssessor = assessor;
        } else {
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.INFO, "The event assessor cannot be null.");
        }
    }

    @Override
    public EventAssessor getEventAssessor() {
        return eventAssessor;
    }

    /**
     * This starts the environment monitor going, in a daemon thread.
     */
    @Override
    public void startListening() {
        if (eventAssessor != null) {
            Thread clockMonThread = new Thread(this);
            clockMonThread.setDaemon(true);
            clockMonThread.start();
        } else {
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.SEVERE, "The clock monitor's event assessor should be set.");
        }
    }

    @Override
    public final void stopListening() {
        if (scheduler != null) {
            try {
                scheduler.clear();
                scheduler.shutdown(false); //don't wait for jobs to complete
                if (scheduler.isShutdown()) {
                    scheduler = null;
                }
            } catch (SchedulerException ex) {
                Logger.getLogger(ClockMonitor.class.getName()).log(Level.SEVERE, "Scheduler failed to shutdown correctly", ex);
            }
        }
    }

    @Override
    public boolean isListening() {
        return scheduler != null;
    }

    @Override
    public void run() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            loadFromDisk(CONFIG_FILE);
            scheduler.start();
        } catch (SchedulerException ex) {
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.SEVERE, null, ex);
            stopListening();
        } catch (Exception ex) {
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        if (eventAssessor == null) {
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.SEVERE, "No Event Assessor was set, now quitting.");
            stopListening();
            return;
        }
        for (EventData event : detectEvent(jec)) {
            eventAssessor.assessEvent(event);
        }
    }

    /**
     * This indicates when the clock has met a given time interval.
     *
     * @return A fully constructed event data for the clock based event.
     */
    private ArrayList<EventData> detectEvent(JobExecutionContext jec) {
        ArrayList<EventData> answer = new ArrayList<>();
        Logger.getLogger(ClockMonitor.class.getName()).log(Level.INFO, "Sending clock trigger event.");
        EventData event = new ClockEventData(TimeUnit.MILLISECONDS.toSeconds(jec.getFireTime().getTime()),
                0.0,
                0.0,
                EventData.Type.WARNING,
                EventData.Operator.EQ,
                "CLOCK_TRIGGER",
                jec.getTrigger().getKey().getName());
        answer.add(event);
        return answer;
    }

    /**
     * This performs a check to see if the settings file is empty or not. It
     * will write out a blank file if the file is not present.
     *
     * @param cronFile The list of cron on disk to load
     * @return If the defaults settings have been written out to disk or not.
     */
    public static boolean writeOutDefaults(ResultsStore cronFile) {
        boolean answer = false;
        //Agreement Term, Guarantee Direction and Response Type
        if (!cronFile.getResultsFile().exists()) {
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.INFO, "Writing out defaults to disk.");
            cronFile.add("Unique Id"); //0
            cronFile.append("Agreement Term"); //1 Trigger name
            cronFile.append("Cron Statement"); //2 Cron trigger statement
            cronFile.save();
            answer = true;
        }
        return answer;
    }

    /**
     * This loads a set of cron conditions in from disk.
     *
     * see: http://www.cronmaker.com/ for instructions on how to write cron
     * expressions or examples at:
     * https://docs.oracle.com/cd/E12058_01/doc/doc.1014/e12030/cron_expressions.htm
     *
     * @param file The name of the file to load the cron rules in from disk.
     */
    public void loadFromDisk(String file) {
        /**
         * Load in from file the following: Agreement Term, Guarantee Direction
         * and Response Type
         */
        ResultsStore cronFile = new ResultsStore(file);
        writeOutDefaults(cronFile);
        cronFile.load();
        Logger.getLogger(ClockMonitor.class.getName()).log(Level.INFO, "There are {0} cron rules to load.", cronFile.size() - 1); //Ignores header row
        if (cronFile.size() <= 1) {
            /**
             * If the file containing the triggers has only its header, then
             * there is no work to do, so the clock monitor will stop.
             */
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.INFO, "Terminating the clock monitor due to no cron rules.", cronFile.size() - 1);
            stopListening();
        }
        //ignore the header of the file
        for (int i = 1; i < cronFile.size(); i++) {
            ArrayList<String> current = cronFile.getRow(i);
            String logString = "Unique Id: " + current.get(0) + " Term: " + current.get(1) + " Cron Statement: " + current.get(2);
            addEvent(current.get(1), current.get(2));
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.INFO, "Adding Clock rule from disk: {0}", logString);
        }
    }

    /**
     * This adds a cron job into the clock monitor A cron scheduler string can
     * be made at: http://www.cronmaker.com
     *
     * @param eventName The event/metric name to trigger the event
     * @param cronSchedule The cron schedule, such as: "0/5 * * * * ?".
     */
    public void addEvent(String eventName, String cronSchedule) {
        // define the job and tie it to the clock monitor class        
        JobDetail job = JobBuilder.newJob(ClockMonitor.class)
                .withIdentity("Clock-Monitor-Event")
                .build();
        // example cron string "0/5 * * * * ?" i.e .every 5 minutes
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(eventName)
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(cronSchedule))
                .build();
        try {
            //Schedule the job, the clock monitor will then wait for a response.
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException ex) {
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This adds a simple event that runs X seconds from now
     *
     * @param eventName The event/metric name to trigger the event
     * @param secondsFromNow The time in seconds from now before it triggers.
     */
    public void addEvent(String eventName, int secondsFromNow) {
        JobDetail job = JobBuilder.newJob(ClockMonitor.class)
                .withIdentity("Clock-Monitor-Event")
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(eventName)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(secondsFromNow))
                .build();

        try {
            //Schedule the job, the clock monitor will then wait for a response.
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException ex) {
            Logger.getLogger(ClockMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
