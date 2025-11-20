package com.github.yun531.climate.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class SwaggerTestController {

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from Swagger Test Controller");
    }

    @GetMapping("/echo/{value}")
    public ResponseEntity<String> echo(@PathVariable String value) {
        return ResponseEntity.ok("Echo value: " + value);
    }

    @GetMapping("/sum")
    public ResponseEntity<Integer> sum(
            @RequestParam int a,
            @RequestParam int b) {

        int result = a + b;
        return ResponseEntity.ok(result);
    }

    @PostMapping("/user")
    @Operation(summary = "요약", description = "설명설멸설명.")
    public ResponseEntity<UserRequest> createUser(@RequestBody UserRequest request) {
        return ResponseEntity.ok(request);
    }


    public static class UserRequest {
        private String name;
        private int age;
    }


}