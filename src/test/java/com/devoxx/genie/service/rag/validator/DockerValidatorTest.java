package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.util.DockerUtil;
import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DockerValidatorTest {

    @Mock
    private DockerClient dockerClient;

    private MockedStatic<DockerUtil> dockerUtilStatic;

    private DockerValidator validator;

    @BeforeEach
    void setUp() {
        dockerUtilStatic = mockStatic(DockerUtil.class);
        validator = new DockerValidator();
    }

    @AfterEach
    void tearDown() {
        dockerUtilStatic.close();
    }

    @Test
    void isValid_whenDockerAvailable_returnsTrue() {
        dockerUtilStatic.when(DockerUtil::getDockerClient).thenReturn(dockerClient);

        boolean result = validator.isValid();

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenDockerNotInstalled_returnsFalse() {
        dockerUtilStatic.when(DockerUtil::getDockerClient).thenThrow(new RuntimeException("Docker not found"));

        boolean result = validator.isValid();

        assertThat(result).isFalse();
    }

    @Test
    void getErrorMessage_whenDockerNotInstalled_returnsMessage() {
        dockerUtilStatic.when(DockerUtil::getDockerClient).thenThrow(new RuntimeException("Docker not found"));

        validator.isValid();

        assertThat(validator.getErrorMessage()).isEqualTo("Docker is not installed");
    }

    @Test
    void getErrorMessage_beforeValidation_returnsNull() {
        assertThat(validator.getErrorMessage()).isNull();
    }

    @Test
    void getMessage_returnsDockerInstalledMessage() {
        assertThat(validator.getMessage()).isEqualTo("Docker is installed");
    }

    @Test
    void getCommand_returnsDockerType() {
        assertThat(validator.getCommand()).isEqualTo(ValidatorType.DOCKER);
    }

    @Test
    void getAction_returnsDefaultOK() {
        assertThat(validator.getAction()).isEqualTo(ValidationActionType.OK);
    }

    @Test
    void isValid_whenDockerClientThrowsIOException_returnsFalse() {
        dockerUtilStatic.when(DockerUtil::getDockerClient).thenThrow(new RuntimeException("Connection refused"));

        boolean result = validator.isValid();

        assertThat(result).isFalse();
        assertThat(validator.getErrorMessage()).isEqualTo("Docker is not installed");
    }
}
