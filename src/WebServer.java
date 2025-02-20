import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new ResultsServlet()), "/");
        server.start();
        System.out.println("Server started at http://localhost:8080");
        server.join();
    }

    public static class ResultsServlet extends HttpServlet {
        private static final String RESULTS_DIR = "results";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
            String mode = req.getParameter("mode");
            if (mode == null || (!mode.equals("min") && !mode.equals("max"))) {
                mode = "max";
            }
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            File resultsDir = new File(RESULTS_DIR);
            if (!resultsDir.exists() || !resultsDir.isDirectory()) {
                out.println("<html><head><title>Results Dashboard</title></head><body><h3>No "
                    + "results directory found.</h3></body></html>");
                return;
            }
            File[] solutionFiles = resultsDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (solutionFiles == null || solutionFiles.length == 0) {
                out.println(
                    "<html><head><title>Results Dashboard</title></head><body><h3>No solution "
                    + "result files found. Please run a solution.</h3></body></html>");
                return;
            }
            class TestRecord {
                double rawScore;
                long execTime;
                String timestamp;
            }
            Map<String, Map<String, TestRecord>> solutionData = new HashMap<>();
            Set<String> allTestNames = new TreeSet<>();
            for (File file : solutionFiles) {
                String fileName = file.getName();
                String solutionName = fileName.substring(0, fileName.length() - 4);
                Map<String, TestRecord> testMap = new HashMap<>();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String header = br.readLine();
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length < 4) {
                            continue;
                        }
                        String testName = parts[0];
                        double rawScore = 0;
                        long execTime = 0;
                        try {
                            rawScore = Double.parseDouble(parts[1]);
                            execTime = Long.parseLong(parts[2]);
                        } catch (NumberFormatException e) {
                        }
                        String timestamp = parts[3];
                        TestRecord tr = new TestRecord();
                        tr.rawScore = rawScore;
                        tr.execTime = execTime;
                        tr.timestamp = timestamp;
                        testMap.put(testName, tr);
                        allTestNames.add(testName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                solutionData.put(solutionName, testMap);
            }
            Map<String, Double> bestScores = new HashMap<>();
            for (String testName : allTestNames) {
                if (mode.equals("max")) {
                    double best = 0;
                    for (Map<String, TestRecord> testMap : solutionData.values()) {
                        TestRecord rec = testMap.get(testName);
                        if (rec != null && rec.rawScore > best) {
                            best = rec.rawScore;
                        }
                    }
                    bestScores.put(testName, best);
                } else {
                    double best = Double.POSITIVE_INFINITY;
                    for (Map<String, TestRecord> testMap : solutionData.values()) {
                        TestRecord rec = testMap.get(testName);
                        if (rec != null && rec.rawScore < best) {
                            best = rec.rawScore;
                        }
                    }
                    if (best == Double.POSITIVE_INFINITY) {
                        best = 0;
                    }
                    bestScores.put(testName, best);
                }
            }
            Map<String, Double> solutionAverages = new HashMap<>();
            Map<String, String> solutionTimestamps = new HashMap<>();
            Map<String, Map<String, Double>> solutionNormalized = new HashMap<>();
            Map<String, Integer> solutionTestCounts = new HashMap<>();
            Map<String, Long> solutionAvgTimes = new HashMap<>();
            Map<String, Long> solutionMaxTimes = new HashMap<>();
            Map<String, Integer> solutionBestCounts = new HashMap<>();
            Map<String, Integer> solutionUniqueBestCounts = new HashMap<>();
            for (Map.Entry<String, Map<String, TestRecord>> entry : solutionData.entrySet()) {
                String sol = entry.getKey();
                Map<String, TestRecord> testMap = entry.getValue();
                double sumNorm = 0;
                int count = 0;
                long totalTime = 0;
                long maxTime = 0;
                int bestCount = 0;
                int uniqueBestCount = 0;
                Map<String, Double> normMap = new HashMap<>();
                String latestTimestamp = "";
                for (String test : allTestNames) {
                    TestRecord rec = testMap.get(test);
                    double norm;
                    if (rec != null) {
                        double best = bestScores.getOrDefault(test, 0.0);
                        if (mode.equals("max")) {
                            norm = (best > 0) ? (rec.rawScore / best) * 100 : 0;
                        } else {
                            if (rec.rawScore == 0) {
                                norm = 100.0;
                            } else {
                                norm = (best > 0) ? (best / rec.rawScore) * 100 : 0;
                            }
                        }
                        if (rec.timestamp.compareTo(latestTimestamp) > 0) {
                            latestTimestamp = rec.timestamp;
                        }
                        totalTime += rec.execTime;
                        if (rec.execTime > maxTime) {
                            maxTime = rec.execTime;
                        }
                        sumNorm += norm;
                        count++;
                        if (Math.abs(norm - 100.0) < 1e-6) {
                            bestCount++;
                            boolean isUnique = true;
                            for (Map.Entry<String, Map<String, TestRecord>> otherEntry :
                                solutionData.entrySet()) {
                                if (otherEntry.getKey().equals(sol))
                                    continue;
                                TestRecord otherRec = otherEntry.getValue().get(test);
                                if (otherRec != null) {
                                    double otherNorm;
                                    if (mode.equals("max")) {
                                        otherNorm =
                                            (best > 0) ? (otherRec.rawScore / best) * 100 : 0;
                                    } else {
                                        if (otherRec.rawScore == 0) {
                                            otherNorm = 100.0;
                                        } else {
                                            otherNorm =
                                                (best > 0) ? (best / otherRec.rawScore) * 100 : 0;
                                        }
                                    }
                                    if (Math.abs(otherNorm - 100.0) < 1e-6) {
                                        isUnique = false;
                                        break;
                                    }
                                }
                            }
                            if (isUnique) {
                                uniqueBestCount++;
                            }
                        }
                    } else {
                        norm = Double.NaN;
                    }
                    normMap.put(test, norm);
                }
                double avgNorm = (count > 0) ? sumNorm / count : 0;
                solutionAverages.put(sol, avgNorm);
                solutionTimestamps.put(sol, latestTimestamp);
                solutionNormalized.put(sol, normMap);
                solutionTestCounts.put(sol, count);
                long avgTime = (count > 0) ? totalTime / count : 0;
                solutionAvgTimes.put(sol, avgTime);
                solutionMaxTimes.put(sol, maxTime);
                solutionBestCounts.put(sol, bestCount);
                solutionUniqueBestCounts.put(sol, uniqueBestCount);
            }
            List<String> sortedSolutions = new ArrayList<>(solutionData.keySet());
            Collections.sort(sortedSolutions,
                (s1, s2) -> Double.compare(solutionAverages.get(s2), solutionAverages.get(s1)));
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Results Dashboard</title>");
            out.println(
                "<link href='https://fonts.googleapis.com/css2?family=Roboto+Mono&display=swap' "
                + "rel='stylesheet'>");
            out.println("<link rel='stylesheet' "
                + "href='https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/"
                + "bootstrap.min.css'>");
            out.println("<style>td, th { white-space: nowrap; text-align: center; font-family: "
                + "'Roboto Mono', monospace; }</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='container mt-4'>");
            out.println("<h2>Results Dashboard</h2>");
            out.println("<form method='GET'>");
            out.println("<label>Task Type: </label>");
            out.println("<select name='mode' onchange='this.form.submit();'>");
            out.println("<option value='max'" + (mode.equals("max") ? " selected" : "")
                + ">Maximization</option>");
            out.println("<option value='min'" + (mode.equals("min") ? " selected" : "")
                + ">Minimization</option>");
            out.println("</select>");
            out.println("</form>");
            out.println("<table class='table table-bordered table-striped'>");
            out.println("<thead><tr>");
            out.println("<th>Timestamp</th>");
            out.println("<th>Solution</th>");
            out.println("<th class='num'>Avg Time</th>");
            out.println("<th class='num'>Max Time</th>");
            out.println("<th class='num'>Score</th>");
            out.println("<th class='num'>Tests</th>");
            out.println("<th class='num'>Best</th>");
            out.println("<th class='num'>Uniq</th>");
            for (String test : allTestNames) {
                String displayTest = test;
                if (displayTest.endsWith(".txt")) {
                    displayTest = displayTest.substring(0, displayTest.length() - 4);
                }
                out.println("<th class='num'>" + displayTest + "</th>");
            }
            out.println("</tr></thead>");
            out.println("<tbody>");
            for (String sol : sortedSolutions) {
                out.println("<tr>");
                out.println("<td>" + solutionTimestamps.get(sol) + "</td>");
                out.println("<td>" + sol + "</td>");
                long avgTime = solutionAvgTimes.get(sol);
                long maxTime = solutionMaxTimes.get(sol);
                out.println("<td class='num'>" + avgTime + " ms"
                    + "</td>");
                out.println("<td class='num'>" + maxTime + " ms"
                    + "</td>");
                double avg = solutionAverages.get(sol);
                String avgDisplay = String.format("%.4f", avg);
                out.println("<td class='num'>" + avgDisplay + "</td>");
                int testsCount = solutionTestCounts.get(sol);
                out.println("<td class='num'>" + testsCount + "</td>");
                int bestCount = solutionBestCounts.get(sol);
                out.println("<td class='num'>" + bestCount + "</td>");
                int uniqueBestCount = solutionUniqueBestCounts.get(sol);
                out.println("<td class='num'>" + uniqueBestCount + "</td>");
                Map<String, Double> normMap = solutionNormalized.get(sol);
                for (String test : allTestNames) {
                    double norm = normMap.getOrDefault(test, Double.NaN);
                    TestRecord rec = solutionData.get(sol).get(test);
                    String tooltip = "";
                    if (rec != null) {
                        tooltip = " title='Score: " + String.format("%.2f", rec.rawScore)
                            + "\nTime: " + rec.execTime + " ms'";
                    }
                    if (Double.isNaN(norm)) {
                        out.println("<td class='num'" + tooltip + ">-</td>");
                    } else {
                        String normDisplay = String.format("%.4f", norm);
                        String cellStyle = "";
                        if (Math.abs(norm - 100.0) < 1e-6) {
                            cellStyle = " style='background-color:#90EE90;'";
                        } else if (norm < 95.0) {
                            cellStyle = " style='background-color:#FFB6B6;'";
                        }
                        out.println(
                            "<td class='num'" + cellStyle + tooltip + ">" + normDisplay + "</td>");
                    }
                }
                out.println("</tr>");
            }
            out.println("</tbody>");
            out.println("</table>");
            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
