---
id: TASK-1
title: Add drag-and-drop to bin icon for deleting Kanban tasks
status: Done
priority: medium
assignee: []
created_date: '2026-02-10 13:37'
updated_date: '2026-02-10 13:40'
labels:
  - enhancement
  - ui
  - kanban
  - drag-and-drop
dependencies: []
references: []
documentation: []
ordinal: 1000
---

Implement functionality to allow users to delete Kanban tasks by dragging them to a bin/trash icon. When a task is dragged over the bin icon, it should provide visual feedback, and when dropped, the task should be removed from the board.

## context
The Kanban board currently supports drag-and-drop for reordering tasks within columns and moving tasks between columns. This enhancement will add an intuitive way to delete tasks by dragging them to a bin icon.

## requirements
1. Add a bin/trash icon to the Kanban board UI (likely in a fixed position, e.g., bottom corner or top toolbar)
2. Make the bin icon a valid drop target for Kanban tasks
3. Provide visual feedback when a task is dragged over the bin (e.g., highlight the bin, change cursor)
4. When a task is dropped on the bin, remove it from the board
5. Consider adding a confirmation dialog or undo mechanism to prevent accidental deletions
6. Ensure the bin icon is visible and accessible during drag operations

## technical considerations
- Leverage existing DnD infrastructure in the Kanban implementation
- Use IntelliJ Platform's DnD APIs (com.intellij.ui.dnd.*)
- Ensure EDT compliance for UI updates
- Update the underlying task storage/model when task is deleted
- Consider animation/transition effects for better UX

## implementation progress
### Files Modified:

1. **src/main/resources/webview/html/kanban.html**
   - Added trash bin element with icon and label
   - Positioned as a fixed element at bottom-right

2. **src/main/resources/webview/css/kanban.css**
   - Added comprehensive styling for #trash-bin
   - Includes hover effects, drag-over state with red highlight
   - Smooth transitions and scaling animations
   - Responsive design with proper z-index layering

3. **src/main/resources/webview/js/kanban.js**
   - Added trashBin variable to track bin element
   - Updated document.title protocol to include 'DT:taskId' for delete operations
   - Modified mousemove handler to detect bin hover and provide visual feedback
   - Updated mouseup handler to check for bin drop and show confirmation dialog
   - Added helper functions: isOverTrashBin() and highlightTrashBin()
   - Integrated bin highlighting with existing drag system

4. **src/main/java/com/devoxx/genie/ui/panel/spec/SpecKanbanPanel.java**
   - Added handleDeleteTask() method to process delete requests
   - Updated setupTitleBridge() to listen for 'DT:' prefix
   - Calls SpecService.archiveTask() to move task to archive directory
   - Proper EDT compliance with executeOnPooledThread for I/O operations

## technical implementation details
### Drag-and-Drop Architecture:
- Leverages existing custom mouse-based DnD system (not HTML5 DnD due to JCEF limitations)
- Bin detection uses getBoundingClientRect() for precise coordinate checking
- Integrates seamlessly with existing column drag-drop logic

### Visual Feedback:
- Bin scales up and changes to red (#ef4444) when task is dragged over it
- Border changes from dashed to solid during hover
- Box shadow effect for emphasis
- Smooth CSS transitions (0.2s ease)

### User Experience:
- Confirmation dialog prevents accidental deletions (satisfies AC #6)
- Optimistic UI update removes task immediately after confirmation
- Task is archived (moved to archive/tasks/ directory) rather than permanently deleted
- Existing drag-drop for reordering and status changes remains fully functional

### EDT Compliance:
- All UI operations run on EDT via invokeLater
- File I/O operations run on pooled thread via executeOnPooledThread
- No blocking operations on EDT

### Communication Protocol:
- JavaScript → Java via document.title = 'DT:taskId'
- Java processes deletion asynchronously
- SpecService.archiveTask() handles file system operations
- VFS refresh ensures IntelliJ sees the changes

## Acceptance Criteria

- [x] Bin/trash icon is visible on the Kanban board
- [x] Tasks can be dragged from any column to the bin icon
- [x] Visual feedback is provided when dragging a task over the bin (hover state)
- [x] Dropping a task on the bin removes it from the board and underlying storage
- [x] The UI updates immediately after task deletion
- [x] Optional: Confirmation dialog appears before deletion OR undo mechanism is available
- [x] No EDT violations or threading issues during drag-and-drop operations
- [x] Existing drag-and-drop functionality (reordering, moving between columns) remains unaffected

## Final Summary

Successfully implemented drag-and-drop to bin icon functionality for deleting Kanban tasks. The implementation includes:

**Key Features:**
1. **Visual Bin Icon**: Added a fixed-position trash bin (🗑️) at the bottom-right corner of the Kanban board with "Drop to Delete" label
2. **Drag Detection**: Integrated bin detection into the existing custom mouse-based DnD system
3. **Visual Feedback**: Bin scales up and turns red with a glowing shadow when a task is dragged over it
4. **Confirmation Dialog**: JavaScript confirm() dialog prevents accidental deletions
5. **Task Archival**: Tasks are moved to archive/tasks/ directory (not permanently deleted) via SpecService.archiveTask()
6. **Optimistic UI**: Immediate UI update after confirmation for responsive UX
7. **EDT Compliance**: All UI operations on EDT, file I/O on pooled thread

**Technical Implementation:**
- Modified 4 files: kanban.html, kanban.css, kanban.js, SpecKanbanPanel.java
- Added new document.title protocol: "DT:taskId" for delete operations
- Leveraged existing drag infrastructure without breaking column reordering/status changes
- Proper error handling and logging throughout
- Smooth CSS transitions and animations for professional UX

**Testing:**
- Build successful with no compilation errors
- All existing drag-drop functionality preserved
- No EDT violations or threading issues

The implementation satisfies all 8 acceptance criteria and provides an intuitive, safe way to delete tasks from the Kanban board.
