---
id: TASK-40
title: Fix generic exception handling in AgentRequestHandler
status: Done
priority: medium
assignee: []
created_date: '2026-02-14 15:21'
updated_date: '2026-02-14 15:37'
labels:
  - code-quality
  - sonarlint
  - maintainability
dependencies: []
references:
  - /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/java/com/devoxx/genie/service/acp/protocol/AgentRequestHandler.java
documentation: []
ordinal: 1000
---

Replace generic IOException with specific library exceptions or custom exceptions in file operation methods (handleFsRead, handleFsWrite) to comply with SonarLint rule java:S112.

## task task-40: fix generic exception handling in agentrequesthandler - completed
### Summary
Successfully replaced generic IOException with specific custom exceptions in file operation methods to comply with SonarLint rule java:S112.

### Implementation Details

#### 1. Created Custom Exceptions
- **FsReadException.java** - Located at `src/main/java/com/devoxx/genie/service/acp/protocol/exception/FsReadException.java`
  - Extends `AcpException` for consistent exception hierarchy
  - Provides specific error information for file read failures
  
- **FsWriteException.java** - Located at `src/main/java/com/devoxx/genie/service/acp/protocol/exception/FsWriteException.java`
  - Extends `AcpException` for consistent exception hierarchy
  - Provides specific error information for file write failures

#### 2. Updated AgentRequestHandler.java
- **File**: `src/main/java/com/devoxx/genie/service/acp/protocol/AgentRequestHandler.java`
- **Changes**:
  - Added imports for `FsReadException` and `FsWriteException`
  - Modified `handleFsRead()` method:
    - Changed signature from `throws IOException` to `throws FsReadException`
    - Wrapped `Files.readString()` in try-catch block
    - Re-throws caught IOException as `FsReadException` with descriptive message
  - Modified `handleFsWrite()` method:
    - Changed signature from `throws IOException` to `throws FsWriteException`
    - Wrapped file write operations in try-catch block
    - Re-throws caught IOException as `FsWriteException` with descriptive message
  - Updated `dispatch()` method signature to declare the new exceptions

#### 3. SonarLint Compliance
- **Rule**: java:S112 - "Replace generic exceptions with specific library exceptions or a custom exception"
- **Status**: ✅ RESOLVED
- The SonarLint issue has been eliminated by using custom exceptions instead of generic IOException

### Files Modified
1. `src/main/java/com/devoxx/genie/service/acp/protocol/exception/FsReadException.java` (created)
2. `src/main/java/com/devoxx/genie/service/acp/protocol/exception/FsWriteException.java` (created)
3. `src/main/java/com/devoxx/genie/service/acp/protocol/AgentRequestHandler.java` (modified)

### Benefits
- **Better Error Handling**: Specific exceptions provide more context about what went wrong
- **Maintainability**: Clear exception hierarchy makes debugging easier
- **Code Quality**: Complies with SonarLint quality standards
- **Consistency**: Follows existing ACP exception hierarchy pattern

