package com.group8.communication.integration;

import java.util.Optional;
public interface UserDirectory {
    Optional<UserProfile> findById(String userId);
}
