# UseSuno MV Studio Android

A native Android port of [UseSuno MV Studio](https://github.com/UseSuno/UseSunoMVStudio) - a lyric video editor with animated templates, built in Kotlin with Jetpack Compose.

## Features Implemented

### ✅ Core Foundation
- **Gradle + Hilt DI** - Modern Android build with Kotlin DSL
- **Room Database** - Project & Asset persistence with auto-save drafts
- **Core Data Models** - Project, LyricLine, LyricWord, Template (20), Background (7), AudioAnalysis, Asset
- **11 Languages** - EN, ZH-CN, ZH-TW, ID, HI, PT-BR, TL, DE, UR, RU, VI

### ✅ Audio Pipeline
- **MediaCodec Decoder** - Hardware-accelerated audio decoding
- **FFT Analysis (JTransforms)** - 5 bands @ 30fps: bass (20-150Hz), low-mid (150-400Hz), mid (400-2500Hz), vocal (2500-5000Hz), treble (5000-20000Hz) + power envelope
- **PlaybackClock** - Synchronized audio playback with frame-accurate timing

### ✅ Lyrics System
- **LRC Parser** - Standard & enhanced (word-level timestamps)
- **TTML Parser** - Basic TTML support
- **Plain Text Parser** - Auto-segmentation by punctuation/length
- **Embedded Lyrics Extraction** - ID3 tags (SYLT, USLT, ©lyr)
- **Segmentation Utilities** - Split/merge lines, global offset, stretch to duration

### ✅ Render Engine
- **RenderEngine Interface** - Abstract renderer for templates
- **Canvas2DRenderer Base** - Typography, intro animations, word highlighting, audio reactivity
- **BackgroundRenderer Interface** - 7 background types

### ✅ Template Renderers (All 20)
| Template | Type | Description |
|----------|------|-------------|
| **Fume** | Particles | Flowing smoke/particles with audio reactivity |
| **Classic** | Typography | Clean animated text with subtle effects |
| **Cadenza** | Ribbons | Flowing wave ribbons |
| **Partita** | Notes | Musical notation inspired |
| **Tilt** | 3D Text | Perspective tilted text |
| **Claddagh** | Celtic | Interwoven Celtic knot patterns |
| **Monet** | Brushstrokes | Impressionist paint strokes |
| **Cappella** | Rings | Vocal-triggered expanding rings |
| **Diorama** | 2.5D Layers | Parallax depth layers |
| **Aurora Traverse** | Curtains | Magnetic aurora curtains |
| **Pendolo** | Pendulums | Physics-based pendulum waves |
| **Tempera** | Paint Drops | Wet paint blending simulation |
| **Sonnet** | Flourishes | Elegant decorative petals |
| **Still** | Minimal | Static text with fade |
| **Astral** | Starfield | 3D starfield with floating lyrics |
| **Kyoto** | Sumi-e | Japanese ink wash brush strokes |
| **Chroma** | RGB Shift | Color channel separation glitch |
| **Elysium** | Spirals | Golden ratio particles |
| **Syzygy** | Orbits | Planetary alignment mechanics |
| **Lumiere** | Light Rays | Cinematic rays & lens flares |

### ✅ Background Renderers (All 7)
- **Custom** - User image/video
- **Theme Space** - Animated radial gradients
- **Fluid Light** - Flowing liquid blobs
- **Aurora Nebula** - Rotating nebula clouds
- **Aurora Curtain** - Vertical wave curtains
- **Light Tide** - Horizontal wave tides
- **Space Starlight** - Parallax starfield

### ✅ Video Export
- **Frame-by-Frame** - MediaCodec H.264/VP8 + MediaMuxer (MP4/WebM, up to 4K @ 30fps)
- **Real-time Fallback** - MediaRecorder with TextureView
- **Foreground Service** - Background export with progress notification

### ✅ Project Bundles
- **`.lyricmv` Format** - ZIP compatible with web version
- **Contents**: `project.json` + `audio.{ext}` + `assets/{id}.{ext}`
- **Import/Export** - Full round-trip support

### ✅ UI (Jetpack Compose)
- **Editor Screen** - Stage preview, timeline, inspector panel
- **Inspector Sections** - Template, Background, Typography, Intro, Audio Reactivity, Export
- **Export Dialog** - Format, quality, FPS, resolution, aspect ratio
- **Timestamp Screen** - Word-level timing editor with waveform
- **Material 3 Dark Theme** - Matches web app aesthetic

### ✅ File Import
- **Audio Picker** - MediaStore integration (MP3, WAV, M4A, FLAC, OGG, OPUS)
- **Image/Video Picker** - For custom backgrounds
- **Metadata Extraction** - Title, artist, album, duration, cover art

### ✅ Font System
- **Google Fonts** - 18 font families via Downloadable Fonts API
- **Local Fonts** - TTF/OTF/WOFF/WOFF2 from project assets

### ✅ Testing
- **Unit Tests** - Lyrics parser, audio analyzer, project model, template renderers
- **Integration Tests** - Full lyrics workflow, LRC parsing

## Architecture

```
app/
├── data/
│   ├── db/                    # Room: ProjectEntity, AssetEntity
│   ├── persistence/           # ProjectBundleSerializer (.lyricmv)
│   └── repository/            # Data repositories
├── domain/
│   ├── model/                 # Core data models
│   ├── audio/                 # AudioDecoder, AudioAnalyzer, PlaybackClock, WaveformExtractor
│   ├── lyrics/                # LyricsParser, EmbeddedLyricsExtractor, LyricsSegmentation
│   └── usecase/               # Business logic
├── presentation/
│   ├── editor/                # MainEditorScreen
│   ├── components/            # StagePreview, Timeline, InspectorPanel, ExportDialog, WaveformView
│   └── timestamp/             # TimestampScreen (word-level timing)
├── render/
│   ├── engine/                # RenderEngine, Canvas2DRenderer, RendererFactory
│   ├── templates/             # 20 template renderers
│   ├── background/            # 7 background renderers
│   └── export/                # VideoExporter, RealtimeVideoRecorder, ExportService
├── utils/                     # FontManager, AudioFileImporter, MediaFileImporter
└── di/                        # Hilt modules
```

## Building

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease

# Tests
./gradlew test
./gradlew connectedAndroidTest
```

## Requirements
- Android Studio Ladybug (2024.2.1)+
- JDK 17
- Android SDK 35
- Min SDK 26 (Android 8.0)

## License
AGPL-3.0 (matching UseSunoMVStudio and Folia)