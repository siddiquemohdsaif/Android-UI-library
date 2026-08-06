# Text bitmap utilities

These APIs generate or compose `Bitmap` objects. They are separate from the direct
Canvas [`Text`](../components/TEXT.md) component.

Use them when text must become an image, be written onto an existing bitmap, or use a
bitmap-font atlas.

## Bitmap-font rendering

`TextMakerEngine` uses a `Map<String, Bitmap>` containing one bitmap per character:

```java
Bitmap text = TextMakerEngine.generateTextBitmap(
        glyphs,
        "PLAYER 1",
        72
);
```

Character spacing:

```java
Bitmap spaced = TextMakerEngine.generateTextBitmapWithSpacing(
        glyphs,
        "PLAYER 1",
        72,
        4
);
```

Line evaluation:

```java
ArrayList<String> lines = TextMakerEngine.textLineEvaluator(
        glyphs,
        message,
        72,
        4,
        600
);
```

Missing glyphs, recycled bitmaps, and invalid dimensions produce clear runtime
exceptions.

## Bitmap-font composition

Return a new bitmap:

```java
Bitmap composed = TextWriter.writeTextToBitmap(
        background,
        textElements
);
```

Modify a mutable bitmap:

```java
TextWriter.writeTextToBitmapByOverWrite(
        mutableBackground,
        textElements
);
```

Spacing and width-reduction variants:

```java
TextWriter.writeTextToBitmapWithSpacing(
        background,
        spacedElements
);

TextWriter.writeTextToBitmapWithSpacingWidthReduction(
        background,
        spacedElements
);
```

Alignment uses:

```java
TextWriter.LineType.START
TextWriter.LineType.MIDDLE
TextWriter.LineType.END
```

Alpha arguments use the `0f..1f` range and are clamped.

## Android font rendering

`TextMakerEngineNative` renders ordinary Android `R.font` resources into new
bitmaps. Despite its historical name, it does not use JNI.

```java
Bitmap text = TextMakerEngineNative.generateTextBitmap(
        context,
        R.font.game_font,
        "PLAY NOW",
        72,
        Color.WHITE
);
```

With character spacing:

```java
Bitmap text = TextMakerEngineNative.generateTextBitmapWithSpacing(
        context,
        R.font.game_font,
        "PLAY NOW",
        Color.WHITE,
        72,
        4
);
```

Line evaluation:

```java
ArrayList<String> lines =
        TextMakerEngineNative.textLineEvaluator(
                context,
                R.font.game_font,
                message,
                72,
                4,
                600
        );
```

## Android font composition

```java
ArrayList<TextWriterNative.ElementWriter> writers =
        new ArrayList<>();

writers.add(new TextWriterNative.ElementWriter(
        R.font.game_font,
        "PLAY NOW",
        background.getWidth() / 2f,
        24f,
        72f,
        TextWriterNative.LineType.MIDDLE,
        1f,
        "#FFFFFF"
));

Bitmap result = TextWriterNative.writeTextToBitmap(
        context,
        background,
        writers
);
```

Available operations:

```java
TextWriterNative.writeTextToBitmap(
        context, background, writers
);

TextWriterNative.writeTextToBitmapWithSpacing(
        context, background, spacedWriters
);

TextWriterNative.writeTextToBitmapByOverWrite(
        context, mutableBitmap, writers
);

TextWriterNative.writeTextToBitmapWithSpacingByOverWrite(
        context, mutableBitmap, spacedWriters
);

TextWriterNative.writeTextToBitmapWithSpacingWidthReduction(
        context, background, spacedWriters
);

TextWriterNative.writeTextToBitmapWithSpacingWidthReductionWithLimit(
        context,
        minimumWidth,
        middle,
        top,
        bottom,
        spacedWriters
);
```

## Image composition

`ImageWriter` places bitmap elements onto a background:

```java
Bitmap withImages = ImageWriter.writeImage(
        background,
        imageElements
);
```

Alignment:

```java
ImageWriter.LineType.START
ImageWriter.LineType.MIDDLE
ImageWriter.LineType.END
```

## Make generated text interactive

Generated text is an ordinary bitmap. It can become a layer in
`CustomAnimatorComponent`:

```java
Bitmap textBitmap =
        TextMakerEngine.generateTextBitmapWithSpacing(
                glyphs,
                "PLAYER 1",
                72,
                4
        );

components.add(
        new CustomAnimatorComponent.Builder(
                getContext(),
                "profile_name",
                textBitmap,
                position
        )
                .setClickListener(id -> openProfile())
                .setPressScale(0.92f)
);
```

For ordinary non-editable text, prefer the direct Canvas `Text` component because it
avoids bitmap allocation and supports inexpensive runtime changes.
