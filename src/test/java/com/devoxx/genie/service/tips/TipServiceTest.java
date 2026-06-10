package com.devoxx.genie.service.tips;

import com.devoxx.genie.model.tips.Tip;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TipServiceTest {

    private Tip tip(String text, int weight) {
        return new Tip(text, weight);
    }

    @Test
    void effectiveWeightCoercesNonPositiveToOne() {
        assertThat(TipService.effectiveWeight(tip("a", 0))).isEqualTo(1);
        assertThat(TipService.effectiveWeight(tip("a", -5))).isEqualTo(1);
        assertThat(TipService.effectiveWeight(tip("a", 4))).isEqualTo(4);
        assertThat(TipService.effectiveWeight(null)).isEqualTo(1);
    }

    @Test
    void selectWeightedRespectsWeightDistribution() {
        List<Tip> tips = List.of(tip("low", 1), tip("high", 3));

        int low = 0;
        int high = 0;
        int draws = 10_000;
        for (int i = 0; i < draws; i++) {
            // sweep r deterministically across [0, 1)
            double r = (double) i / draws;
            Tip selected = TipService.selectWeighted(tips, null, r);
            if ("low".equals(selected.getText())) {
                low++;
            } else {
                high++;
            }
        }

        // weight 3 vs weight 1 -> high should appear ~3x as often as low
        double ratio = (double) high / low;
        assertThat(ratio).isBetween(2.7, 3.3);
    }

    @Test
    void selectWeightedNeverReturnsPreviousWhenAlternativesExist() {
        List<Tip> tips = List.of(tip("a", 1), tip("b", 1));
        for (int i = 0; i < 1000; i++) {
            double r = (double) i / 1000;
            assertThat(TipService.selectWeighted(tips, "a", r).getText()).isEqualTo("b");
        }
    }

    @Test
    void selectWeightedReturnsSingleTipEvenWhenItIsPrevious() {
        List<Tip> tips = List.of(tip("only", 1));
        Tip selected = TipService.selectWeighted(tips, "only", 0.5);
        assertThat(selected).isNotNull();
        assertThat(selected.getText()).isEqualTo("only");
    }

    @Test
    void selectWeightedReturnsNullForEmptyOrBlank() {
        assertThat(TipService.selectWeighted(List.of(), null, 0.5)).isNull();
        assertThat(TipService.selectWeighted(null, null, 0.5)).isNull();
        assertThat(TipService.selectWeighted(List.of(tip("  ", 1)), null, 0.5)).isNull();
    }

    @Test
    void fallbackTipsAreNonEmptyAndValid() {
        List<Tip> fallback = TipService.getFallbackTips();
        assertThat(fallback).isNotEmpty();
        assertThat(fallback).allSatisfy(t -> {
            assertThat(t.getText()).isNotBlank();
            assertThat(t.getWeight()).isPositive();
        });
    }
}
