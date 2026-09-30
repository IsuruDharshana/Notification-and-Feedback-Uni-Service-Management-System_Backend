package com.group8.communication.integration;

import java.util.Optional;
public interface UserDirectory {
    /** Whether a Group 5 base URL is configured; without it announcement targeting uses token roles only. */
    boolean isConfigured();

    Optional<UserProfile> findById(String userId);
}
