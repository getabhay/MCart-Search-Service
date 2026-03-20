package com.nova.mcart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    @GetMapping("favicon.ico")
    @ResponseBody
    void returnNoFavicon() {
        // Silently handle the browser's automatic request
    }
}
