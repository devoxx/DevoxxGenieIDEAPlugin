package com.devoxx.genie.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.devoxx.genie.action.AddSnippetAction.*;
import static com.devoxx.genie.util.ImageUtil.isImageFile;

@Service
public final class FileListManager {

    private final Map<String, List<VirtualFile>> previouslyAddedFiles = new HashMap<>();
    private final Map<String, List<VirtualFile>> filesMap = new HashMap<>();
    private final Map<String, List<FileListObserver>> observersMap = new HashMap<>();
    private final Map<String, List<VirtualFile>> imageFilesMap = new HashMap<>();

    FileListManager() {
    }

    @NotNull
    public static FileListManager getInstance() {
        return ApplicationManager.getApplication().getService(FileListManager.class);
    }

    /**
     * Returns a composite key for per-tab file list isolation.
     * If tabId is non-null, returns "projectHash-tabId"; otherwise just projectHash.
     */
    private static String computeKey(@NotNull Project project, String tabId) {
        String projectHash = project.getLocationHash();
        return tabId != null ? projectHash + "-" + tabId : projectHash;
    }

    public void storeAddedFiles(@NotNull Project project) {
        storeAddedFiles(project, null);
    }

    public void storeAddedFiles(@NotNull Project project, String tabId) {
        String key = computeKey(project, tabId);
        List<VirtualFile> files = filesMap.get(key);
        if (files != null) {
            previouslyAddedFiles.put(key, new ArrayList<>(files));
        }
    }

    public @NotNull List<VirtualFile> getPreviouslyAddedFiles(@NotNull Project project) {
        return getPreviouslyAddedFiles(project, null);
    }

    public @NotNull List<VirtualFile> getPreviouslyAddedFiles(@NotNull Project project, String tabId) {
        return Collections.unmodifiableList(previouslyAddedFiles.computeIfAbsent(computeKey(project, tabId), k -> new ArrayList<>()));
    }

    public void addFile(@NotNull Project project, VirtualFile file) {
        addFile(project, null, file, true);
    }

    public void addFile(@NotNull Project project, String tabId, VirtualFile file) {
        addFile(project, tabId, file, true);
    }

    private void addFile(@NotNull Project project, String tabId, VirtualFile file, boolean notify) {
        String key = computeKey(project, tabId);
        List<VirtualFile> currentFiles = filesMap.computeIfAbsent(key, k -> new ArrayList<>());
        if (isImageFile(file)) {
            List<VirtualFile> imageFiles = imageFilesMap.computeIfAbsent(key, k -> new ArrayList<>());
            imageFiles.add(file);
        } else {
            currentFiles.add(file);
        }
        if (notify) {
            notifyObservers(project, tabId, file);
        }
    }

    public void addFiles(@NotNull Project project, @NotNull List<VirtualFile> newFiles) {
        addFiles(project, null, newFiles);
    }

