package com.devoxx.genie.service.websearch;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** TASK-277.4: Google Custom Search is deprecated; its JSON API is discontinued on 2027-01-01. */
class WebSearchDeprecationTest {

    private static final WebSearchDeprecation GOOGLE =
            WebSearchProviderRegistry.definition(WebSearchProvider.GOOGLE).deprecation();

    private static Clock at(String isoDate) {
        return Clock.fixed(Instant.parse(isoDate + "T12:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    void google_isDeprecated_withGooglesConfirmedShutdownDate_andSource() {
        assertThat(GOOGLE).isNotNull();
        assertThat(GOOGLE.endDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(GOOGLE.detailsUrl()).isEqualTo("https://developers.google.com/custom-search/v1/overview");
    }

    @Test
    void otherProviders_areNotDeprecated() {
        assertThat(WebSearchProviderRegistry.definition(WebSearchProvider.TAVILY).deprecation()).isNull();
        assertThat(WebSearchProviderRegistry.definition(WebSearchProvider.KEENABLE).deprecation()).isNull();
    }

    @Test
    void beforeTheDate_warningNamesTheDate_andSuggestsAlternatives() {
        String warning = GOOGLE.warning("Google Custom Search", at("2026-11-18"));

        assertThat(warning)
                .contains("Google Custom Search")
                .contains("January 1, 2027")
                .contains("Keenable").contains("Tavily")
                .doesNotContain("was discontinued");
    }

    @Test
    void onAndAfterTheDate_warningSaysItWasDiscontinued() {
        assertThat(GOOGLE.warning("Google Custom Search", at("2027-01-01"))).contains("was discontinued on January 1, 2027");
        assertThat(GOOGLE.warning("Google Custom Search", at("2027-03-15"))).contains("was discontinued on January 1, 2027");
    }

    @Test
    void failureBeforeTheDate_pointsAtCredentials_andTheUpcomingShutdown() {
        String message = GOOGLE.failureMessage("Google Custom Search", "GoogleJsonResponseException", at("2026-12-31"));

        assertThat(message)
                .startsWith("Google Custom Search failed (GoogleJsonResponseException).")
                .contains("API key and Search Engine ID")
                .contains("January 1, 2027")
                .contains("Keenable").contains("Tavily")
                .contains(WebSearchReadiness.SETTINGS_PATH);
    }

    @Test
    void failureAfterTheDate_saysTheServiceIsGone_andToPickAnotherProvider() {
        String message = GOOGLE.failureMessage("Google Custom Search", "GoogleJsonResponseException", at("2027-01-02"));

        assertThat(message)
                .contains("was discontinued on January 1, 2027")
                .contains("Choose another provider")
                .contains(WebSearchReadiness.SETTINGS_PATH)
                .doesNotContain("API key and Search Engine ID");
    }

    @Test
    void isPast_flipsOnTheEndDate() {
        assertThat(GOOGLE.isPast(at("2026-12-31"))).isFalse();
        assertThat(GOOGLE.isPast(at("2027-01-01"))).isTrue();
    }
}
