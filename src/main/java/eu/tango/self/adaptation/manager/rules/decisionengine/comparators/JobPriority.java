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
import java.io.Serializable;
import java.util.Comparator;

/**
 * This class compares applications on host by order of their job priority.
 * @author Richard Kavanagh
 */
public class JobPriority implements Comparator<ApplicationOnHost>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(ApplicationOnHost o1, ApplicationOnHost o2) {
        int answer = o1.getPropertyAsString("priority").compareTo(o2.getPropertyAsString("priority"));
        if (answer == 0) { //If equal then apply a second order sort
        //ranks by job id in reverse order! thus should equate to age
            return Integer.valueOf(o2.getId()).compareTo(o1.getId());
        }
        return answer;
    }
    
}
