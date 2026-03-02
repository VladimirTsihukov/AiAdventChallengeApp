# Context Strategy Comparison Report

## Strategies Overview

| Strategy | Description |
|----------|-------------|
| **Summarization** | Compresses old messages via Haiku summarization, keeps recent N messages + summary prefix |
| **Sliding Window** | Keeps only last N messages, drops everything else — no API calls |
| **Sticky Facts** | Extracts key-value facts from old messages via Haiku, injects into system prompt |
| **Branching** | Allows creating checkpoints and dialog branches with independent message histories |

## Comparison Matrix

| Criteria | Summarization | Sliding Window | Sticky Facts | Branching |
|----------|:---:|:---:|:---:|:---:|
| **Context Quality** | ★★★★ | ★★ | ★★★★ | ★★★★★ |
| **Memory Stability** | ★★★★ | ★ | ★★★ | ★★★★★ |
| **Token Efficiency** | ★★★ | ★★★★★ | ★★★★ | ★★★ |
| **Latency** | ★★ | ★★★★★ | ★★ | ★★★★★ |
| **API Cost** | ★★ | ★★★★★ | ★★ | ★★★★ |
| **Ease of Use** | ★★★★★ | ★★★★★ | ★★★★★ | ★★★ |

## Detailed Notes

### Summarization
- **Pros**: Retains context through summaries, good for long conversations
- **Cons**: Extra API calls for summarization, slight information loss
- **Best for**: Long, continuous conversations where context matters

### Sliding Window
- **Pros**: Zero overhead, fastest, cheapest
- **Cons**: Complete loss of old context — agent has "amnesia"
- **Best for**: Quick Q&A sessions, independent questions

### Sticky Facts
- **Pros**: Compact representation of key information, good fact retention
- **Cons**: Facts extraction requires API call, may miss nuanced context
- **Best for**: Task-oriented conversations with clear goals and constraints

### Branching
- **Pros**: Full context preserved, explore alternative paths
- **Cons**: More complex UI, messages accumulate without compression
- **Best for**: Experimenting with different approaches, A/B testing prompts

## Test Results

_To be filled after manual testing_

| Strategy | Test Scenario | Remembered Context? | Tokens Used | Notes |
|----------|--------------|:---:|---:|-------|
| Summarization | | | | |
| Sliding Window | | | | |
| Sticky Facts | | | | |
| Branching | | | | |
