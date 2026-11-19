# ChatOn App — Final Year Project Viva & Defense Guide

**Student Candidate:** Om Thakur  
**Portfolio / Project Hub:** [omthakur.in](https://omthakur.in)  
**Project Title:** ChatOn — An On-Device, Privacy-First Generative AI Mobile Application  
**Target Platform:** Android (Kotlin, Jetpack Compose, C++ llama.cpp, AIDL)  

---

## 🎯 1. Project Abstract & Motivation

### Abstract
With the exponential rise of Large Language Models (LLMs), mainstream conversational AI solutions rely almost exclusively on cloud infrastructure (e.g., OpenAI, Google Cloud). This centralized paradigm introduces critical concerns: **data privacy exposure**, **mandatory internet connectivity**, **recurring token subscription costs**, and **vendor lock-in**.

**ChatOn App** demonstrates that contemporary consumer mobile hardware possesses sufficient compute and memory bandwidth to execute quantized, open-weight generative models directly on-device. By pairing high-performance native C++ kernels (`llama.cpp`) with an elegant, reactive Jetpack Compose interface in a clean **White & Blue** design language, ChatOn delivers an offline, zero-latency-to-network AI chat assistant.

---

## 💡 2. Core Technical Innovations (For Viva Presentation)

### A. Dual-Process Architecture with AIDL IPC
- **The Problem:** Running native C++ inference in the main Android process exposes the application to fatal SIGSEGV or OOM (Out-Of-Memory) crashes if a model exceeds available RAM.
- **The ChatOn Solution:** The app implements a **multi-process design**:
  1. **Main Process (`in.omthakur.chaton`)**: Manages UI state, Jetpack Compose layouts, animations, Room DB persistence, and document parsing.
  2. **Worker Process (`:llama`)**: An isolated Android Service (`LlamaService`) hosting native llama.cpp handles.
  3. **AIDL Interface (`com.druk.llamacpp.InferenceClient`)**: Bridges the two processes asynchronously. If native code crashes, Android OS terminates only `:llama`, while the UI gracefully displays an "Interrupted" banner and offers a one-tap reload.

### B. 4-Bit Integer Quantization (GGUF Format)
- Standard 16-bit FP16 models require ~2 GB RAM per 1 Billion parameters (e.g., a 7B model requires 14 GB RAM).
- Using **Q4_K_M quantization** (4-bit weights with quantized scales), memory consumption shrinks by ~75% down to ~4.5 GB for a 7B model and under 1.5 GB for 1B/2B models, fitting comfortably into modern smartphone RAM.

### C. On-Device Retrieval-Augmented Generation (RAG)
- Document parsers (`pdfbox-android`, `jsoup`, XML pull parser) extract textual content from user-uploaded PDFs, Word documents, and text files.
- Text is split into overlapping chunks (~1000 characters) and passed to an on-device embedding model (e.g., EmbeddingGemma).
- Normalized float vectors are indexed in Room SQLite. During chat, brute-force cosine similarity computes top-$K$ relevant context chunks in milliseconds without external vector databases.

### D. Modern White & Blue UI with Compose Spring Motion
- Re-architected with a clean White & Blue palette (`#1E40AF` Royal Blue, `#2563EB` Electric Azure, `#FFFFFF` Surface, `#F0F7FF` Assistant Card).
- Integrated fluid animations:
  - Spring-based entrance for user and assistant messages.
  - Multi-dot wave thinking indicator with sinus-wave scale and opacity interpolation.
  - Morphing circular action button that scales dynamically on user interaction.

---

## ❓ 3. Frequently Asked Examiner Questions & Model Answers

### Q1: "Why did you build an on-device app instead of using OpenAI or Gemini APIs?"
> **Answer:** "Cloud APIs require constant internet access, charge per token, and most critically, send sensitive user queries to external corporate servers. In sectors like healthcare, enterprise confidentiality, and developing regions with intermittent connectivity, on-device AI guarantees 100% data privacy and works entirely offline."

### Q2: "How does ChatOn achieve fast token generation on mobile CPUs?"
> **Answer:** "ChatOn optimizes inference through three core layers:
> 1. **ARM NEON & KleidiAI Vector Extensions:** Vectorized matrix multiplications optimized for ARM Cortex-A and Cortex-X cores.
> 2. **Vulkan Compute Shaders:** Offloads tensor computations to the mobile Adreno or Mali GPU.
> 3. **Memory-Mapped Files (`mmap`):** Model weights are mapped directly from flash storage into virtual memory, avoiding redundant RAM allocations."

### Q3: "What happens when a user asks a question about an attached PDF?"
> **Answer:** "The document is processed entirely locally. We extract text using PdfBox, segment it into overlapping chunks, generate 256-dimensional embeddings with an on-device embedding model, and calculate cosine similarity against the user query. The top 3 most relevant passages are injected into the model prompt context before generation begins."

### Q4: "How does ChatOn display DeepSeek R1's reasoning process?"
> **Answer:** "Reasoning models output their thought process enclosed within `<think>...</think>` tokens. ChatOn parses the streaming output in real-time, splits the thinking tokens from the final answer, and renders the thought process inside an expandable, animated reasoning capsule with token statistics and execution timers."

---

## 📋 4. Demonstration Checklist for Project Viva

1. **Launch App:** Show the custom **ChatOn** launcher icon with the white-and-blue AI sparkle logo.
2. **Open Drawer:** Demonstrate the "ChatOn by Om Thakur • omthakur.in" brand header and session list.
3. **Model Selection:** Show the catalog of supported models (Qwen, Gemma, DeepSeek, Llama).
4. **Chat Execution:** Type a prompt, showing:
   - The interactive circular send button spring animation.
   - The wave pulsating thinking indicator.
   - The crisp royal blue user bubble and ice-blue assistant card.
5. **Document Q&A:** Attach a sample PDF document, query it, and show contextual answer retrieval.
6. **Settings Screen:** Open Settings $\to$ display Developer profile card ("Om Thakur • omthakur.in") and local Privacy Policy.
