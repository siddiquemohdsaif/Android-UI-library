# Figma Page Extraction Rules

This document defines the required format for extracting a complete Figma screen
and its components into this project.

## 1. Output directory

Create one new directory for every extraction:

```text
{page_name}_{unix_timestamp}
```

Rules:

- Convert the Figma page/frame name to lowercase `snake_case`.
- Use only `a-z`, `0-9`, and `_`.
- Remove duplicate underscores.
- Use a Unix timestamp in seconds (UTC), captured when extraction starts.
- Never overwrite an earlier extraction. A new extraction receives a new timestamp.

Example:

```text
figma_login_page_1_1776257332
```

All output files must be placed directly inside this directory:

```text
figma_login_page_1_1776257332/
├── figma_login_page_1.png
├── figma_login_page_1.json
├── background.webp
├── logo.webp
├── brand_name.webp
├── tagline_primary.webp
├── tagline_secondary.webp
├── phone_entry_card.webp
├── fields_section.webp
├── button_next.webp
└── legal_terms.webp
```

Do not create density folders or nested component folders during extraction.
Android resource placement happens in a later implementation step.

## 2. Source selection

The extraction target must be a specific Figma frame or component URL containing
a `node-id`.

Before exporting:

1. Confirm that the selected node is the complete screen frame.
2. Read its natural Figma width and height.
3. Record its file key, node ID, node name, and URL.
4. Treat the selected frame's top-left corner as coordinate `(0, 0)`.
5. Preserve the Figma child order. The first child is the lowest visual item and
   the last child is the highest visual item unless an explicit z-index says
   otherwise.

Do not guess a missing node ID or extract an entire file when a screen frame was
requested.

## 3. Full-page image

Export the complete selected screen at exactly `1x` as PNG.

Naming:

```text
{page_name}.png
```

Example:

```text
figma_login_page_1.png
```

Requirements:

- Format: PNG
- Scale: natural Figma size (`1x`)
- Width and height: equal to the selected screen frame
- Color mode: sRGB
- Alpha: preserve when present
- Crop: exact screen frame bounds
- Content: every visible layer in final compositing order
- Do not resize, optimize destructively, add padding, or add a watermark.

For a Figma frame sized `852 × 1846`, the PNG must also be `852 × 1846`.

## 4. Component images

Export every meaningful visible component at `1x` as WebP.

Use logical components instead of arbitrary slices. Examples include:

- Background artwork
- Logos and illustrations
- Icons
- Text blocks when the text must be visually preserved as an image
- Cards and panels
- Input-field groups
- Buttons
- Dividers, badges, and decorative elements
- Legal/footer content

### 4.1 WebP requirements

- Format: WebP
- Scale: natural Figma size (`1x`)
- Use lossless WebP for text, icons, UI controls, gradients, and transparency.
- Preserve alpha transparency.
- Do not upscale or downscale.
- Do not recreate an asset by hand when an exported Figma asset exists.
- Do not flatten unrelated sibling components together.
- Keep a composite component intact when its internal layers are not separately
  available from Figma.

### 4.2 Component filenames

Use descriptive lowercase `snake_case` names:

```text
background.webp
logo.webp
brand_name.webp
tagline_primary.webp
country_selector.webp
phone_number_field.webp
security_lock_icon.webp
security_message.webp
button_next.webp
legal_terms.webp
```

Filename rules:

- Use only `a-z`, `0-9`, and `_`.
- The extension must be `.webp`.
- Names must describe purpose, not the original generic Figma layer name.
- Do not use names such as `image_475.webp`, `frame_4118.webp`, or `rectangle_1.webp`
  when a semantic name can be determined.
- Add a short role suffix when two assets would otherwise have the same name,
  for example `icon_arrow_next.webp` and `icon_arrow_dropdown.webp`.
- Every `res` value in JSON must match a real filename exactly, including case.

### 4.3 Bounds and shadows

The JSON `position` and `size` describe the node's Figma layout bounds.

If an exported bitmap contains a shadow, blur, stroke, or another effect extending
outside those bounds, also record:

- `asset_size`: actual WebP pixel dimensions
- `asset_offset`: bitmap top-left offset relative to the component's layout bounds

Example:

