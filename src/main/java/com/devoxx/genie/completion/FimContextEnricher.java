package com.devoxx.genie.completion;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds the FIM context from the code around the caret using only the core
 * (language-agnostic) PSI API: {@link PsiMethod}/{@link PsiClass}/{@link PsiField} and
 * {@link PsiReference} are implemented by every language plugin, so no Java/Kotlin/Python
 * specific classes are used and the enrichment degrades gracefully for languages that
 * do not contribute method/class nodes at all.
 * <p>
 * The context contains, in reference order and within a character budget:
 * the declaration of the method containing the caret, the declarations of the classes
 * referenced inside it, and the signatures (declaration text up to the body) of the
 * methods/fields referenced there. This is the minimum information the model needs to
 * avoid inventing APIs that do not exist.
 */
public final class FimContextEnricher {

    private static final Logger LOG = Logger.getInstance(FimContextEnricher.class);

    /** Max method/field signatures collected per class. */
    static final int MAX_SIGNATURES_PER_CLASS = 12;

    /** Fallback reference window (chars around the caret) when it is not inside a method. */
    static final int FALLBACK_WINDOW_BEFORE = 1500;
    static final int FALLBACK_WINDOW_AFTER = 200;

    /** Max reference-holding elements processed in one enrichment run. */
    static final int MAX_REFERENCE_HOLDERS = 300;

    /** Target prefixes that never add useful FIM context. */
    private static final List<String> SKIP_PREFIXES = List.of(
            "java.", "javax.", "jakarta.", "sun.", "kotlin.", "kotlinx.",
            "org.junit.", "org.mockito.", "org.assertj.", "org.opentest4j.");

    /** Precompiled whitespace-collapse pattern used by {@link #declarationText}. */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private FimContextEnricher() {
    }

    /** One class's contribution: declaration line + referenced member signatures. */
    public record ClassContext(@NotNull String className,
                               @Nullable String declaration,
                               @NotNull List<String> signatures) {
        public ClassContext {
            signatures = List.copyOf(signatures);
        }
    }

    /**
     * Build the enriched prefix: PSI-derived class context blocks followed by the original
     * prefix. Returns the original prefix unchanged when nothing can be collected.
     */
    public static @NotNull String enrich(@NotNull Project project,
                                         @NotNull VirtualFile currentFile,
                                         @NotNull String basePrefix,
                                         int caretOffset,
                                         int budgetChars,
                                         int maxClasses) {
        if (budgetChars <= 0 || maxClasses <= 0) {
            return basePrefix;
        }
        try {
            List<ClassContext> contexts = ApplicationManager.getApplication().runReadAction(
                    (Computable<List<ClassContext>>) () -> collectPsiContext(project, currentFile, caretOffset));
            return buildContext(contexts, basePrefix, budgetChars, maxClasses);
        } catch (RuntimeException | Error e) {
            // Enrichment is best-effort: a PSI/indexing failure must never kill the
            // completion — fall back to the raw prefix.
            LOG.warn("FIM context enrichment failed, using raw prefix: " + e);
            return basePrefix;
        }
    }

    static @NotNull List<ClassContext> collectPsiContext(@NotNull Project project,
                                                         @NotNull VirtualFile currentFile,
                                                         int caretOffset) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(currentFile);
        if (psiFile == null) {
            return Collections.emptyList();
        }
        PsiElement caretElement = psiFile.findElementAt(Math.max(0, caretOffset - 1));
        if (caretElement == null) {
            return Collections.emptyList();
        }

        List<PsiElement> referenceHolders = collectReferenceHolders(psiFile, caretElement, caretOffset);

        Map<String, MutableContext> contexts = new LinkedHashMap<>();
        int processed = 0;
        for (PsiElement holder : referenceHolders) {
            if (processed++ >= MAX_REFERENCE_HOLDERS) {
                break;
            }
            for (PsiReference reference : holder.getReferences()) {
                PsiElement target = reference.resolve();
                if (target == null) {
                    continue;
                }

                if (target instanceof PsiClass targetClass) {
                    MutableContext context = contextFor(targetClass, contexts);
                    if (context == null) {
                        continue;
                    }
                    if (context.declaration == null) {
                        context.declaration = declarationText(targetClass);
                    }
                    for (PsiMethod method : targetClass.getMethods()) {
                        if (method.hasModifierProperty(PsiModifier.PRIVATE)) {
                            continue;
                        }
                        if (context.signatures.size() >= MAX_SIGNATURES_PER_CLASS) {
                            break;
                        }
                        addSignature(context, declarationText(method));
                    }
                    continue;
                }

                PsiClass containingClass = null;
                String signature = null;
                if (target instanceof PsiMethod method) {
                    containingClass = method.getContainingClass();
                    signature = declarationText(method);
                } else if (target instanceof PsiField field
                        && field.hasModifierProperty(PsiModifier.STATIC)
                        && field.hasModifierProperty(PsiModifier.FINAL)) {
                    containingClass = field.getContainingClass();
                    signature = declarationText(field);
                }
                if (containingClass == null || signature == null) {
                    continue;
                }
                MutableContext context = contextFor(containingClass, contexts);
                if (context == null) {
                    continue;
                }
                if (context.declaration == null) {
                    context.declaration = declarationText(containingClass);
                }
                addSignature(context, signature);
            }
        }

