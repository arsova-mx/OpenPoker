package com.openpoker.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openpoker.dto.PublicUserResponse;
import com.openpoker.dto.UpdateProfileRequest;
import com.openpoker.dto.UserResponse;
import com.openpoker.entity.User;
import com.openpoker.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Perfil completo del usuario autenticado (incluye email, teléfono y empresa). */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal String username) {
        return ResponseEntity.ok(new UserResponse(userService.getUserByUsername(username)));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal String username,
            @RequestBody UpdateProfileRequest request) {
        User user = userService.getUserByUsername(username);
        User updatedUser = userService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(new UserResponse(updatedUser));
    }

    /** Perfil público de otro usuario: solo id y username, nunca datos de contacto. */
    @GetMapping("/{id}")
    public ResponseEntity<PublicUserResponse> getPublicProfile(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(new PublicUserResponse(user.getId(), user.getUsername()));
    }
}
