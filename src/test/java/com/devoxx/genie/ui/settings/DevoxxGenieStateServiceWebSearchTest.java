package com.devoxx.genie.ui.settings;

import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-277.1: the selected web search provider replaces the two legacy "enabled" checkboxes.
 * Migration reads only the legacy booleans, never credentials (those live in PasswordSafe).
 */
class DevoxxGenieStateServiceWebSearchTest {

    /** Simulates settings saved by a plugin version that only knew the two checkboxes. */
    private static DevoxxGenieStateService legacySaved(boolean tavilyEnabled, boolean googleEnabled) {
        DevoxxGenieStateService saved = new DevoxxGenieStateService();
        saved.setTavilySearchEnabled(tavilyEnabled);
        saved.setGoogleSearchEnabled(googleEnabled);
        saved.setRawWebSearchProviderForTesting(null);
        saved.setCredentialsMigratedV1(true);
        return saved;
    }

    private static DevoxxGenieStateService load(DevoxxGenieStateService saved) {
        DevoxxGenieStateService loaded = new DevoxxGenieStateService();
        loaded.loadState(saved);
        return loaded;
    }

    @Nested
    class Migration {

        @Test
        void tavilyEnabled_migratesToTavily() {
            assertThat(load(legacySaved(true, false)).getSelectedWebSearchProvider())
                    .isEqualTo(WebSearchProvider.TAVILY);
        }

        @Test
        void googleEnabled_migratesToGoogle() {
            assertThat(load(legacySaved(false, true)).getSelectedWebSearchProvider())
                    .isEqualTo(WebSearchProvider.GOOGLE);
        }

        @Test
        void bothEnabled_tavilyWins_likeBefore() {
            assertThat(load(legacySaved(true, true)).getSelectedWebSearchProvider())
                    .isEqualTo(WebSearchProvider.TAVILY);
        }

        @Test
        void neitherEnabled_migratesToNoProvider() {
            assertThat(load(legacySaved(false, false)).getSelectedWebSearchProvider()).isNull();
        }

        @Test
        void migration_doesNotRequireCredentials() {
            // No keys anywhere: the selection still carries over; readiness (not migration) checks keys.
            DevoxxGenieStateService loaded = load(legacySaved(false, true));
            assertThat(loaded.getGoogleSearchKey()).isEmpty();
            assertThat(loaded.getSelectedWebSearchProvider()).isEqualTo(WebSearchProvider.GOOGLE);
        }

        @Test
        void alreadyMigratedSelection_isKeptEvenIfLegacyBooleansDisagree() {
            DevoxxGenieStateService saved = legacySaved(true, false);
            saved.setRawWebSearchProviderForTesting("GOOGLE");

            assertThat(load(saved).getSelectedWebSearchProvider()).isEqualTo(WebSearchProvider.GOOGLE);
        }

        @Test
        void explicitlyChosenNone_staysNone() {
            DevoxxGenieStateService saved = legacySaved(true, false);
            saved.setRawWebSearchProviderForTesting("");

            assertThat(load(saved).getSelectedWebSearchProvider()).isNull();
        }

        @Test
        void unknownProviderName_fromANewerPlugin_isTreatedAsNone() {
            DevoxxGenieStateService saved = legacySaved(false, false);
            saved.setRawWebSearchProviderForTesting("SOME_FUTURE_PROVIDER");

            assertThat(load(saved).getSelectedWebSearchProvider()).isNull();
        }

        @Test
        void freshInstall_withoutLoadState_hasNoProvider() {
            assertThat(new DevoxxGenieStateService().getSelectedWebSearchProvider()).isNull();
        }
    }

    @Nested
    class Selection {

        @Test
        void selectingAProvider_keepsLegacyBooleansInSync_forDowngrades() {
            DevoxxGenieStateService state = new DevoxxGenieStateService();

            state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
            assertThat(state.isGoogleSearchEnabled()).isTrue();
            assertThat(state.isTavilySearchEnabled()).isFalse();

            state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
            assertThat(state.isTavilySearchEnabled()).isTrue();
            assertThat(state.isGoogleSearchEnabled()).isFalse();

            state.setSelectedWebSearchProvider(null);
            assertThat(state.isTavilySearchEnabled()).isFalse();
            assertThat(state.isGoogleSearchEnabled()).isFalse();
        }
    }

    @Nested
    class Persistence {

        @Test
        void selectedProvider_survivesAnXmlRoundTrip() {
            DevoxxGenieStateService state = new DevoxxGenieStateService();
            state.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);

            Element xml = XmlSerializer.serialize(state);
            DevoxxGenieStateService deserialized = XmlSerializer.deserialize(xml, DevoxxGenieStateService.class);
            DevoxxGenieStateService reloaded = load(deserialized);

            assertThat(reloaded.getSelectedWebSearchProvider()).isEqualTo(WebSearchProvider.GOOGLE);
        }

        @Test
        void explicitNone_survivesAnXmlRoundTrip_andIsNotRemigrated() {
            DevoxxGenieStateService state = new DevoxxGenieStateService();
            state.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
            state.setSelectedWebSearchProvider(null);
            state.setTavilySearchEnabled(true); // stale legacy flag must not resurrect Tavily

            Element xml = XmlSerializer.serialize(state);
            DevoxxGenieStateService reloaded = load(XmlSerializer.deserialize(xml, DevoxxGenieStateService.class));

            assertThat(reloaded.getSelectedWebSearchProvider()).isNull();
        }
    }
}
