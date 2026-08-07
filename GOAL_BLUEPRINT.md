# Native Views SDK Goal Blueprint

## Goal

Build a native Android Canvas UI SDK with a small, consistent component API. Each
component must own its drawing, interaction, animation, resource lifecycle, and
coordinate behavior without requiring project-specific code.

The SDK package remains:

```text
com.ogfa.nativeviews
```

## Component Roadmap

### Primary components

1. Text
2. TextField
3. Button
4. Image
5. Card
6. List
7. Dialog
8. CustomAnimatorComponent
9. AfterEffectAnimator
10. DynamicViewAnimator
11. LottieViewAnimator
12. GifAnimator

### Secondary components

1. Switch
2. CheckBox
3. RadioButton
4. Progress

### Canvas ViewGroup

1. ZLayerGroup / ZLayer

## Current Status

| Component | Status | Current implementation |
|---|---|---|
| Text | Implemented | `Text` and `TextStyle`, hosted by `ZLayerGroup` |
| TextField | Implemented | `TextField`, with IME ownership in `ZLayerGroup` |
| Button | Implemented | Image-backed composite with optional native Text, insets, runtime updates, and click |
| Image | Implemented | Standalone `Image` component with bitmap scaling, runtime updates, and optional click |
| Card | Implemented | Rounded color/image background, outside drop shadow, and one nested mixed-component ZLayer |
| List | Implemented | `ComponentList` provides virtualized vertical/horizontal scrolling, reusable layered items, stable IDs, child touch arbitration, fling, Figma spacing/padding, and dedicated documentation/test coverage. |
| Dialog | Not started | — |
| CustomAnimatorComponent | Implemented foundation | Renamed API and five-layer system are in `animator.component` |
| AfterEffectAnimator | Existing, needs hardening | `animation.aftereffect` |
| DynamicViewAnimator | Existing, needs hardening | `animation.dynamic.DynamicViewAnimator` |
| LottieViewAnimator | Implemented, needs final API review | `animation.LottieViewAnimator` |
| GifAnimator | Existing, needs rename and hardening | Currently `animation.gif.GIFViewAnimator` |
| Switch | Not started | — |
| CheckBox | Not started | — |
| RadioButton | Not started | — |
| Progress | Not started | — |

| ZLayerGroup / ZLayer | Implemented foundation | Mixed-component scene, ordering, touch capture, IME, and lifecycle |

## Shared Component Architecture

All visual elements should follow the same base lifecycle:

```java
public interface Component {
    String getId();
    RectF getBounds();
    void draw(Canvas canvas);
    boolean onTouchEvent(MotionEvent event);
    void release();
}
```

Not every component must be interactive. A non-interactive component returns `false`
from `onTouchEvent()`.

Groups own collections and host-view integration:

```java
public interface ComponentGroup<T extends Component> extends AutoCloseable {
    T find(String id);
    boolean remove(String id);
    void draw(Canvas canvas);
    boolean onTouchEvent(MotionEvent event);
    void release();
}
```

Concrete groups may provide additional behavior such as keyboard connections, focus
management, animation scheduling, or topmost-first touch dispatch.

### Shared rules

- Public methods use Java lower-camel naming: `draw`, not `Draw`.
- Every component has a stable string ID.
- Every visual UI component must support both `Position + Size` and `RectF` for
  defining its region.
- `Position + Size` is the Figma/design-space API. `Position` defines the anchors
  and margins, while `Size` defines the element width and height. Together they
  convert reference-layout coordinates into runtime bounds using the measured host
  view.
- `FigmaConfig` is the immutable, app-configurable source of the reference width.
  Every `Position` captures one config, and all measurements belonging to that
  region use its width-derived scale.
- `RectF` is the direct runtime-space API for callers that already know the final
  pixel bounds.
- Region type never changes styling units. Unsuffixed dimensional styling APIs
  always accept Figma/design units. Matching `Px` APIs always accept exact runtime
  pixels, including when the component region is an explicit `RectF`.
- Both construction paths must resolve to the same internal `RectF` bounds and must
  provide identical drawing, interaction, animation, and lifecycle behavior.
- Drawing order and touch order must be deterministic.
- The topmost overlapping interactive component receives touch first.
- Touch bounds must follow every visual translation or animation.
- Resource ownership and cleanup must be explicit.
- No component may contain hardcoded assets from a consuming project.
- Missing or invalid assets must produce clear runtime exceptions.
- Components must work in a custom Canvas host without XML.

### Shared region types

`Size` is an immutable pair of positive finite `float` dimensions in Figma/design
space:

```java
Size size = new Size(figmaWidth, figmaHeight);
```

`Position` must accept `Size` directly and resolve it using the same width-derived
uniform scale already used for margins and element dimensions:

```java
RectF bounds = position.toRectF(size);
```

