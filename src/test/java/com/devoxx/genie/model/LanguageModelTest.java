package com.devoxx.genie.model;

import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LanguageModelTest {

    /**
     * Reproduces the Jan "refresh models" crash. Jan's OpenAI-compatible {@code /models}
     * endpoint returns entries with an {@code id} but no {@code name}, so {@code displayName}
     * ends up null. Sorting the resulting models then failed inside
     * {@code ModelVersionComparator} with:
     * {@code Cannot invoke "String.split(String)" because "v1" is null}.
     */
    @Test
    void sortingModelsWithNullDisplayNameDoesNotThrow() {
        List<LanguageModel> models = new ArrayList<>();
        models.add(LanguageModel.builder()
                .provider(ModelProvider.Jan)
                .modelName("mistral-7b")
                .displayName(null)
                .build());
        models.add(LanguageModel.builder()
                .provider(ModelProvider.Jan)
                .modelName("llama3.2-3b")
                .displayName(null)
                .build());

        assertThatCode(() -> models.sort(Comparator.naturalOrder()))
                .doesNotThrowAnyException();

        // Falls back to modelName for ordering, so the list is sorted by id.
        assertThat(models.get(0).getModelName()).isEqualTo("llama3.2-3b");
        assertThat(models.get(1).getModelName()).isEqualTo("mistral-7b");
    }

    @Test
    void sortingHandlesMixOfNullAndNonNullDisplayNames() {
        List<LanguageModel> models = new ArrayList<>();
        models.add(LanguageModel.builder().modelName("z-model").displayName(null).build());
        models.add(LanguageModel.builder().modelName("a-model").displayName("Claude 3 Opus").build());

        assertThatCode(() -> models.sort(Comparator.naturalOrder()))
                .doesNotThrowAnyException();
    }
}
