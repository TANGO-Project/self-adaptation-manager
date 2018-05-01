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

import eu.tango.self.adaptation.manager.rules.datatypes.ApplicationEventData;
import eu.tango.self.adaptation.manager.rules.datatypes.EventData;
import eu.tango.self.adaptation.manager.rules.datatypes.HostEventData;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This provides a REST interface for submitting events to the SAM.
 *
 * @author Richard Kavanagh
 */
@Path("/event")
public class RestEventService {

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String getTest() {
        return "This is a test for server liveliness";
    }

    @POST
    @Path("/submit")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addEvent(RestEvent data) {

        /*
     
         curl -H "Content-type: application/json" -d '{
         "time" : 5,
         "origin" : "Host",
         "rawValue" : 0,
         "guaranteedValue" : 0,
         "type" : "SLA_BREACH",
         "guaranteeOperator" : "EQ",
         "agreementTerm" : "test",
         "guaranteeid" : "test",
         "hostname" : "ns51",
         "applicationId" : "",    
         "deploymentId" : ""
         }' 'http://localhost:8080/sam/event/submit'  
        
         */
        if (data == null || data.getOrigin() == null) {
            return Response.status(201).build();
        }
        System.out.println(data);
        EventData toProcess = null;
        switch (data.getOrigin()) {
            case "Application":
                toProcess = new ApplicationEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                        data.getRawValue(),
                        data.getGuaranteedValue(),
                        EventData.Type.valueOf(data.getType()),
                        EventData.Operator.valueOf(data.getGuaranteeOperator()),
                        data.getApplicationId(),
                        data.getDeploymentId(),
                        data.getGuaranteeid(),
                        data.getAgreementTerm());
                break;
            case "Host":
                toProcess = new HostEventData(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                        data.getHostname(),
                        data.getRawValue(),
                        data.getGuaranteedValue(),
                        EventData.Type.valueOf(data.getType()),
                        EventData.Operator.valueOf(data.getGuaranteeOperator()),
                        data.getGuaranteeid(),
                        data.getAgreementTerm());
                break;
            default:
                Logger.getLogger(RestEventService.class.getName()).log(Level.WARNING, "Origin of event unknown!");
                break;
        }
        if (toProcess != null) {
            RestEventMonitor.getInstance().assessEvent(toProcess);
        }
        return Response.status(201).build();
    }

}