The shared component construction rule is:

```java
new Component.Builder(..., position, size);
// or
new Component.Builder(..., rectF);
```

## `CustomAnimatorComponent` Restructure

### Original problem

The former `AnimatedButton` was not only a button. It was a general layered, interactive,
animatable Canvas element that can contain five different layer types:

1. `BitmapLayer`
2. `GifLayer`
3. `LottieLayer`
4. `DynamicLayer`
5. `AfterEffectLayer`

Its former name made the generic layer system look button-specific. It also mixed
component composition with button interaction, press animation, click handling, sound,
movement, bounds calculation, drawing, and cleanup.

### Approved and implemented names

```text
AnimatedButton       -> CustomAnimatorComponent
AnimatedButtonGroup  -> removed; use ZLayerGroup + ZLayer
ViewLayer            -> ComponentLayer

BitmapView           -> BitmapLayer
GIFView              -> GifLayer
LottieView           -> LottieLayer
DynamicView          -> DynamicLayer
AfterEffectView      -> AfterEffectLayer
```

Target package:

```text
com.ogfa.nativeviews.animator.component
com.ogfa.nativeviews.animator.component.layer
```

### Target responsibility

`CustomAnimatorComponent` owns:

- ordered layers;
- component bounds;
- drawing;
- top-level visibility;
- animation-frame invalidation;
- position animation;
- synchronized layer and touch-bound movement;
- optional interaction configuration;
- layer resource cleanup.

It must not assume that every instance is visually or semantically a button.

### Interaction configuration

Button-like behavior becomes optional configuration:

```java
new CustomAnimatorComponent.Builder(context, id, layers, bounds)
        .setOnClickListener(listener)
        .setOnLongClickListener(listener)
        .setPressScale(0.92f)
        .setSoundAction(soundAction)
        .build();
```

If no interaction listener is supplied, the component is display-only.

The future primary `Button` component should use this interaction model internally but
provide a simpler API for common bitmap, text, icon, background, and click use cases.

### Layer contract

```java
public interface ComponentLayer {
    void draw(Canvas canvas);
    RectF getBounds();
    void setBounds(RectF bounds);
    boolean isAnimating();
    void release();
}
```

Animator-specific layers may extend the contract without adding type checks to the
component:

```java
public interface AnimatedComponentLayer extends ComponentLayer {
    void start();
    void stop();
}
```

### Bounds policy

Replace implicit bitmap-only bounds discovery with an explicit policy:

```java
BoundsPolicy.EXPLICIT
BoundsPolicy.LARGEST_LAYER
BoundsPolicy.UNION_OF_LAYERS
```

Default:

```text
Explicit RectF or Position dimensions when supplied.
Otherwise use UNION_OF_LAYERS.
```

This removes the current requirement that a position-based component must contain a
`BitmapLayer`.

### Migration mapping

| Current API | Target API |
|---|---|
| `AnimatedButton` | `CustomAnimatorComponent` |
| `AnimatedButtonGroup` | Removed; use `ZLayerGroup` + `ZLayer` |
| `ViewLayer` | `ComponentLayer` |
| `BitmapView.get(...)` | `BitmapLayer.create(...)` |
| `GIFView.get(...)` | `GifLayer.create(...)` |
| `LottieView.get(...)` | `LottieLayer.create(...)` |
| `DynamicView.get(...)` | `DynamicLayer.create(...)` |
| `AfterEffectView.get(...)` | `AfterEffectLayer.create(...)` |
| `setShrink(...)` | `setPressScale(...)` |
| `setProxySoundPlay(...)` | `setSoundAction(...)` |
| `Draw(...)` | `draw(...)` |
| `HandleTouch(...)` | `onTouchEvent(...)` |

## Target Packages

```text
com.ogfa.nativeviews.component
com.ogfa.nativeviews.text
com.ogfa.nativeviews.textfield
com.ogfa.nativeviews.button
com.ogfa.nativeviews.image
com.ogfa.nativeviews.card
com.ogfa.nativeviews.list
com.ogfa.nativeviews.dialog
com.ogfa.nativeviews.animator.component
com.ogfa.nativeviews.animator.component.layer
com.ogfa.nativeviews.animation.aftereffect
com.ogfa.nativeviews.animation.dynamic
com.ogfa.nativeviews.animation.lottie
com.ogfa.nativeviews.animation.gif
com.ogfa.nativeviews.switchcomponent
com.ogfa.nativeviews.checkbox
com.ogfa.nativeviews.radiobutton
com.ogfa.nativeviews.progress
```

`List` conflicts with `java.util.List`. The public class should therefore be named
`ComponentList` unless a better non-conflicting name is selected before implementation.

## Component Requirements

### Text

