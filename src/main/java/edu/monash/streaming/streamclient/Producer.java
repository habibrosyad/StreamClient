package edu.monash.streaming.streamclient;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

public class Producer {
    private static int POOL_SIZE=10;
    private final Path dir;
    private final String host;
    private final int port;
    private final Map<String, Integer> rates;
    private final LongAdder n;
    private final LongAdder completion;

    /**
     * Initialise Reader
     *
     * @param dir
     * @param host
     * @param port
     * @throws IOException
     */
    public Producer(String dir, String host, int port, Map rates) throws IOException {
        this.dir = Paths.get(dir);

        // Check collection path existence and permission of source.
        if (!Files.isDirectory(this.dir) || !Files.isReadable(this.dir))
            throw new IOException("Invalid path");

        // Connection parameters
        this.host = host;
        this.port = port;
        this.rates = rates;

        // Number of sources to process
        n = new LongAdder();

        // Completion tracker
        completion = new LongAdder();
    }

    /**
     * Send stream of data read from the files in the path defined
     *
     * Valid data format in the source would be 2 values separated by a space and ended with newline:
     * x y
     * where:
     * x -> id
     * y -> value
     *
     * @throws IOException
     */
    public void start() throws Exception {
        // Define executors pool
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);

        // Reading sources and sending through socket
        try (Stream<Path> files = Files.walk(dir, 1)) {
            // Get files to process
            Stream<Path> sources = files.filter(source -> Files.isRegularFile(source));

            // Process each file in parallel
            sources.forEach(source -> {
                // Get rate definition for this stream
                Integer rate = rates.get(source.getFileName().toString().toLowerCase());
                pool.submit(new Reader(source, rate));

                // Update number of sources
                n.increment();
            });
        }
    }

    /**
     * File reader task definition
     */
    private class Reader implements Runnable {
        private final Path source;
        private final Integer rate;

        public Reader(Path source, Integer rate) {
            this.source = source;
            this.rate = rate;
        }

        @Override
        public void run(){
            Socket socket = null;
            String name = source.getFileName().toString().toLowerCase();

            try {
                // Define connection
                socket = new Socket(host, port);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                // Read file
                Scanner scanner = new Scanner(new FileReader(source.toFile()));
                System.out.println("Start streaming for " + name + " from " + Thread.currentThread().getName());
                // Rate limiter (tuples/sec)
                long before = 0;
                long ahead = 0;

                while (scanner.hasNextLine()) {
                    // StreamName:Key:Value
                    String data = name + ":" + scanner.nextLine().trim().replace(" ",":") + "\n";

                    // Send data
                    outputStream.writeBytes(data);

                    // Control stream rate (tuples/sec)
                    if (rate != null) {
                        long now = System.currentTimeMillis();
                        if (before != 0) {
                            ahead -= (now - before)/1000 * rate - 1;
                            if (ahead > 0) {
                                Thread.sleep(ahead/rate*1000);
                            }
                        }
                        before = now;
                    }
                }
                // Send blank indicating for finish
                outputStream.writeBytes("\n");

                // Flushing
                outputStream.flush();

                System.out.println("Streaming finished for " + name);
            } catch (Exception e) {
                System.out.println("Error on stream " + name + ": " + e.getMessage());
            } finally{
                // Increase completion
                completion.increment();

                // Close socket properly
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (completion.sum() == n.sum()) {
                        // If completion reaches n then exit the system
                        System.exit(0);
                    }
                }
            }
        }
    }
}
