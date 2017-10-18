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
        ResultsStore answer = compare.averageAndGroup(data, "cpu");
        for (int i = 0; i < answer.size(); i++) {
            System.out.println(answer.getRow(i).toString());
        }
        System.out.println("-------------------");
        String energyWinner = compare.getConfigWithLowestEnergy(answer);
        String timeWinner = compare.getConfigWithLowestTime(answer);
        System.out.println("Lowest Energy: " + energyWinner);
        System.out.println("Lowest Time: " + timeWinner);
        System.out.println("Lowest Energy Ratio: " + compare.getEnergyUsedVsXRatio(answer, energyWinner));
        System.out.println("Lowest Time Ratio: " + compare.getDurationVsXRatio(answer, timeWinner));
    }
    
    /**
     * This gets the table with the rankings of each configuration
     * @param applicationName The application name
     * @param referenceConfig The reference configuration for comparison against
     * @return The complete set of results.
     */
    public ResultsStore compare(String applicationName, String referenceConfig) {
        ResultsStore data = loadComparisonData();
        data = filterOnName(data, applicationName);
        return averageAndGroup(data, referenceConfig);
    }
    
    public String getConfigWithLowestTime(ResultsStore original) {
        String answer = "";
        double lowestScore = Double.MAX_VALUE;
        for (int i = 1; i < original.size(); i++) { //1 skips header
            double current = Double.parseDouble(original.getElement(i, 5).trim());
            if (current < lowestScore) {
                lowestScore = current;
                answer = original.getElement(i, 0);
            }
        }
        return answer;
    }
    
    public String getConfigWithLowestEnergy(ResultsStore original) {
        String answer = "";
        double lowestScore = Double.MAX_VALUE;
        for (int i = 1; i < original.size(); i++) { //1 skips header
            double current = Double.parseDouble(original.getElement(i, 3).trim());
            if (current < lowestScore) {
                lowestScore = current;
                answer = original.getElement(i, 0);
            }
        }
        return answer;
    }
    
    public double getEnergyUsedVsXRatio(ResultsStore original, String configId) {
        for (int i = 1; i < original.size(); i++) { //1 skips header
            String current = original.getElement(i, 0).trim();
            if (current.equals(configId)) {
                return Double.parseDouble(original.getElement(i, 6).trim());
            }
        }
        return Double.NaN;
    }
    
    public double getDurationVsXRatio(ResultsStore original, String configId) {
        for (int i = 1; i < original.size(); i++) { //1 skips header
            String current = original.getElement(i, 0).trim();
            if (current.equals(configId)) {
                return Double.parseDouble(original.getElement(i, 7).trim());
            }
        }
        return Double.NaN;
    }    
    
    /**
     * This loads the measurement data by which a decision should be made
     * @return 
     */
    public ResultsStore loadComparisonData() {
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
    public ResultsStore filterOnName(ResultsStore toFilter, String applicationName) {
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
    public ResultsStore averageAndGroup(ResultsStore toAverage, String referenceConfig) {
        /**
         * To Average input row = (name, energy used, time used, job id, config id)
         * example: (Benchmark,9844,122,3604,cpu)
         */
        ResultsStore answer = new ResultsStore();
        answer.add("Name");
        answer.append("Count");
        answer.append("Total Energy");
        answer.append("Average Energy");
        answer.append("Total Time");
        answer.append("Average Time");
        answer.append("Energy Used vs CPU"); //vs configuration 1?? any one config will do
        answer.append("Duration vs CPU");
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
            answer.add(item.getKey());
            double countVal = item.getValue();
            double energyVal = energy.get(item.getKey());
            double timeVal = time.get(item.getKey());
            answer.append(item.getValue());
            answer.append(energyVal + "");
            answer.append(energyVal / countVal + "");
            answer.append(timeVal + "");
            answer.append(timeVal / countVal + "");
            answer.append((energyVal/countVal)/(energy.get(referenceConfig)/count.get(referenceConfig)));
            answer.append((timeVal/countVal)/(time.get(referenceConfig)/count.get(referenceConfig)));
        }
        return answer;
    }
    
}
