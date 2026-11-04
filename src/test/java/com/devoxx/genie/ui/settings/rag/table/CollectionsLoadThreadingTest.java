package com.devoxx.genie.ui.settings.rag.table;

import com.devoxx.genie.service.chromadb.ChromaDBManager;
import com.devoxx.genie.service.chromadb.model.ChromaCollection;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** TASK-267: listing and counting collections calls ChromaDB, so it must stay off the EDT. */
class CollectionsLoadThreadingTest {

    private final Application application = mock(Application.class);
    private final ModalityState anyModality = mock(ModalityState.class);
    private final ChromaDBManager chroma = mock(ChromaDBManager.class);
    private final DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Name", "Count", "Action"}, 0);

    private MockedStatic<ApplicationManager> applicationManager;
    private MockedStatic<ChromaDBManager> chromaStatic;
    private MockedStatic<DevoxxGenieStateService> stateStatic;

    private ButtonEditor editor;

    @BeforeEach
    void setUp() throws IOException {
        applicationManager = mockStatic(ApplicationManager.class);
        applicationManager.when(ApplicationManager::getApplication).thenReturn(application);
        when(application.getAnyModalityState()).thenReturn(anyModality);
        chromaStatic = mockStatic(ChromaDBManager.class);
        chromaStatic.when(() -> ChromaDBManager.getInstance(any())).thenReturn(chroma);
        stateStatic = mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);
        when(state.getRagEnabled()).thenReturn(true);
        when(chroma.listCollections()).thenReturn(List.of(new ChromaCollection("id-1", "my-project", 768, "tenant", "database")));
        when(chroma.countDocuments("id-1")).thenReturn(42);

        editor = new ButtonEditor(mock(Project.class), null, tableModel);
    }

    @AfterEach
    void tearDown() {
        stateStatic.close();
        chromaStatic.close();
        applicationManager.close();
    }

    @Test
    void chromaIsOnlyCalledFromThePooledTask() throws IOException {
        editor.safeLoadCollections();

        verify(chroma, never()).listCollections();
        verify(application, never()).invokeLater(any());

        pooledTask().run();

        verify(chroma).listCollections();
        verify(chroma).countDocuments("id-1");
    }

    @Test
    void rowsAppearOnlyOnceTheEdtRunsTheUpdate() {
        editor.safeLoadCollections();
        pooledTask().run();

        assertThat(tableModel.getRowCount()).isZero();

        edtTask().run();

        assertThat(tableModel.getRowCount()).isEqualTo(1);
        assertThat(tableModel.getValueAt(0, 0)).isEqualTo("my-project");
        assertThat(tableModel.getValueAt(0, 1)).isEqualTo(42);
    }

    @Test
    void chromaBeingUnreachableEmptiesTheTable() throws IOException {
        tableModel.addRow(new Object[]{"old", 1, "Delete"});
        when(chroma.listCollections()).thenThrow(new IOException("connection refused"));

        editor.safeLoadCollections();
        pooledTask().run();
        edtTask().run();

        assertThat(tableModel.getRowCount()).isZero();
    }

    @Test
    void aSlowerEarlierLoadDoesNotOverwriteALaterOne() throws IOException {
        editor.safeLoadCollections();
        Runnable firstLoad = pooledTask();
        firstLoad.run();
        Runnable firstUpdate = edtTask();

        when(chroma.listCollections()).thenReturn(List.of());
        clearInvocations(application);
        editor.safeLoadCollections();
        pooledTask().run();
        edtTask().run();

        firstUpdate.run();

        assertThat(tableModel.getRowCount()).isZero();
    }

    private Runnable pooledTask() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(application).executeOnPooledThread(task.capture());
        return task.getValue();
    }

    /** The update must use ModalityState.any(), or it waits for the modal Settings dialog to close. */
    private Runnable edtTask() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(application).invokeLater(task.capture(), eq(anyModality));
        return task.getValue();
    }
}
