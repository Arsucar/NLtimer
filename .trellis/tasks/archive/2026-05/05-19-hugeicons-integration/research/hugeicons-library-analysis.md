# Research: HugeIcons Compose Library Analysis

- **Query**: Understand HugeIcons Compose library structure, icon count, naming, categories, and how the `HugeIcons` object works
- **Scope**: External (GitHub repo + official site + API)
- **Date**: 2026-05-19

## Findings

### Library Overview

- **GitHub repo**: https://github.com/rikkahub/hugeicons-compose
- **Package**: `me.rerere.hugeicons` (published via JitPack as `com.github.rikkahub:hugeicons-compose`)
- **Namespace**: `me.rerere.hugeicons`
- **Style**: Only `stroke-rounded` style currently supported
- **minSdk**: 24, **compileSdk**: 36, **JVM Target**: 11

### Total Icon Count

| Source | Count |
|---|---|
| Official API (`hugeicons.com/api/icons`) | **5,131 icons** |
| CLAUDE.md in repo | 4,500+ icons |
| Hugeicons.com total (all styles) | 51,000+ |
| Stroke-rounded (free) style | 5,131 |
| Files currently in library `stroke/` dir | 1,000+ (GitHub API pagination limit hit at 1000) |

The library auto-generates Kotlin code for ALL 5,131 stroke-rounded icons from the HugeIcons API.

### How the `HugeIcons` Object Works

The `HugeIcons` object is a **bare Kotlin `object`** with no internal properties:

```kotlin
// library/src/main/java/me/rerere/hugeicons/HugeIcons.kt
package me.rerere.hugeicons

object HugeIcons
```

Each icon is an **extension property** on `HugeIcons`, defined in its own file under `me.rerere.hugeicons.stroke`:

```kotlin
// library/src/main/java/me/rerere/hugeicons/stroke/Earth.kt
package me.rerere.hugeicons.stroke

import me.rerere.hugeicons.HugeIcons
// ... other imports

val HugeIcons.Earth: ImageVector
    get() {
        if (_earth != null) {
            return _earth!!
        }
        _earth = ImageVector.Builder(
            name = "Earth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // SVG path commands converted to Compose
            }
        }.build()
        return _earth!!
    }

private var _earth: ImageVector? = null
```

**Key design**: Lazy-loaded + cached via nullable private var. Each icon is one file. All icons share the `HugeIcons` receiver via extension properties.

### Icon Naming Convention

| Source Format | Target Kotlin Name | Example |
|---|---|---|
| kebab-case (`ai-search-02`) | PascalCase (`AiSearch02`) | `HugeIcons.AiSearch02` |
| Starts with digit (`01-key`) | Prefixed with "Icon" (`Icon01Key`) | `HugeIcons.Icon01Key` |
| All special chars stripped and replaced with `-` | Then PascalCase conversion | — |

Conversion is handled by `toValidKotlinName()` in `library/gen/src/convert.ts`.

### Package Structure

```
library/src/main/java/me/rerere/hugeicons/
├── HugeIcons.kt           # bare object (receiver for extension props)
└── stroke/                # 5000+ icon files, one per icon
    ├── Abacus.kt
    ├── AiSearch.kt
    ├── ArrowDown01.kt
    ├── Earth.kt
    └── ... (5131 files)
```

### Official HugeIcons Categories (from API)

The HugeIcons API returns a `category` field for each icon. Here are all 62 official categories with icon counts:

| Category | Count | Category | Count |
|---|---|---|---|
| editing | 435 | date-time | 81 |
| business | 377 | furnitures | 80 |
| communications | 246 | programming | 78 |
| files-folders | 210 | image-camera | 76 |
| devices | 195 | crypto | 75 |
| logos | 193 | wifi | 73 |
| arrows | 182 | media | 72 |
| hands | 168 | users | 70 |
| e-commerce | 149 | clothing | 64 |
| mathematics | 148 | emojis | 62 |
| games | 135 | hierarchy | 60 |
| foods | 126 | layout | 57 |
| education | 115 | gym | 50 |
| maps | 107 | kitchen | 46 |
| energy | 100 | islamic | 45 |
| security | 99 | settings | 45 |
| weather | 96 | bookmark | 42 |
| logistics | 93 | filter-sorting | 39 |
| ai | 93 | check | 39 |
| mouse | 88 | award | 38 |
| medical | 88 | alert | 37 |
| buildings | 82 | legal | 36 |
| | | animation | 35 |
| | | add-remove | 29 |
| | | notes-tasks | 29 |
| | | space | 28 |
| | | menu | 28 |
| | | link-unlink | 27 |
| | | dashboard | 24 |
| | | download-upload | 24 |
| | | science-technology | 22 |
| | | home | 21 |
| | | search | 16 |
| | | login-logout | 16 |
| | | shapes | 16 |
| | | presentation | 15 |
| | | git | 11 |

