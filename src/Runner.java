import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Runner {
    static class TestResult {
        String testName;
        double rawScore;
        long execTime;
        String timestamp;
    }

    private static final boolean AHC_MODE = false;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Runner <solutionFilename>");
            return;
        }
        String solutionName = args[0];
        File solutionFile = new File("solutions", solutionName);
        if (!solutionFile.exists()) {
            System.err.println("Solution file not found: " + solutionFile.getAbsolutePath());
            return;
        }
        File testsDir = new File("tests");
        File[] testFiles = testsDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (testFiles == null || testFiles.length == 0) {
            System.err.println("No test files found in " + testsDir.getAbsolutePath());
            return;
        }
        Arrays.sort(testFiles, Comparator.comparing(File::getName));
        List<TestResult> results = new ArrayList<>();
        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        File csvFile = new File(resultsDir, solutionName + ".csv");
        for (File testFile : testFiles) {
            String testName = testFile.getName();
            TestResult tr = new TestResult();
            tr.testName = testName;
            tr.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            try {
                if (AHC_MODE) {
                    int testNumber = Integer.parseInt(testName.replaceAll("[^0-9]", ""));
                    ProcessBuilder pb = new ProcessBuilder("cargo", "run", "-r", "-q", "--bin",
                        "vis", "in/" + String.format("%04d.txt", testNumber), "../running.txt");
                    pb.directory(new File("tools"));

                    ProcessBuilder solutionPb = new ProcessBuilder(solutionFile.getAbsolutePath());
                    solutionPb.redirectInput(testFile);
                    solutionPb.redirectOutput(new File("running.txt"));

                    long startTime = System.currentTimeMillis();
                    Process solutionProcess = solutionPb.start();

                    BufferedReader solutionErrReader =
                        new BufferedReader(new InputStreamReader(solutionProcess.getErrorStream()));
                    String timeLine = null;
                    String line;
                    while ((line = solutionErrReader.readLine()) != null) {
                        if (line.trim().startsWith("Time =")) {
                            timeLine = line.trim();
                        }
                    }

                    solutionProcess.waitFor();

                    Process visProcess = pb.start();
                    BufferedReader outReader =
                        new BufferedReader(new InputStreamReader(visProcess.getInputStream()));

                    String scoreLine = null;
                    while ((line = outReader.readLine()) != null) {
                        if (line.trim().startsWith("Score =")) {
                            scoreLine = line.trim();
                        }
                    }

                    visProcess.waitFor();
                    long endTime = System.currentTimeMillis();
                    long measuredTime = endTime - startTime;

                    tr.execTime = parseTimeOutput(timeLine, measuredTime);

                    if (scoreLine != null) {
                        tr.rawScore = parseScoreOutput(scoreLine);
                    } else {
                        System.err.println("No score output from visualizer on " + testName);
                        tr.rawScore = 0;
                    }
                } else {
                    ProcessBuilder pb = new ProcessBuilder(solutionFile.getAbsolutePath());
                    pb.redirectInput(testFile);
                    long startTime = System.currentTimeMillis();
                    Process process = pb.start();
                    BufferedReader errReader =
                        new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String scoreLine = null;
                    String timeLine = null;
                    String line;
                    while ((line = errReader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("Score =")) {
                            scoreLine = trimmed;
                        } else if (trimmed.startsWith("Time =")) {
                            timeLine = trimmed;
                        }
                    }
                    process.waitFor();
                    long endTime = System.currentTimeMillis();
                    long measuredTime = endTime - startTime;

                    tr.execTime = parseTimeOutput(timeLine, measuredTime);
                    tr.rawScore = parseScoreOutput(scoreLine);

                    if (scoreLine == null) {
                        System.err.println(
                            "No score output from " + solutionName + " on " + testName);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error running " + solutionName + " on " + testName);
                e.printStackTrace();
                tr.rawScore = 0;
                tr.execTime = 0;
            }
            results.add(tr);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                writer.write("testName,rawScore,execTime,timestamp");
                writer.newLine();
                for (TestResult res : results) {
                    writer.write(String.format(
                        "%s,%.2f,%d,%s", res.testName, res.rawScore, res.execTime, res.timestamp));
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            System.out.println(String.format("%s - %s: Score = %.2f, Time = %d ms", solutionName,
                testName, tr.rawScore, tr.execTime));
        }
        System.out.println("Results written to " + csvFile.getAbsolutePath());
    }

    private static long parseTimeOutput(String timeLine, long measuredTime) {
        if (timeLine != null) {
            int eqIndex = timeLine.indexOf("=");
            int msIndex = timeLine.indexOf("ms", eqIndex);
            if (eqIndex != -1 && msIndex != -1) {
                String timeStr = timeLine.substring(eqIndex + 1, msIndex).trim();
                try {
                    return Long.parseLong(timeStr);
                } catch (NumberFormatException e) {
                    return measuredTime;
                }
            }
        }
        return measuredTime;
    }

    private static double parseScoreOutput(String scoreLine) {
        if (scoreLine != null) {
            int eqIndex = scoreLine.indexOf("=");
            if (eqIndex != -1) {
                String numStr = scoreLine.substring(eqIndex + 1).trim();
                try {
                    return Double.parseDouble(numStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
