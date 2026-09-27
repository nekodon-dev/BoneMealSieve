# Bone meal overlays

Originally created with the built-in ImageGen tool, then sampled to 32×32. The user subsequently replaced both overlays with 16×16 PNGs. Minecraft's item model combines the untouched vanilla `minecraft:item/bone_meal` layer with these current overlays. Their alpha and alignment were checked with a 16×16 composite preview; the user's textures were preserved unchanged.

Saved assets:
- `src/main/resources/assets/bonemeal_sieve/textures/item/poor_bone_meal_overlay.png`
- `src/main/resources/assets/bonemeal_sieve/textures/item/quality_bone_meal_overlay.png`

## Final prompts

### Poor bone meal

Create a transparent pixel-art overlay texture for a Minecraft bone meal item. Output a square PNG with genuine alpha transparency. Logical pixel grid exactly 16x16, enlarged crisply if necessary with nearest-neighbor style: no antialiasing. Draw ONLY 10 sparse single-logical-pixel dark burnt-brown speckles, concentrated on the central bone meal pile area (x=4..11 y=5..12 in a 16x16 grid), with varied dark brown #57321e and warm brown #80502f. All other pixels transparent. No bone meal base, no background, no checkerboard, no outline, no text. The overlay will be layered atop the existing vanilla white bone meal sprite. Small uneven dark flecks, restrained enough to keep most white bone meal visible.

### Quality bone meal

Create a transparent pixel-art overlay texture for a high-quality Minecraft bone meal item. Output square PNG with genuine alpha transparency. Logical pixel grid exactly 16x16 enlarged crisply if necessary, nearest-neighbor aesthetic no antialiasing. Draw ONLY sparse pale buttery-yellow #fff0a0 and pale pastel-pink #ffd0e3 grains in the central pile area x=4..11 y=5..12, plus two tiny 3-pixel-wide cross-shaped sparkles with creamy white centers and pale yellow or pale pink arms near coordinates (10,4) and (5,10). About 16 colored logical pixels total, all remaining pixels completely transparent. No bone meal base, no background, no checkerboard, no outline, no text, no gradients or blurry glow. This is a subtle overlay atop the vanilla white bone meal sprite; preserve the appearance of tiny glinting grains rather than a large star icon.
