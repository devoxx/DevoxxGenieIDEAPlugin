package com.devoxx.genie.service.tdg;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ToString
@EqualsAndHashCode
@Setter
@Getter
public final class CodeContainer {

    private final String content;
    private final String fileName;
    private final String packageName;
    private final int attempts;

    public CodeContainer(String content) throws ClassNameNotFoundException {
        this(content, 1);
    }

    public CodeContainer(String content, int attempts) throws ClassNameNotFoundException {
        this.content = content;
        this.fileName = extractClassName() + ".java";
        this.packageName = extractPackageName();
        this.attempts = attempts;
    }

    // TODO: first look for 'public class' and then for 'class
    private String extractClassName() throws ClassNameNotFoundException {
        // matches "public" (optional) followed by "class" and then the class name
        String regex = "\\b(?:public\\s+)?class\\s+(\\w+)\\b";
        Matcher matcher = Pattern.compile(regex).matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new ClassNameNotFoundException("Class name not found in: " + content);
        }
    }

    private String extractPackageName() {
        String regex = "package\\s+(\\w+(\\.\\w+)*)";
        Matcher matcher = Pattern.compile(regex).matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }
}
