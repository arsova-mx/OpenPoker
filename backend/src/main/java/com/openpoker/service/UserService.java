package com.openpoker.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openpoker.dto.UpdateProfileRequest;
import com.openpoker.entity.User;
import com.openpoker.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User updateProfile(UUID id,UpdateProfileRequest request){
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el ID: " + id));
            if(request.getCompanyName() != null){
                user.setCompanyName(request.getCompanyName());
            }
            if(request.getPhoneNumber() != null){
                user.setPhoneNumber(request.getPhoneNumber());
            }

            return userRepository.save(user);
    }

        public User getUserById(UUID id){
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("usuario no encontrado con el id: "+ id));

        return user;
    }
    

}
