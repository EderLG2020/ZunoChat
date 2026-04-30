package com.example.backend.module.usermanagement.application;

import com.example.backend.common.service.JwtService;
import com.example.backend.module.usermanagement.domain.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public String login(@RequestBody UserModel user) {
        return jwtService.generateToken(user);
    }
}