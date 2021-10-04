package com.company.contoller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class HelloCloudServiceController {

    @Value("${officialGreeting")
    private String officialGreeting;

    @GetMapping("/hello")
    public String helloCloud(){
        return officialGreeting;
    }
}
