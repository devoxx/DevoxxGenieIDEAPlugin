package com.devoxx.genie.service;

import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OllamaModelServiceTest {

    @Test
    public void testGetModels() throws IOException {

        // Create an instance of the OllamaModelService with the mocked OkHttpClient
        OllamaModelEntryDTO[] models = new OllamaService().getModels();
        assertNotNull(models);
        assertTrue(models.length > 0);
    }
}