### Suggested Categories for a Picker UI (200-400 icons)

For a timer/productivity app, I recommend selecting from these high-value categories. The goal is ~200-350 icons:

| Picker Category | Source Category(s) | Suggested Count | Notes |
|---|---|---|---|
| **Navigation** | arrows (182→~30), add-remove (29→~10) | ~40 | Pick most common arrows + add/remove |
| **Communication** | communications (246→~40) | ~40 | Chat, call, mail, notification icons |
| **Media** | media (72→~20), image-camera (76→~10) | ~30 | Play, pause, camera, music |
| **Files & Docs** | files-folders (210→~25) | ~25 | Core file/document icons |
| **Business** | business (377→~30) | ~30 | Briefcase, analytics, office |
| **Users** | users (70→~20) | ~20 | Person, group, team icons |
| **Settings** | settings (45→~15), security (99→~10) | ~25 | Gear, lock, key, shield |
| **Date & Time** | date-time (81→~20) | ~20 | Calendar, clock, alarm |
| **Weather** | weather (96→~20) | ~20 | Sun, cloud, rain, snow |
| **Devices** | devices (195→~20) | ~20 | Phone, laptop, bluetooth |
| **UI Core** | check (39), alert (37), bookmark (42), search (16), menu (28) | ~30 | Essential UI elements |
| **Editing** | editing (435→~20) | ~20 | Copy, paste, cut, align |
| **Education** | education (115→~10) | ~10 | Book, graduation, study |
| **Maps & Location** | maps (107→~10) | ~10 | Map, compass, pin |
| **E-Commerce** | e-commerce (149→~10) | ~10 | Cart, store, tag |
| **Energy & Power** | energy (100→~10) | ~10 | Battery, plug, solar |
| **Health** | medical (88→~10) | ~10 | Heart, medicine, hospital |
| **Misc** | shapes (16), login-logout (16), download-upload (24) | ~20 | Shapes, actions |
| **Total** | | **~350** | |

### Recommended Subset Strategy

1. **DO NOT include** the full 5,131 icons — too many for a picker UI
2. **Target 300-350 icons** selected across categories
3. **Selection criteria**:
   - For categories with many variants (e.g., `ArrowDown01` through `ArrowDown05`), pick only 1-2 best variants
   - Skip brand logos (Adobe, Apple, Bitcoin etc.) unless specifically needed
   - Skip niche categories (islamic, crypto) unless user requests
   - Prefer icons with clear, universally recognizable symbols
4. **The API returns category data** — we can use it to group icons in the picker without manual categorization

### How to Use in Compose

```kotlin
// Simple usage
Icon(HugeIcons.Earth, contentDescription = "Earth")
Icon(HugeIcons.AiSearch, contentDescription = "AI Search")
Icon(HugeIcons.AddCircle, contentDescription = "Add")
```

### API Data Available Per Icon

```json
{
  "name": "1st-bracket",         // kebab-case, used as SVG filename
  "tags": "parenthesis, bracket", // comma-separated search tags
  "category": "mathematics",      // official category slug
  "featured": false,              // featured flag
  "version": "1.0.0",            // version
  "cache_buster": 416151350       // CDN cache buster
}
```

The `tags` and `category` fields are very useful for building a searchable icon picker.

## Caveats / Not Found

- The library only supports **stroke-rounded** style currently (the free tier)
- The GitHub API truncated at 1000 files (pagination limit), but the actual library has 5,131 icon files
- No pre-built category grouping in the library itself — icons are flat extension properties
- The `category` and `tags` metadata is available from the HugeIcons API but NOT bundled in the library; we would need to include a mapping file
- Icon stroke color is hardcoded to black (`0xFF000000`) — tinting must be done via Compose `tint` parameter on `Icon()`
