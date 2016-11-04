package com.janitovff.terminalproxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.janitovff.terminalproxy.Server.ConnectionHandler;

public class TerminalProxy {
    private BashTerminal bash = new BashTerminal();
    private Server server = new Server(15100);

    private ConnectionHandler connectionHandler = new ConnectionHandler() {
        @Override
        public void newConnection(Socket socket) throws IOException {
            InputStream commandStream = socket.getInputStream();
            OutputStream displayStream = socket.getOutputStream();

            CommandHandler handler = new CommandHandler(bash, commandStream);
            handler.start();

            bash.forwardOutputTo(displayStream);
        }
    };

    private void run() throws Exception {
        bash.setEnvironmentVariable("TERM", "xterm");
        bash.start();

        server.setConnectionHandler(connectionHandler);
        server.start();

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
