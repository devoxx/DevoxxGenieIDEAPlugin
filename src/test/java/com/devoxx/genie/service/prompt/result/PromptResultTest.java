package com.devoxx.genie.service.prompt.result;

import com.devoxx.genie.model.request.ChatMessageContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PromptResultTest {

    @Mock
    private ChatMessageContext context;
    
    @Mock
    private Throwable error;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn("test-context-id");
        when(error.getMessage()).thenReturn("Test error message");
    }

    @Test
    public void testSuccess() {
        // Create success result
        PromptResult result = PromptResult.success(context);
        
        // Verify result has context and no error
        assertEquals(context, result.getContext());
        assertNull(result.getError());
        
        // Verify toString contains expected information
        String resultString = result.toString();
        assertTrue(resultString.contains("successful=true"));
        assertTrue(resultString.contains("error=none"));
        assertTrue(resultString.contains("contextId=test-context-id"));
    }
    
    @Test
    public void testFailure() {
        // Create failure result
        PromptResult result = PromptResult.failure(context, error);
        
        // Verify result has context and error
        assertEquals(context, result.getContext());
        assertEquals(error, result.getError());
        
        // Verify toString contains expected information
        String resultString = result.toString();
        assertTrue(resultString.contains("successful=false"));
        assertTrue(resultString.contains("error=Test error message"));
        assertTrue(resultString.contains("contextId=test-context-id"));
    }
    
    @Test(expected = NullPointerException.class)
    public void testSuccess_WithNullContext() {
        // Should throw NullPointerException
        PromptResult.success(null);
    }
    
    @Test(expected = NullPointerException.class)
    public void testFailure_WithNullContext() {
        // Should throw NullPointerException
        PromptResult.failure(null, error);
    }
    
    @Test(expected = NullPointerException.class)
    public void testFailure_WithNullError() {
        // Should throw NullPointerException
        PromptResult.failure(context, null);
    }
}
