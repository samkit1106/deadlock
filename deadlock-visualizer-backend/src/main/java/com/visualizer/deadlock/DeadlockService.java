package com.visualizer.deadlock;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DeadlockService {

    public GraphDTO.Response detectDeadlock(GraphDTO.Request rq) {
        int n = rq.nodes;
        List<List<Integer>> a = new ArrayList<>();
        
        for (int i = 0; i < n; i++) {
            a.add(new ArrayList<>());
        }
        
        if (rq.edges != null) {
            for (GraphDTO.Edge e : rq.edges) {
                a.get(e.from).add(e.to);
            }
        }
        
        boolean[] v = new boolean[n];
        boolean[] rs = new boolean[n];
        List<GraphDTO.TraceEvent> t = new ArrayList<>();
        
        t.add(new GraphDTO.TraceEvent("info", "============ STARTING DFS (JAVA BACKEND) ============"));
        boolean hd = false;
        
        for (int i = 0; i < n; i++) {
            if (!v[i]) {
                t.add(new GraphDTO.TraceEvent("info", "Initiating DFS from component root: " + i));
                
                if (d(i, v, rs, a, t, new ArrayList<>())) {
                    hd = true;
                    break;
                }
            }
        }
        
        t.add(new GraphDTO.TraceEvent("info", "============ DFS COMPLETE ============"));
        
        if (!hd) {
            t.add(new GraphDTO.TraceEvent("safeMsg", "Status: SAFE. No deadlocks or cycles detected."));
        } else {
            t.add(new GraphDTO.TraceEvent("deadlockMsg", "Status: DEADLOCK. Cycle found."));
        }
        
        return new GraphDTO.Response(hd, t);
    }

    private boolean d(int u, boolean[] v, boolean[] rs, List<List<Integer>> a, List<GraphDTO.TraceEvent> t, List<Integer> pp) {
        v[u] = true;
        rs[u] = true;
        
        List<Integer> cs = new ArrayList<>(pp);
        cs.add(u);
        
        t.add(new GraphDTO.TraceEvent("visit", "▶ Visiting node " + u).setNode(u).setStack(new ArrayList<>(cs)));
        
        for (int x : a.get(u)) {
            t.add(new GraphDTO.TraceEvent("checkEdge", "Checking edge " + u + " → " + x).setEdge(u, x));
            
            if (!v[x]) {
                if (d(x, v, rs, a, t, cs)) {
                    return true;
                }
            } else if (rs[x]) {
                t.add(new GraphDTO.TraceEvent("deadlock", "💥 CYCLE DETECTED! Node " + x + " is already on the recursion stack.").setNode(x));
                List<Integer> cy = new ArrayList<>(cs);
                cy.add(x);
                t.add(new GraphDTO.TraceEvent("deadlockCycle", "Deadlock cycle path isolated.").setCyclePath(cy.subList(cy.indexOf(x), cy.size())).setEdge(u, x));
                
                return true;
            } else {
                t.add(new GraphDTO.TraceEvent("info", "Node " + x + " was previously visited and is safe. Skipping."));
            }
            
            t.add(new GraphDTO.TraceEvent("uncheckEdge", "").setEdge(u, x));
        }
        
        rs[u] = false;
        t.add(new GraphDTO.TraceEvent("safe", "✔ Node " + u + " fully explored (safe)").setNode(u));
        
        return false;
    }

    public BankersDTO.Response detectBankers(BankersDTO.Request rq) {
        int m = rq.processes;
        int n = rq.resources;
        
        int[][] al = rq.allocation;
        int[][] mr = rq.max;
        int[] av = Arrays.copyOf(rq.available, rq.available.length);
        
        int[][] rqd = new int[m][n];
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                rqd[i][j] = mr[i][j] - al[i][j];
            }
        }
        
        List<BankersDTO.TraceEvent> t = new ArrayList<>();
        t.add(new BankersDTO.TraceEvent("info", "Starting Banker's Algorithm evaluation."));
        t.add(new BankersDTO.TraceEvent("init", "Initialized Data Structures.").setAvailable(av));
        
        boolean[] ar = new boolean[m];
        List<Integer> sq = new ArrayList<>();
        boolean f = false;
        
        while (!f) {
            f = true;
            for (int i = 0; i < m; i++) {
                if (ar[i]) continue;
                
                t.add(new BankersDTO.TraceEvent("check", "Checking Process P" + i).setNode(i));
                boolean tmp = true;
                
                for (int j = 0; j < n; j++) {
                    if (av[j] < rqd[i][j]) {
                        tmp = false;
                        break;
                    }
                }
                
                if (tmp) {
                    t.add(new BankersDTO.TraceEvent("allocate", "Allocating needed resources to P" + i).setNode(i));
                    ar[i] = true;
                    sq.add(i);
                    
                    for (int k = 0; k < n; k++) {
                        av[k] += al[i][k];
                    }
                    
                    f = false;
                    t.add(new BankersDTO.TraceEvent("release", "Process P" + i + " finished and released resources.")
                            .setNode(i).setAvailable(Arrays.copyOf(av, av.length)));
                } else {
                    t.add(new BankersDTO.TraceEvent("wait", "Process P" + i + " must wait. Need > Available.").setNode(i));
                }
            }
        }
        
        boolean sf = true;
        for (int i = 0; i < m; i++) {
            if (!ar[i]) {
                sf = false;
                break;
            }
        }
        
        if (sf) {
            t.add(new BankersDTO.TraceEvent("safeMsg", "System is in a SAFE state. Safe sequence found."));
        } else {
            t.add(new BankersDTO.TraceEvent("deadlockMsg", "System is NOT in safe state. Possible Deadlock."));
        }
        
        return new BankersDTO.Response(sf, sq, t);
    }
}
