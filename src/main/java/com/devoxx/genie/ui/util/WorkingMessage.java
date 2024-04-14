package com.devoxx.genie.ui.util;

public class WorkingMessage {

    private WorkingMessage() {
    }

    public static final String[] WORKING_MESSAGES = {
        "Summoning the local wizardry of LLM",
        "Unleashing the local genie of LLM",
        "Spinning the local sorcery wheel of LLM",
        "Firing up the local LLM cauldron",
        "Rolling out the local LLM magic carpet",
        "Setting off the local LLM fireworks",
        "Conducting a local LLM spellcasting session",
        "Dancing with the local LLM spirits",
        "Cooking up some local LLM alchemy",
        "Waving the local LLM magic wand",
        "Killing kittens to appease the LLM gods",
        "Sacrificing a goat to the LLM demons",
        "Burning incense to the LLM spirits",
        "Chanting to the LLM deities",
        "Praying to the LLM gods",
    };

    public static String getWorkingMessage() {
        return WORKING_MESSAGES[(int) (Math.random() * WORKING_MESSAGES.length)] + "... please be patient!";
    }
}
