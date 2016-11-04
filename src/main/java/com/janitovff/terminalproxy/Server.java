package com.janitovff.terminalproxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private Socket socket;

    public Server(int port) throws IOException {
        ServerSocket listener = new ServerSocket(port);

        socket = listener.accept();
    }

    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }
}
