package com.devoxx.genie.model.lmstudio;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LMStudioModelDTO {
    private LMStudioModelEntryDTO[] data;
    private String object;
}
