package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.debug.RawTrafficListenerService;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.common.ThreadLeakTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BedrockModelFactoryTest extends AbstractLightPlatformTestCase {
    private static final String DUMMY_AWS_ACCESS_KEY = "dummy-aws-api-key";
    private static final String DUMMY_AWS_SECRET_KEY = "dummy-aws-secret-key";
    private static final String US_EAST_1 = "us-east-1";
    private static final String US_WEST_2 = "us-west-2";

    private DevoxxGenieStateService settingsStateMock;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Building a BedrockRuntimeClient starts the AWS SDK's JVM-wide singleton
        // "idle-connection-reaper" daemon thread, which deliberately outlives the individual
        // clients created by the factory. Exempt it from the platform's thread-leak assertion.
        // Registered on the application disposable (not the test root one) because the leak
        // check runs after the test root disposable has already been disposed.
        ThreadLeakTracker.longRunningThreadCreated(ApplicationManager.getApplication(), "idle-connection-reaper");

        // Mock SettingsState
        settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getAwsAccessKeyId()).thenReturn(DUMMY_AWS_ACCESS_KEY);
        when(settingsStateMock.getAwsSecretKey()).thenReturn(DUMMY_AWS_SECRET_KEY);
        when(settingsStateMock.getAwsRegion()).thenReturn(US_EAST_1);
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.ACCESS_KEY);

        // Replace the service instance with the mock
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DevoxxGenieStateService.class, settingsStateMock, getTestRootDisposable());

        LLMModelRegistryService modelRegistryServiceMock = mock(LLMModelRegistryService.class);
        when(modelRegistryServiceMock.getModels()).thenReturn(List.of(
            model("anthropic.claude-sonnet-4-20250514-v1:0"),
            model("anthropic.claude-3-7-sonnet-20250219-v1:0"),
            model("mistral.mistral-large-2402-v1:0")
        ));
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), LLMModelRegistryService.class, modelRegistryServiceMock, getTestRootDisposable());
    }

    @Test
    void getModels() {
        BedrockModelFactory factory = new BedrockModelFactory();
        assertThat(factory.getModels()).isNotEmpty();

        List<LanguageModel> models = factory.getModels();
        assertThat(models).size().isGreaterThan(2);
    }

    @Test
    void getRegion() {
        BedrockModelFactory factory = new BedrockModelFactory();
        assertThat(factory.getRegion().toString()).isEqualTo(US_EAST_1);

        when(settingsStateMock.getAwsRegion()).thenReturn(US_WEST_2);
        assertThat(factory.getRegion().toString()).isEqualTo(US_WEST_2);
    }

    @Test
    void getCredentialsProviderForBasicCredentials() {
        BedrockModelFactory factory = new BedrockModelFactory();
        AwsCredentialsProvider awsCredentialsProvider = factory.getCredentialsProvider();
        assertThat(awsCredentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo(DUMMY_AWS_SECRET_KEY);
        assertThat(awsCredentialsProvider.resolveCredentials().accessKeyId()).isEqualTo(DUMMY_AWS_ACCESS_KEY);
    }

    @Test
    void getCredentialsProviderForProfile() {
        BedrockModelFactory factory = new BedrockModelFactory();
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.PROFILE);
        when(settingsStateMock.getAwsProfileName()).thenReturn("bedrock");
        AwsCredentialsProvider awsCredentialsProvider = factory.getCredentialsProvider();
        assertThat(awsCredentialsProvider).isInstanceOf(ProfileCredentialsProvider.class);
    }

    @Test
    void createChatModelAttachesRawTrafficListenerWhenEnabled() {
        when(settingsStateMock.getMcpEnabled()).thenReturn(false);
        when(settingsStateMock.getAgentModeEnabled()).thenReturn(false);
        when(settingsStateMock.getRawRequestResponseLoggingEnabled()).thenReturn(true);
        when(settingsStateMock.getShouldEnableAWSRegionalInference()).thenReturn(false);

        BedrockModelFactory factory = new BedrockModelFactory();

        CustomChatModel anthropicModel = new CustomChatModel();
        anthropicModel.setModelName("anthropic.claude-sonnet-4-20250514-v1:0");
        assertThat(factory.createChatModel(anthropicModel).listeners())
                .anyMatch(RawTrafficListenerService.class::isInstance);
        assertThat(factory.createStreamingChatModel(anthropicModel).listeners())
                .anyMatch(RawTrafficListenerService.class::isInstance);

        CustomChatModel mistralModel = new CustomChatModel();
        mistralModel.setModelName("mistral.mistral-large-2402-v1:0");
        assertThat(factory.createChatModel(mistralModel).listeners())
                .anyMatch(RawTrafficListenerService.class::isInstance);
    }

    private static LanguageModel model(String modelName) {
        return LanguageModel.builder()
            .provider(ModelProvider.Bedrock)
            .modelName(modelName)
            .displayName(modelName)
            .inputCost(1)
            .outputCost(1)
            .inputMaxTokens(200_000)
            .apiKeyUsed(true)
            .build();
    }
}
