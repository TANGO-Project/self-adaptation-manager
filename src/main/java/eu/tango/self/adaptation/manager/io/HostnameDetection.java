/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.tango.self.adaptation.manager.io;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class detects the hostname of the machine in which it is running
 *
 * @author Richard Kavanagh
 */
public class HostnameDetection {

    /**
     * This gets the hostname of the machine in which this class is running
     *
     * @return The hostname of the current machine
     */
    public static String getHostname() {
        String os = System.getProperty("os.name").toLowerCase();
        String answer = "";

        try {
            if (os.contains("win")) {
                answer = System.getenv("COMPUTERNAME");
                answer = (answer.isEmpty() ? execReadToString("hostname") : answer);
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac os x")) {
                answer = System.getenv("HOSTNAME");
                answer = (answer.isEmpty() ? execReadToString("hostname") : answer);
                answer = (answer.isEmpty() ? execReadToString("cat /etc/hostname") : answer);
            }
        } catch (IOException ex) {
            Logger.getLogger(HostnameDetection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    /**
     * This executes commands via java's exec command in cases where the
     * System.getEnv property is missing
     *
     * @param execCommand The command to execute
     * @return The text output of the command
     * @throws IOException
     */
    private static String execReadToString(String execCommand) throws IOException {
        try (Scanner s = new Scanner(Runtime.getRuntime().exec(execCommand).getInputStream()).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

}
