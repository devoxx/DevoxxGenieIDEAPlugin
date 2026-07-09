---
slug: mtplx-local-llm-apple-silicon
title: "MTPLX: 2x Faster Local LLMs on Apple Silicon, Wired Into DevoxxGenie"
authors: [stephanj]
tags: [mtplx, mlx, apple silicon, local llm, speculative decoding, qwen, custom openai, intellij idea, open source]
date: 2026-07-09
description: MTPLX uses a model's built-in multi-token prediction heads to roughly double local decode speed on Apple Silicon, with no draft model and no change to the output distribution. Here's how to point DevoxxGenie at it in about a minute.
keywords: [devoxxgenie, mtplx, mlx, multi-token prediction, speculative decoding, apple silicon, qwen 3.6, local llm, custom openai, openai compatible, intellij plugin]
image: /img/mtplx-dashboard.png
---

# MTPLX: 2x Faster Local LLMs on Apple Silicon, Wired Into DevoxxGenie

Running a 35B model on your laptop is impressive right up until you watch it type. Local inference on Apple Silicon is memory-rich and bandwidth-poor: every single token needs its own forward pass through the whole model, and that pass is dominated by shuffling weights from unified memory into the GPU. The math is fast. The waiting is not.

[MTPLX](https://mtplx.com) attacks exactly that bottleneck, and it does it without the usual trade-offs. On a Mac mini it lands around **1.6x faster decode**; on Qwen 3.6 27B the author measures up to **2.24x**. And it does that while producing the *same* output your model would have produced anyway.

<!-- truncate -->

![The MTPLX dashboard running Qwen 3.6 35B-A3B at 89 tokens per second](/img/mtplx-dashboard.png)

## How it gets the speedup

The trick is **multi-token prediction (MTP)**, a form of speculative decoding.

Ordinary speculative decoding runs a small, fast "draft" model to guess several tokens ahead, then has the big model verify those guesses in a single batched pass. When the guesses are right, you got several tokens for the price of one. It works, but you now have two models resident in memory, and on a 32GB Mac that second model is exactly the RAM you didn't have.

MTPLX skips the draft model entirely. Modern models like Qwen 3.5 and 3.6 ship with **built-in MTP heads**, extra prediction heads trained to guess the next few tokens. MTPLX uses the model to draft against itself. No second model, no extra memory, no separate download.

The other half is what happens to those drafted tokens. MTPLX verifies them with **exact rejection sampling** (the Leviathan & Chen scheme, with residual correction), which is a mathematically strong statement: at `temperature=0.6, top_p=0.95` the output distribution is *identical* to normal decoding. This is worth dwelling on, because a lot of "fast local inference" tricks quietly drop you to greedy decoding and hand you a subtly different, flatter model. MTPLX doesn't. Same model, same sampling behaviour, roughly half the wall-clock.

The dashboard above makes the mechanism legible. That **acceptance** bar, `mean P=77.8%`, is the draft hit rate: about three out of four speculated tokens survive verification and get committed for free. That number is what turns into the 89 tokens/sec on the dial. When acceptance drops, so does throughput, and MTPLX shows you rather than hides it.

There's an **auto-tune** step too. Rather than assuming a universal optimal draft depth, MTPLX benchmarks *your* Mac across depths and picks what actually wins on your memory bandwidth. An M4 Mac mini and an M5 Max have very different sweet spots.

## What you need

- Apple Silicon (M1 or newer), since this is MLX and therefore Mac-only by construction
- macOS 14+
- 16GB RAM minimum; 32GB+ if you want the 27B and 35B models
- Free, Apache-2.0 licensed, built by [Youssof Altoukhi](https://github.com/youssofal/MTPLX)

Grab the DMG from [mtplx.com](https://mtplx.com), drag it to Applications, and it handles the rest: it checks your hardware and recommends a model that genuinely fits your memory rather than one that will swap you into misery. There's a CLI too, via Homebrew or pip, if you'd rather live in the terminal.

Models come pre-converted on Hugging Face under [Youssofal](https://huggingface.co/Youssofal): Qwen 3.5 (4B, 9B), Qwen 3.6 (27B, and the 35B-A3B MoE), plus Gemma 4, each in *speed*, *balance*, and *quality* builds. And if your favourite model isn't in the catalogue, **Forge** converts a Hugging Face model into an MTP-optimized MLX build and reports honest before/after numbers, so you can see whether the conversion actually bought you anything.

## Pointing DevoxxGenie at it

MTPLX serves an **OpenAI-compatible API** (`/v1/chat/completions`, `/v1/completions`) and an **Anthropic-compatible** `/v1/messages`, both with streaming. That's all DevoxxGenie needs.

The server settings live in the MTPLX app, and the two fields that matter are the host/port and the generation mode:

![MTPLX server settings: host 127.0.0.1, port 8001, generation mode MTP](/img/mtplx-server-config.png)

The default port is **8000**. I moved mine to **8001** to keep it out of the way of everything else that thinks it owns port 8000. Leave **Generation mode** on `MTP`, because `Baseline` is there so you can A/B the speedup for yourself. That's a nice touch of intellectual honesty for a project whose entire pitch is "it's faster."

Now, in DevoxxGenie:

1. Open **Settings → LLM Providers** and pick **CustomOpenAI**
2. Set the base URL to `http://127.0.0.1:8001/v1/` (match whatever port you configured)
3. **Leave the model name blank**
4. No API key needed, since MTPLX is listening on loopback

Step 3 is the one people trip on. Just like **LM Studio**, MTPLX decides which model is loaded from *its own* UI, not from the client. So you don't name a model in DevoxxGenie. Leave the field empty, and the plugin probes the endpoint's `/models` and shows you whatever MTPLX currently has in memory.

## It just works, including agent mode

Here's DevoxxGenie talking to a locally-served Qwen 3.6 35B-A3B through MTPLX, with **agent mode on**:

![DevoxxGenie chatting with MTPLX-served Qwen 3.6 35B, showing a run_command tool call and a thinking block](/img/mtplx-chat-devoxxgenie.png)

The model reasons about the question, decides it should check the real system clock rather than trust its own assumptions, calls the `run_command` tool, and answers from the result. That's 2.6 seconds end to end, 10.9K tokens in, 132 out. Tool calling, streaming, and the thinking block all come through the OpenAI-compatible path unchanged. Nothing about this is a special case: to DevoxxGenie it's just another OpenAI endpoint that happens to be running on your own GPU.

Which is the actual point. A 35B MoE, doing real agentic tool use, at ~89 tokens/sec, entirely on-device. No API key, no per-token bill, no code leaving the laptop.

## An open question: what if it spoke DFlash, or DSpark?

MTP is not the last word in speculative decoding, and this is where it gets interesting. Two newer drafters both explicitly benchmark themselves *against* native MTP, and both beat it.

**[DFlash](https://arxiv.org/abs/2602.06036)** (Chen, Liang & Liu at Z Lab, ICML 2026) attacks MTP's sequential-ness. MTP and EAGLE-style drafters are still *autoregressive*: they guess token *n+2* conditioned on their own guess for *n+1*, so errors compound along the unverified draft. DFlash instead uses a lightweight **block diffusion** draft model that emits an entire block of draft tokens in one parallel forward pass, conditioned on the target model's hidden states via KV injection. No sequential chain, so no error accumulation down it. It reports over **6x lossless acceleration** and end-to-end speedups up to **2.5x greater than EAGLE-3**, and [LMSYS measured](https://www.lmsys.org/blog/2026-06-15-next-generation-speculative-decoding-dflash-v2/) a DFlash drafter on Qwen 3.5 397B-A17B running at roughly **1.5x the throughput of that model's native MTP** on HumanEval at concurrency 1.

**[DSpark](https://arxiv.org/abs/2607.05147)** (Xin Cheng et al. at DeepSeek, June 2026) then points out what pure parallel drafting *costs* you. Predicting every position in a block independently causes **suffix decay**: acceptance falls off a cliff at the later positions, because nothing tells token 5 what token 4 turned out to be. DSpark's fix is **semi-autoregressive** generation. A parallel backbone produces hidden states and base logits for the whole block in one pass, then a very cheap sequential head (a low-rank Markov transition) puts the intra-block dependency back. You get parallel drafting that still knows what it just said.

The result is measured directly in **accepted length**, the same quantity as that acceptance bar on the MTPLX dashboard. Against a Qwen3-4B target, DSpark improves macro-average accepted length by **~31% over EAGLE-3 and ~16% over DFlash**. In DeepSeek's own V4 production serving, swapping MTP-1 for DSpark sped up per-user generation by **60–85%** at matched throughput, with the target model untouched: no retraining, no quantization, and the output distribution preserved exactly by rejection sampling. Same losslessness guarantee MTPLX already gives you.

So: **what would MTPLX look like on top of one of these?**

Three things make the question more than idle speculation. Local decode on a Mac is **memory-bandwidth-bound**, and MTP's drafting is sequential, so every speculated token is another trip through the heads. Both DFlash and DSpark collapse that into a single pass, which is exactly the axis a bandwidth-starved machine cares about. Second, DeepSeek shipped **[DeepSpec](https://github.com/deepseek-ai/DeepSpec)**, an MIT-licensed full-stack codebase for training and evaluating drafters, with ready-made EAGLE-3, DFlash *and* DSpark checkpoints. Third, those checkpoints include **`google/gemma-4-12b-it`**, and Gemma 4 is already in MTPLX's catalogue.

### llama.cpp is already most of the way there

This has stopped being hypothetical. Over on **llama.cpp**, MTP landed back in May ([#22673](https://github.com/ggml-org/llama.cpp/pull/22673)), EAGLE-3 followed in June ([#18039](https://github.com/ggml-org/llama.cpp/pull/18039)), and **DFlash was merged on 28 June** ([#22105](https://github.com/ggml-org/llama.cpp/pull/22105)), with refinements since. You can run block-diffusion speculative decoding there today.

**DSpark is not merged yet.** It's [PR #25173](https://github.com/ggml-org/llama.cpp/pull/25173) by `wjinxu`, open and under active review at the time of writing. It's a small patch, around 280 added lines, because it layers straight on top of the merged DFlash implementation: same encoder/decoder graph, same target-layer feature extraction, same KV-cache injection and verify path. The only real addition is the low-rank Markov head that biases each draft token on the one actually sampled before it, turning DFlash's independent per-position argmax into left-to-right semi-autoregressive sampling. Note that this is **phase one**: the confidence head is converted and loaded but not yet used at inference, so the load-aware scheduler half of the paper isn't wired up.

And that is exactly what makes its numbers so interesting for us. Benchmarked on Qwen3-8B with `speed-bench`, greedy, **at `-np 1`, which is to say concurrency one**, DSpark reaches **1.88x decode speedup over baseline** where the merged DFlash gets **1.55x**. Against DFlash directly it wins **1.21x overall, and on every one of the 11 categories**, peaking at 1.28x on roleplay and 1.26x on QA.

That's the answer to the question I was about to hand-wave. The semi-autoregressive acceptance gain shows up **without the scheduler, at a single user**, which is precisely the regime a developer running DevoxxGenie against a local server lives in.

Now the honest catches, of which there are two.

The first is memory. Both methods need a separately trained draft module, precisely the thing MTPLX proudly does *not* have. "No external drafter" isn't marketing on a 16GB Mac; it's the difference between a model fitting and not fitting. And the published checkpoints target Qwen3-4B/8B/14B, not the Qwen 3.5/3.6 builds MTPLX actually serves, so someone would have to train new drafters. DeepSpec's README warns that the target cache for the *default* Qwen3-4B recipe runs to roughly **38 TB**. This is not a weekend project.

The second is that only half of DSpark is aimed at you. The confidence-scheduled verifier is built for **high-concurrency serving**, sizing each request's verify budget against live engine load. Running DevoxxGenie against a local MTPLX server, you are concurrency *one*. As the authors themselves note, at low fixed load you get the semi-autoregressive acceptance gains but little from the scheduler, since verifying extra tokens is nearly free when the GPU is otherwise idle. So treat that headline "60–85%" as what it is: a *matched-throughput, many-user* number, not one a single developer on a MacBook should expect.

The good news is that the half aimed at you is the half that already works. llama.cpp's phase-one port skips the scheduler entirely and still lands 1.21x over DFlash at a single user, which is about as clean a controlled experiment as you could ask for. And the ported gain is hardware-agnostic in the way that matters: more accepted tokens per verification pass is more accepted tokens per verification pass, whether you're serving ten thousand users or exactly yourself. It would show up straight away on that dashboard dial.

MTPLX already has most of the harness to find out: `Forge` for converting models, and the `MTP`/`Baseline` toggle for an honest A/B. There's now a merged DFlash implementation and an open DSpark one to read from. Consider this an open invitation to [@youssofal](https://github.com/youssofal/MTPLX). We'd love to see the numbers.

## Should you use it?

If you're on an Apple Silicon Mac and you already run local models, MTPLX is close to a free lunch. The speedup is real, the memory cost is zero, and the output distribution is provably unchanged. The main constraint is the model catalogue: MTP needs those built-in prediction heads, so you're choosing from Qwen 3.5/3.6 and Gemma 4 (or forging your own). MTPLX will refuse to run an incompatible model rather than silently fall back to slow decoding, which I'd take over the alternative every time.

Pair it with DevoxxGenie's agent mode and you have a genuinely capable, fully local coding assistant that costs nothing to run.

- MTPLX: [mtplx.com](https://mtplx.com) · [GitHub](https://github.com/youssofal/MTPLX)
- DevoxxGenie is [open source on GitHub](https://github.com/devoxx/DevoxxGenieIDEAPlugin) · issues, ideas, and stars all welcome.
