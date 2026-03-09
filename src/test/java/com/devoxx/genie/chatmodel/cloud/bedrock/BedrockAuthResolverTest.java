package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BedrockAuthResolverTest extends AbstractLightPlatformTestCase {
    private DevoxxGenieStateService settingsStateMock;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        settingsStateMock = mock(DevoxxGenieStateService.class);
        when(settingsStateMock.getAwsRegion()).thenReturn("us-east-1");
        when(settingsStateMock.getAwsAccessKeyId()).thenReturn("access-key");
        when(settingsStateMock.getAwsSecretKey()).thenReturn("secret-key");
        when(settingsStateMock.getAwsProfileName()).thenReturn("bedrock-profile");
        when(settingsStateMock.getAwsBearerToken()).thenReturn("bedrock-token");
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.ACCESS_KEY);

        ServiceContainerUtil.replaceService(
                ApplicationManager.getApplication(),
                DevoxxGenieStateService.class,
                settingsStateMock,
                getTestRootDisposable()
        );
    }

    @Test
    void shouldResolveStaticCredentialsForAccessKeyMode() {
        BedrockAuthResolver resolver = new BedrockAuthResolver();

        AwsCredentialsProvider credentialsProvider = resolver.getCredentialsProvider();

        assertThat(credentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("access-key");
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("secret-key");
    }

    @Test
    void shouldResolveProfileCredentialsForProfileMode() {
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.PROFILE);

        BedrockAuthResolver resolver = new BedrockAuthResolver();

        assertThat(resolver.getCredentialsProvider()).isInstanceOf(ProfileCredentialsProvider.class);
    }

    @Test
    void shouldResolveBearerTokenForBearerMode() {
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.BEARER_TOKEN);

        BedrockAuthResolver resolver = new BedrockAuthResolver();

        TokenIdentity tokenIdentity = resolver.getTokenProvider().resolveIdentity().join();
        assertThat(tokenIdentity.token()).isEqualTo("bedrock-token");
    }
}
