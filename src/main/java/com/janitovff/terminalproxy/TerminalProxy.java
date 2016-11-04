package com.janitovff.terminalproxy;

import java.io.InputStream;

public class TerminalProxy {
    public static void main(String[] args) throws Exception {
        try {
            safeMain();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void safeMain() throws Exception {
        BashTerminal bash = new BashTerminal();
        Server server = new Server(15100);

        bash.setEnvironmentVariable("TERM", "xterm");
        bash.start();

        InputStream commandStream = server.getInputStream();
        ConnectionHandler handler = new ConnectionHandler(bash, commandStream);

        new Thread(() -> handler.run()).start();

        bash.forwardOutputTo(server.getOutputStream());
        bash.join();
    }
}
