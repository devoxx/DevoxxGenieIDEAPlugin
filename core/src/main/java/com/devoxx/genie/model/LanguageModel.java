package com.devoxx.genie.model;

import com.devoxx.genie.model.enumarations.ModelProvider;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

@Data
@Builder
public class LanguageModel implements Comparable<LanguageModel> {

    private ModelProvider provider;
    private String modelName;
    private String displayName;
    private boolean apiKeyUsed;
    private double inputCost;
    private double outputCost;
    private int contextWindow;

    public LanguageModel() {
        this(ModelProvider.OpenAI, "", "", false, 0.0, 0.0, 0);
    }

    public LanguageModel(ModelProvider provider,
                         String modelName,
                         String displayName,
                         boolean apiKeyUsed,
                         double inputCost,
                         double outputCost,
                         int contextWindow) {
        this.provider = provider;
        this.modelName = modelName;
        this.displayName = displayName;
        this.apiKeyUsed = apiKeyUsed;
        this.inputCost = inputCost;
        this.outputCost = outputCost;
        this.contextWindow = contextWindow;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public int compareTo(@NotNull LanguageModel other) {
        return new ModelVersionComparator().compare(this.displayName, other.displayName);
    }

    private static class ModelVersionComparator implements Comparator<String> {
        @Override
        public int compare(String v1, String v2) {
            String[] parts1 = v1.split(" ");
            String[] parts2 = v2.split(" ");

            // Compare model names
            int modelNameCompare = parts1[0].compareTo(parts2[0]);
            if (modelNameCompare != 0) return modelNameCompare;

            // Extract version strings
            String version1 = parts1.length > 1 ? parts1[1] : "";
            String version2 = parts2.length > 1 ? parts2[1] : "";

            // Handle special versions (Sonnet, Haiku, Opus)
            if (isSpecialVersion(version1) || isSpecialVersion(version2)) {
                return compareSpecialVersions(version1, version2);
            }

            // Compare version strings
            return compareVersions(version1, version2);
        }

        private boolean isSpecialVersion(@NotNull String version) {
            return version.equals("Sonnet") || version.equals("Haiku") || version.equals("Opus");
        }

        private int compareSpecialVersions(@NotNull String v1, String v2) {
            if (v1.equals(v2)) return 0;
            if (v1.equals("Opus")) return 1;
            if (v2.equals("Opus")) return -1;
            if (v1.equals("Sonnet")) return 1;
            if (v2.equals("Sonnet")) return -1;
            return v1.compareTo(v2);
        }

        private int compareVersions(@NotNull String v1, @NotNull String v2) {
            String[] parts1 = v1.split("[^a-zA-Z0-9]+");
            String[] parts2 = v2.split("[^a-zA-Z0-9]+");

            for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
                String part1 = i < parts1.length ? parts1[i] : "";
                String part2 = i < parts2.length ? parts2[i] : "";

                int cmp = compareAlphanumeric(part1, part2);
                if (cmp != 0) return cmp;
            }

            return 0;
        }

        private int compareAlphanumeric(@NotNull String s1, String s2) {
            if (s1.matches("\\d+") && s2.matches("\\d+")) {
                return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
            }
            return s1.compareTo(s2);
        }
    }
}
