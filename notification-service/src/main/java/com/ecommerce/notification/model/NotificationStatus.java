package com.ecommerce.notification.model;

public enum NotificationStatus {
    SENT,
    SIMULATED,  // channel disabled — logged but not actually sent
    FAILED
}
