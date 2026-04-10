package com.devoxx.genie.model.exo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExoModelDTO {
    private String object;
    private ExoModelEntryDTO[] data;
}
