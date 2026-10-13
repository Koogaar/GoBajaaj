---
name: Urban Mobility & Dispatch
colors:
  surface: '#0f131c'
  surface-dim: '#0f131c'
  surface-bright: '#353942'
  surface-container-lowest: '#0a0e16'
  surface-container-low: '#181c24'
  surface-container: '#1c2028'
  surface-container-high: '#262a33'
  surface-container-highest: '#31353e'
  on-surface: '#dfe2ee'
  on-surface-variant: '#e9bcb6'
  inverse-surface: '#dfe2ee'
  inverse-on-surface: '#2c3039'
  outline: '#af8782'
  outline-variant: '#5e3f3b'
  surface-tint: '#ffb4aa'
  primary: '#ffb4aa'
  on-primary: '#690003'
  primary-container: '#e50914'
  on-primary-container: '#fff7f6'
  inverse-primary: '#c0000c'
  secondary: '#ffb95f'
  on-secondary: '#472a00'
  secondary-container: '#ee9800'
  on-secondary-container: '#5b3800'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#00835a'
  on-tertiary-container: '#e8ffef'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdad5'
  primary-fixed-dim: '#ffb4aa'
  on-primary-fixed: '#410001'
  on-primary-fixed-variant: '#930007'
  secondary-fixed: '#ffddb8'
  secondary-fixed-dim: '#ffb95f'
  on-secondary-fixed: '#2a1700'
  on-secondary-fixed-variant: '#653e00'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#0f131c'
  on-background: '#dfe2ee'
  surface-variant: '#31353e'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  margin: 1rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system targets urban commuters, Bajaj (auto-rickshaw) drivers, and municipal navigation users operating in dense, fast-evolving metropolitan environments like Mogadishu. The aesthetic balances emergency-grade clarity with modern civic utility: high-contrast dark surfaces designed for extreme outdoor glare, sudden lighting shifts, and rapid in-transit comprehension.

The design movement combines **High-Contrast Functionalism** with refined **Material 3 Surface Architecture**. Visual hierarchy prioritizes glanceable telematics, real-time routing status, and street-level safety indicators. The emotional tone is authoritative, kinetic, and dependable—eliminating non-essential decoration in favor of crisp structural containers, distinct status signifiers, and instant tactile feedback.

## Colors

The palette is engineered for critical legibility under low-light cabin interiors and bright equatorial sunlight:

- **Primary (`#E50914` Crimson Red):** Core brand identification, primary dispatch triggers, urgent road closures, and destructive actions.
- **Secondary (`#F59E0B` Amber Gold):** Bajaj hazard warnings, traffic bottleneck alerts, active fare meters, and cautionary telemetry.
- **Tertiary (`#10B981` Emerald Green):** Clear intersections, driver availability status, active rides, and affirmative state confirmations.
- **Neutral Canvas (`#0B0F17` Slate Black):** Ground base for OLED efficiency and eye-strain reduction during night transit.
- **Surface Elevation Hierarchy:**
  - Base Background: `#0B0F17`
  - Container Low: `#151C28` (List cards, secondary trays)
  - Container Mid: `#1E2738` (Active cards, modal surfaces, floating sheets)
  - Container High: `#28344A` (Hover states, elevated badges, active pill toggles)
  - Stroke / Border: `rgba(255, 255, 255, 0.08)` for container separation without color pollution.
- **Typography on Dark:** High-emphasis text uses `#F9FAFB` (98% white); medium-emphasis telemetry uses `#94A3B8` (Slate 400); disabled states use `#475569`.

## Typography

The design system standardizes on **Plus Jakarta Sans** across all roles. It offers tall x-height, clear open counters, and robust diacritic and Latin-script support necessary for bilingual Somali and English interfaces (e.g., handling standard Somali orthography with clarity at small tracking).

- **Headlines & Metrics:** Bold weights (`700`, `800`) ensure fares, distance, and road names stand out instantly against dark substrates.
- **Body:** Standard weights (`400`, `500`) maintain crisp legibility in Somali text, which frequently features paired vowels (`aa`, `ee`, `oo`) and apostrophes for glottal stops.
- **Labels & Micro-Indicators:** Uppercase tracking (`0.02em` to `0.04em`) is applied to telemetry metadata, license plate chips, and status badges to maintain scanning fidelity under vehicle vibrations.

## Layout & Spacing

A strictly regulated 4px/8px incremental spatial rhythm governs component density for mobile hand-held use and dashboard mounts:

