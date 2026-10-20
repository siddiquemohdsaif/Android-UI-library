package com.ogfa.nativeviews.text.selection;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

/** Common selection actions and toolbar placement for Text and TextField. */
public final class TextSelectionActions {
    private TextSelectionActions() {}

    public static void addCommonActions(Menu menu, Context context, int copyOrder,
                                        int selectAllOrder, int shareOrder,
                                        CharSequence shareTitle) {
        menu.add(0, android.R.id.copy, copyOrder, context.getString(android.R.string.copy))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, android.R.id.selectAll, selectAllOrder,
                context.getString(android.R.string.selectAll))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, android.R.id.shareText, shareOrder, shareTitle)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    public static void updateSecondaryActionPlacement(Menu menu) {
        int visible = 0;
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).isVisible()) visible++;
        }
        int placement = visible >= 4
                ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_IF_ROOM;
        MenuItem share = menu.findItem(android.R.id.shareText);
        MenuItem replace = menu.findItem(android.R.id.replaceText);
        if (share != null) share.setShowAsAction(placement);
        if (replace != null) replace.setShowAsAction(placement);
    }

    public static boolean copy(Context context, CharSequence label, CharSequence selected) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return false;
        clipboard.setPrimaryClip(ClipData.newPlainText(label, selected));
        return true;
    }

    public static boolean share(Context context, CharSequence selected) {
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, selected.toString());
        Intent chooser = Intent.createChooser(send, null);
        if (findActivity(context) == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(chooser);
            return true;
        } catch (ActivityNotFoundException exception) {
            return false;
        }
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            Context base = ((ContextWrapper) current).getBaseContext();
            if (base == current) break;
            current = base;
        }
        return current instanceof Activity ? (Activity) current : null;
    }
}
