package com.group8.communication.notification;

/** Boundary for validating recipients owned by the Group 5 identity/directory services. */
public interface RecipientDirectory {
    boolean exists(String recipientId);
}
