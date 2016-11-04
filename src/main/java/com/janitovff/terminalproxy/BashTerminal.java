package com.janitovff.terminalproxy;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import jpty.JPty;
import jpty.Pty;
import jpty.WinSize;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BashTerminal {
    Map<String, String> environmentVariables;
    Pty tty;
    Reader ttyReader;
    Writer ttyWriter;

    Thread forwarder;

    public BashTerminal() {
        environmentVariables = new LinkedHashMap<String, String>();

        getEnvironmentVariables();
    }

    private void getEnvironmentVariables() {
        Map<String, String> currentVariables = System.getenv();

        environmentVariables.putAll(currentVariables);
    }

    public void setEnvironmentVariable(String name, String value) {
        environmentVariables.put(name, value);
    }

    public void start() {
        String[] command = new String[] { "/bin/bash", "-i" };
        String[] environment = collectEnvironmentVariables();

        tty = JPty.execInPTY(command[0], command, environment);

        ttyReader = new InputStreamReader(tty.getInputStream(), UTF_8);
        ttyWriter = new OutputStreamWriter(tty.getOutputStream(), UTF_8);
    }

    private String[] collectEnvironmentVariables() {
        int numberOfVariables = environmentVariables.size();
        String[] collectedVariables = new String[numberOfVariables];
        int currentIndex = 0;

        for (String variable : environmentVariables.keySet()) {
            String value = environmentVariables.get(variable);

            collectedVariables[currentIndex] = variable + "=" + value;

            ++currentIndex;
        }

        return collectedVariables;
    }

    public void join() throws InterruptedException {
        tty.waitFor();
    }

    public void sendKey(char key) throws IOException {
        ttyWriter.write(key);
        ttyWriter.flush();
    }

    public void resize(short columns, short rows) throws IOException {
        tty.setWinSize(new WinSize(columns, rows));
    }

    public void forwardOutputTo(Writer sink) {
        forwarder = new Thread(() -> forwarderThreadBody(sink));
        forwarder.start();
    }

    private void forwarderThreadBody(Writer sink) {
        try {
            forwardData(sink);
        } catch (IOException cause) {
            System.err.println("Failed to forward terminal output data");
            cause.printStackTrace();
        }
    }

    private void forwardData(Writer sink) throws IOException {
        int data = ttyReader.read();

        while (data >= 0) {
            sink.write(data);
            sink.flush();
            data = ttyReader.read();
        }
    }
}