    public void addFiles(@NotNull Project project, String tabId, @NotNull List<VirtualFile> newFiles) {
        List<VirtualFile> actuallyAddedFiles = new ArrayList<>();
        Set<VirtualFile> currentFilesSet = new HashSet<>(getFiles(project, tabId));

        for (VirtualFile file : newFiles) {
            if (!currentFilesSet.contains(file)) {
                addFile(project, tabId, file, false);
                actuallyAddedFiles.add(file);
                currentFilesSet.add(file);
            }
        }
        if (!actuallyAddedFiles.isEmpty()) {
            actuallyAddedFiles.sort(Comparator.comparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER));
            notifyObserversOfBatchAdd(project, tabId, actuallyAddedFiles);
        }
    }

    private void notifyObserversOfBatchAdd(@NotNull Project project, String tabId, @NotNull List<VirtualFile> addedFiles) {
        String key = computeKey(project, tabId);
        List<FileListObserver> observers = observersMap.computeIfAbsent(key, k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.filesAdded(addedFiles);
        }
    }

    public void removeFile(@NotNull Project project, VirtualFile file) {
        removeFile(project, null, file);
    }

    public void removeFile(@NotNull Project project, String tabId, VirtualFile file) {
        String key = computeKey(project, tabId);
        if (isImageFile(file)) {
            List<VirtualFile> imageFiles = imageFilesMap.computeIfAbsent(key, k -> new ArrayList<>());
            imageFiles.remove(file);
        } else {
            List<VirtualFile> currentFiles = filesMap.computeIfAbsent(key, k -> new ArrayList<>());
            currentFiles.remove(file);
        }
        clearSelectionMetadata(file);
    }

    public @NotNull List<VirtualFile> getFiles(@NotNull Project project) {
        return getFiles(project, null);
    }

    public @NotNull List<VirtualFile> getFiles(@NotNull Project project, String tabId) {
        String key = computeKey(project, tabId);
        List<VirtualFile> virtualFiles = Collections.unmodifiableList(filesMap.computeIfAbsent(key, k -> new ArrayList<>()));
        List<VirtualFile> imageFiles = Collections.unmodifiableList(imageFilesMap.computeIfAbsent(key, k -> new ArrayList<>()));
        List<VirtualFile> allFiles = new ArrayList<>(virtualFiles);
        allFiles.addAll(imageFiles);
        return allFiles;
    }

    public @NotNull List<VirtualFile> getNonImageFiles(@NotNull Project project) {
        return getNonImageFiles(project, null);
    }

    public @NotNull List<VirtualFile> getNonImageFiles(@NotNull Project project, String tabId) {
        return Collections.unmodifiableList(filesMap.computeIfAbsent(computeKey(project, tabId), k -> new ArrayList<>()));
    }

    public @NotNull List<VirtualFile> getImageFiles(@NotNull Project project) {
        return getImageFiles(project, null);
    }

    public @NotNull List<VirtualFile> getImageFiles(@NotNull Project project, String tabId) {
        return Collections.unmodifiableList(imageFilesMap.computeIfAbsent(computeKey(project, tabId), k -> new ArrayList<>()));
    }

    public boolean isEmpty(@NotNull Project project) {
        return isEmpty(project, null);
    }

    public boolean isEmpty(@NotNull Project project, String tabId) {
        String key = computeKey(project, tabId);
        boolean empty = filesMap.computeIfAbsent(key, k -> new ArrayList<>()).isEmpty();
        empty &= imageFilesMap.computeIfAbsent(key, k -> new ArrayList<>()).isEmpty();
        return empty;
    }

    public int size(@NotNull Project project) {
        return size(project, null);
    }

    public int size(@NotNull Project project, String tabId) {
        String key = computeKey(project, tabId);
        int size = filesMap.computeIfAbsent(key, k -> new ArrayList<>()).size();
        size += imageFilesMap.computeIfAbsent(key, k -> new ArrayList<>()).size();
        return size;
    }

    public boolean contains(@NotNull Project project, VirtualFile file) {
        return contains(project, null, file);
    }

    public boolean contains(@NotNull Project project, String tabId, VirtualFile file) {
        String key = computeKey(project, tabId);
        boolean contains = filesMap.computeIfAbsent(key, k -> new ArrayList<>()).contains(file);
        contains |= imageFilesMap.computeIfAbsent(key, k -> new ArrayList<>()).contains(file);
        return contains;
    }

    public void addObserver(@NotNull Project project, FileListObserver observer) {
        addObserver(project, null, observer);
    }

    public void addObserver(@NotNull Project project, String tabId, FileListObserver observer) {
        observersMap.computeIfAbsent(computeKey(project, tabId), k -> new ArrayList<>()).add(observer);
    }

    public void clear(@NotNull Project project) {
        clear(project, null);
    }

    public void clear(@NotNull Project project, String tabId) {
        String key = computeKey(project, tabId);

        // Clear selection metadata from all files before removing them
        clearSelectionMetadataForProject(key);

        // Remove the entries
        filesMap.remove(key);
        imageFilesMap.remove(key);
        previouslyAddedFiles.remove(key);

        notifyAllObservers(project, tabId);
    }

    /**
     * Clear only the previously added files for a new conversation, keeping current files intact.
     */
    public void clearPreviouslyAddedFiles(@NotNull Project project) {
        clearPreviouslyAddedFiles(project, null);
    }

    public void clearPreviouslyAddedFiles(@NotNull Project project, String tabId) {
        previouslyAddedFiles.remove(computeKey(project, tabId));
    }

    /**
     * Clear current files but keep previously added files.
     */
    public void clearCurrentFiles(@NotNull Project project) {
        clearCurrentFiles(project, null);
    }

    public void clearCurrentFiles(@NotNull Project project, String tabId) {
        String key = computeKey(project, tabId);
        clearSelectionMetadataForProject(key);
        filesMap.remove(key);
        imageFilesMap.remove(key);
        notifyAllObservers(project, tabId);
    }

    /**
     * Clear only non-image files and previously added files, preserving image files.
     */
    public void clearNonImageFiles(@NotNull Project project) {
        clearNonImageFiles(project, null);
    }

    public void clearNonImageFiles(@NotNull Project project, String tabId) {
        String key = computeKey(project, tabId);
        List<VirtualFile> files = filesMap.get(key);
        if (files != null) {
            files.forEach(FileListManager::clearSelectionMetadata);
        }
        filesMap.remove(key);
        previouslyAddedFiles.remove(key);
        notifyAllObservers(project, tabId);
    }

    /**
     * Clear selection metadata from all files (both regular and image) for a project.
     * Must be called before removing the file lists from the maps.
     */
    private void clearSelectionMetadataForProject(String projectHash) {
        List<VirtualFile> files = filesMap.get(projectHash);
        if (files != null) {
            files.forEach(FileListManager::clearSelectionMetadata);
        }
        List<VirtualFile> images = imageFilesMap.get(projectHash);
        if (images != null) {
            images.forEach(FileListManager::clearSelectionMetadata);
        }
    }

    /**
     * Clear snippet/selection UserData keys from a VirtualFile.
     * Fixes issue #783: stale selection metadata caused re-added files
     * to still display as code snippets with old line numbers.
     */
    private static void clearSelectionMetadata(VirtualFile file) {
        file.putUserData(ORIGINAL_FILE_KEY, null);
        file.putUserData(SELECTED_TEXT_KEY, null);
        file.putUserData(SELECTION_START_KEY, null);
        file.putUserData(SELECTION_END_KEY, null);
        file.putUserData(SELECTION_START_LINE_KEY, null);
        file.putUserData(SELECTION_END_LINE_KEY, null);
    }

    private void notifyObservers(@NotNull Project project, String tabId, VirtualFile file) {
        String key = computeKey(project, tabId);
        List<FileListObserver> observers = observersMap.computeIfAbsent(key, k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.fileAdded(file);
        }
    }

    private void notifyAllObservers(@NotNull Project project, String tabId) {
        String key = computeKey(project, tabId);
        List<FileListObserver> observers = observersMap.computeIfAbsent(key, k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.allFilesRemoved();
        }
    }
}
