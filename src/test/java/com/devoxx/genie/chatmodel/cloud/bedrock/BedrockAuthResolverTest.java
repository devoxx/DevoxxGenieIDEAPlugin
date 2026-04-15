package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

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

        System.clearProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        System.clearProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY);
        super.tearDown();
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

    @Test
    void shouldPublishBearerTokenSystemPropertyWhenConfiguringRuntimeClient() {
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.BEARER_TOKEN);

        BedrockAuthResolver resolver = new BedrockAuthResolver();
        resolver.configure(BedrockRuntimeClient.builder());

        // The AWS SDK v2 Bedrock Runtime builder only selects HTTP bearer auth (vs. falling back to
        // SigV4 + default credentials chain) when aws.bearerTokenBedrock / AWS_BEARER_TOKEN_BEDROCK
        // is set. Ensure the resolver publishes the token on the system property each time a client
        // is configured so bearer mode actually works end-to-end.
        assertThat(System.getProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY))
                .isEqualTo("bedrock-token");
    }

    @Test
    void shouldPublishBearerTokenSystemPropertyWhenConfiguringAsyncRuntimeClient() {
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.BEARER_TOKEN);

        BedrockAuthResolver resolver = new BedrockAuthResolver();
        resolver.configure(BedrockRuntimeAsyncClient.builder());

        assertThat(System.getProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY))
                .isEqualTo("bedrock-token");
    }

    @Test
    void shouldPublishBearerTokenSystemPropertyWhenConfiguringControlPlaneClient() {
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.BEARER_TOKEN);

        BedrockAuthResolver resolver = new BedrockAuthResolver();
        resolver.configure(BedrockClient.builder());

        assertThat(System.getProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY))
                .isEqualTo("bedrock-token");
    }

    @Test
    void shouldClearBearerTokenSystemPropertyWhenSwitchingAwayFromBearerMode() {
        System.setProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY, "stale-token");
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.ACCESS_KEY);

        BedrockAuthResolver resolver = new BedrockAuthResolver();
        resolver.configure(BedrockRuntimeClient.builder());

        // Switching back to access-key / profile auth must not leave the bearer token sys prop set
        // — otherwise the SDK would keep preferring HTTP bearer auth for future Bedrock calls.
        assertThat(System.getProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY)).isNull();
    }

    @Test
    void shouldClearBearerTokenSystemPropertyWhenBearerModeSelectedButTokenEmpty() {
        System.setProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY, "stale-token");
        when(settingsStateMock.getAwsBedrockAuthMode()).thenReturn(AwsBedrockAuthMode.BEARER_TOKEN);
        when(settingsStateMock.getAwsBearerToken()).thenReturn("");

        BedrockAuthResolver resolver = new BedrockAuthResolver();
        resolver.syncBearerTokenSystemProperty();

        assertThat(System.getProperty(BedrockAuthResolver.AWS_BEARER_TOKEN_BEDROCK_PROPERTY)).isNull();
    }
}
