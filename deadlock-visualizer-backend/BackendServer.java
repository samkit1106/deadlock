import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BackendServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/bankers", new BankersHandler());
        server.setExecutor(null);
        System.out.println("Started Pure Java Backend on port 8080...");
        server.start();
    }

    static class BankersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS, GET");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Access-Control-Request-Private-Network");
            exchange.getResponseHeaders().add("Access-Control-Allow-Private-Network", "true");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                try {
                    String jsonResponse = runBankersAlgorithm(body);
                    byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseBytes);
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    String error = "{ \"error\": \"Invalid Input\" }";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, error.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(error.getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // A simple JSON parser to extract values without external dependencies
    private static String runBankersAlgorithm(String json) {
        int processes = extractInt(json, "\"processes\":");
        int resources = extractInt(json, "\"resources\":");

        int[] available = extractIntArray(json, "\"available\":", resources);
        int[][] max = extractIntMatrix(json, "\"max\":", processes, resources);
        int[][] allocation = extractIntMatrix(json, "\"allocation\":", processes, resources);

        int[][] need = new int[processes][resources];
        for (int i = 0; i < processes; i++) {
            for (int j = 0; j < resources; j++) {
                need[i][j] = max[i][j] - allocation[i][j];
            }
        }

        boolean[] finish = new boolean[processes];
        int[] work = Arrays.copyOf(available, available.length);
        List<Integer> safeSequence = new ArrayList<>();
        List<String> traceJson = new ArrayList<>();

        traceJson.add(createTraceEvent("info", "Starting Banker's Algorithm evaluation.", null, null));
        traceJson.add(createTraceEvent("init", "Initialized Data Structures.", null, work));

        int count = 0;
        boolean foundSafeSequence = true;

        while (count < processes) {
            boolean foundProcess = false;
            for (int p = 0; p < processes; p++) {
                if (!finish[p]) {
                    traceJson.add(createTraceEvent("check", "Checking Process P" + p, p, null));

                    boolean canAccommodate = true;
                    for (int j = 0; j < resources; j++) {
                        if (need[p][j] > work[j]) {
                            canAccommodate = false;
                            break;
                        }
                    }

                    if (canAccommodate) {
                        traceJson.add(createTraceEvent("allocate", "Allocating needed resources to P" + p, p, null));
                        for (int j = 0; j < resources; j++) {
                            work[j] += allocation[p][j];
                        }
                        finish[p] = true;
                        safeSequence.add(p);
                        foundProcess = true;
                        count++;
                        traceJson.add(createTraceEvent("release", "Process P" + p + " finished and released resources.", p, Arrays.copyOf(work, work.length)));
                        break; // Start over checking from beginning
                    } else {
                        traceJson.add(createTraceEvent("wait", "Process P" + p + " must wait. Need > Available.", p, null));
                    }
                }
            }
            if (!foundProcess) {
                foundSafeSequence = false;
                break;
            }
        }

        if (foundSafeSequence) {
            traceJson.add(createTraceEvent("safeMsg", "System is in a SAFE state. Safe sequence found.", null, null));
        } else {
            traceJson.add(createTraceEvent("deadlockMsg", "System is in an UNSAFE state. Possible Deadlock.", null, null));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"isSafe\": ").append(foundSafeSequence).append(",")
          .append("\"safeSequence\": ").append(safeSequence).append(",")
          .append("\"trace\": [").append(String.join(",", traceJson)).append("]")
          .append("}");

        return sb.toString();
    }

    private static int extractInt(String source, String key) {
        int idx = source.indexOf(key);
        if (idx == -1) return 0;
        int start = source.indexOf(":", idx) + 1;
        while (start < source.length() && Character.isWhitespace(source.charAt(start))) start++;
        int end = start;
        while (end < source.length() && Character.isDigit(source.charAt(end))) end++;
        if (start == end) return 0;
        return Integer.parseInt(source.substring(start, end));
    }

    private static int[] extractIntArray(String source, String key, int size) {
        int[] arr = new int[size];
        int idx = source.indexOf(key);
        if (idx == -1) return arr;
        int start = source.indexOf("[", idx) + 1;
        int end = source.indexOf("]", start);
        String[] tokens = source.substring(start, end).split(",");
        for (int i = 0; i < Math.min(size, tokens.length); i++) {
            arr[i] = Integer.parseInt(tokens[i].trim());
        }
        return arr;
    }

    private static int[][] extractIntMatrix(String source, String key, int rows, int cols) {
        int[][] matrix = new int[rows][cols];
        int idx = source.indexOf(key);
        if (idx == -1) return matrix;
        idx = source.indexOf("[", idx) + 1; // main array open

        for (int r = 0; r < rows; r++) {
            int rowStart = source.indexOf("[", idx) + 1;
            int rowEnd = source.indexOf("]", rowStart);
            if (rowStart == 0 || rowEnd == -1) break;
            
            String[] tokens = source.substring(rowStart, rowEnd).split(",");
            for (int c = 0; c < Math.min(cols, tokens.length); c++) {
                matrix[r][c] = Integer.parseInt(tokens[c].trim());
            }
            idx = rowEnd + 1;
        }
        return matrix;
    }

    private static String createTraceEvent(String type, String message, Integer node, int[] availableUpdated) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"type\":\"").append(type).append("\"");
        if (message != null) sb.append(",\"message\":\"").append(message.replace("\"", "\\\"")).append("\"");
        if (node != null) sb.append(",\"node\":").append(node);
        if (availableUpdated != null) {
            sb.append(",\"available\":[");
            for (int i = 0; i < availableUpdated.length; i++) {
                sb.append(availableUpdated[i]);
                if (i < availableUpdated.length - 1) sb.append(",");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }
}
