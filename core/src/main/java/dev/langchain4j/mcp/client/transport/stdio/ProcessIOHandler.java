package dev.langchain4j.mcp.client.transport.stdio;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.client.transport.McpOperationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.function.Consumer;

class ProcessIOHandler implements Runnable {

    private final Process process;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ProcessIOHandler.class);
    private static final Logger trafficLog = LoggerFactory.getLogger("MCP");
    private final boolean logEvents;
    private final McpOperationHandler messageHandler;
    private final PrintStream out;
    private final Consumer<String> trafficConsumer;

    public ProcessIOHandler(Process process, McpOperationHandler messageHandler, boolean logEvents,
                            Consumer<String> trafficConsumer) {
        this.process = process;
        this.logEvents = logEvents;
        this.messageHandler = messageHandler;
        this.out = new PrintStream(process.getOutputStream(), true);
        this.trafficConsumer = trafficConsumer;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (logEvents) {
                    trafficLog.debug("< {}", line);
                    if (trafficConsumer != null) {
                        trafficConsumer.accept("< " + line);
                    }
                }
                messageHandler.handle(OBJECT_MAPPER.readTree(line));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("ProcessIOHandler has finished reading output from process with PID = {}", process.pid());
    }

    public void submit(String message) throws IOException {
        if (logEvents) {
            trafficLog.debug("> {}", message);
            if (trafficConsumer != null) {
                trafficConsumer.accept("> " + message);
            }
        }
        out.println(message);
    }
}