- Builder overloads for both `Position + Size` and explicit `RectF` regions.
- Android default, application `R.font`, asset, direct `Typeface`, and
  library-owned `NativeFonts` (Inter, Montserrat, Roboto, and Lilita One).
- Named variable-font weight presets from thin through black, with safe
  non-variable-font and pre-API-26 fallback.
- Start, center, and end alignment.
- Top, center, and bottom vertical alignment.
- Color, alpha, size, padding, letter spacing, and line spacing.
- Optional line wrapping and ellipsizing.
- Reusable immutable `TextStyle`.
- Runtime text, style, visibility, and region updates.
- Optional lightweight click callback and enabled state.
- Topmost-first `ZLayerGroup` touch dispatch with move-out and cancel handling.
- Clickable text has no implicit sound, haptic, press animation, or long-click behavior.
- `ZLayerGroup` drawing order, ID lookup, invalidation, and cleanup.
- Direct Canvas rendering; bitmap composition remains a separate utility.

### TextField

- Android default font when no font is configured.
- Optional `Typeface`, application `R.font`, and bundled `NativeFonts`.
- Named variable-font weights on both entered text and hint, with builder and
  runtime APIs matching `Text`.
- Native Android IME connection.
- Cursor placement by tap and continuous cursor movement by drag.
- Selection, composition, clipboard, maximum length, password mode, and editor actions.
- `ZLayerGroup` focus and keyboard ownership.
- Full-Canvas keyboard avoidance supported by a reusable viewport/pan helper.

### Button

- Implemented required private `Image` background and optional private `Text`.
- Implemented supplied-component, bitmap, and privately owned solid-color
  background builders, each with optional text.
- Implemented `Position + Size` and `RectF` regions.
- Implemented Figma/Px `TextInsets`, containment validation, and automatic child
  region updates.
- Implemented component alpha, visibility, enabled state, and cancellation-safe click.
- Implemented cached composite corner clipping with Figma-scaled and fixed-pixel
  radius APIs for both region forms.
- Implemented runtime bitmap, label, region, inset, image, text, font, and listener APIs.
- Child components are privately hosted and never registered or dispatched separately.
- Future interaction expansion: long-click, pressed/selected visuals, optional
  sound, and optional haptic actions.

### Image

- Implemented bitmap source with `Position + Size` and `RectF` regions.
- Implemented `FIT_CENTER`, `CENTER_CROP`, and `FIT_XY` scale modes.
- Implemented alpha, filtering, visibility, enabled state, and Text-compatible click.
- Implemented runtime bitmap, region, scale, alpha, state, and listener updates.
- Component does not own or recycle caller-provided bitmaps.
- Future expansion: asset/resource decoding, tint, rounded clipping, and additional
  center/fill scale modes.

### Card

- Implemented default white color and optional bitmap backgrounds.
- Implemented Figma-scaled and fixed-pixel rounded clipping.
- Implemented immutable Figma-style `DropShadow` with offset, blur, spread,
  ARGB color, default values, fixed-pixel mode, and outside visual bounds.
- Implemented one nested mixed-component `ZLayer` with global ID lookup,
  reverse-order touch routing, lifecycle ownership, and root IME delegation.
- Future expansion: border, padding helpers, and child-relative layout.

### ComponentList

- Implemented vertical and horizontal layouts.
- Implemented `Position + Size` and runtime `RectF` viewport regions.
- Implemented viewport clipping, drag scrolling, fling, smooth/programmatic
  scrolling, optional resisted overscroll, and edge settlement.
- Implemented holder recycling by view type and visible-item lazy creation.
- Implemented fixed and variable Figma item sizes, optional cross size, Figma/Px
  spacing, and Figma/Px padding.
- Each reusable item owns ordered `ZLayer`s containing mixed components.
- `ItemScope` provides item-relative Figma bounds and globally safe component IDs.
- Implemented stable data IDs, visible item/component lookup, and adapter data
  notifications.
- Implemented item click and long-click.
- Implemented child-first touch routing and child cancellation when scrolling starts.
- Implemented nested component registration, IME delegation, holder cleanup, and
  dedicated `ComponentListTestActivity`/`COMPONENT_LIST.md` coverage.

### Dialog

- Modal overlay and background dimming.
- Show, dismiss, back handling, and outside-touch policy.
- Enter and exit animation.
- Child component composition and lifecycle cleanup.

### CustomAnimatorComponent

- Five supported layer families.
- Pluggable bounds policy.
- Display-only or interactive behavior.
- Press, movement, and custom animations.
- One group for drawing, touch dispatch, invalidation, and release.

### AfterEffectAnimator

- Asset preload and cache.
- Start, pause, resume, stop, seek, repeat, and release.
- Clear invalid-asset failures.
- No project-specific sound or resource assumptions.

### DynamicViewAnimator

