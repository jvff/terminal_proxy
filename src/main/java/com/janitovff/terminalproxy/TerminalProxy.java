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
import java.util.Map;

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
        BashTerminal bash = new BashTerminal();

        bash.setEnvironmentVariable("TERM", "xterm");
        bash.start();

        Server server = new Server(15100);

        InputStream socketInputStream = server.getInputStream();
        Reader socketReader = new InputStreamReader(socketInputStream, UTF_8);

        handleReceivedData(bash, socketReader);

        bash.forwardOutputTo(server.getOutputStream());
        bash.join();
    }

    private static void handleReceivedData(BashTerminal bash, Reader dataIn) {
        handleData(() -> handleReceivedCommands(bash, dataIn));
    }

    private static void handleReceivedCommands(BashTerminal bash,
            Reader commandIn) throws IOException {
        int command = commandIn.read();

        while (command >= 0) {
            switch (command) {
                case CMD_KEY:
                    forwardChar(commandIn, bash);
                    break;
                case CMD_RESIZE:
                    resizeTerminal(commandIn, bash);
                    break;
                case CMD_UPLOAD:
                    uploadFile(commandIn);
            }

            command = commandIn.read();
        }
    }

    private static void forwardChar(Reader in, BashTerminal bash)
            throws IOException {
        int data = in.read();

        if (data >= 0)
            bash.sendKey((char)data);
    }

    private static void resizeTerminal(Reader in, BashTerminal bash)
            throws IOException {
        short columns = (short)Base64Reader.readIntFrom(in);
        short rows = (short)Base64Reader.readIntFrom(in);

        bash.resize(columns, rows);
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
}
