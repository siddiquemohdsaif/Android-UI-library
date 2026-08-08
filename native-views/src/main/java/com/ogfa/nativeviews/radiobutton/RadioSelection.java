package com.ogfa.nativeviews.radiobutton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Non-visual mutual-exclusion controller for RadioButtons placed in any ZLayers. */
public final class RadioSelection {
    private final String id;
    private final LinkedHashMap<String, RadioButton> buttons = new LinkedHashMap<>();
    private String selectedId;
    private boolean selectionRequired;
    private boolean released;
    private OnSelectionChangedListener listener;

    public RadioSelection(String id) { this.id = requireId(id, "RadioSelection"); }

    public String getId() { return id; }
    public String getSelectedId() { ensureActive(); return selectedId; }
    public RadioButton getSelectedButton() {
        ensureActive(); return selectedId == null ? null : buttons.get(selectedId);
    }
    public boolean hasSelection() { ensureActive(); return selectedId != null; }
    public boolean isSelectionRequired() { return selectionRequired; }
    public RadioButton find(String buttonId) {
        ensureActive(); return buttons.get(requireId(buttonId, "RadioButton"));
    }
    public boolean contains(String buttonId) { return find(buttonId) != null; }
    public List<RadioButton> getButtons() {
        ensureActive();
        return Collections.unmodifiableList(new ArrayList<>(buttons.values()));
    }

    public RadioSelection select(String buttonId) { return select(buttonId, true); }
    public RadioSelection select(String buttonId, boolean notifyListener) {
        RadioButton target = requireButton(buttonId);
        requestChecked(target, true, true, notifyListener, false);
        return this;
    }
    public RadioSelection selectImmediately(String buttonId) {
        RadioButton target = requireButton(buttonId);
        requestChecked(target, true, false, true, false);
        return this;
    }
    public RadioSelection clearSelection() { return clearSelection(true); }
    public RadioSelection clearSelection(boolean notifyListener) {
        ensureActive();
        if (selectionRequired && selectedId != null) {
            throw new IllegalStateException(
                    "RadioSelection requires one selected button: " + id);
        }
        if (selectedId == null) return this;
        RadioButton old = buttons.get(selectedId);
        String oldId = selectedId;
        selectedId = null;
        if (old != null) old.applyCheckedFromSelection(false, true, false, notifyListener);
        notifySelection(oldId, null, false, notifyListener);
        return this;
    }

    public RadioSelection setSelectionRequired(boolean value) {
        ensureActive(); selectionRequired = value;
        if (value && selectedId == null) {
            RadioButton first = firstEnabledButton();
            if (first != null) requestChecked(first, true, false, true, false);
        }
        return this;
    }
    public RadioSelection setOnSelectionChangedListener(OnSelectionChangedListener value) {
        ensureActive(); listener = value; return this;
    }
    public RadioSelection removeOnSelectionChangedListener() {
        return setOnSelectionChangedListener(null);
    }

    void register(RadioButton button, boolean initiallyChecked) {
        ensureActive();
        String buttonId = button.getId();
        RadioButton existing = buttons.get(buttonId);
        if (existing != null && existing != button) {
            throw new IllegalArgumentException(
                    "Duplicate RadioButton ID in selection " + id + ": " + buttonId);
        }
        buttons.put(buttonId, button);
        if (!initiallyChecked) return;
        if (selectedId != null && !selectedId.equals(buttonId)) {
            buttons.remove(buttonId);
            throw new IllegalStateException(
                    "RadioSelection cannot register multiple initially checked buttons: " + id);
        }
        selectedId = buttonId;
    }

    void unregister(RadioButton button) {
        if (released) return;
        RadioButton existing = buttons.get(button.getId());
        if (existing != button) return;
        boolean wasSelected = button.getId().equals(selectedId);
        String oldId = wasSelected ? selectedId : null;
        buttons.remove(button.getId());
        if (!wasSelected) return;
        selectedId = null;
        RadioButton replacement = selectionRequired ? firstEnabledButton() : null;
        if (replacement != null) {
            selectedId = replacement.getId();
            replacement.applyCheckedFromSelection(true, false, false, true);
        }
        notifySelection(oldId, selectedId, false, true);
    }

    void requestChecked(
            RadioButton button,
            boolean checked,
            boolean animate,
            boolean notifyListener,
            boolean fromUser
    ) {
        ensureActive();
        if (buttons.get(button.getId()) != button) {
            throw new IllegalStateException(
                    "RadioButton is not registered with selection " + id + ": " + button.getId());
        }
        if (checked) {
            if (button.getId().equals(selectedId)) return;
            String oldId = selectedId;
            RadioButton old = oldId == null ? null : buttons.get(oldId);
            selectedId = button.getId();
            if (old != null) old.applyCheckedFromSelection(
                    false, animate, fromUser, notifyListener);
            button.applyCheckedFromSelection(true, animate, fromUser, notifyListener);
            notifySelection(oldId, selectedId, fromUser, notifyListener);
            return;
        }
        if (!button.getId().equals(selectedId)) {
            button.applyCheckedFromSelection(false, animate, fromUser, notifyListener);
            return;
        }
        if (selectionRequired) {
            throw new IllegalStateException(
                    "Cannot uncheck the selected RadioButton while selection is required: " + id);
        }
        String oldId = selectedId;
        selectedId = null;
        button.applyCheckedFromSelection(false, animate, fromUser, notifyListener);
        notifySelection(oldId, null, fromUser, notifyListener);
    }

    void onButtonEnabledChanged(RadioButton button) {
        if (released || !selectionRequired) return;
        if (selectedId == null && button.isEnabled()) {
            requestChecked(button, true, true, true, false);
            return;
        }
        if (button.getId().equals(selectedId) && !button.isEnabled()) {
            RadioButton replacement = firstEnabledButton();
            if (replacement != null) requestChecked(replacement, true, true, true, false);
        }
    }

    public void release() {
        if (released) return;
        released = true;
        for (Map.Entry<String, RadioButton> entry : buttons.entrySet()) {
            entry.getValue().detachSelectionFromController(this);
        }
        buttons.clear(); selectedId = null; listener = null;
    }

    private void notifySelection(
            String oldId, String newId, boolean fromUser, boolean notifyListener) {
        if (notifyListener && listener != null && !equalsNullable(oldId, newId)) {
            listener.onSelectionChanged(id, oldId, newId, fromUser);
        }
    }
    private RadioButton requireButton(String buttonId) {
        ensureActive();
        RadioButton button = buttons.get(requireId(buttonId, "RadioButton"));
        if (button == null) {
            throw new IllegalArgumentException(
                    "Unknown RadioButton ID in selection " + id + ": " + buttonId);
        }
        return button;
    }
    private RadioButton firstEnabledButton() {
        for (RadioButton button : buttons.values()) if (button.isEnabled()) return button;
        return null;
    }
    private void ensureActive() {
        if (released) throw new IllegalStateException("RadioSelection has been released: " + id);
    }
    private static String requireId(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " ID cannot be blank.");
        }
        return value.trim();
    }
    private static boolean equalsNullable(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}
