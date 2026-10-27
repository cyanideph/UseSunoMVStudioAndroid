# UseSuno MV Studio Lyric Animation Engine Port

Port the exact lyric animation engine, kinetic typography motion curves, line transitions, and audio-reactive typographic choreography from the open-source UseSuno MV Studio (Folia) engine into the native Android Canvas render pipeline across all 20 built-in templates.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions incorporate your confirmed requirements from the planning interview:

- **Template Scope (Confirmed)**: Full parity across **all 20 built-in templates** (`Classic`, `Fume`, `Tilt`, `Cadenza`, `Partita`, `Chroma`, `Astral`, `Kyoto`, `Lumiere`, `Cappella`, `Diorama`, `Aurora Traverse`, `Pendolo`, `Tempera`, `Sonnet`, `Still`, `Elysium`, `Syzygy`, `Claddagh`, and `Monet`).
- **Word Synchronization Style (Confirmed)**: Exact Folia/UseSuno source clone rather than traditional karaoke bounding boxes. Active words receive luminescence scaling, spoken/unspoken opacity differentiation, and audio-reactive pulse without crude yellow highlight boxes.
- **Line Arrangement & Motion Dynamics (Confirmed)**: Faithful recreation of the source template layout behaviors. Multi-line transitions use smooth vertical translation with ease-out interpolation (dimmed previous line gliding up, active line centered with full contrast, dimmed upcoming line gliding into position), and templates with custom 3D/canvas spatial transformations (`Tilt`, `Fume`, `Astral`, `Partita`) handle their lyric geometry natively without duplicate overlays.
- **Double-Rendering Fix (Critical Architecture)**: Currently `Canvas2DRenderer` draws lines once in `renderTemplate()` and a second time in `renderLyrics()`. The pipeline will be refactored into a unified lyric rendering contract where each template either provides its own custom typographic stage or inherits the enhanced Folia multi-line engine.

---

## 1. Overview & Core Concept

### What It Does
This feature replaces the rudimentary text drawing in `Canvas2DRenderer` and the template subclasses with the exact visual and typographic behavior of **UseSuno MV Studio** (derived from the **Folia** engine). It delivers:
1. **Continuous Time & Motion Windows**: Eliminates harsh pop-in/pop-out transitions by calculating smooth entry, active focus, and lingering exit phases for each line.
2. **True Source Word Synchronization**: Words in active lines are measured and rendered with precise typographic states:
   - **Spoken words**: Crisp text color, high opacity (0.95), resting baseline.
   - **Active word**: High-contrast luminescence, subtle scale accent (1.05x-1.12x), optional audio-reactive glow, and baseline lift.
   - **Upcoming words**: Subdued ambient contrast (0.40-0.50 opacity).
3. **Template-Specific Kinetic Typographic Identity**: Each of the 20 templates receives its authentic visual treatment (e.g., chromatic RGB separation in `Chroma`, 3D tilted plane with depth grid in `Tilt`, atmospheric drift in `Fume`, brush stroke ink bleed in `Kyoto`, radiant light sweeps in `Lumiere`).

### Target Audience & Persona
Music video creators, Suno/Udio AI music producers, and social media creators generating vertical (9:16) or widescreen (16:9) lyric visualizers directly on Android mobile and tablet devices.

---

## 2. User Experience & Visual Design

### Kinetic Typography & Motion Dynamics

```
[ Incoming Line: -40dp Y, Alpha 0.35, Scale 0.90 ]
                       │
                       ▼ (Smoothstep easing transition)
[ ★ ACTIVE LINE ★:   0dp Y, Alpha 1.00, Scale 1.00 + Bass Pulse ]
  Words: [Spoken: 90% α] [★ CURRENT: 100% α + Glow + 1.08x ★] [Future: 45% α]
                       │
                       ▼ (Smoothstep easing transition)
[ Outgoing Line: +40dp Y, Alpha 0.25, Scale 0.90 ]
```

- **Line Easing & Scroll Physics**: Line transitions use non-linear cubic easing (`f(t) = t * t * (3 - 2 * t)` smoothstep) to transition lines seamlessly as timestamps progress.
- **Audio Pulse Integration**: Bass frequency bands (`20Hz - 150Hz`) subtly modulate the active word and line scale (up to +4-8%) without jitter, creating a rhythmic breathing sensation synchronized with the music.
- **Edge-to-Edge Responsive Layout**: Lyrics automatically adapt to Canvas bounds for `16:9` (YouTube), `9:16` (TikTok/Shorts/Reels), and `1:1` (Instagram) with dynamic line wrapping, font scale clamping, and safe-area margins.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Unified Typographic Engine vs. Per-Template Monoliths
- **Chosen Approach**: A centralized, high-performance `FoliaLyricEngine` that calculates spatial transforms, line interpolation, word layouts, and alpha/scale states, coupled with pluggable `LyricVisualShader` hooks for templates that apply custom spatial matrices (e.g. `Tilt` 3D rotation, `Chroma` RGB split, `Partita` stepped blocks).
- **Why**: Eliminates 800+ lines of duplicated code across 20 renderers while guaranteeing consistent frame-rate (60 FPS) and preventing double-drawing bugs.
- **Alternatives Considered**: Modifying all 20 renderers independently would lead to fragmented timing logic, maintenance overhead, and audio desync.

