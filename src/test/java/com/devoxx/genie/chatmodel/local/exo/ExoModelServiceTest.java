package com.devoxx.genie.chatmodel.local.exo;

import com.devoxx.genie.model.exo.ExoModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import okhttp3.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExoModelServiceTest {

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<HttpClientProvider> mockedHttpClient;
    private DevoxxGenieStateService mockState;
    private OkHttpClient mockClient;

    private static final String MODELS_RESPONSE = """
            {
                "object": "list",
                "data": [
                    {
                        "id": "mlx-community/Llama-3.2-1B-Instruct-4bit",
                        "name": "Llama-3.2-1B-Instruct-4bit",
                        "context_length": 131072,
                        "storage_size_megabytes": 696,
                        "supports_tensor": true,
                        "family": "llama",
                        "quantization": "4bit",
                        "base_model": "Llama 3.2 1B Instruct",
                        "capabilities": ["text"]
                    },
                    {
                        "id": "mlx-community/MiniMax-M2.5-6bit",
                        "name": "MiniMax-M2.5-6bit",
                        "context_length": 196608,
                        "storage_size_megabytes": 173000,
                        "supports_tensor": true,
                        "family": "minimax",
                        "quantization": "6bit",
                        "base_model": "MiniMax M2.5",
                        "capabilities": ["text", "thinking"]
                    }
                ]
            }
            """;

    private static final String STATE_NO_INSTANCES = """
            {
                "instances": {},
                "runners": {}
            }
            """;

    private static final String STATE_WITH_INSTANCE = """
            {
                "instances": {
                    "abc-123": {
                        "MlxRingInstance": {
                            "instanceId": "abc-123",
                            "shardAssignments": {
                                "modelId": "mlx-community/MiniMax-M2.5-6bit"
                            }
                        }
                    }
                },
                "runners": {
                    "runner-1": {"RunnerReady": {}}
                }
            }
            """;

    private static final String PREVIEW_RESPONSE = """
            {
                "previews": [
                    {
                        "model_id": "mlx-community/MiniMax-M2.5-6bit",
                        "sharding": "Pipeline",
                        "instance_meta": "MlxRing",
                        "instance": {"MlxRingInstance": {"instanceId": "new-123"}},
                        "memory_delta_by_node": {},
                        "error": null
                    }
                ]
            }
            """;

    private static final String PREVIEW_NO_VALID = """
            {
                "previews": [
                    {
                        "model_id": "mlx-community/MiniMax-M2.5-6bit",
                        "sharding": "Pipeline",
                        "instance_meta": "MlxRing",
                        "instance": null,
                        "memory_delta_by_node": null,
                        "error": "No cycles found with sufficient memory"
                    }
                ]
            }
            """;

    @BeforeEach
    void setUp() {
        mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getExoModelUrl()).thenReturn("http://localhost:52415/");

        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(mockState);

        mockClient = mock(OkHttpClient.class);
        mockedHttpClient = Mockito.mockStatic(HttpClientProvider.class);
        mockedHttpClient.when(HttpClientProvider::getClient).thenReturn(mockClient);
    }

    @AfterEach
    void tearDown() {
        if (mockedStateService != null) mockedStateService.close();
        if (mockedHttpClient != null) mockedHttpClient.close();
    }

    private Call mockCall(String responseBody, int code) throws IOException {
        Call mockCall = mock(Call.class);
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:52415/models").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("OK")
                .body(ResponseBody.create(responseBody, MediaType.parse("application/json")))
                .build();
        when(mockCall.execute()).thenReturn(response);
        return mockCall;
    }

    @Test
    void getModelsShouldParseResponseCorrectly() throws IOException {
        Call mockCall = mockCall(MODELS_RESPONSE, 200);
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);

        ExoModelService service = new ExoModelService();
        ExoModelEntryDTO[] models = service.getModels();

        assertThat(models).hasSize(2);
        assertThat(models[0].getId()).isEqualTo("mlx-community/Llama-3.2-1B-Instruct-4bit");
        assertThat(models[0].getName()).isEqualTo("Llama-3.2-1B-Instruct-4bit");
        assertThat(models[0].getContextLength()).isEqualTo(131072);
        assertThat(models[0].getFamily()).isEqualTo("llama");
        assertThat(models[1].getId()).isEqualTo("mlx-community/MiniMax-M2.5-6bit");
        assertThat(models[1].getContextLength()).isEqualTo(196608);
    }

    @Test
    void getModelsShouldThrowOnHttpError() throws IOException {
        Call mockCall = mockCall("error", 500);
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);

        ExoModelService service = new ExoModelService();

        assertThatThrownBy(service::getModels)
                .isInstanceOf(IOException.class);
    }

    @Test
    void ensureInstanceShouldSkipWhenInstanceAlreadyExists() throws IOException {
        // State check returns existing instance for this model
        Call stateCall = mockCall(STATE_WITH_INSTANCE, 200);
        when(mockClient.newCall(any(Request.class))).thenReturn(stateCall);

        ExoModelService service = new ExoModelService();
        // Should return without calling preview/create
        service.ensureInstance("mlx-community/MiniMax-M2.5-6bit");

        // Only 1 call should be made (state check), not 3 (state + preview + create)
        Mockito.verify(mockClient, Mockito.times(1)).newCall(any(Request.class));
    }

    @Test
    void ensureInstanceShouldThrowWhenNoValidPlacement() throws IOException {
        // First call: state check returns no instances
        Call stateCall = mockCall(STATE_NO_INSTANCES, 200);
        // Second call: preview returns no valid placements
        Call previewCall = mockCall(PREVIEW_NO_VALID, 200);

        when(mockClient.newCall(any(Request.class)))
                .thenReturn(stateCall)
                .thenReturn(previewCall);

        ExoModelService service = new ExoModelService();

        assertThatThrownBy(() -> service.ensureInstance("mlx-community/MiniMax-M2.5-6bit"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No valid placement found");
    }
}
