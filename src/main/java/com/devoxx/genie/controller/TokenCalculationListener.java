package com.devoxx.genie.controller;

public interface TokenCalculationListener {
    void onTokenCalculationComplete(int tokenCount, int tokenLimit);
}