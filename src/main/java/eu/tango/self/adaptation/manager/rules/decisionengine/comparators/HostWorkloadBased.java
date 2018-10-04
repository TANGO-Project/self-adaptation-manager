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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This has the effect of implementing job priority ranking against hosts.
 * @author Richard Kavanagh
 */
public class HostWorkloadBased implements Comparator<Host>, Serializable {   

    private static final long serialVersionUID = 1L;
    private HashMap<Host, HostWorkload> workloadMap;
    
    /**
     * This creates a new comparator based upon host workload and its type
     */
    public HostWorkloadBased() {
    }
    
    /**
     * This creates a new comparator based upon host workload and its type,
     * This allows the workload to be set.
     * @param workload 
     */
    public HostWorkloadBased(List<ApplicationOnHost> workload) {
        System.out.println("Seen here workload based");
        workloadMap = HostWorkload.getHostWorkloadsMap(workload);
    }

    @Override
    public int compare(Host o1, Host o2) {
        mapContainsHostGuard(o1);
        mapContainsHostGuard(o2);
        //Rank Malable 0 -> Rigid 3 last
        int answer = workloadMap.get(o2).getPriority().compareTo(workloadMap.get(o1).getPriority());
        if (answer == 0) { //If equal then apply a second order sort
            //Ensure smallest queues are cancelled first
            answer = workloadMap.get(o1).getQueueLength().compareTo(workloadMap.get(o2).getQueueLength());
            if (answer == 0) { //largest power consumption first
                answer = Double.valueOf(o2.getIdlePowerConsumption()).compareTo(o1.getIdlePowerConsumption());
            }
        }
        return answer;
    }
    
    /**
     * This checks to see if the workload map contains a given host
     * @param host The host to perform the check against
     */
    private void mapContainsHostGuard(Host host) {
        if (!workloadMap.containsKey(host)) {
            workloadMap.put(host, new HostWorkload(host, new ArrayList<ApplicationOnHost>()));
        }
    }

    /**
     * @return the workloadMap
     */
    public HashMap<Host, HostWorkload> getWorkloadMap() {
        return workloadMap;
    }

    /**
     * @param workloadMap the workloadMap to set
     */
    public void setWorkloadMap(HashMap<Host, HostWorkload> workloadMap) {
        this.workloadMap = workloadMap;
    }
    
}
