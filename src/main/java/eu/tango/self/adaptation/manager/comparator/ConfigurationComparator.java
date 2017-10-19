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

import eu.ascetic.ioutils.io.ResultsStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The aim of this class is to read in comparison information, so that it can be
 * used to decide which configuration/accelerator is best to use for a given
 * deployment. The data is read in from file, which contains information such as:
 * (Application name, energy consumed (j), duration (s), job id (slurm), and configuration id)
 * @author Richard Kavanagh
 */
public class ConfigurationComparator {

    public ConfigurationComparator() {
    }
    
    public static void main(String[] args) {
        ConfigurationComparator compare = new ConfigurationComparator();
        ResultsStore data = compare.loadComparisonData();
        data = compare.filterOnName(data, "Benchmark");
        for (int i = 0; i < data.size(); i++) {
            System.out.println(data.getRow(i).toString());
        }
        System.out.println("-------------------");
        ArrayList<ConfigurationRank> answer = compare.averageAndGroup(data, "cpu");
        String energyWinner = compare.getConfigWithLowestEnergy(answer);
        String timeWinner = compare.getConfigWithLowestTime(answer);
        System.out.println("Lowest Energy: " + energyWinner);
        System.out.println("Lowest Time: " + timeWinner);
        System.out.println("Lowest Energy Ratio: " + compare.getEnergyUsedVsXRatio(answer, energyWinner));
        System.out.println("Lowest Time Ratio: " + compare.getDurationVsXRatio(answer, timeWinner));
        System.out.println("--------- Energy ----------");
        ArrayList<ConfigurationRank> rank = compare.compare("Benchmark", "cpu");
        rank.sort(new EnergyComparator());
        for (ConfigurationRank item : rank) {
            System.out.println(item.toString());
        }
        //Time
        System.out.println("--------- Time ----------");
        rank.sort(new TimeComparator());
        for (ConfigurationRank item : rank) {
            System.out.println(item.toString());
        }        
    }
    
    /**
     * This gets the table with the rankings of each configuration
     * @param applicationName The application name
     * @param referenceConfig The reference configuration for comparison against
     * @return The complete set of results.
     */
    public ArrayList<ConfigurationRank> compare(String applicationName, String referenceConfig) {
        ResultsStore data = loadComparisonData();
        data = filterOnName(data, applicationName);
        return averageAndGroup(data, referenceConfig);
    }
    
    /**
     * This returns the configuration which completes the fastest
     * @param original The set of results from which to select
     * @return The id of the configuration with the lowest completion time
     */
    public String getConfigWithLowestTime(ArrayList<ConfigurationRank> original) {
        String answer = "";
        double lowestScore = Double.MAX_VALUE;
        for (ConfigurationRank item : original) {
            double current = item.getAverageTime();
            if (current < lowestScore) {
                lowestScore = current;
                answer = item.getConfigName();
            }
        }
        return answer;
    }
    
    /**
     * This returns the configuration which on completion uses the lowest amount
     * of energy.
     * @param original The set of results from which to select
     * @return The id of the configuration with the lowest completion time
     */    
    public String getConfigWithLowestEnergy(ArrayList<ConfigurationRank> original) {
        String answer = "";
        double lowestScore = Double.MAX_VALUE;
        for (ConfigurationRank item : original) {
            double current = item.getAverageEnergy();
            if (current < lowestScore) {
                lowestScore = current;
                answer = item.getConfigName();
            }
        }
        return answer;
    }
    
    /**
     * This gets for a given configuration the ratio between energy usage in 
     * comparison to the baseline configuration
     * @param original The set of results from which to select
     * @param configId The configuration to recover the energy usage ratio for
     * @return The configurations energy usage as compared to a baseline
     */
    public double getEnergyUsedVsXRatio(ArrayList<ConfigurationRank> original, String configId) {
        for (ConfigurationRank item : original) {
            String current = item.getConfigName().trim();
            if (current.equals(configId)) {
                return item.getEnergyUsedVsReference();
            }
        }
        return Double.NaN;
    }
    
    /**
     * Indicates if an energy or time reference value is better than the target 
     * reference. e.g. the reference might be the currently running case, thus
     * only configurations that are better than the reference should be selected.
     * @param xVsYRatio
     * @return 
     */
    public boolean isBetterThanReference(double xVsYRatio) {
        return (xVsYRatio < 1);
    }

    /**
     * This gets for a given configuration the ratio between completion time in 
     * comparison to the baseline configuration
     * @param original The set of results from which to select
     * @param configId The configuration to recover the energy usage ratio for
     * @return The configurations energy usage as compared to a baseline
     */    
    public double getDurationVsXRatio(ArrayList<ConfigurationRank> original, String configId) {
        for (ConfigurationRank item : original) {
            String current = item.getConfigName().trim();
            if (current.equals(configId)) {
                return item.getDurationVsReference();
            }
        }
        return Double.NaN;
    }    
    
    /**
     * This loads the measurement data by which a decision should be made
     * @return 
     */
    private ResultsStore loadComparisonData() {
        ResultsStore comparisonData = new ResultsStore("Measurements.csv");
        comparisonData.load();
        for (int i = 0; i < comparisonData.size(); i++) {
            if (comparisonData.getRowSize(i) != 5) {
                comparisonData.removeRow(i);
            }
        }
        return comparisonData;
    }
    
    /**
     * This filters the results data by its name.
     * @param toFilter The result store to filter
     * @param applicationName The application name to filter against
     * @return 
     */
    private ResultsStore filterOnName(ResultsStore toFilter, String applicationName) {
        ResultsStore answer = new ResultsStore();       
        for (int i = 0; i < toFilter.size(); i++) {
            //First element is name
            if (toFilter.getElement(i, 0).trim().equals(applicationName)) {
                answer.addRow(toFilter.getRow(i));
            }
        }        
        return answer;
    }

    /**
     * This filters the results data by its name.
     * @param toAverage The set of measurements of historic logs.
     * @param referenceConfig The reference configuration id to compare others against
     * @return 
     */
    private ArrayList<ConfigurationRank> averageAndGroup(ResultsStore toAverage, String referenceConfig) {
        /**
         * To Average input row = (name, energy used, time used, job id, config id)
         * example: (Benchmark,9844,122,3604,cpu)
         */
        ArrayList<ConfigurationRank> answer = new ArrayList<>();
        HashMap<String, Double> count = new HashMap<>(); //count double
        HashMap<String, Double> energy = new HashMap<>(); //J
        HashMap<String, Integer> time = new HashMap<>(); //s
        for (int i = 0; i < toAverage.size(); i++) {
            String configName = toAverage.getElement(i, 4).trim();
            String energyValue = toAverage.getElement(i, 1).trim();
            String timeValue = toAverage.getElement(i, 2).trim();
            if (count.containsKey(configName)) {
                count.put(configName, count.get(configName) + 1);
                energy.put(configName, energy.get(configName) + Double.parseDouble(energyValue));
                time.put(configName, time.get(configName) + Integer.parseInt(timeValue));
            } else {
                count.put(configName, 1.0);
                energy.put(configName, Double.parseDouble(energyValue));
                time.put(configName, Integer.parseInt(timeValue));          
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
