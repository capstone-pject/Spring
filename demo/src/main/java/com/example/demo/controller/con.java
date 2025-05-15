package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class con {
    
    @Autowired
    private com.example.demo.Sercive.testSerive testSerive;
@GetMapping("/")

public String getMethodName() {
    System.out.println("Hello from getMethodName");
    testSerive.test();
    return "main";
}




}
