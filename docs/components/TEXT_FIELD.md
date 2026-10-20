# TextField

`TextField` is a single-line Canvas editor connected to Android's native input method.
It renders directly with `TextPaint` and provides cursor placement, continuous cursor
dragging, selection, composition, clipboard operations, password masking, editor
actions, and focus management.

Package:

```java
import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.textfield.TextSelectionActionCallback;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;
```

## Responsibilities

`TextField` owns one field's content, drawing, cursor, selection, colors, typeface,
input configuration, and callbacks.

`ZLayerGroup` owns:

- field drawing and topmost-first touch dispatch;
- focus and keyboard visibility;
- the `InputConnection` bridge;
- cursor placement by tap and continuous movement by drag;
- hardware key dispatch;
- clipboard actions;
- lookup, removal, and cleanup.

## Create the group

```java
private final ZLayerGroup ui;
private final ZLayer textFields;

public GameCanvasView(Context context, AttributeSet attrs) {
    super(context, attrs);
    ui = new ZLayerGroup(this);
    textFields = ui.addLayer("fields");
}
```

The constructor makes the host focusable and focusable in touch mode.

## Region APIs

Figma-space region:

```java
new TextField.Builder(
        context,
        "player_name",
        position,
        720f,
        120f
)
```

The final two values are currently the Figma width and height. They resolve through
`Position` using the host width.

Runtime-pixel region:

```java
new TextField.Builder(
        context,
        "player_name",
        new RectF(left, top, right, bottom)
)
```

The preferred shared API uses `new Size(720f, 120f)` instead of separate width
and height values.

An explicit `RectF` remains a runtime-pixel region. Unsuffixed visual dimensions
such as text size, padding, radius, stroke width, and cursor width still use
Figma units. Use the matching `Px` method only when exact runtime pixels are
required.

Center the complete field region inside its owning ZLayer:

```java
new TextField.Builder(context, "player_name", position, size)
        .horizontalCenter(true)
        .verticalCenter(true);
```

Both axes are independent. A root ZLayer centers against the host view; a Card
content ZLayer centers against the Card. Disabling centering restores the
original `Position` or `RectF`. Cursor, selection, touch, and IME geometry use
the centered bounds.

## Build a field

```java
TextField playerName = textFields.add(
        new TextField.Builder(
                getContext(),
                "player_name",
                position,
                720f,
                120f
        )
                .setFont(R.font.game_font)
                .setHint("Enter player name")
                .setText("Player")
                .setMaxLength(20)
                .setInputType(
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                )
                .setImeOptions(EditorInfo.IME_ACTION_DONE)
                .setTextColor(Color.WHITE)
                .setHintColor(0x88ffffff)
                .setCursorColor(0xffffd166)
                .setCursorWidth(6f)
                .setSelectionColor(0x6690e0ef)
                .setBackgroundColor(0xff182840, 0xff203a5c)
                .setStrokeColor(0xff54708f, 0xff90e0ef)
                .setOnTextChangedListener((id, text) ->
                        savePlayerName(text))
                .setOnEditorActionListener((id, action) -> {
                    startGame();
                    return true;
                })
);
```

## Connect the Canvas host

Drawing:

```java
@Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ui.draw(canvas);
}
```

Touch:

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
    return ui.onTouchEvent(event)
            || super.onTouchEvent(event);
}
```

Keyboard connection:

```java
@Override
public boolean onCheckIsTextEditor() {
    return ui.onCheckIsTextEditor();
}

@Override
public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    InputConnection connection =
            ui.onCreateInputConnection(outAttrs);
    return connection != null
            ? connection
            : super.onCreateInputConnection(outAttrs);
}

@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    return ui.onKeyDown(keyCode, event)
            || super.onKeyDown(keyCode, event);
}
```

Cleanup:

```java
@Override
protected void onDetachedFromWindow() {
    ui.release();
    super.onDetachedFromWindow();
}
```

## Fonts

No font call uses `Typeface.DEFAULT`.

```java
.useDefaultFont()
.setFont(Typeface.DEFAULT)
.setFont(R.font.game_font)
```

Fonts transported by the Native Views AAR are available without adding font
files to the consuming application:

```java
import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.text.FontVariation;

