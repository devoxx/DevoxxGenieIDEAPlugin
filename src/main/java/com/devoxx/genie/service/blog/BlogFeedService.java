package com.devoxx.genie.service.blog;

import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Loads blog posts to surface on the welcome screen.
 *
 * <p>Two sources:
 * <ol>
 *   <li>A bundled {@code blog-posts.json} generated at build time from {@code docusaurus/blog/*.md}.
 *       Always available, works offline, but stale between releases.</li>
 *   <li>The live RSS feed at {@code https://genie.devoxx.com/blog/rss.xml}, fetched asynchronously
 *       and cached in {@link PropertiesComponent} for {@value #CACHE_TTL_MILLIS} ms.</li>
 * </ol>
 *
 * <p>Callers should display the bundled list immediately, then call
 * {@link #refreshRemoteAsync(Consumer)} to update the UI when fresh posts arrive.
 */
@Slf4j
public final class BlogFeedService {

    private static final String BUNDLED_RESOURCE = "/devoxxgenie/blog-posts.json";
    private static final String FEED_URL = "https://genie.devoxx.com/blog/rss.xml";

    private static final String CACHE_KEY = "devoxxgenie.blog.cache.json";
    private static final String CACHE_TIMESTAMP_KEY = "devoxxgenie.blog.cache.ts";
    private static final long CACHE_TTL_MILLIS = 6L * 60 * 60 * 1000; // 6 hours

    private static final BlogFeedService INSTANCE = new BlogFeedService();

    private final Gson gson = new Gson();

    private BlogFeedService() {}

    public static BlogFeedService getInstance() {
        return INSTANCE;
    }

    /** Synchronously load the bundled blog post list from the plugin classpath. */
    public @NotNull List<BlogPost> getBundled() {
        try (InputStream in = BlogFeedService.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (in == null) {
                log.warn("Bundled blog index not found at {}", BUNDLED_RESOURCE);
                return Collections.emptyList();
            }
            BlogPost[] posts = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), BlogPost[].class);
            return posts == null ? Collections.emptyList() : List.of(posts);
        } catch (Exception e) {
            log.warn("Failed to load bundled blog index", e);
            return Collections.emptyList();
        }
    }

    /**
     * Returns cached remote posts if a fresh cache exists; otherwise {@code null}.
     * Cheap and safe to call on the EDT.
     */
    public @Nullable List<BlogPost> getCachedRemote() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        String json = props.getValue(CACHE_KEY);
        long ts = props.getLong(CACHE_TIMESTAMP_KEY, 0L);
        if (json == null || json.isEmpty()) return null;
        if (System.currentTimeMillis() - ts > CACHE_TTL_MILLIS) return null;
        try {
            BlogPost[] posts = gson.fromJson(json, BlogPost[].class);
            return posts == null ? null : List.of(posts);
        } catch (Exception e) {
            log.debug("Failed to parse cached blog posts", e);
            return null;
        }
    }

    /**
     * Best-effort: returns cached remote posts if fresh, otherwise the bundled list.
     * Never blocks on network — safe for the initial welcome render.
     */
    public @NotNull List<BlogPost> getInitialPosts() {
        List<BlogPost> cached = getCachedRemote();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        return getBundled();
    }

    /**
     * Fetch the live RSS feed off the EDT and invoke {@code onUpdated} with fresh posts when
     * (and only if) they differ from what is currently cached. Failures are silent.
     */
    public void refreshRemoteAsync(@NotNull Consumer<List<BlogPost>> onUpdated) {
        // Snapshot whether the UI is currently backed by a fresh remote cache. If it is,
        // and the fetched feed matches that cache, we can skip the UI update. Otherwise
        // (no cache, expired cache, or content changed) we must push the fetched posts
        // to the UI so the welcome screen leaves bundled-stale data behind.
        final boolean initialWasFreshRemote = getCachedRemote() != null;

        CompletableFuture.supplyAsync(this::fetchRemote, ApplicationManager.getApplication()::executeOnPooledThread)
                .thenAccept(posts -> {
                    if (posts == null || posts.isEmpty()) return;

                    PropertiesComponent props = PropertiesComponent.getInstance();
                    String previous = props.getValue(CACHE_KEY);
                    String fresh = gson.toJson(posts);
                    boolean contentChanged = !fresh.equals(previous);

                    if (contentChanged) {
                        props.setValue(CACHE_KEY, fresh);
                    }
                    props.setValue(CACHE_TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));

                    // Push to UI when content actually changed, OR when the UI was
                    // previously showing bundled/stale data (so it can switch to remote).
                    if (contentChanged || !initialWasFreshRemote) {
                        ApplicationManager.getApplication().invokeLater(() -> onUpdated.accept(posts));
                    }
                })
                .exceptionally(ex -> {
                    log.debug("Remote blog refresh failed: {}", ex.getMessage());
                    return null;
                });
    }

    private @Nullable List<BlogPost> fetchRemote() {
        Request request = new Request.Builder().url(FEED_URL).get().build();
        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.debug("Blog RSS fetch returned {}", response.code());
                return null;
            }
            ResponseBody body = response.body();
            if (body == null) return null;
            return parseRss(body.bytes());
        } catch (Exception e) {
            log.debug("Blog RSS fetch failed", e);
            return null;
        }
    }

    private @Nullable List<BlogPost> parseRss(byte[] xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Defensive XML parser configuration
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml));

            NodeList items = doc.getElementsByTagName("item");
            List<BlogPost> posts = new ArrayList<>(items.getLength());
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String title = textOf(item, "title");
                String link = textOf(item, "link");
                String description = stripHtml(textOf(item, "description"));
                String pubDate = textOf(item, "pubDate");
                if (title == null || link == null) continue;

                String slug = slugFromLink(link);
                String date = normalizeDate(pubDate);
                posts.add(new BlogPost(slug, title, date, description == null ? "" : description));
            }
            return posts;
        } catch (Exception e) {
            log.debug("Failed to parse RSS feed", e);
            return null;
        }
    }

    private static @Nullable String textOf(@NotNull Element parent, @NotNull String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String text = nl.item(0).getTextContent();
        return text == null ? null : text.trim();
    }

    private static @NotNull String slugFromLink(@NotNull String link) {
        // Expect https://genie.devoxx.com/blog/<slug>(/)
        int idx = link.indexOf("/blog/");
        if (idx < 0) return link;
        String tail = link.substring(idx + "/blog/".length());
        if (tail.endsWith("/")) tail = tail.substring(0, tail.length() - 1);
        return tail;
    }

    private static @NotNull String normalizeDate(@Nullable String pubDate) {
        if (pubDate == null || pubDate.isEmpty()) return "";
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.toLocalDate().toString();
        } catch (Exception e) {
            return pubDate;
        }
    }

    private static @NotNull String stripHtml(@Nullable String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }
}
