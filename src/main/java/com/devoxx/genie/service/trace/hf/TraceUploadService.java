package com.devoxx.genie.service.trace.hf;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.debug.SecretRedactor.TraceRedactionOptions;
import com.devoxx.genie.service.trace.StoredTraceEvent;
import com.devoxx.genie.service.trace.TraceRedaction;
import com.devoxx.genie.service.trace.TraceScanStatus;
import com.devoxx.genie.service.trace.TraceSettings;
import com.devoxx.genie.service.trace.TruffleHogMode;
import com.devoxx.genie.service.trace.hf.HuggingFaceHubClient.UploadFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Converts conversations to STS JSONL and uploads them to a Hugging Face dataset in one commit
 * (task-280). Blocking: run it on a background thread. A conversation is held back (not
 * uploaded) when TruffleHog is {@code required} but some of its turns were stored unscanned,
 * or when it has nothing to upload.
 */
public final class TraceUploadService {

    public record HeldBack(@NotNull String conversationId, @Nullable String title, @NotNull String reason) {}

    /** What an upload did. {@code firstFileUrl} links to the first uploaded trace on the Hub. */
    public record Outcome(@NotNull HfDatasetTarget target, @NotNull List<String> uploadedPaths,
                          @NotNull List<HeldBack> heldBack, boolean createdDataset,
                          @Nullable String firstFileUrl, @Nullable String commitUrl) {}

    /** A converted file, ready to upload or export. */
    public record Prepared(@NotNull Conversation conversation, @NotNull String path, @NotNull StsConverter.Result sts) {}

    private final HuggingFaceHubClient client;
    private final Function<String, List<StoredTraceEvent>> traceReader;

    public TraceUploadService(@NotNull HuggingFaceHubClient client,
                              @NotNull Function<String, List<StoredTraceEvent>> traceReader) {
        this.client = client;
        this.traceReader = traceReader;
    }

    /**
     * Converts the conversations; the ones that must not leave the machine go to {@code heldBack}.
     */
    public static @NotNull List<Prepared> prepare(@NotNull List<Conversation> conversations,
                                                  @NotNull Function<String, List<StoredTraceEvent>> traceReader,
                                                  @NotNull HfUploadSettings upload, @NotNull TraceSettings trace,
                                                  @Nullable String projectName, @NotNull List<HeldBack> heldBack) {
        TraceRedactionOptions redaction = TraceRedaction.optionsFor(trace);
        StsConverter.Options options = new StsConverter.Options(redaction, trace.maxToolResultChars(),
                upload.includeProjectName() ? projectName : null);
        List<Prepared> prepared = new ArrayList<>();
        for (Conversation c : conversations) {
            List<StoredTraceEvent> events = traceReader.apply(c.getId());
            if (trace.truffleHogMode() == TruffleHogMode.REQUIRED
                    && events.stream().anyMatch(e -> e.scanStatus() == TraceScanStatus.NOT_SCANNED)) {
                heldBack.add(new HeldBack(c.getId(), c.getTitle(),
                        "TruffleHog is required, but part of this trace was stored without a scan"));
                continue;
            }
            StsConverter.Result sts = StsConverter.convert(c, events, options);
            if (sts.messageCount() == 0) {
                heldBack.add(new HeldBack(c.getId(), c.getTitle(), "nothing to upload yet"));
                continue;
            }
            prepared.add(new Prepared(c, upload.filePath(c.getId()), sts));
        }
        return prepared;
    }

    /**
     * Uploads the conversations to {@code upload.target()}, creating the dataset when it doesn't
     * exist. Returns without contacting the Hub when everything was held back.
     */
    public @NotNull Outcome upload(@NotNull List<Conversation> conversations, @NotNull String token,
                                   @NotNull HfUploadSettings upload, @NotNull TraceSettings trace,
                                   @Nullable String projectName, @NotNull BooleanSupplier cancelled)
            throws HubApiException {
        HfDatasetTarget target = upload.target();
        if (target == null) {
            throw new IllegalArgumentException("No Hugging Face dataset is configured");
        }
        List<HeldBack> heldBack = new ArrayList<>();
        List<Prepared> prepared = prepare(conversations, traceReader, upload, trace, projectName, heldBack);
        if (prepared.isEmpty() || cancelled.getAsBoolean()) {
            return new Outcome(target, List.of(), heldBack, false, null, null);
        }

        boolean created = false;
        if (!client.datasetExists(target, token)) {
            client.createDataset(target, token, upload.isPrivate());
            created = true;
        }
        if (cancelled.getAsBoolean()) {
            return new Outcome(target, List.of(), heldBack, created, null, null);
        }

        List<UploadFile> files = prepared.stream()
                .map(p -> new UploadFile(p.path(), p.sts().jsonl().getBytes(StandardCharsets.UTF_8)))
                .toList();
        String title = files.size() == 1
                ? "Upload DevoxxGenie trace: " + shortTitle(prepared.get(0).conversation().getTitle())
                : "Upload " + files.size() + " DevoxxGenie traces";
        HuggingFaceHubClient.CommitResult commit = client.commit(target, token, files, title);

        List<String> paths = files.stream().map(UploadFile::path).toList();
        return new Outcome(target, paths, heldBack, created, target.fileUrl(client.hubUrl(), paths.get(0)),
                commit.commitUrl());
    }

    private static String shortTitle(@Nullable String title) {
        if (title == null || title.isBlank()) {
            return "conversation";
        }
        String t = title.replaceAll("\\s+", " ").trim();
        return t.length() > 60 ? t.substring(0, 57) + "..." : t;
    }
}
