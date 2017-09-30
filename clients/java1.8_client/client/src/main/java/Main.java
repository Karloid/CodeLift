/**
 * Created by Boris on 01.02.17.
 */

import core.Client;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class Main {
    public static void main(String args[]) throws IOException, ParseException {
        runServer();
        runPythonBaseline();

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

    private static void runServer() {
        //TODO
        runProc("python", "localrunner/world/run.py");
        runProc("python", "clients/python2_client/client/run.py");
    }

    private static void runProc(String... runProc) {
        try {
            Process process = new ProcessBuilder(runProc).start();
            System.out.println("started process: " + runProc.toString());
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runPythonBaseline() {
        //TODO
    }
}
