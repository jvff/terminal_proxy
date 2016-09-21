import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
        String[] environment = buildEnvironmentVariables();

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
