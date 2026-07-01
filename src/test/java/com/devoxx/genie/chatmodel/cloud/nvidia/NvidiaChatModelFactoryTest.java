package com.devoxx.genie.chatmodel.cloud.nvidia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link NvidiaChatModelFactory#contextWindowFor(String)} — the heuristic that assigns a
 * context window to each dynamically-listed NVIDIA model, since NVIDIA's {@code /v1/models}
 * endpoint carries no context length. Cases cover the representative families and, crucially, the
 * substring-collision edge cases (e.g. an embedder named after a base chat model).
 */
class NvidiaChatModelFactoryTest {

    /** The 128K default must cover the bulk of NVIDIA's instruct LLMs. */
    @ParameterizedTest
    @ValueSource(strings = {
            "meta/llama-3.1-70b-instruct",
            "meta/llama-3.3-70b-instruct",
            "meta/llama-3.2-3b-instruct",
            "nvidia/llama-3.1-nemotron-70b-instruct",
            "nvidia/llama-3.1-nemotron-ultra-253b-v1",
            "nvidia/llama-3.3-nemotron-super-49b-v1.5",
            "qwen/qwen3.5-397b-a17b",
            "deepseek-ai/deepseek-v4-pro",
            "openai/gpt-oss-120b",
            "z-ai/glm-5.1",
            "moonshotai/kimi-k2.6",
            "microsoft/phi-4-multimodal-instruct",
            "mistralai/ministral-14b-instruct-2512",
            "nv-mistralai/mistral-nemo-12b-instruct",
            "google/gemma-3-12b-it"
    })
    void defaultsTo128kForMainstreamInstructModels(String modelId) {
        assertThat(NvidiaChatModelFactory.contextWindowFor(modelId)).isEqualTo(128_000);
    }

    /** Family-specific windows, including the tricky mistral-large v1 (32K) vs v2/v3 (128K) split. */
    @ParameterizedTest
    @CsvSource({
            "mistralai/mistral-large,                       32768",
            "mistralai/mistral-large-2-instruct,            128000",
            "mistralai/mistral-large-3-675b-instruct-2512,  128000",
            "mistralai/mixtral-8x7b-instruct-v0.1,          32768",
            "mistralai/mixtral-8x22b-v0.1,                  65536",
            "mistralai/mistral-7b-instruct-v0.3,            32768",
            "mistralai/codestral-22b-instruct-v0.1,         32768",
            "ai21labs/jamba-1.5-large-instruct,             256000",
            "meta/llama-4-maverick-17b-128e-instruct,       1000000",
            "meta/llama2-70b,                               4096",
            "meta/codellama-70b,                            16384",
            "upstage/solar-10.7b-instruct,                  4096",
            "nvidia/nemotron-4-340b-instruct,               4096",
            "nvidia/nemotron-mini-4b-instruct,              4096",
            "deepseek-ai/deepseek-coder-6.7b-instruct,      16384",
            "bigcode/starcoder2-15b,                        16384",
            "01-ai/yi-large,                                32768",
            "databricks/dbrx-instruct,                      32768",
            "google/gemma-2-2b-it,                          8192",
            "google/gemma-2b,                               8192",
            "google/gemma-3n-e4b-it,                        32768",
            "google/codegemma-7b,                           8192",
            "writer/palmyra-med-70b-32k,                    32768",
            "writer/palmyra-fin-70b-32k,                    32768",
            "nvidia/mistral-nemo-minitron-8b-8k-instruct,   8192",
            "nvidia/llama3-chatqa-1.5-70b,                  8192"
    })
    void mapsKnownFamiliesToTheirWindow(String modelId, int expected) {
        assertThat(NvidiaChatModelFactory.contextWindowFor(modelId)).isEqualTo(expected);
    }

