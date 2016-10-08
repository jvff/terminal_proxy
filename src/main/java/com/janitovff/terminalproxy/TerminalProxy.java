package com.janitovff.terminalproxy;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jpty.JPty;
import jpty.Pty;
import jpty.WinSize;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TerminalProxy {
    private static final char CMD_KEY = 'k';
    private static final char CMD_RESIZE = 'r';
    private static final char CMD_UPLOAD = 'u';

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

        Writer bashWriter = new OutputStreamWriter(bashOutputStream, UTF_8);
        Reader bashReader = new InputStreamReader(bashInputStream, UTF_8);

        ServerSocket listener = new ServerSocket(15100);
        Socket socket = listener.accept();

        InputStream socketInputStream = socket.getInputStream();
        OutputStream socketOutputStream = socket.getOutputStream();

        Writer socketWriter = new OutputStreamWriter(socketOutputStream, UTF_8);
        Reader socketReader = new InputStreamReader(socketInputStream, UTF_8);

        handleReceivedData(bash, socketReader, bashWriter);
        handleConsoleData(bashReader, socketWriter);

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

    private static void handleReceivedData(Pty terminal, Reader dataIn,
            Writer consoleOut) {
        handleData(() -> handleReceivedCommands(terminal, dataIn, consoleOut));
    }

    private static void handleReceivedCommands(Pty terminal, Reader commandIn,
            Writer consoleOut) throws IOException {
        int command = commandIn.read();

        while (command >= 0) {
            switch (command) {
                case CMD_KEY:
                    forwardChar(commandIn, consoleOut);
                    break;
                case CMD_RESIZE:
                    resizeTerminal(commandIn, terminal);
                    break;
                case CMD_UPLOAD:
                    uploadFile(commandIn);
            }

            command = commandIn.read();
        }
    }

    private static void forwardChar(Reader in, Writer out) throws IOException {
        int data = in.read();

        if (data >= 0) {
            out.write(data);
            out.flush();
        }
    }

    private static void resizeTerminal(Reader in, Pty terminal)
            throws IOException {
        short columns = (short)Base64Reader.readIntFrom(in);
        short rows = (short)Base64Reader.readIntFrom(in);

        terminal.setWinSize(new WinSize(columns, rows));
    }

    private static void uploadFile(Reader in) throws IOException {
        String path = readStringFrom(in);
        String contents = readStringFrom(in);

        writeToFile(path, contents);
    }

    private static void writeToFile(String path, String contents)
            throws IOException {
        PrintWriter file = new PrintWriter(path);

        file.println(contents);
        file.close();
    }

    private static String readStringFrom(Reader in) throws IOException {
        int numberOfChars = Base64Reader.readIntFrom(in);
        char[] chars = readCharsFrom(in, numberOfChars);

        return new String(chars);
    }

    private static char[] readCharsFrom(Reader in, int numberOfChars)
            throws IOException {
        char[] chars = new char[numberOfChars];
        int charsRead = 0;

        while (charsRead < numberOfChars) {
            int offset = charsRead;
            int length = numberOfChars - charsRead;
            int charsReadInThisIteration = in.read(chars, offset, length);

            if (charsReadInThisIteration < 0)
                throw new IOException("Failed to read a String");

            charsRead += charsReadInThisIteration;
        }

        return chars;
    }

    private static void handleConsoleData(Reader consoleIn, Writer dataOut) {
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

    private static void forwardData(Reader in, Writer out) throws IOException {
        int data = in.read();

        while (data >= 0) {
            out.write(data);
            out.flush();
            data = in.read();
        }
    }
}