.setFont(NativeFonts.INTER_ITALIC)
.setFontVariations(FontVariation.BOLD)
```

See [FONTS.md](../utilities/FONTS.md) for Inter, Montserrat, Roboto, Lilita One,
italic variants, loading a `Typeface`, and license locations.

The variation is applied to both entered text and hint text. Non-variable fonts
render normally, and API 24–25 safely retain the font's normal weight.

## Builder API

| API | Purpose |
|---|---|
| `useDefaultFont()` | Select `Typeface.DEFAULT` |
| `setFont(Typeface)` | Use an existing typeface |
| `setFont(int)` | Load an Android font resource |
| `setFontVariations(FontVariation)` | Select a named variable-font weight |
| `clearFontVariations()` | Return to the font's default variation |
| `setHint(text)` | Set text shown when empty |
| `setText(text)` | Set initial text |
| `setMaxLength(length)` | Enforce maximum UTF-16 length |
| `setInputType(flags)` | Configure keyboard and text behavior |
| `setImeOptions(flags)` | Configure Done, Next, Search, Send, or Go |
| `setTextColor(color)` | Set entered-text color |
| `setHintColor(color)` | Set hint color |
| `setCursorColor(color)` | Set cursor color |
| `setSelectionColor(color)` | Set selection highlight |
| `setSelectionEnabled(enabled)` | Enable long-press/double-tap selection and Select all |
| `setCutEnabled(enabled)` | Include or reject Cut actions |
| `setCopyEnabled(enabled)` | Include or reject Copy actions |
| `setPasteEnabled(enabled)` | Include or reject Paste actions |
| `setSelectionActionCallback(callback)` | Customize or handle contextual toolbar actions |
| `setBackgroundColor(normal, focused)` | Set background states |
| `setStrokeColor(normal, focused)` | Set border states |
| `setTextSize(figmaSize)` | Set Figma-scaled text size |
| `setTextSizePx(px)` | Set exact runtime-pixel text size |
| `setPadding(horizontal, vertical)` | Set Figma-scaled padding |
| `setPaddingPx(horizontal, vertical)` | Set exact runtime-pixel padding |
| `setCornerRadius(radius)` | Set Figma-scaled background corner radius |
| `setCornerRadiusPx(px)` | Set exact runtime-pixel corner radius |
| `setStrokeWidth(width)` | Set Figma-scaled border width |
| `setStrokeWidthPx(px)` | Set exact runtime-pixel border width |
| `setCursorWidth(figmaWidth)` | Set cursor width in Figma units; scaled with the active `FigmaConfig` |
| `setCursorWidthPx(px)` | Set exact cursor width in runtime pixels |
| `horizontalCenter(enabled)` | Center the complete field region horizontally |
| `verticalCenter(enabled)` | Center the complete field region vertically |
| `setPassword(enabled)` | Mask displayed text |
| `setEnabled(enabled)` | Enable input and focus |
| `setOnTextChangedListener(listener)` | Receive content changes |
| `setOnEditorActionListener(listener)` | Receive IME actions |
| `setOnFocusChangedListener(listener)` | Receive focus changes |

When visual dimensions are omitted, defaults are derived from the resolved field
height.

## Runtime TextField API

Content:

```java
field.getText();
field.getEditable();
field.setText("Player 2");
field.clear();

field.getHint();
field.setHint("Name");

field.getMaxLength();
field.setMaxLength(30);
```

Font:

```java
field.getTypeface();
field.setFont(NativeFonts.INTER_ITALIC);
field.setFont(typeface);
field.useDefaultFont();

field.getFontVariation();
field.setFontVariations(FontVariation.SEMI_BOLD);
field.clearFontVariations();
```

Input:

```java
field.getInputType();
field.setInputType(InputType.TYPE_CLASS_TEXT);

field.getImeOptions();
field.setImeOptions(EditorInfo.IME_ACTION_DONE);

field.isPassword();
field.setPassword(true);
```

State and focus:

```java
field.getId();
field.getBounds();
field.isHorizontalCentered();
field.isVerticalCentered();
field.setHorizontalCenter(true);
field.setVerticalCenter(true);
field.horizontalCenter(false);
field.verticalCenter(false);

field.isEnabled();
field.setEnabled(false);

field.isFocused();
field.requestFocus();
field.clearFocus();
```

Selection:

```java
field.setSelection(index);
field.setSelection(start, end);
field.getSelectionStart();
field.getSelectionEnd();
field.getSelectionAnchor(); // Raw fixed endpoint; may be greater than the extent
field.getSelectionExtent(); // Raw actively moving endpoint