        List<ClassContext> result = new ArrayList<>(contexts.size());
        for (MutableContext context : contexts.values()) {
            if (context.signatures.isEmpty() && (context.declaration == null || context.declaration.isBlank())) {
                continue;
            }
            result.add(new ClassContext(context.className, context.declaration, context.signatures));
        }
        return result;
    }

    /**
     * Collect elements holding references: inside the method containing the caret, or — when the
     * caret is not inside any method — inside a text window around the caret.
     */
    private static @NotNull List<PsiElement> collectReferenceHolders(@NotNull PsiFile psiFile,
                                                                     @NotNull PsiElement caretElement,
                                                                     int caretOffset) {
        List<PsiElement> holders = new ArrayList<>();
        PsiMethod containingMethod = PsiTreeUtil.getParentOfType(caretElement, PsiMethod.class);
        if (containingMethod != null) {
            holders.addAll(Arrays.asList(PsiTreeUtil.collectElements(containingMethod,
                    element -> element.getReferences().length > 0)));
            return holders;
        }

        int from = Math.max(0, caretOffset - FALLBACK_WINDOW_BEFORE);
        int to = caretOffset + FALLBACK_WINDOW_AFTER;
        psiFile.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element != psiFile) {
                    TextRange range = element.getTextRange();
                    if (range != null) {
                        // Prune subtrees entirely outside the window; element ranges are
                        // nested, so children of a skipped node are outside the window too.
                        if (range.getStartOffset() > to || range.getEndOffset() < from) {
                            return;
                        }
                        if (element.getReferences().length > 0
                                && range.getStartOffset() >= from) {
                            holders.add(element);
                        }
                    }
                }
                super.visitElement(element);
            }
        });
        return holders;
    }

    @Nullable
    private static MutableContext contextFor(@NotNull PsiClass psiClass,
                                             @NotNull Map<String, MutableContext> contexts) {
        if (isSkippable(psiClass)) {
            return null;
        }
        String name = psiClass.getQualifiedName();
        if (name == null || name.isBlank()) {
            name = psiClass.getName();
        }
        if (name == null || name.isBlank()) {
            return null;
        }
        return contexts.computeIfAbsent(name, MutableContext::new);
    }

    private static void addSignature(@NotNull MutableContext context, @NotNull String signature) {
        if (signature.isBlank()) {
            return;
        }
        if (!context.signatures.contains(signature)) {
            context.signatures.add(signature);
        }
    }

    private static boolean isSkippable(@NotNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return false;
        }
        for (String prefix : SKIP_PREFIXES) {
            if (qualifiedName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Declaration of an element up to its body, compacted to a single line: annotations and
     * javadoc stripped, whitespace collapsed. Language-agnostic: cuts at {@code {} for
     * brace-languages, at the line ending with {@code :} for indentation-based ones.
     */
    static @NotNull String declarationText(@NotNull PsiElement element) {
        String text = element.getText();
        int brace = text.indexOf('{');
        String head = brace >= 0 ? text.substring(0, brace) : text;

        if (brace < 0) {
            StringBuilder upToColon = new StringBuilder();
            for (String line : head.split("\n", -1)) {
                upToColon.append(line);
                if (line.trim().endsWith(":")) {
                    break;
                }
                upToColon.append('\n');
            }
            head = upToColon.toString();
        }

        List<String> lines = Arrays.asList(head.split("\n", -1));
        int start = 0;
        while (start < lines.size()) {
            String trimmed = lines.get(start).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("@") || trimmed.startsWith("*")
                    || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                start++;
            } else {
                break;
            }
        }

        StringBuilder declaration = new StringBuilder();
        for (int i = start; i < lines.size(); i++) {
            declaration.append(lines.get(i).trim()).append(' ');
        }
        return WHITESPACE.matcher(declaration).replaceAll(" ").trim();
    }

    static @NotNull String buildContext(@NotNull List<ClassContext> contexts,
                                        @NotNull String basePrefix,
                                        int budgetChars,
                                        int maxClasses) {
        if (contexts.isEmpty() || budgetChars <= 0 || maxClasses <= 0) {
            return basePrefix;
        }

        StringBuilder sb = new StringBuilder(budgetChars + basePrefix.length() + 64);
        int included = 0;

        for (ClassContext context : contexts) {
            if (included >= maxClasses || sb.length() >= budgetChars) {
                break;
            }

            // Language-neutral marker: no comment syntax, identical for every language.
            String header = "class: " + context.className() + "\n";
            StringBuilder body = new StringBuilder();
            if (context.declaration() != null && !context.declaration().isBlank()) {
                body.append(context.declaration()).append('\n');
            }
            for (String signature : context.signatures()) {
                if (!signature.isBlank()) {
                    body.append(signature).append('\n');
                }
            }

            int bodyBudget = budgetChars - sb.length() - header.length() - 1;
            if (bodyBudget <= 0) {
                break;
            }
            String bodyText = body.toString();
            if (bodyText.length() > bodyBudget) {
                bodyText = bodyText.substring(0, bodyBudget);
            }
            if (bodyText.isBlank()) {
                break;
            }

            sb.append(header).append(bodyText).append('\n');
            included++;
        }

        if (included == 0) {
            return basePrefix;
        }
        return sb.append(basePrefix).toString();
    }

    private static final class MutableContext {
        private final String className;
        private String declaration;
        private final List<String> signatures = new ArrayList<>();

        private MutableContext(@NotNull String className) {
            this.className = className;
        }
    }
}
