package com.devoxx.genie.ui.util;

import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoubleConverter extends Converter<Double> {

    @Override
    public @Nullable Double fromString(@NotNull String value) {
        return Double.parseDouble(value.replace(",", "."));
    }

    @Override
    public @Nullable String toString(@NotNull Double value) {
        return String.format("%,.2f", value);
    }

}