field.setSelectionEnabled(true);
field.setCutEnabled(true);
field.setCopyEnabled(true);
field.setPasteEnabled(true);
```

Customize the contextual toolbar without replacing the built-in clipboard behavior:

```java
field.setSelectionActionCallback(new TextSelectionActionCallback() {
    @Override
    public void onCreateActionMode(TextField field, ActionMode mode, Menu menu) {
        // Add application-specific actions once.
    }

    @Override
    public void onPrepareActionMode(TextField field, ActionMode mode, Menu menu) {
        // Update, remove, enable, or disable actions as selection state changes.
    }

    @Override
    public boolean onActionItemClicked(
            TextField field,
            ActionMode mode,
            MenuItem item
    ) {
        return false; // Return true when the application handled the action.
    }
});
```

Callbacks may also be replaced after creation:

```java
field.setOnTextChangedListener(listener);
field.setOnEditorActionListener(listener);
field.setOnFocusChangedListener(listener);
```

## Cursor and selection behavior

- Dragging a cursor or selection handle shows a custom rounded magnifier above
  the active text boundary. The lens redraws the same text, highlight, and caret
  layout at a larger scale, clips wrapped text to the active line, and chooses a
  readable background for light or dark text. It disappears when the gesture
  ends or focus is lost.
  `ZLayerGroup` draws it after all layers so it remains above other components.
  The toolbar closes first, then the magnifier opens after 300 ms. When the
  gesture ends, the magnifier closes and the toolbar returns after 300 ms.
- `ACTION_DOWN` focuses the topmost enabled field under the pointer.
- Tapping places the cursor at the nearest character.
- Long press and double tap select a locale-aware word. Double tap selection
  starts on the second touch-down; dragging that second touch extends the
  selection by words, and the contextual toolbar appears after release.
- Long press in an empty field or at the end caret opens the insertion toolbar
  after release. Paste appears there when the field allows it and the clipboard
  has content.
- Dragging a selection handle adjusts only that endpoint. Handles do not cross,
  and the selection retains at least one character.
- A caret inside the text shows a centered teardrop grip beneath it; dragging
  the grip moves the cursor without selecting text. The grip is hidden at the
  end of the text.
- Pressing the cursor's current line and dragging horizontally also moves the
  caret along that line; vertical movement continues to scroll multiline text.
- Start and end handles use directional Android-style teardrop grips below the
  exact text boundary and remain fully inside the field's drawable area.
- Tapping inside the highlighted range collapses the selection at the tapped
  position. Dragging from highlighted text keeps the selection while the field
  scrolls; tapping outside places the cursor normally.
- The contextual toolbar is anchored above the selection start. If that start
  scrolls out of view, the toolbar remains above the top edge of the field.
- Dragging near an edge automatically scrolls long text.
- Left/right movement and deletion operate on user-visible character boundaries.
- Long text scrolls horizontally to keep the cursor visible.
- Selection ranges are drawn when the field is focused.
- The contextual toolbar dynamically shows applicable Cut, Copy, Paste, Select all,
  Share, and Replace actions. Share appears for a nonempty selection outside a
  password field and opens Android's share chooser with the selected text.
  Replace appears only when the selected text or cursor is covered by a
  `SuggestionSpan` with a different candidate, and opens a picker for those
  candidates. The field's `Editable` must receive suggestion spans from an IME
  or application; this Canvas field does not generate spelling suggestions.
  When four or more actions are visible, Share and Replace appear in Android's
  three-dot overflow menu.
- Password fields reject Cut and Copy at both the toolbar and input-connection layers.
- Composing spans from Android keyboards are preserved until committed.
- Delete, forward-delete, Shift+arrow selection, Ctrl+A/C/X/V, Enter, and printable
  hardware keys are supported.

## Group API

```java
TextField added = textFields.add(builder);
TextField found = textFields.find("player_name");

textFields.contains("player_name");
textFields.remove("player_name");
textFields.size();
textFields.isEmpty();

textFields.getFocusedField();
ui.onCheckIsTextEditor();
textFields.requestFocus("player_name");
textFields.clearFocus();

textFields.setHideKeyboardWhenTouchOutside(true);
textFields.restartInput();

ui.draw(canvas);
ui.onTouchEvent(event);
ui.onKeyDown(keyCode, event);
ui.onCreateInputConnection(outAttrs);

textFields.clear();
ui.release();
textFields.close();
```

IDs must be unique within a group.

`clear()` removes fields and focus while keeping the group reusable.
`release()` clears the group for lifecycle cleanup.

## Editor action behavior

```java
.setImeOptions(EditorInfo.IME_ACTION_NEXT)
.setOnEditorActionListener((id, actionId) -> {
    if (actionId == EditorInfo.IME_ACTION_NEXT) {
        textFields.requestFocus("next_field");
        return true;
    }
    return false;
})
```

Returning `true` means the callback handled the action. Unhandled `NEXT` moves to the
next enabled field in drawing order; unhandled completion actions clear focus.

## Layer-scoped keyboard avoidance

`ZLayerGroup` owns input and focus but does not choose which UI should move for
the keyboard. For root content, put the field and its related decoration in a
dedicated `ZLayer`, observe IME insets, and translate that layer:

```java
ZLayer fields = ui.addLayer("fields");
fields.add(new TextField.Builder(...));

fields.setTranslationY(keyboardOffsetPx);
```

The root Canvas and sibling root layers remain fixed. `ZLayer` applies inverse touch
coordinates internally, so forward the original event:

```java
boolean handled = ui.onTouchEvent(event);
```

Translate the root Canvas only when the intended design explicitly requires
the complete scene to move.

For a TextField inside a Card, translating its Card-owned content layer moves
the complete Card composite, including its background, shadow, and all internal
layers. This preserves Card ownership and clipping while sibling root layers
remain fixed.

## Test activity

```text
app.builderx.ogfa.androiduicomponents.TextFieldTestActivity
```

It exercises three screen positions, IME focus transitions, keyboard avoidance,
full-Canvas translation, cursor taps, cursor dragging, selection, and runtime updates.
