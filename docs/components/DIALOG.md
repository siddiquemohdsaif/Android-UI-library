# Dialog

`Dialog` is a Canvas-native modal overlay. It draws a full-parent dim layer and an
internally owned `Card` surface whose ordered `ZLayer`s may contain any Native Views
component. It does not create an Android `Window` or XML view hierarchy.

Use the explicit import when Android's own Dialog is also in scope:

```java
import com.ogfa.nativeviews.dialog.Dialog;
```

## Create and add

```java
ZLayer dialogs = ui.addLayer("dialogs");

Dialog dialog = dialogs.add(new Dialog.Builder(
        getContext(),
        "logout_dialog",
        position,
        new Size(820f, 620f)
)
        .horizontalCenter(true)
        .verticalCenter(true)
        .setBackgroundColor(Color.WHITE)
        .setCornerRadius(40f)
        .setDimColor(Color.BLACK)
        .setDimAlpha(0.5f)
        .setOutsideTouchPolicy(Dialog.OutsideTouchPolicy.DISMISS)
        .setDismissOnBackPressed(true));

dialog.show();
```

Dialogs are hidden by default. Use `.setInitiallyShown(true)` when the first draw must
start the enter transition automatically.

Both shared region forms are supported:

```java
new Dialog.Builder(context, "dialog", position, new Size(820f, 620f));
new Dialog.Builder(context, "dialog", new RectF(left, top, right, bottom));
```

## Build layered content

The compact form resolves the Dialog before creating its children:

```java
.setContent((instance, content, scope) -> {
    content.add(new Text.Builder(
            context,
            scope.id("title"),
            "CONFIRM",
            scope.rect(60f, 50f, 700f, 100f)
    )
            .setTextSizePx(scope.px(48f))
            .setAlignment(Text.Alignment.CENTER));

    content.add(new Button.Builder(
            context,
            scope.id("confirm"),
            0xff009fc8,
            "CONTINUE",
            scope.rect(90f, 390f, 640f, 120f)
    )
            .setCornerRadiusPx(scope.px(28f))
            .setTextSizePx(scope.px(36f))
            .setOnClickListener(id ->
                    instance.dismiss(Dialog.DismissReason.ACTION)));
})
```

Content can also be added after construction:

```java
Dialog.Scope scope = dialog.getScope();
ZLayer content = dialog.getContentLayer();
ZLayer actions = dialog.addContentLayer("actions");
ZLayer effects = dialog.addContentLayer("effects");
```

Layer ordering and lookup:

```java
dialog.bringContentLayerToFront("actions");
dialog.sendContentLayerToBack("content");
dialog.moveContentLayerAbove("effects", "content");
dialog.moveContentLayerBelow("content", "actions");
dialog.setContentLayerIndex("actions", 2);

dialog.findContentLayer("actions");
dialog.find("title");
dialog.find("title", Text.class);
```

## Dialog Scope

`Scope` converts Dialog-local Figma coordinates to absolute runtime regions and
generates globally unique nested IDs:

```java
scope.id("title");
scope.rect(left, top, width, height);
scope.px(24f);
scope.width();
scope.height();
scope.getBounds();
```

Because `scope.rect()` returns a runtime `RectF`, convert other Figma measurements
with `scope.px()` and use their `Px` setters.

## Show, dismiss, and state

```java
dialog.show();
dialog.showImmediately();

dialog.dismiss();
dialog.dismiss(Dialog.DismissReason.ACTION);
dialog.dismissImmediately();
dialog.toggle();

dialog.isShowing();
dialog.isHidden();
dialog.isEntering();
dialog.isExiting();
dialog.isAnimating();
```

Repeated show/dismiss calls are safe. A Dialog remains modal until its exit transition
finishes.

Dismiss reasons:

```java
PROGRAMMATIC
OUTSIDE_TOUCH
BACK_PRESSED
ACTION
HOST_RELEASED
```

Callbacks:

```java
dialog.setOnShowListener(id -> onShown());
dialog.setOnDismissListener((id, reason) -> onDismissed(reason));
```

## Modal touch and outside policy

```java
dialog.setOutsideTouchPolicy(Dialog.OutsideTouchPolicy.IGNORE);
dialog.setOutsideTouchPolicy(Dialog.OutsideTouchPolicy.DISMISS);
```

An outside press is always consumed. `IGNORE` keeps the Dialog open; `DISMISS` starts
its exit transition. The topmost nested component receives inside touches first, and
unused surface space is consumed. Underlying components never receive a gesture while
the Dialog is showing—even when Dialog content is disabled.

## Back handling

`Dialog` implements `BackHandler`. `ZLayerGroup.onBackPressed()` searches visible
components from top to bottom:

