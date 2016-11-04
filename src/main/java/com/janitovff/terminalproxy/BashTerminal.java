package com.janitovff.terminalproxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import jpty.JPty;
import jpty.Pty;
import jpty.WinSize;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.apache.commons.io.IOUtils.copyLarge;

public class BashTerminal {
    Map<String, String> environmentVariables;
    Pty tty;
    InputStream ttyInputStream;
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

        ttyInputStream = tty.getInputStream();
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

    public void forwardOutputTo(OutputStream outputStream) {
        forwarder = new Thread(() -> forwarderThreadBody(outputStream));
        forwarder.start();
    }

    private void forwarderThreadBody(OutputStream outputStream) {
        try {
            copyLarge(ttyInputStream, outputStream);
        } catch (IOException cause) {
            System.err.println("Failed to forward terminal output data");
            cause.printStackTrace();
        }
    }
}
