/*
 * Copyright [2017] [Alexander Reelsen]
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
package de.spinscale.maxcube.cli.renderer;

import com.jakewharton.fliptables.FlipTable;
import de.spinscale.maxcube.data.Parser;
import de.spinscale.maxcube.entities.Cube;
import de.spinscale.maxcube.entities.Room;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.List;

public class CliRenderer implements Renderer{

    static final String[] HEADERS = { "Id", "Room", "Temp", "Window open", "Valve %", "Low battery", "Mode" };

    private final DecimalFormat df = new DecimalFormat("##.#");

    @Override
    public void render(Cube cube, OutputStream os) {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os));
        writeCubeInfo(cube, writer);
        writeRooms(cube.getRooms(), writer);
        writer.flush();
    }

    private void writeCubeInfo(Cube cube, PrintWriter writer) {
        String [][] data = new String[1][3];
        data[0][0] = cube.getSerial();
        data[0][1] = cube.getDate().toString();
        data[0][2] = cube.getFirmwareVersion();
        writer.println(FlipTable.of(new String[]{ "id", "date", "firmware" }, data));
    }

    private void writeRooms(List<Room> rooms, PrintWriter writer) {
        String [][] data = new String[rooms.size()][HEADERS.length];

        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            data[i][0] = String.valueOf(room.getId());
            data[i][1] = room.getName();
            data[i][2] = String.valueOf(df.format(room.getCurrentTemperature()));
            data[i][3] = String.valueOf(room.isWindowOpen());
            data[i][4] = String.valueOf(room.getValvePositionInPercent());
            data[i][5] = String.valueOf(room.isLowBattery());
            data[i][6] = "-";
            try {
                Parser.Mode mode = room.findThermostat().getMode();
                if (mode == Parser.Mode.VACATION) {
                    data[i][6] = "VACATION " + room.findThermostat().getEndTime();
                } else {
                    data[i][6] = mode.toString();
                }
            } catch (IllegalArgumentException e) {}
        }

        writer.println(FlipTable.of(HEADERS, data));
    }
}
