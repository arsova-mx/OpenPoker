package com.openpoker.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openpoker.dto.UpdateProfileRequest;
import com.openpoker.entity.User;
import com.openpoker.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PatchMapping("/{id}/profile")
    public ResponseEntity<User> UpdateProfile(@PathVariable UUID id,@RequestBody UpdateProfileRequest request){

        User updateUser = userService.updateProfile(id, request);

        return ResponseEntity.ok(updateUser);
        

    }

}
