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

    public @NotNull BedrockRuntimeClientBuilder configure(@NotNull BedrockRuntimeClientBuilder builder) {
        return configureAuth(builder.region(getRegion()));
    }

    public @NotNull BedrockRuntimeAsyncClientBuilder configure(@NotNull BedrockRuntimeAsyncClientBuilder builder) {
        return configureAuth(builder.region(getRegion()));
    }

    public @NotNull BedrockClientBuilder configure(@NotNull BedrockClientBuilder builder) {
        return configureAuth(builder.region(getRegion()));
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
