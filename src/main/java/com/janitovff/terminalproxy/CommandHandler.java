package com.janitovff.terminalproxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class CommandHandler {
    private static final char CMD_KEY = 'k';
    private static final char CMD_RESIZE = 'r';
    private static final char CMD_UPLOAD = 'u';

    private BashTerminal bash;
    private CommandStream commandStream;

    public CommandHandler(BashTerminal bash, InputStream connectionStream) {
        this.bash = bash;

        commandStream = new CommandStream(connectionStream);
    }

    public void run() {
        try {
            runUnsafely();
        } catch (IOException exception) {
            System.err.println("Failed to receive commands");
            exception.printStackTrace();
        }
    }

    private void runUnsafely() throws IOException {
        while (true) {
            char command = commandStream.readChar();

            switch (command) {
                case CMD_KEY:
                    forwardChar();
                    break;
                case CMD_RESIZE:
                    resizeTerminal();
                    break;
                case CMD_UPLOAD:
                    uploadFile();
            }
        }
    }

    private void forwardChar() throws IOException {
        char key = commandStream.readChar();

        bash.sendKey(key);
    }

    private void resizeTerminal() throws IOException {
        short columns = commandStream.readShort();
        short rows = commandStream.readShort();

        bash.resize(columns, rows);
    }

    private void uploadFile() throws IOException {
        String path = commandStream.readString();
        String contents = commandStream.readString();

        PrintWriter file = new PrintWriter(path);

        file.println(contents);
        file.close();
    }
}
