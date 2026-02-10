/**
 * Kanban Board JavaScript
 *
 * Uses custom mouse-event drag (mousedown/mousemove/mouseup) instead of HTML5 DnD
 * because JCEF intercepts native drag events and breaks HTML5 drag-and-drop.
 *
 * Communicates with Java via document.title changes (received by CefDisplayHandler):
 *   "SC:{taskId,newStatus}" — on drag-drop
 *   "TC:taskId"             — on card click
 *   "RF:true"               — on refresh button
 *   "DT:taskId"             — on delete task (drag to bin)
 */

// Board state
var tasks = [];
var statuses = ['To Do', 'In Progress', 'Done'];

// Drag state
var dragState = null;  // { taskId, card, startX, startY, ghost, isDragging }
var DRAG_THRESHOLD = 5; // pixels before mousedown becomes a drag

// Trash bin element
var trashBin = null;

/**
 * Called from Java to push task data into the board.
 */
function updateBoard(tasksJson, statusesJson) {
    try {
        tasks = JSON.parse(tasksJson);
        if (statusesJson) {
            var parsed = JSON.parse(statusesJson);
            if (parsed && parsed.length > 0) {
                statuses = parsed;
            }
        }
    } catch (e) {
        console.error('Failed to parse board data:', e);
        return;
    }
    renderBoard();
    initTrashBin();
}

/**
 * Initialize the trash bin element reference.
 */
function initTrashBin() {
    if (!trashBin) {
        trashBin = document.getElementById('trash-bin');
    }
}

/**
 * Render the full board with columns and cards.
 */
function renderBoard() {
    var board = document.getElementById('kanban-board');
    while (board.firstChild) {
        board.removeChild(board.firstChild);
    }

    if (!tasks || tasks.length === 0) {
        var emptyMsg = document.createElement('div');
        emptyMsg.className = 'empty-board-msg';
        emptyMsg.textContent = 'No tasks found. Create tasks in the backlog/ directory to get started.';
        board.appendChild(emptyMsg);
        return;
    }

    // Group tasks by status
    var grouped = {};
    statuses.forEach(function (s) { grouped[s] = []; });
    tasks.forEach(function (task) {
        var status = task.status || 'To Do';
        if (!grouped[status]) { grouped[status] = []; }
        grouped[status].push(task);
    });

    // Sort within each group
    Object.keys(grouped).forEach(function (status) {
        grouped[status].sort(function (a, b) {
            return extractNumber(a.id) - extractNumber(b.id);
        });
    });

    // Build columns
    statuses.forEach(function (status) {
        var col = document.createElement('div');
        col.className = 'kanban-column';
        col.setAttribute('data-status', status);

        var taskList = grouped[status] || [];

        // Header
        var header = document.createElement('div');
        header.className = 'column-header';
        var titleSpan = document.createElement('span');
        titleSpan.className = 'column-title';
        titleSpan.textContent = status;
        header.appendChild(titleSpan);
        var countSpan = document.createElement('span');
        countSpan.className = 'task-count';
        countSpan.textContent = String(taskList.length);
        header.appendChild(countSpan);
        col.appendChild(header);

        // Body (drop zone)
        var body = document.createElement('div');
        body.className = 'column-body';
        body.setAttribute('data-status', status);

        if (taskList.length === 0) {
            var emptyEl = document.createElement('div');
            emptyEl.className = 'empty-column-msg';
            emptyEl.textContent = 'No tasks';
            body.appendChild(emptyEl);
        } else {
            taskList.forEach(function (task) {
                body.appendChild(createCard(task));
            });
        }

        col.appendChild(body);
        board.appendChild(col);
    });
}

/**
 * Create a task card element.
 */
function createCard(task) {
    var card = document.createElement('div');
    card.className = 'task-card';
    card.setAttribute('data-task-id', task.id || '');
    card.setAttribute('data-priority', (task.priority || 'medium').toLowerCase());

    if (task.id) {
        var idEl = document.createElement('div');
        idEl.className = 'task-card-id';
        idEl.textContent = task.id;
        card.appendChild(idEl);
    }

    var titleEl = document.createElement('div');
    titleEl.className = 'task-card-title';
    titleEl.textContent = task.title || 'Untitled';
    card.appendChild(titleEl);

    if (task.labels && task.labels.length > 0) {
        var labelsEl = document.createElement('div');
        labelsEl.className = 'task-card-labels';
        task.labels.forEach(function (label) {
            var chip = document.createElement('span');
            chip.className = 'label-chip';
            chip.textContent = label;
            labelsEl.appendChild(chip);
        });
        card.appendChild(labelsEl);
    }

    if (task.acceptanceCriteria && task.acceptanceCriteria.length > 0) {
        var total = task.acceptanceCriteria.length;
        var checked = 0;
        task.acceptanceCriteria.forEach(function (ac) { if (ac.checked) checked++; });
        var pct = Math.round((checked / total) * 100);

        var progressEl = document.createElement('div');
        progressEl.className = 'task-card-progress';
        var barContainer = document.createElement('div');
        barContainer.className = 'progress-bar';
        var barFill = document.createElement('div');
        barFill.className = 'progress-fill';
        barFill.style.width = pct + '%';
        barContainer.appendChild(barFill);
        progressEl.appendChild(barContainer);
        var countEl = document.createElement('span');
        countEl.textContent = checked + '/' + total;
        progressEl.appendChild(countEl);
        card.appendChild(progressEl);
    }

    // Mouse-based drag: start on mousedown
    card.addEventListener('mousedown', function (e) {
        if (e.button !== 0) return; // left button only
        e.preventDefault();
        dragState = {
            taskId: task.id,
            card: card,
            startX: e.clientX,
            startY: e.clientY,
            ghost: null,
            isDragging: false
        };
    });

    return card;
}

