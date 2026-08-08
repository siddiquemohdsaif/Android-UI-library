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
8. AfterEffectAnimator
9. DynamicViewAnimator
10. LottieAnimator
11. GifAnimator

### Secondary components

1. Switch
2. CheckBox
3. RadioButton
4. Progress

### Canvas ViewGroup

1. ZLayerGroup / ZLayer
2. ZLayerContainer

## Current Status

| Component | Status | Current implementation |
|---|---|---|
| Text | Implemented | `Text` and `TextStyle`, hosted by `ZLayerGroup` |
| TextField | Implemented | `TextField`, with IME ownership in `ZLayerGroup` |
| Button | Implemented | Image-backed composite with optional native Text, insets, runtime updates, and click |
| Image | Implemented | Standalone `Image` component with bitmap scaling, runtime updates, and optional click |
| Card | Implemented | Rounded color/image background, outside drop shadow, and one nested mixed-component ZLayer |
| List | Implemented | `ComponentList` provides virtualized vertical/horizontal scrolling, reusable layered items, stable IDs, child touch arbitration, fling, Figma spacing/padding, and dedicated documentation/test coverage. |
| Dialog | Implemented | Modal dim overlay, Card-backed layered content, local Figma scope, outside/Back policies, show/dismiss lifecycle, transitions, callbacks, translation, and dedicated documentation/test coverage. |
| ZLayerContainer | Complete | Generic bounded composition using nested ZLayers and arbitrary Component children |
| AfterEffectAnimator | Complete | Standalone ZLayer component and reusable immutable composition |
| DynamicViewAnimator | Complete | Standalone ZLayer component using the shared monotonic playback clock |
| LottieAnimator | Complete | Standalone cached ZLayer component; old `LottieViewAnimator` is internal |
| GifAnimator | Complete | Standalone cached ZLayer component; old `GIFViewAnimator` is internal |
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

## Generic Nested Composition

The former `CustomAnimatorComponent` duplicated component APIs through five specialized
layer adapters. It has been removed. Composition is now handled by
`ZLayerContainer`, a normal bounded `Component` that owns nested `ZLayer`s.

Any component can be a child:

1. `Image`
2. `Text`
3. `TextField`
4. `Button`
5. `Card`
6. `ComponentList`
7. `GifAnimator`
8. `LottieAnimator`
9. `DynamicViewAnimator`
10. `AfterEffectAnimator`
11. another `ZLayerContainer`

```java
ZLayerContainer panel = scene.add(
        new ZLayerContainer.Builder(context, "panel", position, size)
                .setClipToBounds(true)
                .setOnClickListener(id -> openPanel())
);

ZLayer background = panel.getContentLayer();
ZLayer animation = panel.addLayer("animation");
ZLayer foreground = panel.addLayer("foreground");

background.add(new Image.Builder(
        context, "background", bitmap, panel.getLocalBounds()));
animation.add(new GifAnimator.Builder(
        context, "sparkles", "sparkles",
        panel.figmaRect(20f, 20f, 120f, 120f)));
foreground.add(new Button.Builder(
        context, "claim", color, "CLAIM",
        panel.figmaRect(180f, 130f, 240f, 70f)));
```

The container owns ordering, local coordinates, clipping, nested touch dispatch,
optional surface interaction, movement, invalidation, nested IME registration, and
idempotent cleanup. Each child retains its own rendering, playback, and resource API.

Migration mapping:

| Removed specialized type | Ordinary component |
|---|---|
| `BitmapLayer` | `Image` |
| `GifLayer` | `GifAnimator` |
| `LottieLayer` | `LottieAnimator` |
| `DynamicLayer` | `DynamicViewAnimator` |
| `AfterEffectLayer` | `AfterEffectAnimator` |

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
com.ogfa.nativeviews.zlayer
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

- Implemented modal overlay and animated full-parent dimming.
- Implemented Card-backed surface with color/image, radius, shadow, alpha, and
  multiple ordered content layers.
- Implemented Dialog-local Figma `Scope`, safe nested IDs, lookup, and Builder
  content callback.
- Implemented show, immediate show, dismiss, immediate dismiss, toggle, state,
  dismissal reasons, and lifecycle callbacks.
- Implemented topmost-first `BackHandler` dispatch through `ZLayerGroup` and
  configurable Back dismissal.
- Implemented modal inside/outside touch routing with IGNORE/DISMISS policy.
- Implemented fade, fade-scale, four slide transitions, and custom transition
  interpolation/configuration, plus nested parallel/sequential transition groups
  and plural parallel setters while preserving the singular API.
- Implemented nested component/IME lifecycle cleanup and `HOST_RELEASED` reporting.
- Covered by `DialogTestActivity` and `docs/components/DIALOG.md`.

### ZLayerContainer

- Arbitrary nested `Component` children, not a fixed set of layer adapters.
- Local Figma-space and runtime-pixel rectangle helpers.
- Nested layer/component ordering and lookup.
- Optional clipping, surface interaction, press feedback, and region movement.
- One root group for drawing, touch dispatch, IME, invalidation, and release.

### AfterEffectAnimator

- Asset preload and cache.
- Start, pause, resume, stop, seek, repeat, and release.
- Clear invalid-asset failures.
- No project-specific sound or resource assumptions.

### DynamicViewAnimator

- Stable `CustomDynamicView` contract.
- Frame timing owned by the host/group.
- Bounds updates and cleanup.

### LottieAnimator

- One-name and multiple-name preload.
- Normalized names with or without `.json`.
- Shared in-flight loading per animation.
- Cache-first and asset fallback.
- External-image resolver.
- Correct source cleanup and release.

### GifAnimator

- `GIFViewAnimator` loading internals are no longer public; `GifAnimator` is the component API.
- One-name and multiple-name preload.
- Normalize names with or without `.gif`.
- Share one in-flight loading task per animation.
- Use cache-first loading with asset fallback.
- Fail clearly for missing or invalid GIF assets.
- Support repeat configuration, drawing, visibility filtering, and release.
- Use `GifAnimator` directly in root or container-owned `ZLayer`s.

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

### Phase 2 — Generic nested composition (complete)

1. Make every animator family a standalone `Component`.
2. Add `ZLayerContainer` with nested `ZLayer` ownership and local coordinates.
3. Support arbitrary visual, input, and animator children.
4. Preserve touch capture, IME registration, invalidation, and cleanup.
5. Remove the fixed five-layer adapters and `CustomAnimatorComponent` API.
6. Migrate tests and documentation.

### Phase 3 — Core visual components

1. Text
2. Image
3. Button
4. Card

### Phase 4 — Containers and overlays

1. ComponentList
2. Dialog

### Phase 5 — Animator hardening (complete)

1. AfterEffectAnimator
2. DynamicViewAnimator
3. LottieAnimator
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
ZLayerContainerTestActivity
AfterEffectAnimatorTestActivity
DynamicViewAnimatorTestActivity
LottieAnimatorTestActivity
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