- Stable `CustomDynamicView` contract.
- Frame timing owned by the host/group.
- Bounds updates and cleanup.

### LottieViewAnimator

- One-name and multiple-name preload.
- Normalized names with or without `.json`.
- Shared in-flight loading per animation.
- Cache-first and asset fallback.
- External-image resolver.
- Correct source cleanup and release.

### GifAnimator

- Rename the current `GIFViewAnimator` API to `GifAnimator`.
- One-name and multiple-name preload.
- Normalize names with or without `.gif`.
- Share one in-flight loading task per animation.
- Use cache-first loading with asset fallback.
- Fail clearly for missing or invalid GIF assets.
- Support repeat configuration, drawing, visibility filtering, and release.
- Keep `GifLayer` focused on component integration while `GifAnimator` owns loading
  and playback.

### Secondary components

`Switch`, `CheckBox`, and `RadioButton` share selectable-state infrastructure:

```java
setChecked(boolean checked)
isChecked()
setOnCheckedChangeListener(listener)
```

`Progress` supports:

- determinate and indeterminate modes;
- linear and circular rendering;
- progress animation;
- configurable track and progress colors.

## Delivery Order

### Phase 1 — Core normalization

1. Introduce `Component`, `ComponentGroup`, and shared lifecycle contracts.
2. Move `Position` to a shared component/geometry package.
3. Normalize public naming and method casing.
4. Add reusable Canvas viewport translation for IME avoidance.

### Phase 2 — Animator component restructure

1. Rename the five view types to layers.
2. Introduce `ComponentLayer`.
3. Extract generic composition from `CustomAnimatorComponent`.
4. Create `CustomAnimatorComponent` and its group.
5. Add explicit bounds policies.
6. Migrate tests and documentation.
7. Remove the old `CustomAnimatorComponent` API after migration.

### Phase 3 — Core visual components

1. Text
2. Image
3. Button
4. Card

### Phase 4 — Containers and overlays

1. ComponentList
2. Dialog

### Phase 5 — Animator hardening

1. AfterEffectAnimator
2. DynamicViewAnimator
3. LottieViewAnimator
4. GifAnimator

### Phase 6 — Secondary components

1. Switch
2. CheckBox
3. RadioButton
4. Progress

## Testing Strategy

Each public element gets a separate activity:

```text
TextTestActivity
TextFieldTestActivity
ButtonTestActivity
ImageTestActivity
CardTestActivity
ComponentListTestActivity
DialogTestActivity
CustomAnimatorComponentTestActivity
AfterEffectAnimatorTestActivity
DynamicViewAnimatorTestActivity
LottieViewAnimatorTestActivity
GifAnimatorTestActivity
SwitchTestActivity
CheckBoxTestActivity
RadioButtonTestActivity
ProgressTestActivity
ZLayerTestActivity
```

Every activity must test:

- default configuration;
- custom styling;
- multiple instances;
- top, middle, and bottom screen positions;
- touch and overlapping hitboxes where applicable;
- animation and transformed hitboxes;
- configuration change or size rebuild;
- cleanup on detach/destroy;
- failure behavior for invalid input.

## Documentation Architecture

The root `README.md` is limited to:

- SDK purpose and requirements;
- Gradle module and fat-AAR integration;
- component status and documentation index;
- shared region/lifecycle conventions;
- build, artifact, and validation commands.

Every completed UI component owns a detailed guide:

```text
docs/components/COMPONENT_NAME.md
```

Each component guide must document:

- purpose, responsibilities, package, and imports;
- every supported `Position + Size` and `RectF` construction path;
- complete custom-View host integration;
- builder and runtime APIs;
- callbacks, drawing order, and touch behavior;
- resource ownership, cleanup, and failure behavior;
- practical examples and its dedicated test activity.

Non-component helpers use:

```text
docs/utilities/UTILITY_NAME.md
```

Do not document a planned API as implemented. Components awaiting implementation or
API hardening remain identified as planned/pending in the root component index.

## Definition of Done

A component is complete only when:

1. Its public API is independent of any game or consuming project.
2. It has a dedicated test activity.
3. It has a dedicated `docs/components/COMPONENT_NAME.md` guide covering creation,
   drawing, input, runtime APIs, validation, and cleanup.
4. It has clear runtime validation and errors.
5. It releases all animation, bitmap, audio, and loader resources it owns.
6. It works from the Gradle module and from the generated fat AAR.
7. The fat AAR contains required animation dependencies but no embedded AndroidX
   classes.
8. The parent test app compiles and runs against the copied AAR.
9. Touch handling remains correct after movement, scaling, scrolling, or Canvas
   translation.
10. No hardcoded assets or package references from another project remain.
11. Every visual UI component exposes both `Position + Size` and `RectF` region
    APIs, with equivalent behavior after bounds resolution.
