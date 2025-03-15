package com.personalfinance.user.service;

import com.personalfinance.user.dto.UserDetailsRequest;
import com.personalfinance.user.dto.UserDetailsResponse;
import com.personalfinance.user.entity.User;
import com.personalfinance.user.exception.UserException;
import com.personalfinance.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public UserDetailsResponse getUserDetails(String email) {
        User user = findUserByEmail(email);
        return mapToUserDetailsResponse(user);
    }


    @Transactional
    public UserDetailsResponse updateUserDetails(String email, UserDetailsRequest request) {
        User user = findUserByEmail(email);

        // Update user fields
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }

        if (request.getProfilePhoto() != null && !request.getProfilePhoto().isEmpty()) {
            try {
                byte[] photoBytes = Base64.getDecoder().decode(request.getProfilePhoto());
                user.setProfilePhoto(photoBytes);
            } catch (IllegalArgumentException e) {
                throw new UserException("Invalid profile photo format. Please provide a valid Base64 encoded image.");
            }
        }

        User updatedUser = userRepository.save(user);
        return mapToUserDetailsResponse(updatedUser);
    }


    @Transactional
    public void createUser(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            return; // User already exists, no need to create
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .build();

        userRepository.save(user);
    }


    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("User not found with email: " + email));
    }


    private UserDetailsResponse mapToUserDetailsResponse(User user) {
        String profilePhotoBase64 = null;
        if (user.getProfilePhoto() != null) {
            profilePhotoBase64 = Base64.getEncoder().encodeToString(user.getProfilePhoto());
        }

        return UserDetailsResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .address(user.getAddress())
                .gender(user.getGender())
                .age(user.getAge())
                .profilePhoto(profilePhotoBase64)
                .build();
    }
}