package com.devoxx.genie.model.lmstudio;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LMStudioModelEntryDTO {
    private String id;
    private String object;
    private String owned_by;
    private Integer max_context_length;
}
