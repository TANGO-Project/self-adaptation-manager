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
package eu.tango.self.adaptation.manager.listeners;

import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.self.adaptation.manager.model.CompssImplementation;
import eu.tango.self.adaptation.manager.model.CompssResource;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Richard Kavanagh
 */
public class ProgrammingModelClientTest {
    
    public ProgrammingModelClientTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getCompssResources method, of class ProgrammingModelClient.
     */
    @Test
    public void testGetCompssResources() {
        System.out.println("getCompssResources");
        ProgrammingModelClient instance = new ProgrammingModelClient();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");
        List<CompssResource> result = instance.getCompssResources();
        assert(result.size() == 1);
        for (CompssResource item : result) {
            System.out.println(item);
        }
    }

    /**
     * Test of getCompssHostList method, of class ProgrammingModelClient.
     */
    @Test
    public void testGetCompssHostList() {
        System.out.println("getCompssHostList");
        ProgrammingModelClient instance = new ProgrammingModelClient();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");
        List<Host> result = instance.getCompssHostList();
        assert(result.size() == 1);
        for (Host item : result) {
            assert(item.getState().equals("IDLE"));            
            System.out.println(item);
        }
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state_running.xml");
        List<Host> result2 = instance.getCompssHostList();
        assert(result2.size() == 1);
        for (Host item : result2) {
            assert(!item.getState().equals("IDLE"));
            System.out.println(item);
        }        
        
    }

    /**
     * Test of getCompssImplementation method, of class ProgrammingModelClient.
     */
    @Test
    public void testGetCompssImplementation() {
        System.out.println("getCompssImplementation");
        ProgrammingModelClient instance = new ProgrammingModelClient();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");        
        List<CompssImplementation> result = instance.getCompssImplementation();
        assert(result.size() == 2);
        for (CompssImplementation item : result) {
            System.out.println(item);
        }
    }
    
}
