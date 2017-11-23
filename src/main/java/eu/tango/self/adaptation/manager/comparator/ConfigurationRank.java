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
package eu.tango.self.adaptation.manager.comparator;

/**
 * This records the output of the configuration comparison methods. presenting
 * the data in a consistent fashion.
 *
 * @author Richard Kavanagh
 */
public class ConfigurationRank implements Comparable<ConfigurationRank> {

    private String configName;
    private double recordCount = 0;
    private double totalEnergy;
    //Average Energy is calculateable
    private double totalTime; //value in seconds
    //Average time is calculateable
    private double energyUsedVsReference;
    private double durationVsReference;

    public ConfigurationRank(String configName, double recordCount,
            double totalEnergy, double totalTime,
            double energyUsedVsReference, double durationVsReference) {
        this.configName = configName;
        this.recordCount = recordCount;
        this.totalEnergy = totalEnergy;
        this.totalTime = totalTime;
        this.energyUsedVsReference = energyUsedVsReference;
        this.durationVsReference = durationVsReference;
    }

    public String getConfigName() {
        return configName;
    }

    public double getRecordCount() {
        return recordCount;
    }

    public double getTotalEnergy() {
        return totalEnergy;
    }

    public double getAverageEnergy() {
        return totalEnergy / recordCount;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public double getAverageTime() {
        return totalTime / recordCount;
    }

    public double getDurationVsReference() {
        return durationVsReference;
    }

    public double getEnergyUsedVsReference() {
        return energyUsedVsReference;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public void setRecordCount(double recordCount) {
        this.recordCount = recordCount;
    }

    public void setTotalEnergy(double totalEnergy) {
        this.totalEnergy = totalEnergy;
    }

    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    public void setDurationVsReference(double DurationVsReference) {
        this.durationVsReference = DurationVsReference;
    }

    public void setEnergyUsedVsReference(double EnergyUsedVsReference) {
        this.energyUsedVsReference = EnergyUsedVsReference;
    }

    @Override
    public String toString() {
        return configName + " : Count: " + ((int) recordCount) + " : Total Energy: "
                + totalEnergy + " : Avg Energy: " + getAverageEnergy() + " : Total Time: "
                + totalTime + " : Avg Time: " + getAverageTime() + " : Energy vs Reference: "
                + energyUsedVsReference + " : Duration vs Reference: " + durationVsReference;
    }

    @Override
    public int compareTo(ConfigurationRank o) {
        return Double.valueOf(getAverageEnergy()).compareTo(o.getAverageEnergy());
    }  
    
}