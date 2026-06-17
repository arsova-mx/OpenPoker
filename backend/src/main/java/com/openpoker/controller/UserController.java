package com.openpoker.controller;

import java.util.Optional;
import java.util.UUID;

import javax.management.RuntimeErrorException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openpoker.dto.UpdateProfileRequest;
import com.openpoker.dto.UserResponse;
import com.openpoker.entity.User;
import com.openpoker.repository.UserRepository;
import com.openpoker.security.JwtService;
import com.openpoker.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;


    @PatchMapping("/profile")
    public ResponseEntity<User> UpdateProfile(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody UpdateProfileRequest request){
        
// 2. Quitamos la palabra "Bearer " para quedarnos solo con el texto puro del JWT
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Falta el token de autorización o no es de tipo Bearer.");
        }
        String token = bearerToken.substring(7);

        // 3. Ahora sí, pasamos el String limpio que no estará vacío
        UUID userId = jwtService.extractUserId(token);

        // 4. Mandamos el ID al servicio
        User updateUser = userService.updateProfile(userId, request);

        return ResponseEntity.ok(updateUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getExternalUserProfile(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String tokenHeader) {

        // 1. Buscas al usuario en la BD (te devuelve el objeto completo con password y fechas)
        User user = userService.getUserById(id);

        // 2. Lo pasamos por el DTO para filtrar los campos sensibles automáticamente
        UserResponse response = new UserResponse(user);

        // 3. Respondemos al cliente de forma segura
        return ResponseEntity.ok(response);
    }



    

}