    /**
     * Auxiliary / non-chat models must NOT be mis-bucketed by a base-model name embedded in their
     * id (e.g. {@code nv-embedqa-mistral-7b} contains "mistral-7b"; a reward model contains
     * "340b"). They are checked first and pinned to the modest auxiliary window.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "nvidia/nv-embedqa-mistral-7b-v2",
            "nvidia/llama-3.2-nv-embedqa-1b-v1",
            "nvidia/llama-nemotron-embed-vl-1b-v2",
            "nvidia/llama-3.2-nemoretriever-1b-vlm-embed-v1",
            "baai/bge-m3",
            "snowflake/arctic-embed-l",
            "nvidia/nvclip",
            "nvidia/nemoretriever-parse",
            "nvidia/nemotron-parse",
            "meta/llama-guard-4-12b",
            "nvidia/llama-3.1-nemoguard-8b-content-safety",
            "nvidia/llama-3.1-nemoguard-8b-topic-control",
            "nvidia/nemotron-4-340b-reward",
            "nvidia/gliner-pii",
            "nvidia/riva-translate-4b-instruct",
            "nvidia/ai-synthetic-video-detector",
            "google/deplot",
            "microsoft/kosmos-2",
            "google/diffusiongemma-26b-a4b-it"
    })
    void auxiliaryModelsGetModestWindowAndAreNeverMisBucketed(String modelId) {
        assertThat(NvidiaChatModelFactory.contextWindowFor(modelId)).isEqualTo(8_192);
    }

    /** Whatever NVIDIA lists, every model must get a positive window — never the zero-context bug. */
    @ParameterizedTest
    @ValueSource(strings = {
            "01-ai/yi-large", "abacusai/dracarys-llama-3.1-70b-instruct", "adept/fuyu-8b",
            "ai21labs/jamba-1.5-large-instruct", "aisingapore/sea-lion-7b-instruct", "baai/bge-m3",
            "bigcode/starcoder2-15b", "bytedance/seed-oss-36b-instruct", "databricks/dbrx-instruct",
            "deepseek-ai/deepseek-coder-6.7b-instruct", "deepseek-ai/deepseek-v4-flash",
            "deepseek-ai/deepseek-v4-pro", "google/codegemma-1.1-7b", "google/codegemma-7b",
            "google/deplot", "google/diffusiongemma-26b-a4b-it", "google/gemma-2-2b-it",
            "google/gemma-2b", "google/gemma-3-12b-it", "google/gemma-3-4b-it", "google/gemma-3n-e2b-it",
            "google/gemma-3n-e4b-it", "google/gemma-4-31b-it", "google/recurrentgemma-2b",
            "ibm/granite-3.0-3b-a800m-instruct", "ibm/granite-3.0-8b-instruct", "ibm/granite-34b-code-instruct",
            "ibm/granite-8b-code-instruct", "meta/codellama-70b", "meta/llama-3.1-70b-instruct",
            "meta/llama-3.1-8b-instruct", "meta/llama-3.2-11b-vision-instruct", "meta/llama-3.2-1b-instruct",
            "meta/llama-3.2-3b-instruct", "meta/llama-3.2-90b-vision-instruct", "meta/llama-3.3-70b-instruct",
            "meta/llama-4-maverick-17b-128e-instruct", "meta/llama-guard-4-12b", "meta/llama2-70b",
            "microsoft/kosmos-2", "microsoft/phi-3-vision-128k-instruct", "microsoft/phi-3.5-moe-instruct",
            "microsoft/phi-4-mini-instruct", "microsoft/phi-4-multimodal-instruct", "minimaxai/minimax-m2.7",
            "minimaxai/minimax-m3", "mistralai/codestral-22b-instruct-v0.1", "mistralai/ministral-14b-instruct-2512",
            "mistralai/mistral-7b-instruct-v0.3", "mistralai/mistral-large", "mistralai/mistral-large-2-instruct",
            "mistralai/mistral-large-3-675b-instruct-2512", "mistralai/mistral-medium-3.5-128b",
            "mistralai/mistral-nemotron", "mistralai/mistral-small-4-119b-2603", "mistralai/mixtral-8x22b-v0.1",
            "mistralai/mixtral-8x7b-instruct-v0.1", "moonshotai/kimi-k2.6", "nv-mistralai/mistral-nemo-12b-instruct",
            "nvidia/ai-synthetic-video-detector", "nvidia/cosmos-reason2-8b", "nvidia/embed-qa-4",
            "nvidia/gliner-pii", "nvidia/ising-calibration-1-35b-a3b", "nvidia/llama-3.1-nemoguard-8b-content-safety",
            "nvidia/llama-3.1-nemoguard-8b-topic-control", "nvidia/llama-3.1-nemotron-51b-instruct",
            "nvidia/llama-3.1-nemotron-70b-instruct", "nvidia/llama-3.1-nemotron-nano-8b-v1",
            "nvidia/llama-3.1-nemotron-nano-vl-8b-v1", "nvidia/llama-3.1-nemotron-safety-guard-8b-v3",
            "nvidia/llama-3.1-nemotron-ultra-253b-v1", "nvidia/llama-3.2-nemoretriever-1b-vlm-embed-v1",
            "nvidia/llama-3.2-nv-embedqa-1b-v1", "nvidia/llama-3.3-nemotron-super-49b-v1",
            "nvidia/llama-3.3-nemotron-super-49b-v1.5", "nvidia/llama-nemotron-embed-1b-v2",
            "nvidia/llama-nemotron-embed-vl-1b-v2", "nvidia/llama3-chatqa-1.5-70b",
            "nvidia/mistral-nemo-minitron-8b-8k-instruct", "nvidia/nemoretriever-parse",
            "nvidia/nemotron-3-content-safety", "nvidia/nemotron-3-nano-30b-a3b",
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", "nvidia/nemotron-3-super-120b-a12b",
            "nvidia/nemotron-3-ultra-550b-a55b", "nvidia/nemotron-3.5-content-safety",
            "nvidia/nemotron-4-340b-instruct", "nvidia/nemotron-4-340b-reward",
            "nvidia/nemotron-content-safety-reasoning-4b", "nvidia/nemotron-mini-4b-instruct",
            "nvidia/nemotron-nano-12b-v2-vl", "nvidia/nemotron-nano-3-30b-a3b", "nvidia/nemotron-parse",
            "nvidia/neva-22b", "nvidia/nv-embed-v1", "nvidia/nv-embedcode-7b-v1", "nvidia/nv-embedqa-e5-v5",
            "nvidia/nv-embedqa-mistral-7b-v2", "nvidia/nvclip", "nvidia/nvidia-nemotron-nano-9b-v2",
            "nvidia/riva-translate-4b-instruct", "nvidia/riva-translate-4b-instruct-v1.1", "nvidia/vila",
            "openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3-next-80b-a3b-instruct",
            "qwen/qwen3.5-122b-a10b", "qwen/qwen3.5-397b-a17b", "sarvamai/sarvam-m", "snowflake/arctic-embed-l",
            "stepfun-ai/step-3.5-flash", "stepfun-ai/step-3.7-flash", "stockmark/stockmark-2-100b-instruct",
            "upstage/solar-10.7b-instruct", "writer/palmyra-creative-122b", "writer/palmyra-fin-70b-32k",
            "writer/palmyra-med-70b", "writer/palmyra-med-70b-32k", "z-ai/glm-5.1", "zyphra/zamba2-7b-instruct"
    })
    void everyListedModelGetsPositiveWindow(String modelId) {
        assertThat(NvidiaChatModelFactory.contextWindowFor(modelId)).isPositive();
    }
}
