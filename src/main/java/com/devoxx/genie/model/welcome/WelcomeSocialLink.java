package com.devoxx.genie.model.welcome;

import lombok.Data;

@Data
public class WelcomeSocialLink {
    private String platform;
    private String url;
    private String label;
}
