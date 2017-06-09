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
package eu.tango.self.adaptation.manager.model;

/**
 * Status of a host of an infrastructure.
 * @deprecated not needed in tango, hangover from ASCETiC
 * @author Richard Kavanagh
 *
 */
public class Slot {
    
    private String hostname;
    private double freeMemoryMb;
    private double freeCpus;
    private double freeDiskGb;

    public Slot() {
    }
    
    public Slot(String hostname, double freeCpus, double freeDiskGb, double freeMemoryMb){
        this.hostname = hostname;
        this.freeCpus = freeCpus;
        this.freeDiskGb = freeDiskGb;
        this.freeMemoryMb = freeMemoryMb;
    }

    /**
     * @return the freeMemoryMb
     */
    public double getFreeMemoryMb() {
        return freeMemoryMb;
    }

    /**
     * @param freeMemoryMb the freeMemoryMb to set
     */
    public void setFreeMemoryMb(double freeMemoryMb) {
        this.freeMemoryMb = freeMemoryMb;
    }

    /**
     * @return the freeCpus
     */
    public double getFreeCpus() {
        return freeCpus;
    }

    /**
     * @param freeCpus the freeCpus to set
     */
    public void setFreeCpus(double freeCpus) {
        this.freeCpus = freeCpus;
    }

    /**
     * @return the freeDiskGb
     */
    public double getFreeDiskGb() {
        return freeDiskGb;
    }

    /**
     * @param freeDiskGb the freeDiskGb to set
     */
    public void setFreeDiskGb(double freeDiskGb) {
        this.freeDiskGb = freeDiskGb;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}

