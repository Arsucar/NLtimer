# 2026-05-17 feature/ai-inter review

## Findings

1. `app/src/main/java/com/nltimer/app/experimental/ai_inter/viewmodel/AiInterViewModel.kt:200`

   The chat request now builds a forced system message from the current time, tool rules, and `cfg.promptChat`, but `cfg.promptNotes` and `cfg.promptTaskGen` are still only saved by `updatePrompts()` and never used in any request path. The settings UI exposes all three prompt fields, so users can edit "notes" and "task generation" prompts with no runtime effect. That is a product-level regression for the AI configuration screen because two persisted settings are dead knobs.

   Recommended fix: either route `promptNotes` and `promptTaskGen` into the workflows they are meant to control, or hide/disable those fields until the corresponding flows exist.

## Notes

- The newly added tool names referenced by `TOOLS_SYSTEM_PROMPT` are present in the tree and Hilt multibindings appear to register the timing/library tools.
- This review did not run Gradle verification yet; it only records the blocking issue found in the current diff.
