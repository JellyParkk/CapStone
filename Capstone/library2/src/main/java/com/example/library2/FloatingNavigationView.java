package com.example.library2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.os.ParcelableCompat;
import androidx.core.os.ParcelableCompatCreatorCallbacks;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.android.material.circularreveal.CircularRevealCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.internal.NavigationMenuView;
import com.google.android.material.navigation.NavigationView;

@SuppressWarnings({"FieldCanBeLocal", "InflateParams", "RtlHardcoded", "unused", "WeakerAccess"})
public class FloatingNavigationView extends FloatingActionButton {

    private final WindowManager mWindowManager;

    private final CircularRevealNavigationView mNavigationView;
    private final NavigationMenuView mNavigationMenuView;
    private final ImageView mFabView;

    private final Rect mFabRect = new Rect();
    private final Rect mNavigationRect = new Rect();

    private final boolean mDrawMenuBelowFab;
    private final OnTouchListener mNavigationTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(@NonNull View v, @NonNull MotionEvent event) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            if ((MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN)
                    && ((x < 0) || (x >= mNavigationView.getWidth())
                    || (y < 0) || (y >= mNavigationView.getHeight()))) {
                close();
                return true;
            } else if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_OUTSIDE) {
                close();
                return true;
            }
            return false;
        }
    };
    private final OnClickListener mFabClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            close();
        }
    };

    public FloatingNavigationView(Context context) {
        this(context, null);
    }

    public FloatingNavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setImageResource(R.drawable.ic_menu_vector);

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        mNavigationView = new CircularRevealNavigationView(context, attrs, defStyleAttr);
        setupNavigationView(context, attrs);
        mNavigationMenuView = mNavigationView.findViewById(R.id.design_navigation_view);

        mFabView = new ImageView(context, attrs, defStyleAttr);
        setupFabView(context, attrs);

        // Custom attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FloatingNavigationView);
        mDrawMenuBelowFab = a.getBoolean(R.styleable.FloatingNavigationView_drawMenuBelowFab, false);
        a.recycle();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupNavigationView(Context context, AttributeSet attrs) {
        mNavigationView.setBackground(new ColorDrawable(getBackgroundColor()));
        mNavigationView.setOnTouchListener(mNavigationTouchListener);
    }

    @SuppressLint("PrivateResource")
    private void setupFabView(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, new int[]{R.attr.selectableItemBackgroundBorderless});
        mFabView.setScaleType(ScaleType.CENTER);
        mFabView.setBackground(a.getDrawable(0));
        mFabView.setOnClickListener(mFabClickListener);
        mFabView.setContentDescription(getContentDescription());
        mFabView.bringToFront();
        a.recycle();
        mNavigationView.addView(mFabView, new FrameLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.design_fab_size_normal),
                getResources().getDimensionPixelSize(R.dimen.design_fab_size_normal)
        ));
    }

    private @ColorInt int getBackgroundColor() {
        ColorStateList colorStateList = getBackgroundTintList();
        if (colorStateList != null) {
            return colorStateList.getDefaultColor();
        }
        Drawable background = getBackground();
        if (background instanceof ColorDrawable) {
            return ((ColorDrawable) background).getColor();
        }
        return getSolidColor();
    }

    @Override
    public void setBackgroundTintList(@Nullable ColorStateList tint) {
        super.setBackgroundTintList(tint);
        ViewCompat.setBackgroundTintList(mNavigationView, tint);
    }

    @NonNull
    private static WindowManager.LayoutParams createLayoutParams(int gravity) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = gravity;
        return layoutParams;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        detachNavigationView(); // Prevent Leaked Window when configuration changes
    }

    public void open() {
        if (isOpened()) {
            return;
        }
        attachNavigationView();
        mNavigationMenuView.scrollToPosition(0);
        mNavigationView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mNavigationView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                updateFabBounds();
                drawMenuBelowFab();
                startOpenAnimations();
            }
        });
    }

    public void close() {
        if (!isOpened()) {
            return;
        }
        startCloseAnimations();
    }

    public boolean isOpened() {
        return mNavigationView.getParent() != null;
    }

    private void attachNavigationView() {

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) getLayoutParams();
        int gravity = Gravity.LEFT;
        if (layoutParams.getAnchorId() != View.NO_ID && layoutParams.anchorGravity != Gravity.NO_GRAVITY) {
            if (Gravity.isHorizontal(layoutParams.anchorGravity)) {
                gravity = layoutParams.anchorGravity;
            }
        } else if (layoutParams.gravity != Gravity.NO_GRAVITY) {
            if (Gravity.isHorizontal(layoutParams.gravity)) {
                gravity = layoutParams.gravity;
            }
        }

        // Gravity.START and Gravity.END don't work for views added in WindowManager with RTL.
        // We need to convert script specific gravity to absolute horizontal value
        // If horizontal direction is LTR, then START will set LEFT and END will set RIGHT.
        // If horizontal direction is RTL, then START will set RIGHT and END will set LEFT.
        gravity = Gravity.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));

        mWindowManager.addView(mNavigationView, createLayoutParams(gravity));
    }

    private void detachNavigationView() {
        if (isOpened()) {
            mWindowManager.removeViewImmediate(mNavigationView);
        }
    }

    private void startOpenAnimations() {

        // Icon
        AnimatedVectorDrawableCompat menuIcon = AnimatedVectorDrawableCompat.create(getContext(),
                R.drawable.ic_menu_animated);
        mFabView.setImageDrawable(menuIcon);
        menuIcon.start();

        // Reveal
        int centerX = mFabRect.centerX();
        int centerY = mFabRect.centerY();
        float startRadius = getMinRadius();
        float endRadius = getMaxRadius();
        Animator reveal = CircularRevealCompat.createCircularReveal(
                mNavigationView, centerX, centerY, startRadius, endRadius
        );

        // Fade in
        mNavigationMenuView.setAlpha(0);
        Animator fade = ObjectAnimator.ofFloat(mNavigationMenuView, View.ALPHA, 0, 1);

        // Animations
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(reveal, fade);
        set.start();
    }

    private void startCloseAnimations() {

        // Icon
        AnimatedVectorDrawableCompat closeIcon = AnimatedVectorDrawableCompat.create(getContext(),
                R.drawable.ic_close_animated);
        mFabView.setImageDrawable(closeIcon);
        closeIcon.start();

        // Unreveal
        int centerX = mFabRect.centerX();
        int centerY = mFabRect.centerY();
        float startRadius = getMaxRadius();
        float endRadius = getMinRadius();
        Animator reveal = CircularRevealCompat.createCircularReveal(
                mNavigationView, centerX, centerY, startRadius, endRadius
        );
        reveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                detachNavigationView();
            }
        });

        // Fade out
        Animator fade = ObjectAnimator.ofFloat(mNavigationMenuView, View.ALPHA, 1, 0);

        // Animations
        AnimatorSet set = new AnimatorSet();
        set.playTogether(fade, reveal);
        set.start();
    }

    private void updateFabBounds() {
        updateFabRect();
        updateNavigationRect();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mFabView.getLayoutParams();
        layoutParams.width = getWidth();
        layoutParams.height = getHeight();
        layoutParams.topMargin = mFabRect.top;
        if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            layoutParams.rightMargin = mNavigationView.getWidth() - mFabRect.right;
        } else {
            layoutParams.leftMargin = mFabRect.left;
        }
        mFabView.setLayoutParams(layoutParams);
    }

    private void updateFabRect() {
        getGlobalVisibleRect(mFabRect);
        int[] offset = new int[2];
        mNavigationView.getLocationOnScreen(offset);
        mFabRect.offset(-offset[0], -offset[1]);
    }

    private void updateNavigationRect() {
        mNavigationView.getGlobalVisibleRect(mNavigationRect);
    }

    private void drawMenuBelowFab() {
        if (mDrawMenuBelowFab) {
            // Add padding top to positioning menu below the FAB
            mNavigationMenuView.setPadding(
                    mNavigationMenuView.getPaddingLeft(),
                    mFabRect.bottom,
                    mNavigationMenuView.getPaddingRight(),
                    mNavigationMenuView.getPaddingBottom());
        }
    }

    private float getMinRadius() {
        return mFabRect.width() / 2f;
    }

    private float getMaxRadius() {
        return (float) Math.hypot(mNavigationRect.width(), mNavigationRect.height());
    }

    /**
     * Set a listener that will be notified when a menu item is clicked.
     *
     * @param listener The listener to notify
     */
    public void setNavigationItemSelectedListener(@Nullable NavigationView.OnNavigationItemSelectedListener listener) {
        mNavigationView.setNavigationItemSelectedListener(listener);
    }

    /*
      {@link NavigationView} methods
     */

    /**
     * Inflate a menu resource into this navigation view.
     * <p/>
     * <p>Existing items in the menu will not be modified or removed.</p>
     *
     * @param resId ID of a menu resource to inflate
     */
    public void inflateMenu(@MenuRes int resId) {
        mNavigationView.inflateMenu(resId);
    }

    /**
     * Returns the {@link Menu} instance associated with this navigation view.
     */
    public Menu getMenu() {
        return mNavigationView.getMenu();
    }

    /**
     * Inflates a View and add it as a header of the navigation menu.
     *
     * @param res The layout resource ID.
     * @return a newly inflated View.
     */
    public View inflateHeaderView(@LayoutRes int res) {
        return mNavigationView.inflateHeaderView(res);
    }

    /**
     * Adds a View as a header of the navigation menu.
     *
     * @param view The view to be added as a header of the navigation menu.
     */
    public void addHeaderView(@NonNull View view) {
        mNavigationView.addHeaderView(view);
    }

    /**
     * Removes a previously-added header view.
     *
     * @param view The view to remove
     */
    public void removeHeaderView(@NonNull View view) {
        mNavigationView.removeHeaderView(view);
    }

    /**
     * Gets the number of headers in this NavigationView.
     *
     * @return A positive integer representing the number of headers.
     */
    public int getHeaderCount() {
        return mNavigationView.getHeaderCount();
    }

    /**
     * Gets the header view at the specified position.
     *
     * @param index The position at which to get the view from.
     * @return The header view the specified position or null if the position does not exist in this
     * NavigationView.
     */
    public View getHeaderView(int index) {
        return mNavigationView.getHeaderView(index);
    }

    /**
     * Sets the currently checked item in this navigation menu.
     *
     * @param id The item ID of the currently checked item.
     */
    public void setCheckedItem(@IdRes int id) {
        mNavigationView.setCheckedItem(id);
    }

    /**
     * {@link SavedState} methods
     */

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.navigationViewState = new SparseArray();
        //noinspection unchecked
        mNavigationView.saveHierarchyState(ss.navigationViewState);
        ss.opened = isOpened();
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        //noinspection unchecked
        mNavigationView.restoreHierarchyState(ss.navigationViewState);
        if (ss.opened) {
            mFabView.setImageResource(R.drawable.ic_close_vector);
            // Run on post to prevent "unable to add window -- token null is not valid" error
            post(new Runnable() {
                @Override
                public void run() {
                    attachNavigationView();
                    mNavigationView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mNavigationView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            updateFabBounds();
                            drawMenuBelowFab();
                        }
                    });
                }
            });
        }
    }

    public static class SavedState extends AbsSavedState {

        private SparseArray navigationViewState;
        private boolean opened;

        private SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            navigationViewState = in.readSparseArray(loader);
            opened = (Boolean) in.readValue(loader);
        }

        private SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            //noinspection unchecked
            dest.writeSparseArray(navigationViewState);
            dest.writeValue(opened);
        }

        @Override
        public String toString() {
            return FloatingNavigationView.class.getSimpleName() + "." + SavedState.class.getSimpleName() + "{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " opened=" + opened + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel, ClassLoader loader) {
                return new SavedState(parcel, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        });
    }
}

