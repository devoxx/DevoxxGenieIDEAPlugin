package com.devoxx.genie.model;

import com.devoxx.genie.model.request.PromptContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class ChatInteraction {
    private PromptContext promptContext;
    private String response;
}
