package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DockerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Image;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChromeDBValidatorTest {

    @Mock
    private DockerClient dockerClient;

    @Mock
    private PingCmd pingCmd;

    @Mock
    private ListImagesCmd listImagesCmd;

    @Mock
    private ListContainersCmd listContainersCmd;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private Application application;

    private MockedStatic<DockerUtil> dockerUtilStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<ApplicationManager> appManagerStatic;

    private ChromeDBValidator validator;

    @BeforeEach
    void setUp() {
        dockerUtilStatic = mockStatic(DockerUtil.class);
        dockerUtilStatic.when(DockerUtil::getDockerClient).thenReturn(dockerClient);

        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        appManagerStatic = mockStatic(ApplicationManager.class);
        appManagerStatic.when(ApplicationManager::getApplication).thenReturn(application);

        when(stateService.getIndexerPort()).thenReturn(8000);

        validator = new ChromeDBValidator();
    }

    @AfterEach
    void tearDown() {
        dockerUtilStatic.close();
        stateServiceStatic.close();
        appManagerStatic.close();
    }

    @Test
    void isValid_whenDockerNotRunning_returnsFalse() {
        when(dockerClient.pingCmd()).thenReturn(pingCmd);
        when(pingCmd.exec()).thenThrow(new RuntimeException("Docker not running"));

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Docker is not running. Please start Docker first.");
    }

    @Test
    void isValid_whenChromaDBImageNotPresent_returnsFalse() {
        when(dockerClient.pingCmd()).thenReturn(pingCmd);
        when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);
        when(listImagesCmd.exec()).thenReturn(Collections.emptyList());

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("ChromaDB Docker image not found");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.PULL_CHROMA_DB);
    }

    @Test
    void isValid_whenContainerNotFound_returnsFalse() {
        setupDockerRunningWithChromaImage();
        when(listContainersCmd.exec()).thenReturn(Collections.emptyList());

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("ChromaDB container not found");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.START_CHROMA_DB);
    }

    @Test
    void isValid_whenContainerNotRunning_returnsFalse() {
        setupDockerRunningWithChromaImage();
        Container container = mock(Container.class);
        when(container.getState()).thenReturn("exited");
        when(listContainersCmd.exec()).thenReturn(List.of(container));

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("ChromaDB container is not running");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.START_CHROMA_DB);
    }

    @Test
    void isValid_whenContainerRunningOnWrongPort_returnsFalse() {
        setupDockerRunningWithChromaImage();
        Container container = mock(Container.class);
        when(container.getState()).thenReturn("running");
        ContainerPort port = mock(ContainerPort.class);
        when(port.getPublicPort()).thenReturn(9999);
        when(container.getPorts()).thenReturn(new ContainerPort[]{port});
        when(listContainersCmd.exec()).thenReturn(List.of(container));

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).contains("ChromaDB container not running on configured port");
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.START_CHROMA_DB);
    }

    @Test
    void isValid_whenEverythingIsCorrect_returnsTrue() {
        setupDockerRunningWithChromaImage();
        Container container = mock(Container.class);
        when(container.getState()).thenReturn("running");
        ContainerPort port = mock(ContainerPort.class);
        when(port.getPublicPort()).thenReturn(8000);
        when(container.getPorts()).thenReturn(new ContainerPort[]{port});
        when(listContainersCmd.exec()).thenReturn(List.of(container));

        boolean result = validator.isValid();

        assertThat(result).isTrue();
        assertThat(validator.getMessage()).isEqualTo("ChromaDB v0.6.2 is running");
    }

    @Test
    void isValid_whenExceptionThrown_returnsFalse() {
        dockerUtilStatic.when(DockerUtil::getDockerClient).thenThrow(new RuntimeException("Unexpected error"));

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).contains("Failed to verify ChromaDB status");
    }

    @Test
    void getCommand_returnsChromaDBType() {
        assertThat(validator.getCommand()).isEqualTo(ValidatorType.CHROMADB);
    }

    @Test
    void getAction_defaultIsOK() {
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }

    @Test
    void isValid_whenContainerStateIsRunningCaseInsensitive_returnsTrue() {
        setupDockerRunningWithChromaImage();
        Container container = mock(Container.class);
        when(container.getState()).thenReturn("Running");
        ContainerPort port = mock(ContainerPort.class);
        when(port.getPublicPort()).thenReturn(8000);
        when(container.getPorts()).thenReturn(new ContainerPort[]{port});
        when(listContainersCmd.exec()).thenReturn(List.of(container));

        boolean result = validator.isValid();

        assertThat(result).isTrue();
    }

    private void setupDockerRunningWithChromaImage() {
        when(dockerClient.pingCmd()).thenReturn(pingCmd);
        when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);

        Image chromaImage = mock(Image.class);
        when(chromaImage.getRepoTags()).thenReturn(new String[]{"chromadb/chroma:latest"});
        when(listImagesCmd.exec()).thenReturn(List.of(chromaImage));

        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
        when(listContainersCmd.withNameFilter(any())).thenReturn(listContainersCmd);
    }
}
