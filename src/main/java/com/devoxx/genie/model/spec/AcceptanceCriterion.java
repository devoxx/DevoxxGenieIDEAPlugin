package com.devoxx.genie.model.spec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single acceptance criterion item in a task spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptanceCriterion {
    private int index;
    private String text;
    private boolean checked;
}
