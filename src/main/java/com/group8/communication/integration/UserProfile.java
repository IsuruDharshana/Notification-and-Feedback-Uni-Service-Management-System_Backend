package com.group8.communication.integration;

import java.util.UUID;

public record UserProfile(UUID id, String role, String department, String faculty, String serviceUnit) {
}