// ===== Global mouse handlers for drag =====

document.addEventListener('mousemove', function (e) {
    if (!dragState) return;

    var dx = e.clientX - dragState.startX;
    var dy = e.clientY - dragState.startY;

    // Start dragging only after passing threshold (to allow clicks)
    if (!dragState.isDragging) {
        if (Math.abs(dx) + Math.abs(dy) < DRAG_THRESHOLD) return;
        dragState.isDragging = true;

        // Create ghost clone
        var ghost = dragState.card.cloneNode(true);
        ghost.className = 'task-card drag-ghost';
        ghost.style.position = 'fixed';
        ghost.style.width = dragState.card.offsetWidth + 'px';
        ghost.style.pointerEvents = 'none';
        ghost.style.zIndex = '9999';
        document.body.appendChild(ghost);
        dragState.ghost = ghost;

        // Dim the original card
        dragState.card.classList.add('dragging');
    }

    // Move ghost to follow cursor
    if (dragState.ghost) {
        dragState.ghost.style.left = (e.clientX - dragState.ghost.offsetWidth / 2) + 'px';
        dragState.ghost.style.top = (e.clientY - 15) + 'px';
    }

    // Check if hovering over trash bin
    var overBin = isOverTrashBin(e.clientX, e.clientY);
    if (overBin) {
        highlightTrashBin(true);
        clearColumnHighlights();
    } else {
        highlightTrashBin(false);
        // Highlight the column under the cursor
        highlightColumnAt(e.clientX, e.clientY);
    }
});

document.addEventListener('mouseup', function (e) {
    if (!dragState) return;

    if (dragState.isDragging) {
        // Remove ghost
        if (dragState.ghost && dragState.ghost.parentNode) {
            dragState.ghost.parentNode.removeChild(dragState.ghost);
        }
        dragState.card.classList.remove('dragging');
        clearColumnHighlights();
        highlightTrashBin(false);

        // Check if dropped on trash bin
        var overBin = isOverTrashBin(e.clientX, e.clientY);
        if (overBin) {
            // Show confirmation and delete task
            if (confirm('Are you sure you want to delete task ' + dragState.taskId + '?')) {
                var task = findTaskById(dragState.taskId);
                if (task) {
                    // Remove from local array for immediate UI update
                    var index = tasks.indexOf(task);
                    if (index > -1) {
                        tasks.splice(index, 1);
                    }
                    renderBoard();
                    // Notify Java to archive the task
                    document.title = 'DT:' + dragState.taskId;
                }
            }
        } else {
            // Find the column-body under the cursor
            var targetBody = findColumnBodyAt(e.clientX, e.clientY);
            if (targetBody) {
                var newStatus = targetBody.getAttribute('data-status');
                var task = findTaskById(dragState.taskId);
                if (task && task.status !== newStatus) {
                    // Optimistic UI update
                    task.status = newStatus;
                    renderBoard();
                    // Notify Java to persist to disk
                    document.title = 'SC:' + JSON.stringify({ taskId: task.id, newStatus: newStatus });
                }
            }
        }
    } else {
        // It was a click (no drag movement) — open the task file
        if (dragState.taskId) {
            document.title = 'TC:' + dragState.taskId;
        }
    }

    dragState = null;
});

// Cancel drag on Escape
document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && dragState && dragState.isDragging) {
        if (dragState.ghost && dragState.ghost.parentNode) {
            dragState.ghost.parentNode.removeChild(dragState.ghost);
        }
        dragState.card.classList.remove('dragging');
        clearColumnHighlights();
        highlightTrashBin(false);
        dragState = null;
    }
});

/**
 * Highlight the column-body element at the given screen coordinates.
 */
function highlightColumnAt(x, y) {
    var bodies = document.querySelectorAll('.column-body');
    bodies.forEach(function (body) {
        var rect = body.getBoundingClientRect();
        if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
            body.classList.add('drag-over');
        } else {
            body.classList.remove('drag-over');
        }
    });
}

/**
 * Find the column-body element at the given screen coordinates.
 */
function findColumnBodyAt(x, y) {
    var bodies = document.querySelectorAll('.column-body');
    for (var i = 0; i < bodies.length; i++) {
        var rect = bodies[i].getBoundingClientRect();
        if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
            return bodies[i];
        }
    }
    return null;
}

/**
 * Remove all column highlight classes.
 */
function clearColumnHighlights() {
    document.querySelectorAll('.drag-over').forEach(function (el) {
        el.classList.remove('drag-over');
    });
}

/**
 * Check if the given coordinates are over the trash bin.
 */
function isOverTrashBin(x, y) {
    if (!trashBin) return false;
    var rect = trashBin.getBoundingClientRect();
    return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
}

/**
 * Highlight or unhighlight the trash bin.
 */
function highlightTrashBin(highlight) {
    if (!trashBin) return;
    if (highlight) {
        trashBin.classList.add('drag-over-bin');
    } else {
        trashBin.classList.remove('drag-over-bin');
    }
}

/**
 * Called when refresh button is clicked.
 */
function requestRefresh() {
    document.title = 'RF:true';
}

/**
 * Find a task object by its ID.
 */
function findTaskById(id) {
    if (!id || !tasks) return null;
    for (var i = 0; i < tasks.length; i++) {
        if (tasks[i].id === id) return tasks[i];
    }
    return null;
}

/**
 * Extract first numeric part from an ID string for sorting.
 */
function extractNumber(id) {
    if (!id) return 999999;
    var match = id.match(/(\d+)/);
    return match ? parseInt(match[1], 10) : 999999;
}
