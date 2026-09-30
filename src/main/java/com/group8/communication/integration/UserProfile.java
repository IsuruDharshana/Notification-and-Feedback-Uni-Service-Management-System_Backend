package com.group8.communication.integration;

import java.util.List;

/** A Group 5 user as announcement targeting needs it: every role held plus the department/faculty affiliation. */
public record UserProfile(String id, List<String> roles, String department, String faculty, String serviceUnit) {
    public UserProfile {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
