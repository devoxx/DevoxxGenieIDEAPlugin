package com.devoxx.genie.service.requesty;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.chatmodel.cloud.requesty.RequestyService;
import com.devoxx.genie.model.requesty.Data;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestyServiceTest extends AbstractLightPlatformTestCase {

    @Test
    void testGetManagedModelsWithoutApiKey() throws IOException {
        RequestyService requestyService = new RequestyService();
        List<Data> models = requestyService.getModels(null);
        assertThat(models).isNotEmpty();

        models.forEach(model -> {
            assertThat(model).isNotNull();
            assertThat(model.getId()).isNotNull();
            assertThat(model.getContextWindow()).isNotNull();
            assertThat(model.getInputPrice()).isNotNull();
        });

        assertThat(models.stream().map(Data::getId).distinct().count()).isEqualTo(models.size());
        assertThat(models.stream().anyMatch(Data::isChatModel)).isTrue();
    }
}
