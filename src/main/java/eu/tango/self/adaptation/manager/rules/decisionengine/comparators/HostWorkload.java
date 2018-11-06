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
package eu.tango.self.adaptation.manager.rules.decisionengine.comparators;

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * This represents a host workload. It is used for ranking hosts based upon the
 * types of workload that are currently running upon them.
 * @author Richard Kavanagh
 */
public class HostWorkload {
    
    //This hashmap holds the ranking between different types of job and how flexible they are to change.
    private static final HashMap<String,Integer> PRIORITY_MAP = new HashMap<>();
    //This is the property that is used to rank how flexible a host workload is to change.
    private static final String APPLICATION_TYPE = "application_type";
    
    static {
        PRIORITY_MAP.put("RIGID", 3); //highest rank least flexible for adaptation
        PRIORITY_MAP.put("MOULDABLE", 2);
        PRIORITY_MAP.put("CHECKPOINTABLE", 1);
        PRIORITY_MAP.put("MALLEABLE", 0);  //lowest rank and most flexible for adaptation
    }     
    
    private final Host host;
    private final List<ApplicationOnHost> applications;
    private int priority = 0;

    /**
     * This creates a new host to workload mapping ready for ranking
     * @param host
     * @param applications
     */
    public HostWorkload(Host host, List<ApplicationOnHost> applications) {
        this.host = host;
        this.applications = applications;
        for(ApplicationOnHost app : applications) {
            if (app.hasProperty(APPLICATION_TYPE)) {
                int appPriority = PRIORITY_MAP.get(app.getPropertyAsString(APPLICATION_TYPE));
                if (appPriority > priority) {
                    priority = appPriority;
                }
            }
        }
    }
    
    /**
     * This takes a list of applications on host and generates a list of host workload objects
     * @param applications The applications list
     * @return The list of host's workloads in a map structure
     */
    public static HashMap<Host,HostWorkload> getHostWorkloadsMap(List<ApplicationOnHost> applications) {
        HashMap<Host,HostWorkload> answer = new HashMap<>();
        List<HostWorkload> apps = getHostWorkloads(applications);
        for (HostWorkload app : apps) {
            answer.put(app.getHost(), app);
        }
        return answer;
    } 
    
    /**
     * This takes a list of applications on host and generates a list of host workload objects
     * @param applications The applications list
     * @return The list of host's workloads
     */
    public static List<HostWorkload> getHostWorkloads(List<ApplicationOnHost> applications) {
        List<ApplicationOnHost> appsList = new ArrayList<>(applications);
        List<HostWorkload> answer = new ArrayList<>();
        while(!appsList.isEmpty()) {
            ApplicationOnHost headApp = appsList.remove(0);
            HostWorkload item = new HostWorkload(headApp.getAllocatedTo(), new ArrayList<ApplicationOnHost>());
            item.applications.add(headApp);
            ListIterator<ApplicationOnHost> iter = appsList.listIterator();
            while(iter.hasNext()){
                ApplicationOnHost app = iter.next();
                if(app.getAllocatedTo().equals(app.getAllocatedTo())){
                    iter.remove();
                    item.applications.add(app);
                }
            }
            answer.add(item);
        }
        return answer;
    }

    /**
     * @return the host
     */
    public Host getHost() {
        return host;
    }
    
    /**
     * This returns the length of the queue of jobs for a given host
     * @return The length of the job queue for this host
     */
    public Integer getQueueLength() {
        return applications.size();
    }

    /**
     * @return the applications
     */
    public List<ApplicationOnHost> getApplications() {
        return applications;
    }

    /**
     * The priority of this host, based upon the job with the highest priority
     * @return the priority
     */
    public Integer getPriority() {
        return priority;
    }
    
}
