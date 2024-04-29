package com.devoxx.genie.model.editor;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SourceLineInfo {

    private int sourceColumnOffset;
    private int sourceLineNumber;
    private int sourceLineCount;
    private int sourceLineStartOffset;

}
