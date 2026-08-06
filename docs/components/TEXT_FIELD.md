# TextField

`TextField` is a single-line Canvas editor connected to Android's native input method.
It renders directly with `TextPaint` and provides cursor placement, continuous cursor
dragging, selection, composition, clipboard operations, password masking, editor
actions, and focus management.

Package:

```java
import com.ogfa.nativeviews.textfield.TextField;
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

The planned shared API replaces the two Figma floats with
`new Size(720f, 120f)`.

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

The current `TextField` API supports default, direct `Typeface`, and Android font
resources.

## Builder API

| API | Purpose |
|---|---|
| `useDefaultFont()` | Select `Typeface.DEFAULT` |
| `setFont(Typeface)` | Use an existing typeface |
| `setFont(int)` | Load an Android font resource |
| `setHint(text)` | Set text shown when empty |
| `setText(text)` | Set initial text |
| `setMaxLength(length)` | Enforce maximum UTF-16 length |
| `setInputType(flags)` | Configure keyboard and text behavior |
| `setImeOptions(flags)` | Configure Done, Next, Search, Send, or Go |
| `setTextColor(color)` | Set entered-text color |
| `setHintColor(color)` | Set hint color |
| `setCursorColor(color)` | Set cursor color |
| `setSelectionColor(color)` | Set selection highlight |
| `setBackgroundColor(normal, focused)` | Set background states |
| `setStrokeColor(normal, focused)` | Set border states |
| `setTextSize(px)` | Set runtime text size |
| `setPadding(horizontal, vertical)` | Set runtime padding |
| `setCornerRadius(px)` | Set background corner radius |
| `setStrokeWidth(px)` | Set border width |
| `setCursorWidth(px)` | Set cursor width |
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
```

Callbacks may also be replaced after creation:

```java
field.setOnTextChangedListener(listener);
field.setOnEditorActionListener(listener);
field.setOnFocusChangedListener(listener);
```

## Cursor and selection behavior

- `ACTION_DOWN` focuses the topmost enabled field under the pointer.
- Tapping places the cursor at the nearest character.
- `ACTION_MOVE` continuously moves the cursor horizontally.
- Long text scrolls horizontally to keep the cursor visible.
- Selection ranges are drawn when the field is focused.
- Composing spans from Android keyboards are preserved until committed.
- Delete, forward-delete, arrow, Enter, and printable hardware keys are supported.

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

## Full-Canvas keyboard avoidance

`ZLayerGroup` owns input and focus but does not translate the whole game Canvas.
The host should observe IME insets, find the focused field with
`getFocusedField()`, and translate the Canvas so its bounds remain above the keyboard.
The sample activity demonstrates this pattern for top, middle, and bottom fields.

When the Canvas is translated, translate the touch event back before forwarding it:

```java
MotionEvent translated = MotionEvent.obtain(event);
translated.offsetLocation(0f, -canvasTranslationY);
boolean handled = textFields.onTouchEvent(translated);
translated.recycle();
```

## Test activity

```text
app.builderx.ogfa.androiduicomponents.TextFieldTestActivity
```

It exercises three screen positions, IME focus transitions, keyboard avoidance,
full-Canvas translation, cursor taps, cursor dragging, selection, and runtime updates.
