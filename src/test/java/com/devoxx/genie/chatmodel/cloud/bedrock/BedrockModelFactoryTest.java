package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BedrockModelFactoryTest extends AbstractLightPlatformTestCase {
    private static final String DUMMY_AWS_ACCESS_KEY = "dummy-aws-api-key";
    private static final String DUMMY_AWS_SECRET_KEY = "dummy-aws-secret-key";
    private static final String US_EAST_1 = "us-east-1";
    private static final String US_WEST_2 = "us-west-2";

    private DevoxxGenieStateService settingsStateMock;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Mock SettingsState
        settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getAwsAccessKeyId()).thenReturn(DUMMY_AWS_ACCESS_KEY);
        when(settingsStateMock.getAwsSecretKey()).thenReturn(DUMMY_AWS_SECRET_KEY);
        when(settingsStateMock.getAwsRegion()).thenReturn(US_EAST_1);

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());
    }

    @Test
    public void getModels() {
        BedrockModelFactory factory = new BedrockModelFactory();
        assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> models = factory.getModels();
        assertThat(models).size().isGreaterThan(2);
    }

    @Test
    public void getRegion() {
        BedrockModelFactory factory = new BedrockModelFactory();
        assertThat(factory.getRegion().toString()).isEqualTo(US_EAST_1);

        when(settingsStateMock.getAwsRegion()).thenReturn(US_WEST_2);
        assertThat(factory.getRegion().toString()).isEqualTo(US_WEST_2);
    }

    @Test
    public void getCredentialsProvider() {
        BedrockModelFactory factory = new BedrockModelFactory();
        AwsCredentialsProvider awsCredentialsProvider = factory.getCredentialsProvider();

        assertThat(awsCredentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo(DUMMY_AWS_SECRET_KEY);
        assertThat(awsCredentialsProvider.resolveCredentials().accessKeyId()).isEqualTo(DUMMY_AWS_ACCESS_KEY);
    }
}
