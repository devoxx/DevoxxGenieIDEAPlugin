package com.devoxx.genie.chatmodel.catalog;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dotted JSON paths naming where a router's model catalog keeps optional fields.
 *
 * @param contextWindow ordered fallbacks; the first path holding a positive number wins
 * @param priceScale    multiplier applied to prices to reach cost per million tokens
 * @param filterField   when set, an entry whose field is present must equal {@code filterValue}, or
 *                      contain it when the field is an array; an absent field counts as a match
 */
public record CatalogFields(@Nullable String displayName,
                            List<String> contextWindow,
                            @Nullable String inputPrice,
                            @Nullable String outputPrice,
                            double priceScale,
                            @Nullable String filterField,
                            @Nullable String filterValue) {
}