```json
{
  "position": { "x": 55, "y": 834 },
  "size": { "w": 742, "h": 849 },
  "asset_size": { "w": 806, "h": 913 },
  "asset_offset": { "x": -32, "y": -32 }
}
```

When no visual effect extends beyond the node, `asset_size` should equal `size`
and `asset_offset` should be `{ "x": 0, "y": 0 }`.

## 5. Structure map JSON

Create one JSON structure map:

```text
{page_name}.json
```

Example:

```text
figma_login_page_1.json
```

The file must contain valid UTF-8 JSON. JSON does not allow comments, trailing
commas, or keys such as `layer_0:` without quotation marks.

### 5.1 Root object

Use this root structure:

```json
{
  "schema_version": 1,
  "page_name": "figma_login_page_1",
  "source": {
    "figma_file_key": "g6g9NbfmbQ4iTdTQRhqXcS",
    "figma_node_id": "2404:4641",
    "figma_node_name": "Login_Page_1",
    "figma_url": "https://www.figma.com/design/g6g9NbfmbQ4iTdTQRhqXcS/bitAim?node-id=2404-4641",
    "extracted_at_unix": 1776257332,
    "scale": 1
  },
  "coordinate_system": {
    "origin": "top_left",
    "x_direction": "right",
    "y_direction": "down",
    "unit": "figma_px"
  },
  "size": {
    "w": 852,
    "h": 1846
  },
  "preview": "figma_login_page_1.png",
  "layers": []
}
```

### 5.2 Layers

`layers` must be a JSON array in back-to-front visual order. Each entry contains:

```json
{
  "layer_index": 0,
  "layer_name": "background",
  "components": []
}
```

Recommended logical layer groups:

1. `background`: page background and full-screen decoration
2. `content`: logo, brand name, headings, taglines, illustrations
3. `card`: card surface, form elements, messages, and actions
4. `overlay`: dialogs, menus, tooltips, loading indicators
5. `footer`: legal text and bottom content

Create only the layers present on the page. `layer_index` must start at `0`, be
unique, and increase by one.

### 5.3 Components

Every component object must contain:

```json
{
  "component_id": "logo",
  "node_id": "2404:4651",
  "component_name": "logo",
  "type": "Image",
  "res": "logo.webp",
  "position": {
    "x": 326,
    "y": 230
  },
  "size": {
    "w": 200,
    "h": 200
  },
  "asset_size": {
    "w": 200,
    "h": 200
  },
  "asset_offset": {
    "x": 0,
    "y": 0
  },
  "fitting_placement": "relative_coordinate",
  "z_index": 1,
  "visible": true,
  "opacity": 1
}
```

Field rules:

| Field | Rule |
| --- | --- |
| `component_id` | Unique lowercase `snake_case` identifier within the page. |
| `node_id` | Original Figma node ID using `page:node` form. |
| `component_name` | Semantic component name. |
| `type` | Semantic type from the supported list below. |
| `res` | Relative WebP filename in the extraction directory. |
| `position` | Figma layout coordinates relative to the page origin. |
| `size` | Figma layout width and height. |
| `asset_size` | Actual exported WebP pixel width and height. |
| `asset_offset` | Exported bitmap offset from the layout bounds. |
| `fitting_placement` | Placement behavior defined below. |
| `z_index` | Integer draw order within the complete page. |
| `visible` | Whether the component was visible at extraction time. |
| `opacity` | Number from `0` through `1`. |

Supported `type` values:

- `Image`
- `Icon`
- `Text`
- `Background`
- `Container`
- `Card`
- `Button`
- `Input`
- `Selector`
- `Divider`
- `Group`
- `Overlay`

The `type` describes the component's design meaning even though its extracted
resource is a WebP image.

### 5.4 Optional component fields

Add these fields when the source contains the corresponding information:

```json
{
  "parent_component_id": "phone_entry_card",
  "text": "Enter Your Phone Number",
  "rotation": 0,
  "corner_radius": 50,
  "clips_content": true,
  "blend_mode": "normal",
  "constraints": {
    "horizontal": "center",
    "vertical": "top"
  },
  "accessibility": {
    "role": "heading",
    "label": "Enter Your Phone Number"
  }
}
```

Rules:

