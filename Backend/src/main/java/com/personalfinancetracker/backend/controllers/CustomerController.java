package com.personalfinancetracker.backend.controllers;


import com.personalfinancetracker.backend.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/names")
    public List<String> getAllCustomerNames() {
        return customerService.getAllCustomerNames();
    }
}
