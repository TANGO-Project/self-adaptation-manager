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

import eu.tango.self.adaptation.manager.model.SLALimits;
import eu.tango.self.adaptation.manager.model.SLATerm;
import eu.tango.self.adaptation.manager.qos.SlaRulesLoader;
import eu.tango.self.adaptation.manager.rules.EventAssessor;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds the generic behaviours of a job monitor
 * @author Richard Kavanagh
 */
public abstract class AbstractJobMonitor implements EventListener, Runnable {

    private EventAssessor eventAssessor;
    private boolean running = false;
    private final SlaRulesLoader limits = SlaRulesLoader.getInstance();    
    
    @Override
    public void setEventAssessor(EventAssessor assessor) {
        eventAssessor = assessor;
    }

    @Override
    public EventAssessor getEventAssessor() {
        return eventAssessor;
    }

    /**
     * This starts the compss job monitor going, in a daemon thread.
     */
    @Override
    public void startListening() {
        Thread jobMonThread = new Thread(this);
        jobMonThread.setDaemon(true);
        jobMonThread.start();
    }

    @Override
    public void stopListening() {
        running = false;
    }

    @Override
    public boolean isListening() {
        return running;
    }
    
    /**
     * This reloads the SLA criteria held in the slurm job monitor.
     */
    public void reloadLimits() {
        limits.reloadLimits();
    }
    
    /**
     * Checks to see if the SLA rules includes a check for a given condition
     *
     * @param limits The SLA terms
     * @param termName The name of the SLA term
     * @return If the term is contained or not within the SLA limits set
     */
    protected boolean containsTerm(SLALimits limits, String termName) {
        for (SLATerm slaTerm : limits.getQosCriteria()) {
            if (slaTerm.getAgreementTerm().equals(termName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * This takes a list of measurements and determines if an SLA breach has
     * occurred by comparing them to the QoS limits.
     *
     * @param limits The QoS goal limits.
     * @return The first SLA breach event. Null if none found.
     */
    protected abstract ArrayList<EventData> detectEvent(SLALimits limits);    

    @Override
    public void run() {
        running = true;
        try {
            // Wait for a message
            while (running) {
                for (EventData event : detectEvent(limits.getLimits())) {
                    eventAssessor.assessEvent(event);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(AbstractJobMonitor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(AbstractJobMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
