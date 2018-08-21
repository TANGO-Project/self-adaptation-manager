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

import eu.tango.self.adaptation.manager.model.ApplicationExecutionInstance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This records the output of the configuration comparison methods, presenting
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

    /**
     * This creates a new Configuration rank record.
     * @param configName The name of the configuration that has been compared
     * @param recordCount The amount of records making up the comparison
     * @param totalEnergy The total energy spent running the application, with
     * this particular configuration
     * @param totalTime The total time used executing this configuration
     * @param energyUsedVsReference The energy consumed vs a reference configuration
     * @param durationVsReference The time consumed vs a reference configuration
     */
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

    /**
     * The name of the ALDE application configuration that has been compared.
     * @return 
     */
    public String getConfigName() {
        return configName;
    }
    
    /**
     * The count of individual runs of the execution configuration used to create 
     * this rank record.
     * @return The count of records this rank record is based upon.
     */
    public double getRecordCount() {
        return recordCount;
    }

    /**
     * The total amount of energy consumed by the applications running this given
     * configuration
     * @return 
     */
    public double getTotalEnergy() {
        return totalEnergy;
    }

    /**
     * The average amount of energy consumed by the applications running this given
     * configuration
     * @return The average energy consumed by each run
     */
    public double getAverageEnergy() {
        return totalEnergy / recordCount;
    }
    
    /**
     * The average power consumption for applications running this given
     * configuration
     * @return The average power consumed by all of the runs 
     */
    public double getAveragePower() {
        return getAverageEnergy() / getAverageTime();
    }

    /**
     * The total amount of time used by the applications running this given
     * configuration, across all runs
     * @return 
     */
    public double getTotalTime() {
        return totalTime;
    }

    /**
     * The average amount of time used by the applications running this given
     * configuration
     * @return 
     */
    public double getAverageTime() {
        return totalTime / recordCount;
    }

    /**
     * For comparison purposes this rank object is compared to another. This could
     * for example be the current running instance. In which case values less than
     * 1 indicates that it is a configuration which is more worthwhile to run.
     * @return The time rank of the execution configuration, lower is better.
     */
    public double getDurationVsReference() {
        return durationVsReference;
    }

    /**
     * For comparison purposes this rank object is compared to another. This could
     * for example be the current running instance. In which case values less than
     * 1 indicates that it is a configuration which is more worthwhile to run.
     * @return The energy rank of the execution configuration, lower is better.
     */
    public double getEnergyUsedVsReference() {
        return energyUsedVsReference;
    }
    
    /**
     * For comparison purposes this rank object is compared to another. This could
     * for example be the current running instance. In which case values less than
     * 1 indicates that it is a configuration which is more worthwhile to run.
     * @return The power rank of the execution configuration, lower is better.
     */    
    public double getAveragePowerUsageVsReference() {
        return getEnergyUsedVsReference() / getDurationVsReference();
    }

    /**
     * This sets the name of the configuration that was run
     * @param configName The name of the execution configuration used for this record
     */
    public void setConfigName(String configName) {
        this.configName = configName;
    }

    /**
     * This sets the count of runs used to determine the execution configurations rank
     * @param recordCount The count of runs that make up this record
     */
    public void setRecordCount(double recordCount) {
        this.recordCount = recordCount;
    }

    /**
     * This sets the total amount of energy used determining the execution
     * configurations rank.
     * @param totalEnergy 
     */
    public void setTotalEnergy(double totalEnergy) {
        this.totalEnergy = totalEnergy;
    }

    /**
     * This sets the total amount of time used determining the execution
     * configurations rank.
     * @param totalTime 
     */
    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    /**
     * This sets the ratio between the current rank and the reference value it
     * is to be compared against
     * @param DurationVsReference 
     */
    public void setDurationVsReference(double DurationVsReference) {
        this.durationVsReference = DurationVsReference;
    }

    /**
     * This sets the ratio between the current rank and the reference value it
     * is to be compared against
     * @param EnergyUsedVsReference 
     */
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
    
    /**
     * This filters the results data by its name.
     * @param toAverage The set of measurements of historic logs.
     * @param referenceConfig The reference configuration id to compare others against
     * @return The list of configurations with ranking applied
     */
    public static List<ConfigurationRank> getConfigurationRank(List<ApplicationExecutionInstance> toAverage, String referenceConfig) {
        /**
         * To Average input row = (name, energy used, time used, job id, config id)
         * example: (Benchmark,9844,122,3604,cpu)
         */
        ArrayList<ConfigurationRank> answer = new ArrayList<>();
        HashMap<String, Double> count = new HashMap<>(); //count double
        HashMap<String, Double> energy = new HashMap<>(); //J
        HashMap<String, Double> time = new HashMap<>(); //s
        for (ApplicationExecutionInstance instance : toAverage) {
            String configName = instance.getExecutionConfigurationsId() + "";
            double energyValue = (double) instance.getEnergy();
            double timeValue = (double) instance.getDuration();
            //Skip empty records
            if (energyValue == 0 || timeValue == 0) {
                continue;
            }
            if (count.containsKey(configName)) { //case where value already exists in the map
                count.put(configName, count.get(configName) + 1);
                energy.put(configName, energy.get(configName) + energyValue);
                time.put(configName, time.get(configName) + timeValue);
            } else { //doesn't exist case
                count.put(configName, 1.0); //put first item in list
                energy.put(configName, energyValue);
                time.put(configName, timeValue);          
            }
        }
        for (Map.Entry<String, Double> item : count.entrySet()) {
            double countVal = item.getValue();
            double energyVal = energy.get(item.getKey());
            double timeVal = time.get(item.getKey());
            answer.add(new ConfigurationRank(item.getKey(), countVal, energyVal, timeVal, 
                    (energyVal/countVal)/(energy.get(referenceConfig)/count.get(referenceConfig)),
                    (timeVal/countVal)/(time.get(referenceConfig)/count.get(referenceConfig))));
        }
        return answer;
    }    
    
}