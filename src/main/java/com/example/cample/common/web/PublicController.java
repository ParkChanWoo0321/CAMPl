// src/main/java/com/example/cample/common/web/PublicController.java
package com.example.cample.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {
    @GetMapping("/oauth/signed-in")
    public String signedIn() {
        return "Signed in";
    }
}
