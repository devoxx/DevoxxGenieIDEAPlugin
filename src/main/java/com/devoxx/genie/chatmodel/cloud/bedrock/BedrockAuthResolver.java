package com.devoxx.genie.chatmodel.cloud.bedrock;

import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

import java.util.concurrent.CompletableFuture;

public class BedrockAuthResolver {

    /**
     * System property read by the AWS SDK v2 Bedrock/Bedrock Runtime generated client builders
     * (equivalent to the {@code AWS_BEARER_TOKEN_BEDROCK} environment variable). When this property
     * is present, the SDK automatically promotes {@code httpBearerAuth} above {@code sigv4} in the
     * auth scheme preference list and uses the bearer token for signing — which is exactly what we
     * need for {@link AwsBedrockAuthMode#BEARER_TOKEN}. Merely calling
     * {@code builder.tokenProvider(...)} installs a token identity provider but does not change the
     * scheme preference, so the SDK still falls back to SigV4 and the default credentials chain.
     */
    static final String AWS_BEARER_TOKEN_BEDROCK_PROPERTY = "aws.bearerTokenBedrock";

    public @NotNull BedrockRuntimeClientBuilder configure(@NotNull BedrockRuntimeClientBuilder builder) {
        syncBearerTokenSystemProperty();
        return configureAuth(builder.region(getRegion()));
    }

    public @NotNull BedrockRuntimeAsyncClientBuilder configure(@NotNull BedrockRuntimeAsyncClientBuilder builder) {
        syncBearerTokenSystemProperty();
        return configureAuth(builder.region(getRegion()));
    }

    public @NotNull BedrockClientBuilder configure(@NotNull BedrockClientBuilder builder) {
        syncBearerTokenSystemProperty();
        return configureAuth(builder.region(getRegion()));
    }

    /**
     * Keep the {@value #AWS_BEARER_TOKEN_BEDROCK_PROPERTY} system property in sync with the current
     * Bedrock auth mode. Set right before every client is built so that switching modes in settings
     * never leaves a stale token behind.
     */
    void syncBearerTokenSystemProperty() {
        if (getAuthMode() == AwsBedrockAuthMode.BEARER_TOKEN) {
            String bearerToken = DevoxxGenieStateService.getInstance().getAwsBearerToken();
            if (bearerToken != null && !bearerToken.isEmpty()) {
                System.setProperty(AWS_BEARER_TOKEN_BEDROCK_PROPERTY, bearerToken);
                return;
            }
        }
        System.clearProperty(AWS_BEARER_TOKEN_BEDROCK_PROPERTY);
    }

    public @NotNull Region getRegion() {
        return Region.of(DevoxxGenieStateService.getInstance().getAwsRegion());
    }

    public @NotNull AwsBedrockAuthMode getAuthMode() {
        AwsBedrockAuthMode authMode = DevoxxGenieStateService.getInstance().getAwsBedrockAuthMode();
        return authMode != null ? authMode : AwsBedrockAuthMode.defaultMode();
    }

    public @NotNull AwsCredentialsProvider getCredentialsProvider() {
        return switch (getAuthMode()) {
            case ACCESS_KEY -> StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    DevoxxGenieStateService.getInstance().getAwsAccessKeyId(),
                    DevoxxGenieStateService.getInstance().getAwsSecretKey()
            ));
            case PROFILE -> ProfileCredentialsProvider.create(DevoxxGenieStateService.getInstance().getAwsProfileName());
            case BEARER_TOKEN -> throw new IllegalStateException("Bearer token auth does not use AWS credentials.");
        };
    }

    public @NotNull IdentityProvider<TokenIdentity> getTokenProvider() {
        String bearerToken = DevoxxGenieStateService.getInstance().getAwsBearerToken();
        return new IdentityProvider<>() {
            @Override
            public Class<TokenIdentity> identityType() {
                return TokenIdentity.class;
            }

            @Override
            public CompletableFuture<TokenIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(TokenIdentity.create(bearerToken));
            }
        };
    }

    private @NotNull BedrockRuntimeClientBuilder configureAuth(@NotNull BedrockRuntimeClientBuilder builder) {
        return switch (getAuthMode()) {
            case ACCESS_KEY, PROFILE -> builder.credentialsProvider(getCredentialsProvider());
            case BEARER_TOKEN -> builder.tokenProvider(getTokenProvider());
        };
    }

    private @NotNull BedrockRuntimeAsyncClientBuilder configureAuth(@NotNull BedrockRuntimeAsyncClientBuilder builder) {
        return switch (getAuthMode()) {
            case ACCESS_KEY, PROFILE -> builder.credentialsProvider(getCredentialsProvider());
            case BEARER_TOKEN -> builder.tokenProvider(getTokenProvider());
        };
    }

    private @NotNull BedrockClientBuilder configureAuth(@NotNull BedrockClientBuilder builder) {
        return switch (getAuthMode()) {
            case ACCESS_KEY, PROFILE -> builder.credentialsProvider(getCredentialsProvider());
            case BEARER_TOKEN -> builder.tokenProvider(getTokenProvider());
        };
    }
}
