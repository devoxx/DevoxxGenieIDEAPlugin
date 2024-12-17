package com.devoxx.genie.chatmodel.local;

import java.io.IOException;

public interface LocalLLMProvider {
    Object getModels() throws IOException;
}
