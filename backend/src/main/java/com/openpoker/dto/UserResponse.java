package com.openpoker.dto;

import java.util.UUID;

import com.openpoker.entity.User;
import com.openpoker.entity.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserResponse {

    private UUID id;

    private String username;

    private String email;

    private UserRole role;

    private String companyName;
    
    private String phoneNumber;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.companyName = user.getCompanyName();
        this.phoneNumber = user.getPhoneNumber();
    }

}
