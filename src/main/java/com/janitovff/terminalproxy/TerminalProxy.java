package com.janitovff.terminalproxy;

import java.io.InputStream;

public class TerminalProxy {
    private BashTerminal bash = new BashTerminal();
    private Server server;

    private void run() throws Exception {
        server = new Server(15100);

        bash.setEnvironmentVariable("TERM", "xterm");
        bash.start();

        InputStream commandStream = server.getInputStream();
        ConnectionHandler handler = new ConnectionHandler(bash, commandStream);

        new Thread(() -> handler.run()).start();

        bash.forwardOutputTo(server.getOutputStream());
        bash.join();
    }

    public static void main(String[] args) throws Exception {
        try {
            new TerminalProxy().run();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
