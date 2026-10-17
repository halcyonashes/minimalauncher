# Bundled fonts

All fonts in `app/src/main/res/font/` are licensed under the SIL Open Font License 1.1,
which permits bundling and redistribution. Full license text: https://openfontlicense.org

| File | Family | Source |
|---|---|---|
| `inter.ttf` | Inter | https://github.com/rsms/inter |
| `jetbrains_mono.ttf` | JetBrains Mono | https://github.com/JetBrains/JetBrainsMono |
| `outfit.ttf` | Outfit | https://github.com/Outfitio/Outfit-Fonts |
| `space_grotesk.ttf` | Space Grotesk | https://github.com/floriankarsten/space-grotesk |
| `lora.ttf` | Lora | https://github.com/cyrealtype/Lora-Cyrillic |

Each is a single variable-weight (`wght` axis) TrueType file, retrieved from the
[google/fonts](https://github.com/google/fonts) repository. `theme/Fonts.kt` instances
Light/Regular/Medium via `FontVariation.weight(...)` rather than bundling separate static
weight files.
