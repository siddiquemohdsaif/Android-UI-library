# FigmaConfig

`FigmaConfig` is the single source of truth for converting Figma/design-space
measurements into runtime pixels. Its initial reference width is `1080f`.

## Configure the application

Set the project reference width once from `Application.onCreate()`:

```java
public final class GameApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FigmaConfig.setDefault(new FigmaConfig(1080f));
    }
}
```

Register the application:

```xml
<application
    android:name=".GameApplication"
    ... />
```

Every subsequently created default `Position` captures that configuration:

```java
Position position = new Position(
        hostView,
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        20f,
        90f
);
```

## Conversion rule

```java
float scale = runtimeWidth / config.getReferenceWidth();
float runtimeValue = figmaValue * scale;
```

The same captured scale is used for margins, `Size`, text measurements,
component insets, corner radius, stroke width, and other component-specific
Figma values. Runtime height does not affect scale; it is used only for a
bottom-anchored position.

## Explicit screen configuration

A screen can use a different Figma frame without changing the app default:

```java
FigmaConfig tabletConfig = new FigmaConfig(1440f);

Position position = new Position(
        hostView,
        tabletConfig,
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        24f,
        120f
);
```

The unbound overload places `tabletConfig` first:

```java
new Position(
        tabletConfig,
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        24f,
        120f
);
```

## API

```java
FigmaConfig.DEFAULT_REFERENCE_WIDTH;

FigmaConfig config = new FigmaConfig(1080f);
config.getReferenceWidth();
config.getScale(runtimeWidth);
config.toRuntime(figmaValue, runtimeWidth);

FigmaConfig.setDefault(config);
FigmaConfig.getDefault();
FigmaConfig.resetDefault();

position.getFigmaConfig();
position.getScale(hostView);
position.getScale();
position.toRuntimePixels(hostView, figmaValue);
position.toRuntimePixels(figmaValue);
```

`getScale()` and no-View conversion methods require a host-bound `Position`.

## Immutable capture behavior

`FigmaConfig` is immutable. A `Position` captures the current default when it
is created:

```java
FigmaConfig.setDefault(new FigmaConfig(1080f));
Position first = new Position(...); // captures 1080

FigmaConfig.setDefault(new FigmaConfig(1440f));
Position second = new Position(...); // captures 1440
```

Changing the default does not mutate existing Positions or partially rescale a
visible screen. When changing design configuration at runtime, clear and
rebuild the affected components.

## RectF components

An explicit `RectF` already contains runtime pixels and does not use
`FigmaConfig`.
