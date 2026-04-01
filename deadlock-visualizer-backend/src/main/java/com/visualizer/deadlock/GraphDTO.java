package com.visualizer.deadlock;

import java.util.List;

public class GraphDTO {

    public static class Edge {
        public int from;
        public int to;
    }

    public static class Request {
        public int nodes;
        public List<Edge> edges;
    }

    public static class TraceEvent {
        public String type;
        public String message;
        public Integer node;
        public Integer from;
        public Integer to;
        public List<Integer> stack;
        public List<Integer> cyclePath;

        public TraceEvent() {}

        public TraceEvent(String t, String m) {
            type = t;
            message = m;
        }

        public TraceEvent setNode(Integer n) {
            node = n;
            return this;
        }

        public TraceEvent setEdge(Integer f, Integer t) {
            from = f;
            this.to = t;
            return this;
        }

        public TraceEvent setStack(List<Integer> s) {
            stack = s;
            return this;
        }

        public TraceEvent setCyclePath(List<Integer> cp) {
            cyclePath = cp;
            return this;
        }
    }

    public static class Response {
        public boolean hasDeadlock;
        public List<TraceEvent> trace;

        public Response(boolean hd, List<TraceEvent> tr) {
            hasDeadlock = hd;
            trace = tr;
        }
    }
}
