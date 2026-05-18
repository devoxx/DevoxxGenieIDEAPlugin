package com.devoxx.genie.ui.util;

import org.jetbrains.annotations.Nullable;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility for ensuring Swing components can render CJK (Chinese / Japanese / Korean)
 * characters.
 *
 * <p>The IntelliJ editor font on most platforms is a monospace font (JetBrains Mono,
 * Consolas, Menlo, ...) which does not contain CJK glyphs. Applying that font directly
 * to a Swing {@code JTextArea} / {@code JTextPane} makes any CJK input render as the
 * "tofu" {@code □} box. See <a href="https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1034">#1034</a>.
 *
 * <p>This helper picks a CJK-capable replacement family when the supplied font cannot
 * render a representative CJK sample, while preserving the original size and style.
 */
public final class CJKFontUtil {

    /**
     * Representative CJK code points covering the common ranges:
     * <ul>
     *   <li>U+4F60 你 — CJK Unified Ideographs (Simplified/Traditional Chinese, Kanji)</li>
     *   <li>U+3042 あ — Japanese Hiragana</li>
     *   <li>U+AC00 가 — Korean Hangul Syllables</li>
     * </ul>
     * If the font can display all three we treat it as CJK-capable.
     */
    static final String CJK_PROBE = "\u4F60\u3042\uAC00";

    /**
     * Candidate font families that are known to ship CJK glyphs on the major desktop
     * platforms. We try them in order before falling back to the logical {@code Dialog}
     * font, which on most JDKs is a composite font that includes a CJK fallback chain.
     */
    static final List<String> CJK_FONT_CANDIDATES = List.of(
            // Cross-platform / shipped with many JDKs
            "Noto Sans CJK SC",
            "Noto Sans CJK",
            "Noto Sans SC",
            "Source Han Sans SC",
            "Source Han Sans",
            // Windows
            "Microsoft YaHei",
            "Microsoft YaHei UI",
            "SimSun",
            "SimHei",
            "Yu Gothic UI",
            "MS Gothic",
            "Malgun Gothic",
            // macOS
            "PingFang SC",
            "PingFang TC",
            "Hiragino Sans GB",
            "Hiragino Sans",
            "Hiragino Kaku Gothic ProN",
            "Apple SD Gothic Neo",
            // Linux
            "WenQuanYi Micro Hei",
            "WenQuanYi Zen Hei",
            "AR PL UMing CN"
    );

    /**
     * Cached result of {@link #findCJKCapableFamily()}. Installed fonts don't change
     * during an IDE session so scanning the full font list once is sufficient. The
     * reference holds {@code null} when not yet resolved, a non-null family name when
     * one was found, or the sentinel {@link #NONE_FOUND} when a scan completed without
     * finding any CJK-capable family (so we don't rescan on every call).
     */
    private static final AtomicReference<String> CACHED_CJK_FAMILY = new AtomicReference<>();

    /** Sentinel marking “scan completed, no CJK family found”. */
    private static final String NONE_FOUND = "\u0000NONE";

    private CJKFontUtil() {
    }

    /**
     * Returns {@code true} if the given font can render the representative CJK probe
     * characters. A {@code null} font is treated as not CJK-capable.
     */
    public static boolean canDisplayCJK(@Nullable Font font) {
        if (font == null) {
            return false;
        }
        // Font#canDisplayUpTo returns -1 when every character can be displayed.
        return font.canDisplayUpTo(CJK_PROBE) == -1;
    }

    /**
     * Ensure the supplied font can render CJK characters. If it already can, the font
     * is returned unchanged so users see the IDE editor font they configured. Otherwise
     * we pick a CJK-capable family and derive a new font that preserves the original
     * size and style.
     *
     * @param font the desired font (typically the IDE editor font); may be {@code null}
     * @return a font that can render CJK input, or {@code font} unchanged if it already
     *         supports CJK. May return {@code null} only if {@code font} itself is
     *         {@code null} and no candidate is available.
     */
    public static @Nullable Font withCJKFallback(@Nullable Font font) {
        if (canDisplayCJK(font)) {
            return font;
        }
        int style = font != null ? font.getStyle() : Font.PLAIN;
        float size = font != null ? font.getSize2D() : 12f;

        String candidate = findCJKCapableFamily();
        if (candidate == null) {
            // As a last resort fall back to the logical Dialog font, which on most JDKs
            // is composed of platform fonts including a CJK range.
            candidate = Font.DIALOG;
        }
        Font replacement = new Font(candidate, style, Math.max(1, Math.round(size)))
                .deriveFont(size);
        // Only swap if the replacement is actually better — otherwise keep the
        // original so we don't surprise users on systems with no CJK font at all.
        if (canDisplayCJK(replacement)) {
            return replacement;
        }
        return font;
    }

    /**
     * Find the first installed font family from {@link #CJK_FONT_CANDIDATES} that can
     * render the CJK probe. Returns {@code null} when none is available — callers
     * should fall back to {@link Font#DIALOG}.
     *
     * <p>The result is cached for the lifetime of the JVM (installed fonts don't
     * change during an IDE session) so repeated scheme-change events don't iterate
     * the full font list.
     */
    static @Nullable String findCJKCapableFamily() {
        String cached = CACHED_CJK_FAMILY.get();
        if (cached != null) {
            return NONE_FOUND.equals(cached) ? null : cached;
        }
        String resolved = resolveCJKCapableFamily();
        // Multiple threads may race here; the result is deterministic so the last
        // writer wins harmlessly.
        CACHED_CJK_FAMILY.set(resolved == null ? NONE_FOUND : resolved);
        return resolved;
    }

    private static @Nullable String resolveCJKCapableFamily() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        // Headless environments still provide a list of logical fonts, so this is safe.
        List<String> installed = List.of(ge.getAvailableFontFamilyNames());
        for (String candidate : CJK_FONT_CANDIDATES) {
            if (installed.contains(candidate) && canDisplayCJK(new Font(candidate, Font.PLAIN, 12))) {
                return candidate;
            }
        }
        // Last-chance scan: the platform may expose a CJK font under a name we don't
        // know about. Probe the installed families directly.
        for (String family : installed) {
            if (canDisplayCJK(new Font(family, Font.PLAIN, 12))) {
                return family;
            }
        }
        return null;
    }

    /** Test-only hook to reset the cached family lookup. */
    static void resetCacheForTesting() {
        CACHED_CJK_FAMILY.set(null);
    }
}
