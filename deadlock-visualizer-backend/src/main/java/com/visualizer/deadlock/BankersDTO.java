package com.visualizer.deadlock;

import java.util.*;

public class BankersDTO {

    public static class Request {
        public int processes;
        public int resources;
        public int[] available;
        public int[][] max;
        public int[][] allocation;
    }

    public static class TraceEvent {
        public String type;
        public String message;
        public Integer node;
        public int[] available;

        public TraceEvent() {}

        public TraceEvent(String t, String m) {
            type = t;
            message = m;
        }

        public TraceEvent setNode(Integer n) {
            node = n;
            return this;
        }

        public TraceEvent setAvailable(int[] a) {
            available = a;
            return this;
        }
    }

    public static class Response {
        public boolean isSafe;
        public List<Integer> safeSequence;
        public List<TraceEvent> trace;

        public Response(boolean s, List<Integer> seq, List<TraceEvent> tr) {
            isSafe = s;
            safeSequence = seq;
            trace = tr;
        }
    }
}
