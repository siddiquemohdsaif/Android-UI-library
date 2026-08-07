package app.builderx.ogfa.androiduicomponents;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.card.Card;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.list.ComponentList;
import com.ogfa.nativeviews.text.FontVariation;
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import java.util.ArrayList;
import java.util.List;

/** Interactive virtualization, recycling, click, scroll, and fling test. */
public final class ComponentListTestActivity extends AppCompatActivity {

    private ComponentListTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testView = new ComponentListTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private static final class ComponentListTestView extends View {
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer content = ui.addLayer("content");
        private final Paint statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ArrayList<Player> players = new ArrayList<>();
        private ComponentList<Player> playerList;
        private String status = "SWIPE, FLING, OR TAP AN ITEM";
        private boolean initialized;

        ComponentListTestView(Context context) {
            super(context);
            setBackgroundColor(0xffeef4f8);
            setClickable(true);
            statusPaint.setColor(0xff123047);
            statusPaint.setTextAlign(Paint.Align.CENTER);
            statusPaint.setTextSize(dp(16));
            for (int index = 0; index < 60; index++) {
                players.add(new Player(10_000L + index,
                        "PLAYER " + (index + 1), "LEVEL " + (index % 12 + 1)));
            }
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width <= 0 || height <= 0) return;
            if (!initialized || width != oldWidth || height != oldHeight) {
                createList();
                initialized = true;
            }
        }

        private void createList() {
            content.clear();
            PlayerAdapter adapter = new PlayerAdapter(players);
            playerList = content.add(new ComponentList.Builder<Player>(
                    getContext(),
                    "players",
                    new Position(
                            this,
                            Position.HorizontalMarginFrom.LEFT,
                            Position.VerticalMarginFrom.TOP,
                            54f,
                            180f
                    ),
                    new Size(972f, 1450f)
            )
                    .setOrientation(ComponentList.Orientation.VERTICAL)
                    .setItemSize(180f)
                    .setItemSpacing(20f)
                    .setPadding(12f)
                    .setAdapter(adapter)
                    .setOnItemClickListener((list, player, position) -> {
                        status = "ITEM " + position + ": " + player.name;
                        invalidate();
                    })
                    .setOnItemLongClickListener((list, player, position) -> {
                        status = "LONG CLICK: " + player.name;
                        invalidate();
                        return true;
                    }));

            if (playerList.getAdapter() != adapter
                    || playerList.getOrientation() != ComponentList.Orientation.VERTICAL
                    || playerList.getBounds().isEmpty()) {
                throw new AssertionError("ComponentList initialization failed.");
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ui.draw(canvas);
            canvas.drawText(status, getWidth() / 2f, dp(55), statusPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        void release() { ui.release(); }

        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

        private final class PlayerAdapter extends ComponentList.Adapter<Player> {
            private final List<Player> values;

            PlayerAdapter(List<Player> values) { this.values = values; }
            @Override public int getItemCount() { return values.size(); }
            @Override public Player getItem(int position) { return values.get(position); }
            @Override public long getItemId(int position) { return values.get(position).id; }

            @Override
            public void onCreateItem(ComponentList.Item item, int viewType) {
                ComponentList.ItemScope scope = item.getScope();
                ZLayer background = item.addLayer("background");
                ZLayer labels = item.addLayer("labels");

                background.add(new Card.Builder(
                        getContext(),
                        scope.id("surface"),
                        scope.rect(0f, 0f, scope.width(), scope.height())
                )
                        .setBackgroundColor(Color.WHITE)
                        .setCornerRadiusPx(scope.px(28f))
                        .removeDropShadow());

                labels.add(new Text.Builder(
                        getContext(),
                        scope.id("name"),
                        "",
                        scope.rect(40f, 28f, scope.width() - 80f, 60f)
                )
                        .setFont(NativeFonts.INTER)
                        .setFontVariations(FontVariation.BOLD)
                        .setTextSizePx(scope.px(34f))
                        .setTextColor(0xff102a43)
                        .setVerticalAlignment(Text.VerticalAlignment.CENTER));

                labels.add(new Text.Builder(
                        getContext(),
                        scope.id("level"),
                        "",
                        scope.rect(40f, 96f, scope.width() - 80f, 45f)
                )
                        .setFont(NativeFonts.INTER)
                        .setTextSizePx(scope.px(25f))
                        .setTextColor(0xff009fc8)
                        .setVerticalAlignment(Text.VerticalAlignment.CENTER));
            }

            @Override
            public void onBindItem(ComponentList.Item item, Player player, int position) {
                item.find("name", Text.class).setText(player.name);
                item.find("level", Text.class).setText(
                        player.level + "  •  ID " + player.id);
            }
        }
    }

    private static final class Player {
        final long id;
        final String name;
        final String level;
        Player(long id, String name, String level) {
            this.id = id;
            this.name = name;
            this.level = level;
        }
    }
}
