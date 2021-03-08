package foundation.e.blisslauncher.core.customviews;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.GridLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.customviews.pageindicators.PageIndicatorDots;
import foundation.e.blisslauncher.core.database.model.ApplicationItem;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.touch.ItemClickHandler;
import foundation.e.blisslauncher.core.touch.ItemLongClickListener;
import foundation.e.blisslauncher.core.utils.Constants;
import foundation.e.blisslauncher.core.utils.LongArrayMap;
import foundation.e.blisslauncher.features.launcher.Hotseat;
import foundation.e.blisslauncher.features.test.Alarm;
import foundation.e.blisslauncher.features.test.CellLayout;
import foundation.e.blisslauncher.features.test.IconTextView;
import foundation.e.blisslauncher.features.test.OnAlarmListener;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.dragndrop.DragController;
import foundation.e.blisslauncher.features.test.dragndrop.DragOptions;
import foundation.e.blisslauncher.features.test.dragndrop.DragSource;
import foundation.e.blisslauncher.features.test.dragndrop.DragView;
import foundation.e.blisslauncher.features.test.dragndrop.DropTarget;
import foundation.e.blisslauncher.features.test.dragndrop.SpringLoadedDragController;
import foundation.e.blisslauncher.features.test.graphics.DragPreviewProvider;

