# Bundled fonts

Native Views includes fonts inside the library AAR. A consuming application
does not need to download or copy these fonts into its own `res/font` folder.

## Available fonts

| Constant | Family |
|---|---|
| `NativeFonts.INTER` | Inter variable, upright |
| `NativeFonts.INTER_ITALIC` | Inter variable, italic |
| `NativeFonts.MONTSERRAT` | Montserrat variable, upright |
| `NativeFonts.MONTSERRAT_ITALIC` | Montserrat variable, italic |
| `NativeFonts.ROBOTO` | Roboto variable, upright |
| `NativeFonts.ROBOTO_ITALIC` | Roboto variable, italic |
| `NativeFonts.LILITA_ONE` | Lilita One Regular |

Android's default typeface remains the default when no font is selected.

## Text

```java
import com.ogfa.nativeviews.font.NativeFonts;

Text title = layer.add(
        new Text.Builder(
                context,
                "title",
                "CHOOSE PLAYER",
                position,
                new Size(900f, 120f)
        )
                .setFont(NativeFonts.INTER)
                .setFontVariations(FontVariation.BOLD)
                .setTextSize(72f)
);
```

## Named variable-font weights

`Text` and `TextField` expose readable weight presets; callers do not need to
work with raw axis strings or numeric weight values:

```java
.setFont(NativeFonts.INTER)
.setFontVariations(FontVariation.SEMI_BOLD)
```

| Preset | OpenType weight |
|---|---:|
| `FontVariation.THIN` | 100 |
| `FontVariation.EXTRA_LIGHT` | 200 |
| `FontVariation.LIGHT` | 300 |
| `FontVariation.REGULAR` | 400 |
| `FontVariation.MEDIUM` | 500 |
| `FontVariation.SEMI_BOLD` | 600 |
| `FontVariation.BOLD` | 700 |
| `FontVariation.EXTRA_BOLD` | 800 |
| `FontVariation.BLACK` | 900 |

Inter, Montserrat, and Roboto support these weight presets. Lilita One is not a
variable font, so Android renders it normally. Variations are applied on API 26
and newer; API 24 and 25 retain normal font rendering.

Runtime:

```java
text.setFontVariations(FontVariation.BOLD);
text.clearFontVariations();

textField.setFontVariations(FontVariation.BOLD);
textField.clearFontVariations();
```

## TextField

```java
TextField name = layer.add(
        new TextField.Builder(
                context,
                "player_name",
                position,
                new Size(720f, 120f)
        )
                .setHint("Enter player name")
                .setFont(NativeFonts.INTER_ITALIC)
                .setFontVariations(FontVariation.BOLD)
);
```

The variation is applied consistently to the TextField's entered text and hint.

## Typeface and Android style

Use `load()` when an API needs a `Typeface` or when an Android style should be
derived:

```java
Typeface boldMontserrat = NativeFonts.load(
        context,
        NativeFonts.MONTSERRAT,
        Typeface.BOLD
);

text.setFont(boldMontserrat);
```

`NativeFonts.load()` uses the SDK's process-wide typeface cache. Accepted style
values are `Typeface.NORMAL`, `BOLD`, `ITALIC`, and `BOLD_ITALIC`.

## Direct Android resource access

The underlying resources are prefixed with `nativeviews_` to minimize resource
name collisions. Prefer `NativeFonts`, but XML or resource-based code can use:

```xml
android:fontFamily="@font/nativeviews_inter_variable"
```

## Custom application fonts

Bundled fonts do not remove existing customization. Components still accept:

```java
.useDefaultFont()
.setFont(R.font.my_game_font)
.setFontAsset("fonts/my_game_font.ttf") // Text
.setFont(existingTypeface)
```

## Licensing

Inter, Montserrat, Roboto, and Lilita One are bundled under the SIL Open Font
License 1.1. The AAR transports the copyright notice and complete license at:

```text
assets/nativeviews/licenses/fonts/FONT_NOTICES.txt
assets/nativeviews/licenses/fonts/OFL-1.1.txt
```
