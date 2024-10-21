package com.personalfinancetracker.backend.services;



import com.personalfinancetracker.backend.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public List<String> getAllCustomerNames() {
        return customerRepository.findAllCustomerNames();
    }
}
