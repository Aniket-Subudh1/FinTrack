package com.personalfinance.user.dto;

import com.personalfinance.user.entity.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsRequest {

    private String address;

    private Gender gender;

    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 150, message = "Age must be less than 150")
    private Integer age;

    private String profilePhoto;
}