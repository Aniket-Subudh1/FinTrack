package com.personalfinancetracker.backend.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloControllers {

    @GetMapping("/hello")
    public String hello() {
  return "Hello from authorized Api request!";
 }
}
