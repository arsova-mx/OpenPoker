package com.openpoker.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.openpoker.dto.UpdateProfileRequest;
import com.openpoker.entity.User;
import com.openpoker.globalexception.UserNotFoundException;
import com.openpoker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User updateProfile(UUID id, UpdateProfileRequest request) {
        User user = getUserById(id);
        if (request.getCompanyName() != null) {
            user.setCompanyName(request.getCompanyName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        return userRepository.save(user);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con el id: " + id));
    }

    public User getUserByUsername(String username) {
        if (username == null) {
            throw new UserNotFoundException("Usuario no encontrado");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + username));
    }
}
