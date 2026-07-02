package device.apps.rfidsamplev2.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helper for handling the system-bar insets now that the app draws edge-to-edge (enforced
 * from {@code targetSdk} 35). {@link #applyBarInsets(View, View, View)} installs a single
 * listener on the layout root — which always receives the window insets — and from there pads
 * the toolbar (behind the status bar) and the bottom bar (above the navigation bar), adding to
 * each view's existing padding so its own design padding (e.g. a button bar's 16dp margins) is
 * preserved. It also calls {@link ViewCompat#requestApplyInsets(View)} so the result is
 * deterministic across cold and warm starts (a freshly installed listener is not otherwise
 * guaranteed to replay an inset dispatch that already happened).
 */
public final class WindowInsetsUtil {

    private WindowInsetsUtil() {
    }

    /**
     * Apply the system-bar insets to a screen's top bar and bottom element via a <b>single
     * listener on the root view</b>. This is the robust variant: a per-child listener is not
     * reliably dispatched through every root container (a plain {@code LinearLayout} root, for
     * instance, does not deliver the status-bar inset to a toolbar child the way a
     * {@code ConstraintLayout} does), whereas the root view always receives the insets. The
     * top bar grows to {@code ?attr/actionBarSize + statusBarInset} so its content row keeps a
     * full action-bar height and the extra inset draws the background behind the status bar —
     * this is independent of whether the toolbar has a navigation icon, so every screen's
     * toolbar ends up the same height (a title-only toolbar like the launcher screen is not
     * shorter than the back-arrow toolbars). Pair the toolbar with {@code layout_height="wrap_content"}
     * + {@code minHeight="?attr/actionBarSize"} so {@link View#getMinimumHeight()} yields the
     * action-bar size. The bottom element gets the navigation-bar + horizontal insets. Existing
     * padding is preserved.
     *
     * @param root       the layout root that receives the window insets
     * @param topBar     the toolbar to grow/pad at the top (may be {@code null} to skip)
     * @param bottomView the sticky bottom bar or bottom-most scroll content (may be {@code null})
     */
    public static void applyBarInsets(View root, View topBar, View bottomView) {
        final int tl = topBar == null ? 0 : topBar.getPaddingLeft();
        final int tr = topBar == null ? 0 : topBar.getPaddingRight();
        final int tb = topBar == null ? 0 : topBar.getPaddingBottom();
        final int barContentHeight = topBar == null ? 0 : topBar.getMinimumHeight();
        final int bl = bottomView == null ? 0 : bottomView.getPaddingLeft();
        final int bt = bottomView == null ? 0 : bottomView.getPaddingTop();
        final int br = bottomView == null ? 0 : bottomView.getPaddingRight();
        final int bb = bottomView == null ? 0 : bottomView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (topBar != null) {
                // Fix the toolbar height at actionBarSize + statusInset so the content row is a
                // full action-bar tall regardless of nav-icon presence; top padding pushes the
                // content below the status bar while the background fills behind it.
                if (barContentHeight > 0) {
                    final ViewGroup.LayoutParams lp = topBar.getLayoutParams();
                    lp.height = barContentHeight + bars.top;
                    topBar.setLayoutParams(lp);
                }
                topBar.setPadding(tl + bars.left, bars.top, tr + bars.right, tb);
            }
            if (bottomView != null) {
                bottomView.setPadding(bl + bars.left, bt, br + bars.right, bb + bars.bottom);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
