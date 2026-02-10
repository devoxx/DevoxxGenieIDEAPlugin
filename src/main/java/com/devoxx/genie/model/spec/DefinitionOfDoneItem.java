package com.devoxx.genie.model.spec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single definition-of-done item in a task spec, with checkbox state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefinitionOfDoneItem {
    private int index;
    private String text;
    private boolean checked;
}
