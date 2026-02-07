package com.devoxx.genie.model.welcome;

import lombok.Data;

import java.util.List;

@Data
public class WelcomeContent {
    private int schemaVersion;
    private String lastUpdated;
    private String title;
    private String description;
    private String instructions;
    private List<WelcomeFeature> features;
    private List<WelcomeAnnouncement> announcements;
    private String tip;
    private String enjoy;
    private List<WelcomeSocialLink> socialLinks;
    private String reviewUrl;
    private String trackingPixelUrl;
}