- **Mobile Viewport Grid:** 4-column fluid layout with `1rem` outer canvas margin and `0.75rem` column gutters.
- **Tablet / In-Dash Mounted Grid:** 8-column layout with `1.5rem` outer canvas margins and `1rem` gutters.
- **Touch Targets:** Any interactive control (buttons, quick-dial chips, map pins) must occupy a strict minimum bounding box of 48×48px to accommodate one-handed operation during road transit.
- **Dynamic Panels:** Bottom sheets snap into three defined vertical detents (25% peek, 50% search/list view, 92% full-canvas route overview) with standard `space-md` inner padding.

## Elevation & Depth

To avoid murky visuals in dark mode, elevation is expressed primarily through **tonal layering** and **controlled edge illumination**, supplemented by subtle directional ambient shadows:

- **Level 0 (Map / Base Canvas):** `#0B0F17` base slate. Flat.
- **Level 1 (Inline Cards, Track Rows):** `#151C28` with an inset `1px` border using `rgba(255, 255, 255, 0.05)`.
- **Level 2 (Floating Action Buttons, Top Route Bar):** `#1E2738` with an ambient drop shadow: `0px 8px 24px rgba(0, 0, 0, 0.45)` and a subtle top-edge highlight using `rgba(255, 255, 255, 0.1)`.
- **Level 3 (Modal Bottom Sheets, Alert Overlays):** `#1E2738` elevated over a `70%` black backdrop scrim (`rgba(11, 15, 23, 0.7)` with `backdrop-filter: blur(8px)`). Shadow: `0px 16px 32px rgba(0, 0, 0, 0.6)`.
- **Alert Elevation:** Urgent Bajaj incident alerts overlaying the screen incorporate a concentrated ambient glow derived from the alert token (e.g., `rgba(245, 158, 11, 0.15)` for amber warnings; `rgba(229, 9, 20, 0.2)` for crimson closures).

## Shapes

The design system employs a **Rounded** aesthetic (Level 2). This balances structural ergonomic comfort with the modern utility of Material 3 components:

- **Small Interactive Elements:** Chips, text inputs, segmented pills, and status tags use `0.5rem` (8px) corner radii.
- **Standard Containers:** List items, incident alert cards, and bottom sheet bodies use `rounded-lg` (`1rem` / 16px).
- **Persistent Bottom Sheets & Floating HUDs:** Top edges of active sheets use `rounded-xl` (`1.5rem` / 24px).
- **Avatar & Map Icons:** Driver profiles, directional vehicle heads, and quick-filter circular icon buttons retain full circular (`9999px`) styling.

## Components

### Buttons
- **Primary Action (Call Bajaj / Confirm Route):** Height 52px. Solid `#E50914` background, white label (`label-lg`), `0.5rem` corner radius. Active state dims to `#C00811`.
- **Secondary / Action Tonal:** Background `#1E2738`, border `1px solid rgba(255, 255, 255, 0.1)`, label `#F9FAFB`.
- **Tertiary Utility:** Transparent background with `#10B981` or `#F59E0B` icon and label for rapid quick-dial or reporting actions.

### Traffic & Status Chips
- **Open Corridor Chip:** `#10B981` at 15% opacity background, `1px solid #10B981`, emerald text with a pulsing 6px dot indicator.
- **Traffic Caution Chip:** `#F59E0B` at 15% opacity background, `1px solid #F59E0B`, amber text.
- **Road Closure Chip:** `#E50914` at 15% opacity background, `1px solid #E50914`, crimson text.
- Chips carry `label-md` typography with `0.5rem` vertical and `0.75rem` horizontal padding.

### Cards & Telemetry Lists
- **Incident & Route Card:** Surface `#151C28`, bordered with `rgba(255, 255, 255, 0.08)`. Left edge features an integrated 4px vertical color bar reflecting condition status (`#E50914`, `#F59E0B`, or `#10B981`).
- **Bajaaj Driver Dispatch Card:** Surface `#1E2738`, featuring driver plate badge, vehicle rating, dynamic fare calculation, and quick-dial action aligned to the trailing edge.

### Inputs & Search Bars
- Background `#151C28`, height 52px, internal padding `0 1rem`. Unfocused border `1px solid rgba(255, 255, 255, 0.1)`. Focus border transitions to `2px solid #E50914` with zero drop-shadow spread. Dual label text support (Somali placeholder with English sub-label).

### Mobile Navigation & Map Floating Controls
- **Bottom Navigation Bar:** Surface `#151C28` with an upper border `1px solid rgba(255, 255, 255, 0.06)`. Active tab displays an `#E50914` pill indicator behind icons.
- **Floating HUD Controls:** Recenter, traffic layer toggle, and emergency SOS buttons occupy 48×48px circles with `#1E2738` background, high-contrast white glyphs, and level-2 elevation.