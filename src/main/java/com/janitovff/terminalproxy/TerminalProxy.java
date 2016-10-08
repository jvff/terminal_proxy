package com.janitovff.terminalproxy;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jpty.JPty;
import jpty.Pty;
import jpty.WinSize;

public class TerminalProxy {
    private static final int CMD_KEY = 1;
    private static final int CMD_RESIZE = 2;

    private interface UnsafeRunnable {
        void run() throws IOException;
    }

    public static void main(String[] args) throws Exception {
        try {
            safeMain();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void safeMain() throws Exception {
        String[] command = new String[] { "/bin/bash", "-i" };
        String[] environment = buildEnvironmentVariables();

        Pty bash = JPty.execInPTY(command[0], command, environment);
        InputStream bashInputStream = bash.getInputStream();
        OutputStream bashOutputStream = bash.getOutputStream();

        PrintWriter writer = new PrintWriter(bashOutputStream);
        InputStreamReader reader = new InputStreamReader(bashInputStream);

        ServerSocket listener = new ServerSocket(15100);
        Socket socket = listener.accept();

        handleReceivedData(bash, socket.getInputStream(), bashOutputStream);
        handleConsoleData(bashInputStream, socket.getOutputStream());

        bash.waitFor();
    }

    private static String[] buildEnvironmentVariables() {
        Map<String, String> environmentVariables = getEnvironmentVariables();
        int numberOfEnvironmentVariables = environmentVariables.size();
        ArrayList<String> collectedVariables =
                new ArrayList<String>(numberOfEnvironmentVariables);
        String[] resultingArray = new String[numberOfEnvironmentVariables];

        for (String variable : environmentVariables.keySet()) {
            String value = environmentVariables.get(variable);

            collectedVariables.add(variable + "=" + value);
        }

        return collectedVariables.toArray(resultingArray);
    }

    private static Map<String, String> getEnvironmentVariables() {
        Map<String, String> allVariables = new LinkedHashMap<>();
        Map<String, String> currentVariables = System.getenv();

        allVariables.putAll(currentVariables);

        addCustomEnvironmentVariables(allVariables);

        return allVariables;
    }

    private static void addCustomEnvironmentVariables(
            Map<String, String> environmentVariables) {
        environmentVariables.put("TERM", "xterm");
    }

    private static void handleReceivedData(Pty terminal, InputStream dataIn,
            OutputStream consoleOut) {
        handleData(() -> handleReceivedCommands(terminal, dataIn, consoleOut));
    }

    private static void handleReceivedCommands(Pty terminal,
            InputStream commandIn, OutputStream consoleOut)
            throws IOException {
        int command = commandIn.read();

        while (command >= 0) {
            switch (command) {
                case CMD_KEY:
                    forwardByte(commandIn, consoleOut);
                    break;
                case CMD_RESIZE:
                    resizeTerminal(commandIn, terminal);
                    break;
            }

            command = commandIn.read();
        }
    }

    private static void forwardByte(InputStream in, OutputStream out)
            throws IOException {
        int data = in.read();

        if (data >= 0) {
            out.write(data);
            out.flush();
        }
    }

    private static void resizeTerminal(InputStream in, Pty terminal)
            throws IOException {
        short columns = readShortIntFrom(in);
        short rows = readShortIntFrom(in);

        terminal.setWinSize(new WinSize(columns, rows));
    }

    private static short readShortIntFrom(InputStream in) throws IOException {
        short value = 0;
        int data = in.read();

        if (data < 0)
            throw new IOException("Expected a short value");

        value |= data;
        value <<= 8;

        data = in.read();

        if (data < 0)
            throw new IOException("Incomplete short value");

        value |= data;

        return value;
    }

    private static void handleConsoleData(InputStream consoleIn,
            OutputStream dataOut) {
        handleData(() -> forwardData(consoleIn, dataOut));
    }

    private static void handleData(UnsafeRunnable handler) {
        new Thread(() -> safelyHandleData(handler)).start();
    }

    private static void safelyHandleData(UnsafeRunnable handler) {
        try {
            handler.run();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void forwardData(InputStream in, OutputStream out)
            throws IOException {
        int data = in.read();

        while (data >= 0) {
            out.write(data);
            out.flush();
            data = in.read();
        }
    }
}
