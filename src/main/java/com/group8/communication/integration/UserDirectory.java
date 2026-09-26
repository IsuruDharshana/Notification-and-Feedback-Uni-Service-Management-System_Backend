package com.group8.communication.integration;

import java.util.Optional;
import java.util.UUID;

public interface UserDirectory {
    Optional<UserProfile> findById(UUID userId);
}
