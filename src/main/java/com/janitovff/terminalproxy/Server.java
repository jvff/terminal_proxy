package com.janitovff.terminalproxy;

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends AbstractService {
    public interface ConnectionHandler {
        void newConnection(Socket socket) throws IOException;
    }

    private ConnectionHandler handler;
    private Thread thread;

    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void setConnectionHandler(ConnectionHandler connectionHandler) {
        handler = connectionHandler;
    }

    protected void run() {
        while (!shouldStop())
            listenForConnections();
    }

    private void listenForConnections() {
        ServerSocket listener = null;

        try {
            listener = new ServerSocket(port);

            handleConnections(listener);
        } catch (IOException cause) {
            System.err.println("Failed to listen for connections");
            cause.printStackTrace();
        } finally {
            safelyClose(listener);
        }
    }

    private void handleConnections(ServerSocket listener) {
        try {
            while (true)
                notifyNewConnection(listener.accept());
        } catch (IOException cause) {
            System.err.println("Failed to accept a connection");
            cause.printStackTrace();
        }
    }

    private void notifyNewConnection(Socket socket) {
        try {
            if (handler != null)
                handler.newConnection(socket);
        } catch (IOException cause) {
            System.err.println("WARN: Failure to handle new connection");
            cause.printStackTrace();
        }
    }

    private void safelyClose(Closeable object) {
        if (object != null) {
            try {
                object.close();
            } catch (IOException exception) {
                System.err.println("WARN: Exception while closing");
                exception.printStackTrace();
            }
        }
    }
}
