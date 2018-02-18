/*
 * Copyright [2018] [Markus Schwarz]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.spinscale.maxcube.cli;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Markus Schwarz
 */
@Path("thermostat")
public class Thermostat {

    /* http://localhost:8080/maxcube-web/resources/thermostat */
    @GET
    public String message() {
        return "Only POST requests were supported";
    }


    /* curl -v --data "room=Kitchen&temperature=20.5" http://localhost:8080/maxcube-web/resources/thermostat */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String setTemperature(@FormParam("room") String room, @FormParam("temperature") Double temperature) throws Exception {
        Cli.ManualTemperature manualTemperature = new Cli.ManualTemperature();
        manualTemperature.roomName = room;
        manualTemperature.temperature = temperature;
        manualTemperature.doRun();
        return "setTemperature() executed";
    }
}
