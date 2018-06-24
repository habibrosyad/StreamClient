# StreamClient
This is a Maven project as part of the stream join processing simulation experiments. StreamClient simulates remote stream sources as a command line application.

## Requirements
- Java
- Stream source (dataset) files containing `key` and `value` atributes, `key` will be used as the join attribute

## How to use
- Build the project (e.g build a StreamClient.jar)
- Run the `StreamClient.jar` within a terminal, e.g. by issuing this command:
```
java -jar StreamClient.jar -h remotehost -p 9999 -d ~/streamSources
```
The command above will read all files inside the `~/streamSources` directory path, treating them as the stream sources. Afterwards, as the data being read, the data will be streamed to the `remotehost` on the port `9999`. Here, `remotehost` is assumed to run the StreamServer command-line application.

##  Command-line arguments
Below are the complete list of arguments accepted by this application:
- `h`, specify the host target, if not specified `localhost` is used as the default value
- `p`, specify the host target's port number, if not specified `4444` is used as the default value
- `d`, specify the source directory for the files which are going to be used as the stream sources, this argument is mandatory. It will read all files inside the directory path specified and stream their contents (assuming `key` and `value` attributes) to the remote target. Each of the streams will be identified by its origin filename, e.g. the directory contains `rstream.txt` and `sstream.txt`, then the remote target will identify the streams (stream name) as `rstream.txt` and `sstream.txt` too.
- `R{stream_name}={rate_value}`, e.g. `Rrstream.txt=1000`, specify the rate of the data being sent to the remote target (for each of the streams). `Rrstream.txt=1000` means that the data in `rstream.txt` will be sent at the rate of `1000 data/second`. Leaving this option blank will assume the stream to use the maximum speed the system could support.