public class LauncherPagedView extends PagedView<PageIndicatorDots> implements View.OnTouchListener,
    Insettable, DropTarget, DragSource, DragController.DragListener {

    private static final String TAG = "LauncherPagedView";
    private static final int DEFAULT_PAGE = 0;
    private static final int SNAP_OFF_EMPTY_SCREEN_DURATION = 400;
    private static final int FADE_EMPTY_SCREEN_DURATION = 150;

    private static final boolean MAP_NO_RECURSE = false;
    private static final boolean MAP_RECURSE = true;

    // The screen id used for the empty screen always present to the right.
    public static final long EXTRA_EMPTY_SCREEN_ID = -201;
    // The is the first screen. It is always present, even if its empty.
    public static final long FIRST_SCREEN_ID = 0;

    private final TestActivity mLauncher;

    private LayoutTransition mLayoutTransition;
    final WallpaperManager mWallpaperManager;

    final LongArrayMap<CellLayout> mWorkspaceScreens = new LongArrayMap<>();
    final ArrayList<Long> mScreenOrder = new ArrayList<>();

    // Variables relating to touch disambiguation (scrolling workspace vs. scrolling a widget)
    private float mXDown;
    private float mYDown;
    final static float START_DAMPING_TOUCH_SLOP_ANGLE = (float) Math.PI / 6;
    final static float MAX_SWIPE_ANGLE = (float) Math.PI / 3;
    final static float TOUCH_SLOP_DAMPING_FACTOR = 4;

    private final static Paint sPaint = new Paint();

    Runnable mRemoveEmptyScreenRunnable;
    boolean mDeferRemoveExtraEmptyScreen = false;
    private boolean mStripScreensOnPageStopMoving = false;
    private SpringLoadedDragController mSpringLoadedDragController;
    private DragController mDragController;
    private boolean mChildrenLayersEnabled = true;
    private CellLayout.CellInfo mDragInfo;
    private CellLayout mDragSourceInternal;
    private DragPreviewProvider mOutlineProvider;
    private final int[] mTempXY = new int[2];
    float[] mDragViewVisualCenter = new float[2];
    private final float[] mTempTouchCoordinates = new float[2];
    private boolean mAddToExistingFolderOnDrop;
    private boolean mCreateUserFolderOnDrop;
    private CellLayout mDropToLayout;
    private CellLayout mDragTargetLayout;
    private CellLayout mDragOverlappingLayout;
    /**
     * Target drop area calculated during last acceptDrop call.
     */
    int[] mTargetCell = new int[2];
    private int mDragOverX = -1;
    private int mDragOverY = -1;

    public static final int REORDER_TIMEOUT = 650;
    private final Alarm mFolderCreationAlarm = new Alarm();
    private final Alarm mReorderAlarm = new Alarm();

    // Related to dragging, folder creation and reordering
    private static final int DRAG_MODE_NONE = 0;
    private static final int DRAG_MODE_CREATE_FOLDER = 1;
    private static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    private static final int DRAG_MODE_REORDER = 3;
    private int mDragMode = DRAG_MODE_NONE;
    int mLastReorderX = -1;
    int mLastReorderY = -1;

    public LauncherPagedView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public LauncherPagedView(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);

        mLauncher = TestActivity.Companion.getLauncher(context);
        mWallpaperManager = WallpaperManager.getInstance(context);
        setHapticFeedbackEnabled(false);
        initWorkspace();

        setMotionEventSplittingEnabled(true);
        setOnTouchListener((v, event) -> false);
    }

    private void initWorkspace() {
        mCurrentPage = DEFAULT_PAGE;
        setClipToPadding(false);
        setupLayoutTransition();
        //setWallpaperDimension();
    }

    private void setupLayoutTransition() {
        // We want to show layout transitions when pages are deleted, to close the gap.
        mLayoutTransition = new LayoutTransition();
        mLayoutTransition.enableTransitionType(LayoutTransition.DISAPPEARING);
        mLayoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        mLayoutTransition.disableTransitionType(LayoutTransition.APPEARING);
        mLayoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        setLayoutTransition(mLayoutTransition);
    }

    void enableLayoutTransitions() {
        setLayoutTransition(mLayoutTransition);
    }

    void disableLayoutTransitions() {
        setLayoutTransition(null);
    }

    @Override
    public void onViewAdded(View child) {
        if (!(child instanceof GridLayout)) {
            throw new IllegalArgumentException("A Workspace can only have GridLayout children.");
        }
        GridLayout grid = (GridLayout) child;
        //grid.setOnInterceptOnTouchListener(this);
        super.onViewAdded(child);
    }

    public boolean isTouchActive() {
        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public void setInsets(WindowInsets insets) {
        mInsets.set(
            insets.getSystemWindowInsetLeft(),
            insets.getSystemWindowInsetTop(),
            insets.getSystemWindowInsetRight(),
            insets.getSystemWindowInsetBottom()
        );

        VariantDeviceProfile grid = mLauncher.getDeviceProfile();
        Rect padding = grid.getWorkspacePadding();
        setPadding(padding.left, padding.top, padding.right, padding.bottom);
        int paddingLeftRight = grid.getCellLayoutPaddingLeftRightPx();
        int paddingBottom = grid.getCellLayoutBottomPaddingPx();
        for (int i = mWorkspaceScreens.size() - 1; i >= 0; i--) {
            mWorkspaceScreens.valueAt(i)
                .setPadding(paddingLeftRight, 0, paddingLeftRight, paddingBottom);
        }
    }

    public void deferRemoveExtraEmptyScreen() {
        mDeferRemoveExtraEmptyScreen = true;
    }

    public void bindAndInitFirstScreen(View view) {
        // Do nothing here.
    }

    public void removeAllWorkspaceScreens() {
        // Disable all layout transitions before removing all pages to ensure that we don't get the
        // transition animations competing with us changing the scroll when we add pages
        disableLayoutTransitions();

        // Remove the pages and clear the screen models
        //removeFolderListeners();
        removeAllViews();
        mScreenOrder.clear();
        mWorkspaceScreens.clear();

        // Ensure that the first page is always present
        //bindAndInitFirstScreen(qsb);

        // Re-enable the layout transitions
        enableLayoutTransitions();
    }

    public void insertNewWorkspaceScreenBeforeEmptyScreen(long screenId) {
        // Find the index to insert this view into.  If the empty screen exists, then
        // insert it before that.
        int insertIndex = mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID);
        if (insertIndex < 0) {
            insertIndex = mScreenOrder.size();
        }
        insertNewWorkspaceScreen(screenId, insertIndex);
    }

    public void bindItems(@NotNull List<? extends LauncherItem> launcherItems) {
        removeAllWorkspaceScreens();
        GridLayout workspaceScreen = insertNewWorkspaceScreen(0);
        for (LauncherItem launcherItem : launcherItems) {
            IconTextView appView = new IconTextView(getContext());
            appView.applyFromShortcutItem(launcherItem);
            appView.setOnClickListener(ItemClickHandler.INSTANCE);
            appView.setOnLongClickListener(ItemLongClickListener.INSTANCE_WORKSPACE);
            if (launcherItem.container == Constants.CONTAINER_DESKTOP) {
                if (workspaceScreen.getChildCount() >= mLauncher.getDeviceProfile().getInv()
                    .getNumRows() * mLauncher.getDeviceProfile().getInv().getNumColumns()
                    || launcherItem.screenId > mScreenOrder.size() - 1) {
                    workspaceScreen = insertNewWorkspaceScreen(mScreenOrder.size());
                }
                launcherItem.screenId = mScreenOrder.size() - 1;
                launcherItem.cell = workspaceScreen.getChildCount();
                /*GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.LayoutParams iconLayoutParams =
                    new GridLayout.LayoutParams(rowSpec, colSpec);
                iconLayoutParams.height = mLauncher.getDeviceProfile().getCellHeightPx();
                iconLayoutParams.width = mLauncher.getDeviceProfile().getCellWidthPx();*/
                //appView.findViewById(R.id.app_label).setVisibility(View.VISIBLE);
                //appView.setLayoutParams(iconLayoutParams);
                //appView.setWithText(true);
                workspaceScreen.addView(appView);
            } else if (launcherItem.container == Constants.CONTAINER_HOTSEAT) {
                //appView.findViewById(R.id.app_label).setVisibility(GONE);
                GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.LayoutParams iconLayoutParams =
                    new GridLayout.LayoutParams(rowSpec, colSpec);
                iconLayoutParams.setGravity(Gravity.CENTER);
                appView.setLayoutParams(iconLayoutParams);
                //appView.setWithText(false);
                mLauncher.getHotseat().getLayout().addView(appView);
            }
        }
    }

    public GridLayout insertNewWorkspaceScreen(long screenId) {
        return insertNewWorkspaceScreen(screenId, getChildCount());
    }

    public GridLayout insertNewWorkspaceScreen(long screenId, int insertIndex) {
        if (mWorkspaceScreens.containsKey(screenId)) {
            throw new RuntimeException("Screen id " + screenId + " already exists!");
        }

        // Inflate the cell layout, but do not add it automatically so that we can get the newly
        // created CellLayout.
        CellLayout newScreen = (CellLayout) LayoutInflater.from(getContext()).inflate(
            R.layout.workspace_screen, this, false /* attachToRoot */);
        int paddingLeftRight = mLauncher.getDeviceProfile().getCellLayoutPaddingLeftRightPx();
        int paddingBottom = mLauncher.getDeviceProfile().getCellLayoutBottomPaddingPx();
        newScreen.setPadding(paddingLeftRight, 0, paddingLeftRight, paddingBottom);
        newScreen.setRowCount(mLauncher.getDeviceProfile().getInv().getNumRows());
        newScreen.setColumnCount(mLauncher.getDeviceProfile().getInv().getNumColumns());

        mWorkspaceScreens.put(screenId, newScreen);
        mScreenOrder.add(insertIndex, screenId);
        addView(newScreen, insertIndex);
        /*mStateTransitionAnimation.applyChildState(
            mLauncher.getStateManager().getState(), newScreen, insertIndex);*/

        return newScreen;
    }

    public void addExtraEmptyScreenOnDrag() {
        boolean lastChildOnScreen = false;
        boolean childOnFinalScreen = false;

        // Cancel any pending removal of empty screen
        mRemoveEmptyScreenRunnable = null;

        if (mDragSourceInternal != null) {
            if (mDragSourceInternal.getChildCount() == 1) {
                lastChildOnScreen = true;
            }
            CellLayout cl = (CellLayout) mDragSourceInternal;
            if (indexOfChild(cl) == getChildCount() - 1) {
                childOnFinalScreen = true;
            }
        }

        // If this is the last item on the final screen
        if (lastChildOnScreen && childOnFinalScreen) {
            return;
        }
        if (!mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)) {
            insertNewWorkspaceScreen(EXTRA_EMPTY_SCREEN_ID);
        }
    }

    public boolean addExtraEmptyScreen() {
        if (!mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)) {
            insertNewWorkspaceScreen(EXTRA_EMPTY_SCREEN_ID);
            return true;
        }
        return false;
    }

    private void convertFinalScreenToEmptyScreenIfNecessary() {
        if (mLauncher.isWorkspaceLoading()) {
            // Invalid and dangerous operation if workspace is loading
            return;
        }

        if (hasExtraEmptyScreen() || mScreenOrder.size() == 0) return;
        long finalScreenId = mScreenOrder.get(mScreenOrder.size() - 1);

        CellLayout finalScreen = mWorkspaceScreens.get(finalScreenId);

        // If the final screen is empty, convert it to the extra empty screen
        if (finalScreen.getChildCount() == 0) {
            mWorkspaceScreens.remove(finalScreenId);
            mScreenOrder.remove(finalScreenId);

            // if this is the last screen, convert it to the empty screen
            mWorkspaceScreens.put(EXTRA_EMPTY_SCREEN_ID, finalScreen);
            mScreenOrder.add(EXTRA_EMPTY_SCREEN_ID);

            // Update the model if we have changed any screens
            //TODO: LauncherModel.updateWorkspaceScreenOrder(mLauncher, mScreenOrder);
        }
    }

    public void removeExtraEmptyScreen(final boolean animate, boolean stripEmptyScreens) {
        removeExtraEmptyScreenDelayed(animate, null, 0, stripEmptyScreens);
    }

    public void removeExtraEmptyScreenDelayed(
        final boolean animate, final Runnable onComplete,
        final int delay, final boolean stripEmptyScreens
    ) {
        if (mLauncher.isWorkspaceLoading()) {
            // Don't strip empty screens if the workspace is still loading
            return;
        }

        if (delay > 0) {
            postDelayed(() -> removeExtraEmptyScreenDelayed(
                animate,
                onComplete,
                0,
                stripEmptyScreens
            ), delay);
            return;
        }

        convertFinalScreenToEmptyScreenIfNecessary();
        if (hasExtraEmptyScreen()) {
            int emptyIndex = mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID);
            if (getNextPage() == emptyIndex) {
                snapToPage(getNextPage() - 1, SNAP_OFF_EMPTY_SCREEN_DURATION);
                fadeAndRemoveEmptyScreen(SNAP_OFF_EMPTY_SCREEN_DURATION, FADE_EMPTY_SCREEN_DURATION,
                    onComplete, stripEmptyScreens
                );
            } else {
                snapToPage(getNextPage(), 0);
                fadeAndRemoveEmptyScreen(0, FADE_EMPTY_SCREEN_DURATION,
                    onComplete, stripEmptyScreens
                );
            }
            return;
        } else if (stripEmptyScreens) {
            // If we're not going to strip the empty screens after removing
            // the extra empty screen, do it right away.
            stripEmptyScreens();
        }

        if (onComplete != null) {
            onComplete.run();
        }
    }

    private void fadeAndRemoveEmptyScreen(
        int delay, int duration, final Runnable onComplete,
        final boolean stripEmptyScreens
    ) {
        // XXX: Do we need to update LM workspace screens below?
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f);
        PropertyValuesHolder bgAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", 0f);

        final GridLayout cl = mWorkspaceScreens.get(EXTRA_EMPTY_SCREEN_ID);

        mRemoveEmptyScreenRunnable = () -> {
            if (hasExtraEmptyScreen()) {
                mWorkspaceScreens.remove(EXTRA_EMPTY_SCREEN_ID);
                mScreenOrder.remove(EXTRA_EMPTY_SCREEN_ID);
                removeView(cl);
                if (stripEmptyScreens) {
                    stripEmptyScreens();
                }
                // Update the page indicator to reflect the removed page.
                //TODO: showPageIndicatorAtCurrentScroll();
            }
        };

        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(cl, alpha, bgAlpha);
        oa.setDuration(duration);
        oa.setStartDelay(delay);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mRemoveEmptyScreenRunnable != null) {
                    mRemoveEmptyScreenRunnable.run();
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        oa.start();
    }

    public boolean hasExtraEmptyScreen() {
        return mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID) && getChildCount() > 1;
    }

    public long commitExtraEmptyScreen() {
        if (mLauncher.isWorkspaceLoading()) {
            // Invalid and dangerous operation if workspace is loading
            return -1;
        }

        CellLayout cl = mWorkspaceScreens.get(EXTRA_EMPTY_SCREEN_ID);
        mWorkspaceScreens.remove(EXTRA_EMPTY_SCREEN_ID);
        mScreenOrder.remove(EXTRA_EMPTY_SCREEN_ID);

        long newId = mScreenOrder.size();
        mWorkspaceScreens.put(newId, cl);
        mScreenOrder.add(newId);

        // Update the model for the new screen
        //TODO: LauncherModel.updateWorkspaceScreenOrder(mLauncher, mScreenOrder);

        return newId;
    }

    public CellLayout getScreenWithId(long screenId) {
        return mWorkspaceScreens.get(screenId);
    }

    public long getIdForScreen(CellLayout layout) {
        int index = mWorkspaceScreens.indexOfValue(layout);
        if (index != -1) {
            return mWorkspaceScreens.keyAt(index);
        }
        return -1;
    }

    /**
     * Converts an AppItem into a View object that can be rendered inside
     * the pages and the mDock.
     * <p>
     * The View object also has all the required listeners attached to it.
     */
    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private BlissFrameLayout prepareLauncherItem(final LauncherItem launcherItem) {
        final BlissFrameLayout iconView = (BlissFrameLayout) mLauncher.getLayoutInflater().inflate(
            R.layout.app_view,
            null
        );

        iconView.setLauncherItem(launcherItem);
        final SquareFrameLayout icon = iconView.findViewById(R.id.app_icon);
        if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
            iconView.applyBadge(
                false,
                launcherItem.container != Constants.CONTAINER_HOTSEAT
            );
        } else if (launcherItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
            ApplicationItem applicationItem = (ApplicationItem) launcherItem;
            iconView.applyBadge(
                false,
                launcherItem.container != Constants.CONTAINER_HOTSEAT
            );
        }

        /*icon.setOnLongClickListener(view -> {
            handleWobbling(true);
            longPressed = true;
            return true;
        });

        icon.setOnTouchListener(new View.OnTouchListener() {
            long iconPressedAt = 0;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (!mLongClickStartsDrag) {
                        iconPressedAt = System.currentTimeMillis();
                    }
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    if (longPressed || (!mLongClickStartsDrag
                        && (System.currentTimeMillis() - iconPressedAt) > 150)) {
                        longPressed = false;
                        movingApp = iconView;
                        dragShadowBuilder = new BlissDragShadowBuilder(
                            icon, (event.getX() < 0 ? 0 : event.getX()),
                            (event.getY() < 0 ? 0 : event.getY())
                        );
                        icon.startDrag(null, dragShadowBuilder, iconView, 0);
                        if (iconView.getParent().getParent() instanceof HorizontalPager) {
                            parentPage = getCurrentAppsPageNumber();
                        } else {
                            parentPage = -99;
                        }
                        iconView.clearAnimation();
                        movingApp.setVisibility(View.INVISIBLE);
                        dragDropEnabled = true;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (movingApp != null && movingApp.getVisibility() != VISIBLE) {
                            movingApp.setVisibility(VISIBLE);
                            movingApp.invalidate();
                        }
                    });
                    return false;
                }
                return false;
            }
        });

        icon.setOnClickListener(view -> {
            if (isWobbling) {
                handleWobbling(false);
                return;
            }

            if (launcherItem.itemType != Constants.ITEM_TYPE_FOLDER) {
                startActivitySafely(getApplicationContext(), launcherItem, view);
            } else {
                folderFromDock = !(iconView.getParent().getParent() instanceof HorizontalPager);
                displayFolder((FolderItem) launcherItem, iconView);
            }
        });*/

        return iconView;
    }

    public int getPageIndexForScreenId(long screenId) {
        return indexOfChild(mWorkspaceScreens.get(screenId));
    }

    public long getScreenIdForPageIndex(int index) {
        if (0 <= index && index < mScreenOrder.size()) {
            return mScreenOrder.get(index);
        }
        return -1;
    }

    public ArrayList<Long> getScreenOrder() {
        return mScreenOrder;
    }

    public void stripEmptyScreens() {
        if (mLauncher.isWorkspaceLoading()) {
            // Don't strip empty screens if the workspace is still loading.
            // This is dangerous and can result in data loss.
            return;
        }

        if (isPageInTransition()) {
            mStripScreensOnPageStopMoving = true;
            return;
        }

        int currentPage = getNextPage();
        ArrayList<Long> removeScreens = new ArrayList<>();
        int total = mWorkspaceScreens.size();
        for (int i = 0; i < total; i++) {
            long id = mWorkspaceScreens.keyAt(i);
            GridLayout cl = mWorkspaceScreens.valueAt(i);
            removeScreens.add(id);
        }

        // We enforce at least one page to add new items to. In the case that we remove the last
        // such screen, we convert the last screen to the empty screen
        int minScreens = 1;

        int pageShift = 0;
        for (Long id : removeScreens) {
            CellLayout cl = mWorkspaceScreens.get(id);
            mWorkspaceScreens.remove(id);
            mScreenOrder.remove(id);

            if (getChildCount() > minScreens) {
                if (indexOfChild(cl) < currentPage) {
                    pageShift++;
                }

                removeView(cl);
            } else {
                // if this is the last screen, convert it to the empty screen
                mRemoveEmptyScreenRunnable = null;
                mWorkspaceScreens.put(EXTRA_EMPTY_SCREEN_ID, cl);
                mScreenOrder.add(EXTRA_EMPTY_SCREEN_ID);
            }
        }

        if (!removeScreens.isEmpty()) {
            // Update the model if we have changed any screens
            //TODO: LauncherModel.updateWorkspaceScreenOrder(mLauncher, mScreenOrder);
        }

        if (pageShift >= 0) {
            setCurrentPage(currentPage - pageShift);
        }
    }

    public void addInScreenFromBind(View child, LauncherItem info) {
        mWorkspaceScreens.get(info.screenId).addView(child);
    }

    /**
     * Called directly from a CellLayout (not by the framework), after we've been added as a
     * listener via setOnInterceptTouchEventListener(). This allows us to tell the CellLayout
     * that it should intercept touch events, which is not something that is normally supported.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //return shouldConsumeTouch(v);
        return true;
    }

    public boolean isSwitchingState() {
        return false;
    }

    /**
     * This differs from isSwitchingState in that we take into account how far the transition
     * has completed.
     */
    /*public boolean isFinishedSwitchingState() {
        return !mIsSwitchingState
            || (mTransitionProgress > FINISHED_SWITCHING_STATE_TRANSITION_PROGRESS);
    }*/
    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mXDown = ev.getX();
            mYDown = ev.getY();
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev) {
        float deltaX = ev.getX() - mXDown;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(ev.getY() - mYDown);

        if (Float.compare(absDeltaX, 0f) == 0) return;

        float slope = absDeltaY / absDeltaX;
        float theta = (float) Math.atan(slope);

        if (absDeltaX > mTouchSlop || absDeltaY > mTouchSlop) {
            cancelCurrentPageLongPress();
        }

        if (theta > MAX_SWIPE_ANGLE) {
            // Above MAX_SWIPE_ANGLE, we don't want to ever start scrolling the workspace
            return;
        } else if (theta > START_DAMPING_TOUCH_SLOP_ANGLE) {
            // Above START_DAMPING_TOUCH_SLOP_ANGLE and below MAX_SWIPE_ANGLE, we want to
            // increase the touch slop to make it harder to begin scrolling the workspace. This
            // results in vertically scrolling widgets to more easily. The higher the angle, the
            // more we increase touch slop.
            theta -= START_DAMPING_TOUCH_SLOP_ANGLE;
            float extraRatio = (float)
                Math.sqrt((theta / (MAX_SWIPE_ANGLE - START_DAMPING_TOUCH_SLOP_ANGLE)));
            super.determineScrollingStart(ev, 1 + TOUCH_SLOP_DAMPING_FACTOR * extraRatio);
        } else {
            // Below START_DAMPING_TOUCH_SLOP_ANGLE, we don't do anything special
            super.determineScrollingStart(ev);
        }
    }

    protected void onPageBeginTransition() {
        super.onPageBeginTransition();
        updateChildrenLayersEnabled();
    }

    protected void onPageEndTransition() {
        super.onPageEndTransition();
        updateChildrenLayersEnabled();

        //TODO:
        /*if (mDragController.isDragging()) {
            if (workspaceInModalState()) {
                // If we are in springloaded mode, then force an event to check if the current touch
                // is under a new page (to scroll to)
                mDragController.forceTouchMove();
            }
        }*/

        if (mStripScreensOnPageStopMoving) {
            stripEmptyScreens();
            mStripScreensOnPageStopMoving = false;
        }
    }

    void moveToDefaultScreen() {
        int page = DEFAULT_PAGE;
        if (getNextPage() != page) {
            snapToPage(page);
        }
        View child = getChildAt(page);
        if (child != null) {
            child.requestFocus();
        }
    }

    @Override
    public int getExpectedHeight() {
        return getMeasuredHeight() <= 0 || !mIsLayoutValid
            ? mLauncher.getDeviceProfile().getHeightPx() : getMeasuredHeight();
    }

    @Override
    public int getExpectedWidth() {
        return getMeasuredWidth() <= 0 || !mIsLayoutValid
            ? mLauncher.getDeviceProfile().getWidthPx() : getMeasuredWidth();
    }

    private void updateChildrenLayersEnabled() {
        boolean enableChildrenLayers = isPageInTransition();

        if (enableChildrenLayers != mChildrenLayersEnabled) {
            mChildrenLayersEnabled = enableChildrenLayers;
            if (mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
            } else {
                for (int i = 0; i < getPageCount(); i++) {
                    final GridLayout grid = (GridLayout) getChildAt(i);
                    grid.setLayerType(LAYER_TYPE_NONE, sPaint);
                }
            }
        }
    }

    private void enableHwLayersOnVisiblePages() {
        if (mChildrenLayersEnabled) {
            final int screenCount = getChildCount();

            final int[] visibleScreens = getVisibleChildrenRange();
            int leftScreen = visibleScreens[0];
            int rightScreen = visibleScreens[1];

            if (leftScreen == rightScreen) {
                // make sure we're caching at least two pages always
                if (rightScreen < screenCount - 1) {
                    rightScreen++;
                } else if (leftScreen > 0) {
                    leftScreen--;
                }
            }

            for (int i = 0; i < screenCount; i++) {
                final GridLayout layout = (GridLayout) getPageAt(i);
                // enable layers between left and right screen inclusive.
                boolean enableLayer = leftScreen <= i && i <= rightScreen;
                layout.setLayerType(enableLayer ? LAYER_TYPE_HARDWARE : LAYER_TYPE_NONE, sPaint);
            }
        }
    }

    public void setup(@NotNull DragController dragController) {
        mSpringLoadedDragController = new SpringLoadedDragController(mLauncher);
        mDragController = dragController;

        // hardware layers on children are enabled on startup, but should be disabled until
        // needed
        updateChildrenLayersEnabled();
    }

    @Override
    public void onDragStart(
        DragObject dragObject, DragOptions options
    ) {
        if (mDragInfo != null && mDragInfo.getCell() != null) {
            CellLayout layout = (CellLayout) mDragInfo.getCell().getParent();
            //TODO: Mark the current cell as unoccupied.
            // layout.markCellsAsUnoccupiedForView(mDragInfo.getCell());
        }

        if (mOutlineProvider != null) {
            if (dragObject.dragView != null) {
                Bitmap preview = dragObject.dragView.getPreviewBitmap();

                // The outline is used to visualize where the item will land if dropped
                mOutlineProvider.generateDragOutline(preview);
            }
        }

        updateChildrenLayersEnabled();

        // Do not add a new page if it is a accessible drag which was not started by the workspace.
        // We do not support accessibility drag from other sources and instead provide a direct
        // action for move/add to homescreen.
        // When a accessible drag is started by the folder, we only allow rearranging withing the
        // folder.
        // boolean addNewPage = !(options.isAccessibleDrag && dragObject.dragSource != this);
        boolean addNewPage = true;

        if (addNewPage) {
            mDeferRemoveExtraEmptyScreen = false;
            addExtraEmptyScreenOnDrag();

            // [BlissLauncher] We don't need this as we don't support widgets on workspace grid.
            /*if (dragObject.dragInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                && dragObject.dragSource != this) {
                // When dragging a widget from different source, move to a page which has
                // enough space to place this widget (after rearranging/resizing). We special case
                // widgets as they cannot be placed inside a folder.
                // Start at the current page and search right (on LTR) until finding a page with
                // enough space. Since an empty screen is the furthest right, a page must be found.
                int currentPage = getPageNearestToCenterOfScreen();
                for (int pageIndex = currentPage; pageIndex < getPageCount(); pageIndex++) {
                    CellLayout page = (CellLayout) getPageAt(pageIndex);
                    if (page.hasReorderSolution(dragObject.dragInfo)) {
                        setCurrentPage(pageIndex);
                        break;
                    }
                }
            }*/
        }

    }

    @Override
    public void onDragEnd() {
        if (!mDeferRemoveExtraEmptyScreen) {
            removeExtraEmptyScreen(true, mDragSourceInternal != null);
        }

        updateChildrenLayersEnabled();
        mDragInfo = null;
        mOutlineProvider = null;
        mDragSourceInternal = null;
    }

    /**
     * Called at the end of a drag which originated on the workspace.
     */
    public void onDropCompleted(
        View target, DragObject d, boolean success
    ) {
        if (success) {
            if (target != this && mDragInfo != null) {
                removeWorkspaceItem(mDragInfo.getCell());
            }
        } else if (mDragInfo != null) {
            final CellLayout cellLayout = mLauncher.getCellLayout(
                mDragInfo.getContainer(), mDragInfo.getScreenId());
            if (cellLayout != null) {
                cellLayout.onDropChild(mDragInfo.getCell());
            } else {
                // TODO: Don't throw this exception in release build.
                throw new RuntimeException("Invalid state: cellLayout == null in "
                    + "Workspace#onDropCompleted. Please file a bug. ");
            }
        }
        mDragInfo = null;
    }

    /**
     * For opposite operation. See {@link #addInScreen}.
     */
    public void removeWorkspaceItem(View v) {
        CellLayout parentCell = getParentCellLayoutForView(v);
        if (parentCell != null) {
            parentCell.removeView(v);
        } else {
            // When an app is uninstalled using the drop target, we wait until resume to remove
            // the icon. We also remove all the corresponding items from the workspace at
            // {@link Launcher#bindComponentsRemoved}. That call can come before or after
            // {@link Launcher#mOnResumeCallbacks} depending on how busy the worker thread is.
            Log.e(TAG, "mDragInfo.cell has null parent");
        }
        if (v instanceof DropTarget) {
            mDragController.removeDropTarget((DropTarget) v);
        }
    }

    /**
     * Returns a specific CellLayout
     */
    CellLayout getParentCellLayoutForView(View v) {
        ArrayList<CellLayout> layouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layout : layouts) {
            if (layout.indexOfChild(v) > -1) {
                return layout;
            }
        }
        return null;
    }

    /**
     * Returns a list of all the CellLayouts in the workspace.
     */
    ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
        ArrayList<CellLayout> layouts = new ArrayList<>();
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            layouts.add(((CellLayout) getChildAt(screen)));
        }
        if (mLauncher.getHotseat() != null) {
            layouts.add(mLauncher.getHotseat().getLayout());
        }
        return layouts;
    }

    @Override
    public boolean isDropEnabled() {
        Log.d(TAG, "isDropEnabled() called");
        return true;
    }

    @Override
    public void onDrop(
        DragObject dragObject, DragOptions options
    ) {
        Log.d(
            TAG,
            "onDrop() called with: dragObject = [" + dragObject + "], options = [" + options + "]"
        );
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
        mCreateUserFolderOnDrop = false;
        mAddToExistingFolderOnDrop = false;

        mDropToLayout = null;
        mDragViewVisualCenter = dragObject.getVisualCenter(mDragViewVisualCenter);
        setDropLayoutForDragObject(dragObject, mDragViewVisualCenter[0], mDragViewVisualCenter[1]);
    }

    /**
     * Updates {@link #mDragTargetLayout} and {@link #mDragOverlappingLayout}
     * based on the DragObject's position.
     *
     * The layout will be:
     * - The Hotseat if the drag object is over it
     * - A side page if we are in spring-loaded mode and the drag object is over it
     * - The current page otherwise
     *
     * @return whether the layout is different from the current {@link #mDragTargetLayout}.
     */
    private boolean setDropLayoutForDragObject(DragObject d, float centerX, float centerY) {
        CellLayout layout = null;
        // Test to see if we are over the hotseat first
        if (mLauncher.getHotseat() != null) {
            if (isPointInSelfOverHotseat(d.x, d.y)) {
                layout = mLauncher.getHotseat().getLayout();
            }
        }

        int nextPage = getNextPage();
        if (layout == null && !isPageInTransition()) {
            // Check if the item is dragged over left page
            mTempTouchCoordinates[0] = Math.min(centerX, d.x);
            mTempTouchCoordinates[1] = d.y;
            layout = verifyInsidePage(nextPage + (mIsRtl ? 1 : -1), mTempTouchCoordinates);
        }

        if (layout == null && !isPageInTransition()) {
            // Check if the item is dragged over right page
            mTempTouchCoordinates[0] = Math.max(centerX, d.x);
            mTempTouchCoordinates[1] = d.y;
            layout = verifyInsidePage(nextPage + (mIsRtl ? -1 : 1), mTempTouchCoordinates);
        }

        // Always pick the current page.
        if (layout == null && nextPage >= 0 && nextPage < getPageCount()) {
            layout = (CellLayout) getChildAt(nextPage);
        }
        if (layout != mDragTargetLayout) {
            setCurrentDropLayout(layout);
            setCurrentDragOverlappingLayout(layout);
            return true;
        }
        return false;
    }

    void setCurrentDropLayout(CellLayout layout) {
        if (mDragTargetLayout != null) {
            mDragTargetLayout.revertTempState();
            mDragTargetLayout.onDragExit();
        }
        mDragTargetLayout = layout;
        if (mDragTargetLayout != null) {
            mDragTargetLayout.onDragEnter();
        }
        cleanupReorder(true);
        cleanupFolderCreation();
        setCurrentDropOverCell(-1, -1);
    }

    void setCurrentDragOverlappingLayout(CellLayout layout) {
        if (mDragOverlappingLayout != null) {
            mDragOverlappingLayout.setIsDragOverlapping(false);
        }
        mDragOverlappingLayout = layout;
        if (mDragOverlappingLayout != null) {
            mDragOverlappingLayout.setIsDragOverlapping(true);
        }
        // Invalidating the scrim will also force this CellLayout
        // to be invalidated so that it is highlighted if necessary.
        //mLauncher.getDragLayer().getScrim().invalidate();
    }

    public CellLayout getCurrentDragOverlappingLayout() {
        return mDragOverlappingLayout;
    }

    void setCurrentDropOverCell(int x, int y) {
        if (x != mDragOverX || y != mDragOverY) {
            mDragOverX = x;
            mDragOverY = y;
            setDragMode(DRAG_MODE_NONE);
        }
    }

    void setDragMode(int dragMode) {
        if (dragMode != mDragMode) {
            if (dragMode == DRAG_MODE_NONE) {
                cleanupAddToFolder();
                // We don't want to cancel the re-order alarm every time the target cell changes
                // as this feels to slow / unresponsive.
                cleanupReorder(false);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_ADD_TO_FOLDER) {
                cleanupReorder(true);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_CREATE_FOLDER) {
                cleanupAddToFolder();
                cleanupReorder(true);
            } else if (dragMode == DRAG_MODE_REORDER) {
                cleanupAddToFolder();
                cleanupFolderCreation();
            }
            mDragMode = dragMode;
        }
    }

    private void cleanupFolderCreation() {
        // TODO: Enable when supporting folder creation
        /*if (mFolderCreateBg != null) {
            mFolderCreateBg.animateToRest();
        }*/
        mFolderCreationAlarm.setOnAlarmListener(null);
        mFolderCreationAlarm.cancelAlarm();
    }

    // Enable when supporting folder
    private void cleanupAddToFolder() {
        /*if (mDragOverFolderIcon != null) {
            mDragOverFolderIcon.onDragExit();
            mDragOverFolderIcon = null;
        }*/
    }

    private void cleanupReorder(boolean cancelAlarm) {
        // Any pending reorders are canceled
        if (cancelAlarm) {
            mReorderAlarm.cancelAlarm();
        }
        mLastReorderX = -1;
        mLastReorderY = -1;
    }

    /**
     * Returns the child CellLayout if the point is inside the page coordinates, null otherwise.
     */
    private CellLayout verifyInsidePage(int pageNo, float[] touchXy)  {
        if (pageNo >= 0 && pageNo < getPageCount()) {
            CellLayout cl = (CellLayout) getChildAt(pageNo);
            mapPointFromSelfToChild(cl, touchXy);
            if (touchXy[0] >= 0 && touchXy[0] <= cl.getWidth() &&
                touchXy[1] >= 0 && touchXy[1] <= cl.getHeight()) {
                // This point is inside the cell layout
                return cl;
            }
        }
        return null;
    }

    /*
     *
     * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
     * coordinate space. The argument xy is modified with the return result.
     */
    void mapPointFromSelfToChild(View v, float[] xy) {
        xy[0] = xy[0] - v.getLeft();
        xy[1] = xy[1] - v.getTop();
    }
    boolean isPointInSelfOverHotseat(int x, int y) {
        mTempXY[0] = x;
        mTempXY[1] = y;
        mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, mTempXY, true);
        View hotseat = mLauncher.getHotseat();
        return mTempXY[0] >= hotseat.getLeft() &&
            mTempXY[0] <= hotseat.getRight() &&
            mTempXY[1] >= hotseat.getTop() &&
            mTempXY[1] <= hotseat.getBottom();
    }

    void mapPointFromSelfToHotseatLayout(Hotseat hotseat, float[] xy) {
        mTempXY[0] = (int) xy[0];
        mTempXY[1] = (int) xy[1];
        mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, mTempXY, true);
        mLauncher.getDragLayer().mapCoordInSelfToDescendant(hotseat.getLayout(), mTempXY);

        xy[0] = mTempXY[0];
        xy[1] = mTempXY[1];
    }

    @Override
    public void onDragOver(DragObject d) {
        Log.d(TAG, "onDragOver() called with: dragObject = [" + d + "]");

        LauncherItem item = d.dragInfo;
        if (item == null) {
            throw new NullPointerException("DragObject has null info");
        }
        // Ensure that we have proper spans for the item that we are dropping
        if (item.cell < 0) throw new RuntimeException("Improper spans found");
        mDragViewVisualCenter = d.getVisualCenter(mDragViewVisualCenter);

        final View child = (mDragInfo == null) ? null : mDragInfo.getCell();
        if (setDropLayoutForDragObject(d, mDragViewVisualCenter[0], mDragViewVisualCenter[1])) {
            if (mLauncher.isHotseatLayout(mDragTargetLayout)) {
                mSpringLoadedDragController.cancel();
            } else {
                mSpringLoadedDragController.setAlarm(mDragTargetLayout);
            }
        }

        // Handle the drag over
        if (mDragTargetLayout != null) {
            // We want the point to be mapped to the dragTarget.
            if (mLauncher.isHotseatLayout(mDragTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(mDragTargetLayout, mDragViewVisualCenter);
            }

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1],
                mDragTargetLayout, mTargetCell);
            int reorderX = mTargetCell[0];
            int reorderY = mTargetCell[1];

            setCurrentDropOverCell(mTargetCell[0], mTargetCell[1]);

            float targetCellDistance = mDragTargetLayout.getDistanceFromCell(
                mDragViewVisualCenter[0], mDragViewVisualCenter[1], mTargetCell);

            //TODO: Enable when supporting foler
            //manageFolderFeedback(mDragTargetLayout, mTargetCell, targetCellDistance, d);

            boolean nearestDropOccupied = mDragTargetLayout.isNearestDropLocationOccupied((int)
                    mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], child, mTargetCell);

            if (!nearestDropOccupied) {
                mDragTargetLayout.visualizeDropLocation(child, mOutlineProvider,
                    mTargetCell[0], mTargetCell[1], false, d);
            } else if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
                && !mReorderAlarm.alarmPending() && (mLastReorderX != reorderX ||
                mLastReorderY != reorderY)) {

                int[] resultSpan = new int[2];
                mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], 1, 1, 1, 1,
                    child, mTargetCell, resultSpan, CellLayout.MODE_SHOW_REORDER_HINT);

                // Otherwise, if we aren't adding to or creating a folder and there's no pending
                // reorder, then we schedule a reorder
                ReorderAlarmListener listener = new ReorderAlarmListener(mDragViewVisualCenter, d, child);
                mReorderAlarm.setOnAlarmListener(listener);
                mReorderAlarm.setAlarm(REORDER_TIMEOUT);
            }

            if (mDragMode == DRAG_MODE_CREATE_FOLDER || mDragMode == DRAG_MODE_ADD_TO_FOLDER ||
                !nearestDropOccupied) {
                if (mDragTargetLayout != null) {
                    mDragTargetLayout.revertTempState();
                }
            }
        }
    }

    @Override
    public void onDragExit(DragObject dragObject) {
        Log.d(TAG, "onDragExit() called with: dragObject = [" + dragObject + "]");
    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        Log.d(TAG, "acceptDrop() called with: dragObject = [" + dragObject + "]");
        return false;
    }

    @Override
    public void prepareAccessibilityDrop() {
        Log.d(TAG, "prepareAccessibilityDrop() called");
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        // We want the workspace to have the whole area of the display (it will find the correct
        // cell layout to drop to in the existing drag/drop logic.
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, outRect);
    }

    public void startDrag(CellLayout.CellInfo cellInfo, DragOptions dragOptions) {
        Log.d(
            TAG,
            "startDrag() called with: longClickCellInfo = [" + cellInfo + "], dragOptions = [" + dragOptions + "]"
        );
        View child = cellInfo.getCell();
        mDragInfo = cellInfo;
        child.setVisibility(GONE);
        beginDragShared(child, this, dragOptions);
    }

    public void beginDragShared(View child, DragSource source, DragOptions options) {
        Object dragObject = child.getTag();
        if(!(dragObject instanceof LauncherItem)) {
            String msg = "Drag started with a view that has no tag set. This "
                + "will cause a crash (issue 11627249) down the line. "
                + "View: " + child + "  tag: " + child.getTag();
            throw new IllegalStateException(msg);
        }

        beginDragShared(child, source, (LauncherItem) dragObject, new DragPreviewProvider(child), options);
    }

    private DragView beginDragShared(
        View child,
        DragSource source,
        LauncherItem dragObject,
        DragPreviewProvider previewProvider,
        DragOptions dragOptions
    ) {
        float iconScale = 1f;
        if (child instanceof  IconTextView) {
            Drawable icon = ((IconTextView) child).getIcon();
        }

        child.clearFocus();
        child.setPressed(false);
        mOutlineProvider = previewProvider;

        // The drag bitmap follows the touch point around on the screen
        final Bitmap b = previewProvider.createDragBitmap();
        int halfPadding = previewProvider.previewPadding / 2;

        float scale = previewProvider.getScaleAndPosition(b, mTempXY);
        int dragLayerX = mTempXY[0];
        int dragLayerY = mTempXY[1];

        VariantDeviceProfile grid = mLauncher.getDeviceProfile();
        Point dragVisualizeOffset = null;
        Rect dragRect = null;
        if (child instanceof IconTextView) {
            dragRect =
            ((IconTextView) child).getIconBounds();
            dragLayerY += dragRect.top;
            // Note: The dragRect is used to calculate drag layer offsets, but the
            // dragVisualizeOffset in addition to the dragRect (the size) to position the outline.
            dragVisualizeOffset = new Point(- halfPadding, halfPadding);
        }

        // Clear the pressed state if necessary
        if (child instanceof IconTextView) {
            IconTextView icon = (IconTextView) child;
            icon.clearPressedBackground();
        }

        if (child.getParent() instanceof CellLayout) {
            mDragSourceInternal = (CellLayout) child.getParent();
        }

        DragView dv = mDragController.startDrag(b, dragLayerX, dragLayerY, source,
            dragObject, dragVisualizeOffset, dragRect, scale * iconScale, scale, dragOptions);
        dv.setIntrinsicIconScaleFactor(dragOptions.intrinsicIconScaleFactor);
        return dv;
    }

    /**
     * Calculate the nearest cell where the given object would be dropped.
     *
     * pixelX and pixelY should be in the coordinate system of layout
     */
    int[] findNearestArea(int pixelX, int pixelY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(
            pixelX, pixelY,recycle);
    }

    class ReorderAlarmListener implements OnAlarmListener {
        final float[] dragViewCenter;
        final DragObject dragObject;
        final View child;

        public ReorderAlarmListener(float[] dragViewCenter, DragObject dragObject, View child) {
            this.dragViewCenter = dragViewCenter;
            this.child = child;
            this.dragObject = dragObject;
        }

        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1],mDragTargetLayout,
                mTargetCell);
            mLastReorderX = mTargetCell[0];
            mLastReorderY = mTargetCell[1];

            mTargetCell = mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], 1, 1, 1, 1,
                child, mTargetCell, resultSpan, CellLayout.MODE_DRAG_OVER);

            if (mTargetCell[0] < 0 || mTargetCell[1] < 0) {
                mDragTargetLayout.revertTempState();
            } else {
                setDragMode(DRAG_MODE_REORDER);
            }
            mDragTargetLayout.visualizeDropLocation(child, mOutlineProvider,
                mTargetCell[0], mTargetCell[1], false, dragObject);
        }
    }

}
