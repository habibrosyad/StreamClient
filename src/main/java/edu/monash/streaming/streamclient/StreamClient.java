package edu.monash.streaming.streamclient;

import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.Map;

public class StreamClient {
    /**
     * java -jar StreamClient.jar -h localhost -p 4444 -d ~/dir -RstreamR=1 -RstreamS=1 ...
     * @param args
     */
    public static void main(String[] args) {
        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addRequiredOption("d", "dir", true, "source directory path");
        options.addOption("h", true, "hostname");
        options.addOption("p", true, "port number");
        options.addOption(Option.builder("R")
                                .hasArgs()
                                .valueSeparator('=')
                                .desc("rates for each stream")
                                .build());
        try {
            // parse the command line arguments
            CommandLine cmd = parser.parse(options, args);

            // Default parameters
            String dir = cmd.getOptionValue("d");
            String host = cmd.getOptionValue("h", "localhost");
            int port = Integer.parseInt(cmd.getOptionValue("p", "4444"));
            Map<String,Integer> rates = new HashMap<>();

            // Get rates definition if any
            int i = 0;
            String key = "";
            String[] values = cmd.getOptionValues("R");
            if (values != null && values.length > 0) {
                for (String v : values) {
                    if (i++ % 2 == 0) {
                        key = v.toLowerCase();
                    } else {
                        rates.put(key, Integer.parseInt(v));
                    }
                }
            }

            // Initialise producer
            Producer producer = new Producer(dir, host, port, rates);

            // Send data to the host:port
            producer.start();
        } catch (Exception e) {
            System.out.println("Unexpected error:" +e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "StreamClient", options);
        }
    }
}
