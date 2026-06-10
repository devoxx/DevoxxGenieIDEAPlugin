package com.devoxx.genie.model.tips;

import lombok.Data;

import java.util.List;

@Data
public class TipConfig {
    private int schemaVersion;
    private String lastUpdated;
    private List<Tip> tips;
}
