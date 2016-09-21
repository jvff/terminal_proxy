import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import jpty.JPty;
import jpty.Pty;

public class TerminalProxy {
    public static void main(String[] args) throws Exception {
        try {
            safeMain();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void safeMain() throws Exception {
        String[] command = new String[] { "/bin/bash", "-i" };
        String[] environment = new String[] { "TERM=xterm" };

        Pty bash = JPty.execInPTY(command[0], command, environment);
        InputStream bashInputStream = bash.getInputStream();
        OutputStream bashOutputStream = bash.getOutputStream();

        PrintWriter writer = new PrintWriter(bashOutputStream);
        InputStreamReader reader = new InputStreamReader(bashInputStream);

        ServerSocket listener = new ServerSocket(15100);
        Socket socket = listener.accept();

        forwardKeys(socket.getInputStream(), bashOutputStream);
        forwardKeys(bashInputStream, socket.getOutputStream());

        bash.waitFor();
    }

    private static void forwardKeys(InputStream in, OutputStream out) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    safelyForwardKeys(in, out);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }).start();
    }

    private static void safelyForwardKeys(InputStream in, OutputStream out)
            throws Exception {
        int data = in.read();

        while (data >= 0) {
            out.write(data);
            out.flush();
            data = in.read();
        }
    }
}
