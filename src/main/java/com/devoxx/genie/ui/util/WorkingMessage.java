package com.devoxx.genie.ui.util;

public class WorkingMessage {

    private WorkingMessage() {
    }

    protected static final String[] WORKING_MESSAGES = {
        "Summoning the wizardry of LLM",
        "Unleashing the genie of LLM",
        "Spinning the sorcery wheel of LLM",
        "Firing up the LLM cauldron",
        "Rolling out the LLM magic carpet",
        "Setting off the LLM fireworks",
        "Conducting a LLM spellcasting session",
        "Dancing with the LLM spirits",
        "Cooking up some LLM alchemy",
        "Waving the LLM magic wand",
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
