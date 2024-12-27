package com.devoxx.genie.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FileListManager {

    private final Map<String, List<VirtualFile>> filesMap = new HashMap<>();
    private final Map<String, List<FileListObserver>> observersMap = new HashMap<>();
    @Getter
    private int totalFileCount = 0;

    private static FileListManager instance = null;

    private FileListManager() {
    }

    public static FileListManager getInstance() {
        if (instance == null) {
            instance = new FileListManager();
        }
        return instance;
    }

    public void addFile(Project project, VirtualFile file) {
        List<VirtualFile> currentFiles = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        currentFiles.add(file);
        notifyObservers(project, file);
    }

    public void addFiles(Project project, @NotNull List<VirtualFile> newFiles) {
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


    private void notifyObserversOfBatchAdd(Project project, @NotNull List<VirtualFile> addedFiles) {
        List<FileListObserver> observers = observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.filesAdded(addedFiles);
        }
    }

    public void removeFile(Project project, VirtualFile file) {
        List<VirtualFile> currentFiles = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        currentFiles.remove(file);
    }

    public List<VirtualFile> getFiles(Project project) {
        return Collections.unmodifiableList(filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()));
    }

    public boolean isEmpty(Project project) {
        return filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).isEmpty();
    }

    public int size(Project project) {
        return filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).size();
    }

    public boolean contains(Project project, VirtualFile file) {
        return filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).contains(file);
    }

    public void addObserver(Project project, FileListObserver observer) {
        observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>()).add(observer);
    }

    public void clear(Project project) {
        List<VirtualFile> files = filesMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        files.clear();
        totalFileCount = 0;
        notifyAllObservers(project);
    }

    private void notifyObservers(Project project, VirtualFile file) {
        List<FileListObserver> observers = observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.fileAdded(file);
        }
    }

    private void notifyAllObservers(Project project) {
        List<FileListObserver> observers = observersMap.computeIfAbsent(project.getLocationHash(), k -> new ArrayList<>());
        for (FileListObserver observer : observers) {
            observer.allFilesRemoved();
        }
    }
}
