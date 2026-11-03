---
slug: run-script-code-mode
title: "run_script: Let the Agent Batch Its Tool Calls in JavaScript"
authors: [stephanj]
tags: [agent mode, tools, code mode, run_script, javascript, quickjs, tokens, intellij idea, open source]
date: 2026-11-03
description: DevoxxGenie 1.16.0 adds run_script, an experimental "code mode" tool. The agent writes a short sandboxed JavaScript snippet that calls other tools, filters their results, and returns only what it prints, saving model round trips and context-window tokens.
keywords: [devoxxgenie, run_script, code mode, agent mode, tool calls, javascript, quickjs, quickjs4j, webassembly, token usage, context window, intellij plugin]
---

# run_script: Let the Agent Batch Its Tool Calls in JavaScript

DevoxxGenie 1.16.0 ships a new experimental agent tool, **`run_script`**. Instead of making one tool call per round trip, the agent can write a short JavaScript snippet that calls several tools, does the looping and filtering itself, and hands back only the answer.

This idea is often called **code mode**, and it was proposed and implemented for DevoxxGenie by [@omarmahamid](https://github.com/omarmahamid) (#1352, #1353). Thank you, Omar! This post explains the problem it solves, how it works, and how it stays inside the agent's existing safety rules.

<!-- truncate -->

## The problem: one round trip per tool call

In [Agent Mode](/docs/features/agent-mode), the model gets tools like `read_file`, `search_files` and `find_references`. Each tool call works like this:

1. The model asks for a tool call.
2. DevoxxGenie runs it and puts the **full result** into chat memory.
3. The whole conversation, including that result, goes back to the model so it can decide the next step.

That's fine for a few calls. It gets expensive for "wide" questions:

- *"Which of our REST controllers don't have a test?"*
- *"Check every caller of `ChatMemoryManager.prepareMemory` and tell me which ones pass a null context."*
- *"Which of these 30 config files still set `timeout` below 10 seconds?"*

Each of these takes dozens of tool calls. Every one is a separate model round trip, and every raw result (whole files, long search listings) stays in the context window for the rest of the conversation, even though the model only needed a yes/no or a name from each one.

## The idea: let the model write the loop

Models are good at writing code. So instead of having the model *be* the loop, `run_script` lets it *write* the loop:

```js
const listing = await tools.search_files({ pattern: "@RestController", path: "src/main/java" });
const controllers = [...new Set(
  listing.split("\n")
    .map(line => line.match(/([A-Za-z0-9_]+)\.java/))
    .filter(Boolean)
    .map(m => m[1])
)];

const missing = [];
for (const name of controllers) {
  const hits = await tools.search_files({ pattern: `class ${name}Test`, path: "src/test/java" });
  if (!hits.includes(`${name}Test`)) missing.push(name);
}

text(missing.length ? missing.join("\n") : "Every controller has a test.");
```

That's **one** `run_script` call. The searches happen inside it, and the model gets back a short list of class names instead of every search result. Fewer round trips, a much smaller context window, and usually a lower bill.

## How it works

### The tool description is the API

When code mode is on, `run_script` is registered next to the other agent tools. Its description lists **every tool the agent can currently call** as a TypeScript-style signature, followed by the first sentence of that tool's own description:

```text
Callable tools:
tools.read_file(args: { path: string }) - Read the contents of a file in the project.
tools.search_files(args: { pattern: string, path?: string, file_pattern?: string }) - Search for a regex pattern ...
tools.find_references(args: { ... }) - ...
```

So the model already knows the exact argument names when it writes the script. The list is built from the tools that are actually enabled, so a tool you've switched off in settings isn't listed and can't be called. MCP tools you have enabled are listed too. At runtime, `ALL_TOOLS` gives the same catalog as an array of `{ name, description }`.

### The script

The model's code runs as the **body of an async function**:

- `await tools.<name>({ ...args })` calls a tool with the same arguments it takes when called directly.
- A tool returns the **same plain text** it returns when called directly. That's usually not JSON, so the script only calls `JSON.parse` when a tool really returns JSON.
- A failed call returns a string starting with `Error:` instead of throwing, so one bad lookup doesn't kill the whole script.
- `text(value)`, `console.log(...)` and the script's `return` value are the **only** things the model sees. Objects are printed as pretty JSON.
- Tool calls run **one at a time**, so `Promise.all` doesn't make them faster.

The shipped description also tells the model *not* to wrap a single tool call in `run_script` just to print its result. For a one-off lookup, calling the tool directly is simpler, and good models follow that.

### The sandbox: QuickJS on plain Java bytecode

Every script runs in a **fresh** [QuickJS](https://bellard.org/quickjs/) engine via [quickjs4j](https://github.com/roastedroot/quickjs4j). QuickJS is compiled to WebAssembly, and that WebAssembly is compiled to **plain Java bytecode**. That means:

- No native libraries are bundled or downloaded, so it works the same on macOS, Windows and Linux.
- The runtime adds only about 1.2 MB to the plugin.
- The JavaScript can't reach anything the JVM doesn't hand it.

What the script **doesn't** get: Node.js, `require`/`import`, `fetch`, timers, the file system or the network. The only way out of the sandbox is the `tools` object.

| Limit | Value |
|-------|-------|
| Memory | 128 MiB per script; going over returns an error instead of affecting the IDE |
| Time budget | 30 s of **JavaScript** time; time spent inside tool calls (for example, waiting for you to approve one) doesn't count |
| Tool calls inside one script | *Max tool calls per script*, default 100 |
| Output | 20,000 characters |
| Cancellation | Stopping the prompt stops the script |

QuickJS on Java bytecode isn't fast for CPU-heavy code. A 5-million-iteration loop takes about 9.5 seconds. That's fine here: these scripts are short glue between tool calls, and the 30-second budget stops anything heavier.

## Safety: the same rules as a direct call

Code mode doesn't open a back door. `run_script` wraps the agent's **already-approved** tool chain, so every call inside a script passes through exactly what a direct call does:

- **Tool toggles**: a disabled tool can't be called.
- **Approval dialogs**: a `write_file` or `edit_file` inside a script still shows its diff and waits for you. Auto-approval of read-only tools applies as usual.
- **Command blacklist**: `run_command` inside a script is checked against your blacklist like any other.
- **Activity rows and the Agent Log**: each nested call shows up, so you can see what the script did.
- **Loop limits**: `run_script` itself counts as **one** call toward *Max tool calls per prompt*, so even a script that calls no tools can't loop past the limit. The calls *inside* one script are capped by *Max tool calls per script*.

```
User prompt
    |
    v
Agent (LLM)
    |--> read_file, search_files, ...          ← direct calls, as before
    |--> run_script(code)                      ← counts as 1 call per prompt
            |
            |  QuickJS sandbox (no fs / net / timers)
            |--> tools.search_files(...)  ┐
            |--> tools.read_file(...)     ├─ same toggles, approvals, blacklist,
            |--> tools.find_references(...)┘  activity rows; ≤ max calls per script
            |
            v
         only what the script prints
```

## Turning it on

`run_script` is **off by default** while we test it across models and machines.

1. Open **Settings → Tools → DevoxxGenie → Agent**.
2. In **Built-in Tools**, tick **run_script — (Experimental)**.
3. Adjust **Max tool calls per script** underneath it if you need more (or fewer) than 100.

When it's off, the agent's tool chain is exactly what it was before 1.16.0. Like the other built-in tools, you can [edit the description](/docs/features/agent-mode#editing-a-tools-description) the model receives, for example to nudge a particular model toward or away from scripting.

### Which models?

Code mode works best with **strong coding models** that write correct JavaScript the first time: recent Claude, GPT and Gemini models, and the larger open-weight coding models. Smaller local models may be better off with plain tool calls. If a model writes a script with a syntax error, it gets the error back and can fix it in the next step.

## When it helps most

- **Checking every reference** of a symbol and keeping only the interesting ones.
- **Reading many small files** to answer one question about them.
- **Filtering large search results** down to a count or a short list.
- **Cross-checking** two listings, like "controllers without tests" or "keys in one file but not the other".

For a single lookup, or when you want to read the raw result yourself, the agent should (and usually does) just call the tool directly.

## Thank you, Omar

Code mode was suggested in #1352 and implemented in #1353 by [@omarmahamid](https://github.com/omarmahamid), including the sandbox, the tool signatures, the call tracking and 30 tests that run real scripts against fake tools (time budget, memory cap, cancellation, promises that never settle, and checks that `require`, `process`, `fetch` and `setTimeout` don't exist). It's a careful piece of work, and we're grateful for it.

Try it on a wide question in your own project and tell us how it goes, which models handle it well and where it falls short, in the [GitHub issues](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues). The full reference is in the [Agent Mode docs](/docs/features/agent-mode#batching-tool-calls-with-run_script-experimental).
