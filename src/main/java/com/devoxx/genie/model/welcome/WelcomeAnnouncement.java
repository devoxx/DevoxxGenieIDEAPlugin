package com.devoxx.genie.model.welcome;

import lombok.Data;

@Data
public class WelcomeAnnouncement {
    private String type; // info, warning, success
    private String message;
}
