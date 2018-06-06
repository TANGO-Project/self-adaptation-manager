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
package eu.tango.self.adaptation.manager.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains generic utility functions for executing applications
 * @author Richard Kavanagh
 */
public abstract class ExecuteUtils {

    /**
     * This utility class is not expected to be instantiated. It is a collection
     * of static methods.
     */    
    private ExecuteUtils() {
    }
    
    /**
     * This executes a command and returns the output as a line of strings.
     *
     * @param cmd The command to execute
     * @return A list of output broken down by line
     */
    public static ArrayList<String> execCmd(String cmd) {
        String wholeCmd[] = {"/bin/sh",
            "-c",
            cmd};
        try {
            return execCmd(wholeCmd);
        } catch (IOException ex) {
            Logger.getLogger(ExecuteUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    /**
     * This executes a command and returns the output as a line of strings.
     *
     * @param cmd The command to execute
     * @return A list of output broken down by line
     * @throws java.io.IOException
     */
    public static ArrayList<String> execCmd(String[] cmd) throws java.io.IOException {
        ArrayList<String> output = new ArrayList<>();
        Process proc = Runtime.getRuntime().exec(cmd);
        java.io.InputStream is = proc.getInputStream();
        java.util.Scanner s = new java.util.Scanner(is);
        String outputLine;
        while (s.hasNextLine()) {
            outputLine = s.nextLine();
            output.add(outputLine);
        }
        return output;
    }    
    
}
