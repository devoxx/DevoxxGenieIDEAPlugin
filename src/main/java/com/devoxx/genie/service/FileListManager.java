package com.devoxx.genie.service;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileListManager {

    private final List<VirtualFile> files = new ArrayList<>();
    private final List<FileListObserver> observers = new ArrayList<>();
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

    public void addFile(VirtualFile file) {
        files.add(file);
        notifyObservers(file);
    }

    public void addFiles(@NotNull List<VirtualFile> newFiles) {
        List<VirtualFile> actuallyAddedFiles = new ArrayList<>();
        for (VirtualFile file : newFiles) {
            if (!files.contains(file)) {
                files.add(file);
                actuallyAddedFiles.add(file);
            }
        }
        if (!actuallyAddedFiles.isEmpty()) {
            notifyObserversOfBatchAdd(actuallyAddedFiles);
        }
    }

    private void notifyObserversOfBatchAdd(List<VirtualFile> addedFiles) {
        for (FileListObserver observer : observers) {
            observer.filesAdded(addedFiles);
        }
    }

    public void removeFile(VirtualFile file) {
        files.remove(file);
    }

    public List<VirtualFile> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    public int size() {
        return files.size();
    }

    public boolean contains(VirtualFile file) {
        return files.contains(file);
    }

    public void addObserver(FileListObserver observer) {
        observers.add(observer);
    }

    public void clear() {
        files.clear();
        totalFileCount = 0;
        notifyAllObservers();
    }

    private void notifyObservers(VirtualFile file) {
        for (FileListObserver observer : observers) {
            observer.fileAdded(file);
        }
    }

    private void notifyAllObservers() {
        for (FileListObserver observer : observers) {
            observer.allFilesRemoved();
        }
    }
}
