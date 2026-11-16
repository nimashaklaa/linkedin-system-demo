package com.linkedin.userservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserProfileRequest {
    private String firstName;
    private String lastName;
    private String headline;
    private String location;
    private String about;
    private String profilePhotoUrl;
    private String coverPhotoUrl;
    private List<String> skills;
}
