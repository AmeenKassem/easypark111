package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Notification {
    @Id
    private Long notification_id;

    public void setNotification_id(Long notificationId) {
        this.notification_id = notificationId;
    }

    public Long getNotification_id() {
        return notification_id;
    }
}
