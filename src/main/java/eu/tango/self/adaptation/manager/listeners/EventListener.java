/**
 * Copyright 2015 University of Leeds
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

import eu.tango.self.adaptation.manager.rules.EventAssessor;

/**
 * This is a the event listener interface for the self adaption manager.
 * @author Richard Kavanagh
 */
public interface EventListener {
    
    /**
     * This sets the event assessor that is used once an event has occurred.
     * @param assessor The event assessor to set
     */
    public void setEventAssessor(EventAssessor assessor);
    
    /**
     * This gets the event assessor that is used once an event has occurred.
     * @return  The event assessor that this event listener is using
     */
    public EventAssessor getEventAssessor();
    
    /**
     * This starts the event listener listening.
     */    
    public void startListening();    
    
    /**
     * This stops the event listener from listening.
     */
    public void stopListening();
    
    /**
     * Indicates if this listener is listening.
     */
    public boolean isListening();
    
}
