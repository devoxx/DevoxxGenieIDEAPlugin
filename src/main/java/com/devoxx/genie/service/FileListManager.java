package com.devoxx.genie.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

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

    public void storeAddedFiles(@NotNull Project project) {
        filesMap.forEach((key, value) -> {
            if (key.equals(project.getLocationHash())) {
                previouslyAddedFiles.put(key, new ArrayList<>(value));
            }
        });
    }

    public @NotNull List<VirtualFile> getPreviouslyAddedFiles(@NotNull Project project) {
        return Collections.unmodifiableList(previouslyAddedFiles.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()));
    }

    public void addFile(@NotNull Project project, VirtualFile file) {
        addFile(project, file, true);
    }

    private void addFile(@NotNull Project project, VirtualFile file, boolean notify) {
        List<VirtualFile> currentFiles = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        if (isImageFile(file)) {
            List<VirtualFile> imageFiles = imageFilesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
            imageFiles.add(file);
        } else {
            currentFiles.add(file);
        }
        if (notify) {
            notifyObservers(project, file);
        }
    }

    public void addFiles(@NotNull Project project, @NotNull List<VirtualFile> newFiles) {
        List<VirtualFile> actuallyAddedFiles = new ArrayList<>();
        Set<VirtualFile> currentFilesSet = new HashSet<>(getFiles(project));

        for (VirtualFile file : newFiles) {
            if (!currentFilesSet.contains(file)) {
                addFile(project, file, false);
                actuallyAddedFiles.add(file);
                currentFilesSet.add(file);
            }
        }
        if (!actuallyAddedFiles.isEmpty()) {
            actuallyAddedFiles.sort(Comparator.comparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER));
            notifyObserversOfBatchAdd(project, actuallyAddedFiles);
        }
    }

    private void notifyObserversOfBatchAdd(@NotNull Project project, @NotNull List<VirtualFile> addedFiles) {
        List<FileListObserver> observers = observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.filesAdded(addedFiles);
        }
    }

    public void removeFile(@NotNull Project project, VirtualFile file) {
        if (isImageFile(file)) {
            List<VirtualFile> imageFiles = imageFilesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
            imageFiles.remove(file);
        } else {
            List<VirtualFile> currentFiles = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
            currentFiles.remove(file);
        }
    }

    public @NotNull List<VirtualFile> getFiles(@NotNull Project project) {
        List<VirtualFile> virtualFiles = Collections.unmodifiableList(filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()));
        List<VirtualFile> imageFiles = Collections.unmodifiableList(imageFilesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()));
        List<VirtualFile> allFiles = new ArrayList<>(virtualFiles);
        allFiles.addAll(imageFiles);
        return allFiles;
    }

    public @NotNull List<VirtualFile> getNonImageFiles(@NotNull Project project) {
        return Collections.unmodifiableList(filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()));
    }

    public @NotNull List<VirtualFile> getImageFiles(@NotNull Project project) {
        return Collections.unmodifiableList(imageFilesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()));
    }

    public boolean isEmpty(@NotNull Project project) {
        boolean empty = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).isEmpty();
        empty &= imageFilesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).isEmpty();
        return empty;
    }

    public int size(@NotNull Project project) {
        int size = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).size();
        size += imageFilesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).size();
        return size;
    }

    public boolean contains(@NotNull Project project, VirtualFile file) {
        boolean contains = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).contains(file);
        contains |= imageFilesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).contains(file);
        return contains;
    }

    public void addObserver(@NotNull Project project, FileListObserver observer) {
        observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).add(observer);
    }

    public void clear(@NotNull Project project) {
        String projectHash = project.getLocationHash();

        // Remove the entries for this project
        filesMap.remove(projectHash);
        imageFilesMap.remove(projectHash);
        previouslyAddedFiles.remove(projectHash);

        notifyAllObservers(project);
    }

    /**
     * Clear only the previously added files for a new conversation, keeping current files intact.
     * This is used when starting a new conversation to ensure all current files are treated as "new".
     */
    public void clearPreviouslyAddedFiles(@NotNull Project project) {
        String projectHash = project.getLocationHash();
        previouslyAddedFiles.remove(projectHash);
    }

    /**
     * Clear current files but keep previously added files.
     * This is useful when replacing the current file list but preserving the conversation history.
     */
    public void clearCurrentFiles(@NotNull Project project) {
        String projectHash = project.getLocationHash();
        filesMap.remove(projectHash);
        imageFilesMap.remove(projectHash);
        notifyAllObservers(project);
    }

    /**
     * Clear only non-image files and previously added files, preserving image files.
     * Used when switching to editor context to avoid destroying attached images.
     */
    public void clearNonImageFiles(@NotNull Project project) {
        String projectHash = project.getLocationHash();
        filesMap.remove(projectHash);
        previouslyAddedFiles.remove(projectHash);
        notifyAllObservers(project);
    }

    private void notifyObservers(@NotNull Project project, VirtualFile file) {
        List<FileListObserver> observers = observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.fileAdded(file);
        }
    }

    private void notifyAllObservers(@NotNull Project project) {
        List<FileListObserver> observers = observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.allFilesRemoved();
        }
    }
}
