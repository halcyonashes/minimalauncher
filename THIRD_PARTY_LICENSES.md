# Bundled fonts

All fonts in `app/src/main/res/font/` are licensed under the SIL Open Font License 1.1,
which permits bundling and redistribution. Each font's copyright notice and full license text
is in `licenses/fonts/`.

| File | Family | Source | License |
|---|---|---|---|
| `inter.ttf` | Inter | https://github.com/rsms/inter | [`inter-OFL.txt`](licenses/fonts/inter-OFL.txt) |
| `jetbrains_mono.ttf` | JetBrains Mono | https://github.com/JetBrains/JetBrainsMono | [`jetbrains_mono-OFL.txt`](licenses/fonts/jetbrains_mono-OFL.txt) |
| `outfit.ttf` | Outfit | https://github.com/Outfitio/Outfit-Fonts | [`outfit-OFL.txt`](licenses/fonts/outfit-OFL.txt) |
| `space_grotesk.ttf` | Space Grotesk | https://github.com/floriankarsten/space-grotesk | [`space_grotesk-OFL.txt`](licenses/fonts/space_grotesk-OFL.txt) |
| `lora.ttf` | Lora | https://github.com/cyrealtype/Lora-Cyrillic | [`lora-OFL.txt`](licenses/fonts/lora-OFL.txt) |

Each is a single variable-weight (`wght` axis) TrueType file, retrieved from the
[google/fonts](https://github.com/google/fonts) repository. `theme/Fonts.kt` instances
Light/Regular/Medium via `FontVariation.weight(...)` rather than bundling separate static
weight files.
