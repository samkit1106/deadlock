package com.visualizer.deadlock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DeadlockController {
    
    @Autowired
    private DeadlockService ds;

    @PostMapping("/detect")
    public GraphDTO.Response dt(@RequestBody GraphDTO.Request rq) {
        return ds.detectDeadlock(rq);
    }

    @PostMapping("/bankers")
    @CrossOrigin(origins = "*")
    public BankersDTO.Response bk(@RequestBody BankersDTO.Request rq) {
        return ds.detectBankers(rq);
    }
}
