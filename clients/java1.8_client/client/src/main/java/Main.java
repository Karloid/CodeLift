/**
 * Created by Boris on 01.02.17.
 */

import core.Client;
import core.Strategy;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Main {
    public static void main(String args[]) throws IOException, ParseException, InterruptedException {
        if (args.length > 0) {
            runServer();
            Strategy.printEnabled = true;
        }

        String host = System.getenv("WORLD_NAME");
        if (host == null) {
            host = "127.0.0.1";
        }

        String solutionId = System.getenv("SOLUTION_ID");
        if (solutionId == null) {
            solutionId = "-1";
        }
        Client client = new Client(host, 8000, solutionId);
        client.connect();
    }

    private static void runServer() throws InterruptedException {
        //TODO
        runProc("python", "localrunner/world/run.py");
        Thread.sleep(1000);
        //runProc("python", "clients/python2_client/client/run.py");
        runProc("java", "-jar", "clients/java1.8_client/client/java1.8_client_3859.jar");
        Thread.sleep(1000);
    }

    private static void runProc(String... runProc) {
        new Thread(() -> {
            try {
                Process process = new ProcessBuilder(runProc).start();
                System.out.println("started process: " + Arrays.toString(runProc));

                InputStream inputStream = process.getInputStream();


                readStream(process.getErrorStream(), runProc[0]);
                readStream(inputStream, runProc[0]);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();

    }

    private static void readStream(InputStream inputStream, String s) throws IOException {
        new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    System.out.println(s + ": " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private static void runPythonBaseline() {
        //TODO
    }
}