```java
getOnBackPressedDispatcher().addCallback(
        this,
        new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!ui.onBackPressed()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        }
);
```

Configure the Dialog:

```java
dialog.setDismissOnBackPressed(true);
dialog.setDismissOnBackPressed(false);
```

A non-dismissible visible Dialog still consumes Back to preserve modality.

## Built-in transitions

```java
DialogTransition.none();
DialogTransition.fade(180L);
DialogTransition.scale(180L, 0.92f);
DialogTransition.fadeScale(220L, 0.92f);
DialogTransition.slideFromBottom(240L, 120f);
DialogTransition.slideFromTop(240L, 120f);
DialogTransition.slideFromLeft(240L, 120f);
DialogTransition.slideFromRight(240L, 120f);
```

Assign independent enter and exit transitions:

```java
dialog.setEnterTransition(DialogTransition.fadeScale(220L, 0.92f));
dialog.setExitTransition(DialogTransition.slideFromBottom(180L, 80f));
```

The singular setters remain supported. Calling a singular setter again replaces its
previous value.

Run multiple effects in parallel with the plural setters:

```java
dialog.setExitTransitions(
        DialogTransition.fade(190L),
        DialogTransition.scale(190L, 0.88f),
        DialogTransition.slideFromBottom(190L, 80f)
);
```

The plural setters wrap their arguments in a parallel group. Parallel duration is the
longest child duration; shorter children finish and retain their final effect while
the remaining children continue.

Explicit parallel and sequential composition:

```java
DialogTransition.parallel(
        DialogTransition.fade(190L),
        DialogTransition.scale(190L, 0.88f)
);

DialogTransition.sequence(
        DialogTransition.scale(100L, 0.92f),
        DialogTransition.slideFromBottom(140L, 80f),
        DialogTransition.fade(100L)
);
```

Groups may be nested:

```java
dialog.setExitTransition(
        DialogTransition.sequence(
                DialogTransition.scale(100L, 0.92f),
                DialogTransition.parallel(
                        DialogTransition.fade(160L),
                        DialogTransition.slideFromBottom(160L, 80f)
                )
        )
);
```

For exit, a sequence runs in declaration order. Enter playback reverses the complete
effect path, matching the existing enter-transition model.

Custom transition:

```java
DialogTransition transition = new DialogTransition.Builder()
        .setDuration(240L)
        .setEffectAlpha(0f)
        .setEffectScale(0.9f)
        .setTranslationY(80f)
        .setInterpolator(DialogTransition.Interpolator.EASE_OUT)
        .build();
```

`setTranslationX/Y()` uses Figma values. `setTranslationXPx/YPx()` uses exact runtime
pixels. Content is non-interactive during a transition unless explicitly enabled:

```java
dialog.setInteractiveDuringTransition(true);
```

## Surface styling

The surface delegates to the SDK's Card renderer:

```java
dialog.setBackgroundColor(Color.WHITE);
dialog.setBackgroundImage(bitmap);
dialog.setBackgroundScaleType(Image.ScaleType.CENTER_CROP);

dialog.setCornerRadius(40f);
dialog.setCornerRadiusPx(40f);

dialog.setDropShadow(dropShadow);
dialog.setDropShadowPx(dropShadow);
dialog.removeDropShadow();

dialog.setSurfaceAlpha(0.95f);
dialog.getSurface();
```

## Dim overlay

```java
dialog.setDimEnabled(true);
dialog.setDimColor(Color.BLACK);
dialog.setDimAlpha(0.5f);
```

The dim overlay covers `ComponentHost.getComponentBounds()`. Its opacity follows the
enter and exit transition while the surface animates independently.

## Region, centering, translation, and state

```java
dialog.setRegion(position, size);
dialog.setRegion(rectF);
dialog.horizontalCenter(true);
dialog.verticalCenter(true);

dialog.setTranslation(0f, offset);
dialog.setTranslationX(offset);
dialog.setTranslationY(offset);
dialog.resetTranslation();

dialog.setAlpha(0.9f);
dialog.setEnabled(false); // content disabled; modal blocking remains
dialog.setVisible(false); // immediate hide
dialog.setVisible(true);  // immediate show
```

When a Dialog-owned content layer is translated, the existing Card ownership rule
moves the surface, shadow, and every content layer together. The dim overlay remains
fixed.

## Multiple dialogs

Add multiple Dialogs to one overlay layer. Normal `ZLayer` ordering determines which
visible Dialog receives touch and Back first:

```java
dialogs.bringToFront("error_dialog");
dialogs.sendToBack("profile_dialog");
```

## Release

Releasing the root `ZLayerGroup` cancels active transitions, releases the surface and
all nested layers, unregisters nested components and TextFields, and reports
`HOST_RELEASED` when a Dialog was still showing.
