package com.personalfinance.user.dto;

import com.personalfinance.user.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsResponse {

    private Long id;

    private String name;

    private String email;

    private String address;

    private Gender gender;

    private Integer age;

    private String profilePhoto;
}