- `parent_component_id` references another `component_id`, never a filename.
- Preserve `text` as plain Unicode text even when the visual is exported as WebP.
- Record rotation in degrees.
- Use `corner_radius` only when one radius applies to every corner. Otherwise use
  `corner_radii` with `top_left`, `top_right`, `bottom_right`, and `bottom_left`.
- Accessibility metadata should describe the intended UI role, not the bitmap.

## 6. Fitting placement encoding

`fitting_placement` records how the component should be positioned when the page
is later translated into a responsive Android layout.

### `fill_parent`

Use for a component that fills the complete page:

```json
{
  "position": { "x": 0, "y": 0 },
  "size": { "w": 852, "h": 1846 },
  "fitting_placement": "fill_parent"
}
```

### `relative_coordinate`

Use when the component is positioned from the page's top-left corner:

```json
{
  "position": { "x": 326, "y": 230 },
  "size": { "w": 200, "h": 200 },
  "fitting_placement": "relative_coordinate"
}
```

### `relative_coordinate_bottom`

Use when preserving the component's distance from the bottom is more important
than preserving its top coordinate. Include `bottom_margin`:

```json
{
  "position": { "x": 232, "y": 1719 },
  "size": { "w": 389, "h": 70 },
  "bottom_margin": 57,
  "fitting_placement": "relative_coordinate_bottom"
}
```

Calculate:

```text
bottom_margin = page_height - (y + component_height)
```

### `relative_coordinate_end`

Use when preserving distance from the right/end edge. Include `end_margin`:

```text
end_margin = page_width - (x + component_width)
```

### `relative_coordinate_bottom_end`

Use when both bottom and end margins must be preserved. Include both
`bottom_margin` and `end_margin`.

### `center_horizontal_relative_y`

Use for a horizontally centered component whose vertical position is measured
from the top. Include `center_x`:

```json
{
  "position": { "x": 326, "y": 230 },
  "size": { "w": 200, "h": 200 },
  "center_x": 426,
  "fitting_placement": "center_horizontal_relative_y"
}
```

### `match_width_relative_y`

Use when a component maintains left and right margins while its width adapts.
Include `start_margin` and `end_margin`:

```json
{
  "position": { "x": 55, "y": 834 },
  "size": { "w": 742, "h": 849 },
  "start_margin": 55,
  "end_margin": 55,
  "fitting_placement": "match_width_relative_y"
}
```

Choose one placement value per component. Do not use an undefined free-form
placement string.

## 7. Complete JSON example

