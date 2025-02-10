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
                ProcessBuilder pb = new ProcessBuilder(solutionFile.getAbsolutePath());
                pb.redirectInput(testFile);
                long startTime = System.currentTimeMillis();
                Process process = pb.start();
                BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
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
                tr.execTime = measuredTime;
                if (timeLine != null) {
                    int eqIndex = timeLine.indexOf("=");
                    int msIndex = timeLine.indexOf("ms", eqIndex);
                    if (eqIndex != -1 && msIndex != -1) {
                        String timeStr = timeLine.substring(eqIndex + 1, msIndex).trim();
                        try {
                            tr.execTime = Long.parseLong(timeStr);
                        } catch (NumberFormatException e) {
                            tr.execTime = measuredTime;
                        }
                    }
                }
                if (scoreLine != null) {
                    int eqIndex = scoreLine.indexOf("=");
                    if (eqIndex != -1) {
                        String numStr = scoreLine.substring(eqIndex + 1).trim();
                        tr.rawScore = Double.parseDouble(numStr);
                    } else {
                        tr.rawScore = 0;
                    }
                } else {
                    System.err.println("No score output from " + solutionName + " on " + testName);
                    tr.rawScore = 0;
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
                    writer.write(String.format("%s,%.2f,%d,%s", res.testName, res.rawScore, res.execTime, res.timestamp));
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            System.out.println(String.format("%s - %s: Score = %.2f, Time = %d ms", solutionName, testName, tr.rawScore, tr.execTime));
        }
        System.out.println("Results written to " + csvFile.getAbsolutePath());
    }
}
