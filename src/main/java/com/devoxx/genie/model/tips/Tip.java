package com.devoxx.genie.model.tips;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tip {
    private String text;
    private int weight;
}
