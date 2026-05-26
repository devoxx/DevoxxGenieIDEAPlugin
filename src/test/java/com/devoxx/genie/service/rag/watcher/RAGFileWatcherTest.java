package com.devoxx.genie.service.rag.watcher;

import com.devoxx.genie.service.rag.ProjectIndexerService;
import com.devoxx.genie.service.rag.manifest.IndexManifest;
import com.devoxx.genie.service.rag.manifest.IndexManifestService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RAGFileWatcherTest {

    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<IndexManifestService> manifestServiceStatic;

    private Project project;
    private IndexManifest manifest;
    private ProjectIndexerService indexer;

    @BeforeEach
    void setUp() {
        stateServiceStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        manifestServiceStatic = Mockito.mockStatic(IndexManifestService.class);

        DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);
        when(state.getRagEnabled()).thenReturn(true);

        project = mock(Project.class);
        when(project.getBasePath()).thenReturn("/abs/project");
        MessageBus bus = mock(MessageBus.class);
        when(project.getMessageBus()).thenReturn(bus);
        MessageBusConnection connection = mock(MessageBusConnection.class);
        when(bus.connect(any(com.intellij.openapi.Disposable.class))).thenReturn(connection);

        manifest = mock(IndexManifest.class);
        IndexManifestService manifestService = mock(IndexManifestService.class);
        manifestServiceStatic.when(IndexManifestService::getInstance).thenReturn(manifestService);
        when(manifestService.forProject(project)).thenReturn(manifest);

        indexer = mock(ProjectIndexerService.class);
    }

    @AfterEach
    void tearDown() {
        stateServiceStatic.close();
        manifestServiceStatic.close();
    }

    @Test
    void untrackedFilesDoNotTriggerReindex() throws Exception {
        RAGFileWatcher watcher = new RAGFileWatcher(project, indexer);
        Path tracked = Path.of("/abs/project/Tracked.java");
        Path untracked = Path.of("/abs/project/Untracked.java");
        when(manifest.isTracked(tracked)).thenReturn(true);
        when(manifest.isTracked(untracked)).thenReturn(false);

        watcher.handleEvents(List.of(
                changeEvent(untracked),
                changeEvent(tracked)
        ));

        // Debounce window — wait just past it.
        Thread.sleep(RAGFileWatcher.DEBOUNCE_MILLIS + 500);

        ArgumentCaptor<Collection<Path>> captor = collectionCaptor();
        verify(indexer).reindexFiles(eq(project), captor.capture());
        assertThat(captor.getValue())
                .as("only tracked files survive the watcher's filter")
                .containsExactly(tracked);
        watcher.dispose();
    }

    @Test
    void deletesArePassedToRemoveFilesNotReindex() throws Exception {
        RAGFileWatcher watcher = new RAGFileWatcher(project, indexer);
        Path tracked = Path.of("/abs/project/Removed.java");
        when(manifest.isTracked(tracked)).thenReturn(true);

        watcher.handleEvents(List.of(deleteEvent(tracked)));
        Thread.sleep(RAGFileWatcher.DEBOUNCE_MILLIS + 500);

        verify(indexer).removeFiles(eq(project), any());
        verify(indexer, never()).reindexFiles(any(), any());
        watcher.dispose();
    }

    @Test
    void rapidEditsAreDebouncedIntoASingleReindexCall() throws Exception {
        RAGFileWatcher watcher = new RAGFileWatcher(project, indexer);
        Path file = Path.of("/abs/project/Hot.java");
        when(manifest.isTracked(file)).thenReturn(true);

        // Three saves in rapid succession — the user typing Ctrl+S, Ctrl+S, Ctrl+S.
        for (int i = 0; i < 3; i++) {
            watcher.handleEvents(List.of(changeEvent(file)));
            Thread.sleep(100);
        }

        // Now hold quiet for one full debounce window.
        Thread.sleep(RAGFileWatcher.DEBOUNCE_MILLIS + 500);

        // The whole burst should produce exactly one reindex call.
        verify(indexer, Mockito.times(1)).reindexFiles(eq(project), any());
        watcher.dispose();
    }

    @Test
    void deleteSupersedesPendingChangeForTheSamePath() throws Exception {
        RAGFileWatcher watcher = new RAGFileWatcher(project, indexer);
        Path file = Path.of("/abs/project/Gone.java");
        when(manifest.isTracked(file)).thenReturn(true);

        // Edit then delete: only the delete should reach the indexer.
        watcher.handleEvents(List.of(changeEvent(file)));
        Thread.sleep(50);
        watcher.handleEvents(List.of(deleteEvent(file)));
        Thread.sleep(RAGFileWatcher.DEBOUNCE_MILLIS + 500);

        verify(indexer).removeFiles(eq(project), any());
        verify(indexer, never()).reindexFiles(any(), any());
        watcher.dispose();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Collection<Path>> collectionCaptor() {
        return ArgumentCaptor.forClass((Class) Collection.class);
    }

    private static VFileEvent changeEvent(Path file) {
        VFileContentChangeEvent e = mock(VFileContentChangeEvent.class);
        VirtualFile vf = mock(VirtualFile.class);
        when(vf.getPath()).thenReturn(file.toString());
        when(e.getFile()).thenReturn(vf);
        return e;
    }

    private static VFileEvent deleteEvent(Path file) {
        VFileDeleteEvent e = mock(VFileDeleteEvent.class);
        VirtualFile vf = mock(VirtualFile.class);
        when(vf.getPath()).thenReturn(file.toString());
        when(e.getFile()).thenReturn(vf);
        return e;
    }
}