### Decision 2: Pure Android Canvas 2D Vector Drawing (Zero External Render Dependencies)
- **Chosen Approach**: Native `android.graphics.Canvas`, `Paint`, `Path`, `Camera` (for 3D tilt transformations), and `ColorMatrix` / `PorterDuff` blend modes.
- **Why**: Highly optimized on Android, zero extra APK weight, hardware accelerated, and runs flawlessly inside both the Compose preview viewport and background video MP4 export pipeline.
- **Alternatives Considered**: WebViews or OpenGL shaders would add immense latency, memory overhead, and video export encoding complexities.

---

## 4. Technical Architecture & Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                 RenderEngine (Playback Clock)               │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Canvas2DRenderer                       │
│  - BackgroundRenderer (Nebula, Fluid, Aurora, etc.)         │
│  - Audio Analyzer Frame (Bass, Mid, Treble, Power)          │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               ▼                               ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│      Template Background     │ │      FoliaLyricEngine      │
│   (Particles, Ribbons, Grid) │ │  - Active Line Windowing   │
└──────────────────────────────┘ │  - Cubic Transition Curves │
                                 │  - Word Measurement Cache  │
                                 │  - Luminescence & Alpha    │
                                 └──────────────┬─────────────┘
                                                │
                                                ▼
                        ┌──────────────────────────────────────────────┐
                        │      Template-Specific Typographic FX        │
                        ├──────────────────────────────────────────────┤
                        │ • Classic: Centered Luminescence + Bass Pop  │
                        │ • Fume: Smoke Dispersal + Drift              │
                        │ • Tilt: 3D Camera Rotation Matrix (Pitch/Yaw)│
                        │ • Chroma: Tri-Color RGB Channel Shift Glitch │
                        │ • Cadenza: Wave Ribbon Baseline Interaction  │
                        │ • Partita: Ascending Stair-Step Stanza Blocks│
                        │ • Astral: Starfield Z-Depth Projection       │
                        │ • Kyoto: Sumi-e Ink Bleed Contours           │
                        │ • Lumiere: Cinematic Ray Flare Sweeps        │
                        │ • Cappella: Vocal Ripple Cones               │
                        │ • Diorama: Parallax Depth Planes             │
                        │ • Aurora: Wave Tint Sync                     │
                        │ • Pendolo: Harmonic Oscillation Baselines    │
                        │ • Tempera: Watercolor Droplet Diffusion      │
                        │ • Sonnet: Ornamental Serif Flourishes        │
                        │ • Still: Swiss Minimalist Breathing          │
                        │ • Elysium: Golden Ratio Spiral Alignment     │
                        │ • Syzygy: Orbital Planetary Trajectories     │
                        │ • Claddagh: Celtic Ribbon Braids             │
                        │ • Monet: Impressionist Light Dabs            │
                        └──────────────────────────────────────────────┘
```

---

## 5. Execution Stages

1. **Phase 1: Folia Core Lyric Engine (`FoliaLyricEngine.kt`)**
   - Implement `FoliaLyricEngine` with continuous time windowing, line boundary transitions, cubic smoothstep interpolation, and word-state evaluation.
   - Implement word measuring, cache calculations, and multi-line vertical stacking.
   - Refactor `Canvas2DRenderer` to eliminate duplicate `renderLyrics()` calls and decouple template background drawing from typography rendering.

2. **Phase 2: Template-Specific Typographic Porting (All 20 Templates)**
   - Update `ClassicRenderer`, `FumeRenderer`, `TiltRenderer`, and `CadenzaRenderer` to use the unified Folia typography pipeline with their exact source motion characteristics.
   - Update remaining 16 templates (`Partita`, `Chroma`, `Astral`, `Kyoto`, `Lumiere`, `Cappella`, `Diorama`, `AuroraTraverse`, `Pendolo`, `Tempera`, `Sonnet`, `Still`, `Elysium`, `Syzygy`, `Claddagh`, and `Monet`) with matching kinetic behaviors and audio-reactive properties.

3. **Phase 3: Inspector Controls & Tuning Verification**
   - Ensure `project.templateTuning` (speed, intensity, complexity, color shift) dynamically governs the lyric motion, scale punch, and transition pacing across all templates.
   - Verify both standard LRC (line-level) and enhanced LRC (word-level) timing.

4. **Phase 4: Compilation & Playback Verification**
   - Validate with `compile_applet`.
   - Test seamless playback and video export rendering.
