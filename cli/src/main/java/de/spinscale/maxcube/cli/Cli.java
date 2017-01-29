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
package de.spinscale.maxcube.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import de.spinscale.maxcube.cli.renderer.CliRenderer;
import de.spinscale.maxcube.cli.renderer.Renderer;
import de.spinscale.maxcube.client.CubeClient;
import de.spinscale.maxcube.client.SocketCubeClient;
import de.spinscale.maxcube.data.DurationParser;
import de.spinscale.maxcube.discovery.DiscoveredCube;
import de.spinscale.maxcube.discovery.DiscoveryClient;
import de.spinscale.maxcube.discovery.MinaDiscoveryClient;
import de.spinscale.maxcube.entities.Cube;
import de.spinscale.maxcube.entities.Room;
import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class Cli {

    private final static ByteArrayOutputStream bos = new ByteArrayOutputStream();

    public static void main(String[] args) throws IOException {
        configureInMemoryLogging();

        // argument parsing
        io.airlift.airline.Cli.CliBuilder<Runnable> builder = io.airlift.airline.Cli.<Runnable>builder("eq3")
                .withCommands(Help.class, Info.class, Discover.class, Version.class, Boost.class, Holiday.class)
                .withDescription("Tool to manage Max!EQ3 cubes from the command line")
                .withDefaultCommand(Help.class);

        builder.withGroup("report")
                .withDescription("Reporting to a configurable backend")
                .withCommands(ReportCli.class)
                .withDefaultCommand(Help.class);

        io.airlift.airline.Cli<Runnable> gitParser = builder.build();
        gitParser.parse(args).run();

        // some parts do not exit correctly or daemon thread, this is why we need to call System.exit here
        System.exit(0);
    }

    private static void configureInMemoryLogging() {
        org.slf4j.Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Logger logbackRootLogger = (Logger) rootLogger;
        logbackRootLogger.setLevel(Level.INFO);

        OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("[%date] [%level] [%logger{10}] %msg%n");
        ple.setContext(lc);
        ple.start();
        try {
            ple.init(bos);
        } catch (IOException e) {
        }
        appender.setContext(lc);
        appender.setOutputStream(bos);
        appender.setName("buffered");
        appender.setEncoder(ple);
        appender.start();

        logbackRootLogger.detachAppender("console");
        logbackRootLogger.addAppender(appender);
        logbackRootLogger.setAdditive(true);
    }

    public abstract static class Eq3Command implements Runnable {

        protected final org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

        @Option(type = OptionType.GLOBAL, name = "-d", description = "Debug mode")
        public boolean debug;

        public void run() {
            if (debug) {
                org.slf4j.Logger rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                Logger logbackRootLogger = (Logger) rootLogger;
                logbackRootLogger.setLevel(Level.TRACE);
            }
            try {
                doRun();
            } catch (Exception e) {
                if (debug) {
                    LoggerFactory.getLogger(this.getClass()).error("Error while running", e);
                } else {
                    System.err.println(String.format(Locale.ROOT, "Error occured: %s", e.getMessage()));
                }
            }

            if (debug) {
                try {
                    bos.flush();
                    System.err.write(bos.toByteArray());
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        }

        abstract void doRun() throws Exception;
    }

    public abstract static class CubeHostCommand extends Eq3Command {

        @Arguments(description = "host of cube to query")
        public String host;

        abstract void doRun(String host) throws Exception ;

        @Override
        void doRun() throws Exception {
            if (host == null || host.trim().length() == 0) {
                host = System.getenv("EQ3_HOST");
                if (host == null || host.trim().length() == 0) {
                    throw new IllegalArgumentException("Either set a host to query or export EQ3_HOST");
                }
            }

            doRun(host);
        }
    }

    @Command(name = "discover", description = "Discover eq3 cubes")
    public static class Discover extends Eq3Command {

        @Arguments(description = "interface to scan on, i.e. eth0/en0", required = true)
        public String networkInterface;

        @Option(name = { "-t", "timeout" } , description = "Time to wait for responses, in seconds, defaults to 2")
        public Integer timeout = 2;

        public void doRun() throws Exception {
            DiscoveryClient client = new MinaDiscoveryClient();
            List<DiscoveredCube> cubes = client.discover(NetworkInterface.getByName(networkInterface), timeout);
            cubes.forEach(cube -> System.out.println(String.format(Locale.ROOT, "%s   %s", cube.getId(), cube.getHost())));
        }
    }

    @Command(name = "boost", description = "Boost a room")
    public static class Boost extends CubeHostCommand {

        @Option(name = { "-r", "--room" } , description = "The name of the room to boost", required = true)
        public String roomName;

        public void doRun(String host) throws Exception {
            try (CubeClient client = new SocketCubeClient(host)) {
                Cube cube = client.connect();
                Room room = cube.findRoom(roomName);
                boolean success = client.boost(room);
                if (!success) {
                    logger.error("Setting holiday interval was not successful!");
                }
            }
        }
    }

    @Command(name = "holiday", description = "Set holiday mode for a room")
    public static class Holiday extends CubeHostCommand {

        @Option(name = { "-r", "--room" } , description = "The name of the room to boost", required = true)
        public String roomName;

        @Option(name = { "-d", "--duration" } , description = "The duration of boosting", required = true)
        public String duration;

        @Option(name = { "-t", "--temperature" } , description = "The temperature in °C", required = true)
        public Integer temperature;

        public void doRun(String host) throws Exception {
            // exit early if parsing fails
            Duration duration = DurationParser.parse(this.duration);
            LocalDateTime endDateTime = LocalDateTime.now().plusSeconds(duration.getSeconds());

            try (CubeClient client = new SocketCubeClient(host)) {
                Cube cube = client.connect();
                Room room = cube.findRoom(roomName);
                boolean success = client.holiday(room, endDateTime, temperature);
                if (!success) {
                    logger.error("Setting holiday interval was not successful!");
                }
            }
        }
    }

    @Command(name = "version", description = "Display version and exit")
    public static class Version extends Eq3Command {

        @Override
        void doRun() throws Exception {
            System.out.println(String.format(Locale.ROOT, "Version: %s", getClass().getPackage().getImplementationVersion()));
        }
    }

    public abstract static class AbstractReport extends CubeHostCommand {
    }

    public abstract static class AbstractCliReport extends AbstractReport {
        public void doRun(String host) throws Exception {
            try (CubeClient cubeClient = new SocketCubeClient(host, 62910)) {
                Cube cube = cubeClient.connect();
                Renderer renderer = new CliRenderer();
                renderer.render(cube, System.out);
            }
        }
    }

    @Command(name = "info", description = "Return some standard information about the cube. Alias for `report cli`")
    public static class Info extends AbstractCliReport {}

    @Command(name = "cli", description = "Return some standard information about the cube to the terminal")
    public static class ReportCli extends AbstractCliReport {}
}
