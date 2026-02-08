package com.example.Evaluation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mic3/Evaluation")
public class EvaluationRestApi {
    
    @GetMapping("/Test1")
    public String sayHello() {
        return "Hello I'm Microservice 3: " +
                "I work on Evaluation Entities";
    }
}
