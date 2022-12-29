package com.eudagama12.example.filewriter.controller;

import com.eudagama12.example.filewriter.service.FileProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    FileProcessService fileProcessService;

    @PostMapping(value = "/send/")
    @ResponseStatus(HttpStatus.CREATED)
    public String sendAllCustomerTransactionsReport() {
        fileProcessService.generateReport();
        return "Successfully created report. Please check destination location";
    }
}