```json
{
  "schema_version": 1,
  "page_name": "figma_login_page_1",
  "source": {
    "figma_file_key": "g6g9NbfmbQ4iTdTQRhqXcS",
    "figma_node_id": "2404:4641",
    "figma_node_name": "Login_Page_1",
    "figma_url": "https://www.figma.com/design/g6g9NbfmbQ4iTdTQRhqXcS/bitAim?node-id=2404-4641",
    "extracted_at_unix": 1776257332,
    "scale": 1
  },
  "coordinate_system": {
    "origin": "top_left",
    "x_direction": "right",
    "y_direction": "down",
    "unit": "figma_px"
  },
  "size": {
    "w": 852,
    "h": 1846
  },
  "preview": "figma_login_page_1.png",
  "layers": [
    {
      "layer_index": 0,
      "layer_name": "background",
      "components": [
        {
          "component_id": "background",
          "node_id": "2404:4642",
          "component_name": "background",
          "type": "Background",
          "res": "background.webp",
          "position": { "x": 0, "y": 0 },
          "size": { "w": 852, "h": 1846 },
          "asset_size": { "w": 852, "h": 1846 },
          "asset_offset": { "x": 0, "y": 0 },
          "fitting_placement": "fill_parent",
          "z_index": 0,
          "visible": true,
          "opacity": 1
        }
      ]
    },
    {
      "layer_index": 1,
      "layer_name": "content",
      "components": [
        {
          "component_id": "logo",
          "node_id": "2404:4651",
          "component_name": "logo",
          "type": "Image",
          "res": "logo.webp",
          "position": { "x": 326, "y": 230 },
          "size": { "w": 200, "h": 200 },
          "asset_size": { "w": 200, "h": 200 },
          "asset_offset": { "x": 0, "y": 0 },
          "center_x": 426,
          "fitting_placement": "center_horizontal_relative_y",
          "z_index": 1,
          "visible": true,
          "opacity": 1
        },
        {
          "component_id": "tagline_primary",
          "node_id": "2404:4648",
          "component_name": "tagline_primary",
          "type": "Text",
          "res": "tagline_primary.webp",
          "text": "HD Video Calls. Crystal Clear.",
          "position": { "x": 231, "y": 556 },
          "size": { "w": 390, "h": 31 },
          "asset_size": { "w": 390, "h": 31 },
          "asset_offset": { "x": 0, "y": 0 },
          "center_x": 426,
          "fitting_placement": "center_horizontal_relative_y",
          "z_index": 3,
          "visible": true,
          "opacity": 1
        }
      ]
    },
    {
      "layer_index": 2,
      "layer_name": "card",
      "components": [
        {
          "component_id": "phone_entry_card",
          "node_id": "2410:4692",
          "component_name": "phone_entry_card",
          "type": "Card",
          "res": "phone_entry_card.webp",
          "position": { "x": 55, "y": 834 },
          "size": { "w": 742, "h": 849 },
          "asset_size": { "w": 806, "h": 913 },
          "asset_offset": { "x": -32, "y": -32 },
          "start_margin": 55,
          "end_margin": 55,
          "fitting_placement": "match_width_relative_y",
          "z_index": 5,
          "visible": true,
          "opacity": 1
        }
      ]
    },
    {
      "layer_index": 3,
      "layer_name": "footer",
      "components": [
        {
          "component_id": "legal_terms",
          "node_id": "2404:4647",
          "component_name": "legal_terms",
          "type": "Text",
          "res": "legal_terms.webp",
          "text": "By continuing, you agree to our Terms of Service and Privacy Policy.",
          "position": { "x": 232, "y": 1719 },
          "size": { "w": 389, "h": 70 },
          "asset_size": { "w": 389, "h": 70 },
          "asset_offset": { "x": 0, "y": 0 },
          "bottom_margin": 57,
          "fitting_placement": "relative_coordinate_bottom",
          "z_index": 20,
          "visible": true,
          "opacity": 1
        }
      ]
    }
  ]
}
```

## 8. Parent and child components

When a component contains separately exported child components:

- Add the parent and children to the appropriate layer.
- Give every object a unique `component_id`.
- Set each child's `parent_component_id`.
- Keep every `position` page-relative, not parent-relative. This makes all
  coordinates directly comparable and avoids nested-coordinate ambiguity.
- Use `z_index` to preserve draw order.

If a composite Figma element cannot be separated faithfully, export only the
composite and do not invent child assets.

## 9. Hidden, clipped, and repeated elements

- Do not export hidden Figma layers as image files.
- Hidden layers may be omitted from JSON. If retained for auditing, set
  `"visible": false` and omit `res`.
- Respect clipping masks in the exported WebP.
- Export repeated visible instances separately only when their appearance differs.
- Identical repeated instances may share one `res`, but each instance must have its
  own component object, position, and `component_id`.

## 10. Validation checklist

An extraction is complete only when all checks pass:

1. The extraction directory follows `{page_name}_{unix_timestamp}`.
2. The full-page PNG exists and matches the Figma frame dimensions at `1x`.
3. Every meaningful visible component has a lossless WebP export.
4. Every asset uses a semantic lowercase `snake_case` filename.
5. The JSON parses successfully as UTF-8 JSON.
6. JSON page dimensions match the full-page PNG.
7. Every visible component's `res` file exists.
8. Every component has a unique `component_id`.
9. Every Figma-backed component records its real `node_id`.
10. Positions and layout sizes use page-relative Figma coordinates.
11. Actual bitmap dimensions are recorded in `asset_size`.
12. Effects outside layout bounds are represented with `asset_offset`.
13. Layer and component z-order reproduce the full-page PNG.
14. No asset URL is stored in JSON; all resources are local filenames.
15. No temporary PNG component exports remain after verified WebP conversion.

## 11. Extraction must not modify Android source

The extraction stage produces source artifacts only. It must not copy files into
`app/src/main/res`, generate Android XML, or modify Java/Kotlin code.

Android integration begins only after the extraction directory, preview PNG,
component WebPs, and JSON structure map have all passed validation.
