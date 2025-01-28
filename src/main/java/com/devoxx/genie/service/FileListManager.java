package com.devoxx.genie.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FileListManager {

    private final Map<String, List<VirtualFile>> previouslyAddedFiles = new HashMap<>();
    private final Map<String, List<VirtualFile>> filesMap = new HashMap<>();
    private final Map<String, List<FileListObserver>> observersMap = new HashMap<>();

    private static FileListManager instance = null;

    private FileListManager() {
    }

    public static FileListManager getInstance() {
        if (instance == null) {
            instance = new FileListManager();
        }
        return instance;
    }

    public void storeAddedFiles(@NotNull Project project) {
        filesMap.forEach((key, value) -> {
            if (key.equals(project.getLocationHash())) {
                previouslyAddedFiles.put(key, new ArrayList<>(value));
            }
        });
    }

    public List<VirtualFile> getPreviouslyAddedFiles(@NotNull Project project) {
        return Collections.unmodifiableList(previouslyAddedFiles.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()));
    }

    public void addFile(@NotNull Project project, VirtualFile file) {
        List<VirtualFile> currentFiles = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        currentFiles.add(file);
        notifyObservers(project, file);
    }

    public void addFiles(@NotNull Project project, @NotNull List<VirtualFile> newFiles) {
        List<VirtualFile> actuallyAddedFiles = new ArrayList<>();
        List<VirtualFile> currentFiles = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        Set<VirtualFile> currentFilesSet = new HashSet<>(currentFiles);

        for (VirtualFile file : newFiles) {
            if (!currentFilesSet.contains(file)) {
                currentFiles.add(file);
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
        List<VirtualFile> currentFiles = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        currentFiles.remove(file);
    }

    public List<VirtualFile> getFiles(@NotNull Project project) {
        return Collections.unmodifiableList(filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()));
    }

    public boolean isEmpty(@NotNull Project project) {
        return filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).isEmpty();
    }

    public int size(@NotNull Project project) {
        return filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).size();
    }

    public boolean contains(@NotNull Project project, VirtualFile file) {
        return filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).contains(file);
    }

    public void addObserver(@NotNull Project project, FileListObserver observer) {
        observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).add(observer);
    }

    public void clear(@NotNull Project project) {
        String projectHash = project.getLocationHash();

        // Remove the entries for this project
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
