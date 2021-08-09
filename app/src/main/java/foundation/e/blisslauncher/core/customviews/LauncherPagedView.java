package foundation.e.blisslauncher.core.customviews;

import static foundation.e.blisslauncher.core.utils.Constants.ITEM_TYPE_APPLICATION;
import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.SPRING_LOADED_TRANSITION_MS;
import static foundation.e.blisslauncher.features.test.dragndrop.DragLayer.ALPHA_INDEX_OVERLAY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.Toast;
import foundation.e.blisslauncher.BuildConfig;
import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.customviews.pageindicators.PageIndicatorDots;
import foundation.e.blisslauncher.core.database.model.ApplicationItem;
import foundation.e.blisslauncher.core.database.model.FolderItem;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.core.touch.ItemClickHandler;
import foundation.e.blisslauncher.core.touch.ItemLongClickListener;
import foundation.e.blisslauncher.core.utils.Constants;
import foundation.e.blisslauncher.core.utils.GraphicsUtil;
import foundation.e.blisslauncher.core.utils.LongArrayMap;
import foundation.e.blisslauncher.core.utils.PackageUserKey;
import foundation.e.blisslauncher.features.launcher.Hotseat;
import foundation.e.blisslauncher.features.notification.FolderDotInfo;
import foundation.e.blisslauncher.features.test.Alarm;
import foundation.e.blisslauncher.features.test.CellLayout;
import foundation.e.blisslauncher.features.test.IconTextView;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.OnAlarmListener;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.WorkspaceStateTransitionAnimation;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;
import foundation.e.blisslauncher.features.test.anim.Interpolators;
import foundation.e.blisslauncher.features.test.dragndrop.DragController;
import foundation.e.blisslauncher.features.test.dragndrop.DragOptions;
import foundation.e.blisslauncher.features.test.dragndrop.DragSource;
import foundation.e.blisslauncher.features.test.dragndrop.DragView;
import foundation.e.blisslauncher.features.test.dragndrop.DropTarget;
import foundation.e.blisslauncher.features.test.dragndrop.SpringLoadedDragController;
import foundation.e.blisslauncher.features.test.graphics.DragPreviewProvider;
import foundation.e.blisslauncher.features.test.uninstall.UninstallHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

public class LauncherPagedView extends PagedView<PageIndicatorDots> implements View.OnTouchListener,
    Insettable, DropTarget, DragSource, DragController.DragListener,
    LauncherStateManager.StateHandler, OnAlarmListener {

    private static final String TAG = "LauncherPagedView";
    private static final int DEFAULT_PAGE = 0;
    private static final int SNAP_OFF_EMPTY_SCREEN_DURATION = 400;
    private static final int FADE_EMPTY_SCREEN_DURATION = 150;

    /**
     * The value that {@link #mTransitionProgress} must be greater than for
     * {@link #isFinishedSwitchingState()} ()} to return true.
     */
    private static final float FINISHED_SWITCHING_STATE_TRANSITION_PROGRESS = 0.5f;

    private static final boolean MAP_NO_RECURSE = false;
    private static final boolean MAP_RECURSE = true;

    // The screen id used for the empty screen always present to the right.
    public static final long EXTRA_EMPTY_SCREEN_ID = -201;
    // The is the first screen. It is always present, even if its empty.
    public static final long FIRST_SCREEN_ID = 0;
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;

    private final TestActivity mLauncher;

    private LayoutTransition mLayoutTransition;
    final WallpaperManager mWallpaperManager;

    public final LongArrayMap<CellLayout> mWorkspaceScreens = new LongArrayMap<>();
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
    private CellLayout mDropToLayout;
    private CellLayout mDragTargetLayout;
    private CellLayout mDragOverlappingLayout;
    /**
     * Target drop area calculated during last acceptDrop call.
     */
    int[] mTargetCell = new int[2];
    private int mDragOverX = -1;
    private int mDragOverY = -1;

    private static final int FOLDER_CREATION_TIMEOUT = 0;
    public static final int REORDER_TIMEOUT = 650;
    private final Alarm mFolderCreationAlarm = new Alarm();
    private final Alarm mReorderAlarm = new Alarm();
    //private FolderIcon mDragOverFolderIcon = null;
    private boolean mCreateUserFolderOnDrop = false;
    private boolean mAddToExistingFolderOnDrop = false;
    private float mMaxDistanceForFolderCreation;

    // Related to dragging, folder creation and reordering
    private static final int DRAG_MODE_NONE = 0;
    private static final int DRAG_MODE_CREATE_FOLDER = 1;
    private static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    private static final int DRAG_MODE_REORDER = 3;
    private int mDragMode = DRAG_MODE_NONE;
    int mLastReorderX = -1;
    int mLastReorderY = -1;
    private IconTextView parentFolderCell;
    private float mTransitionProgress;
    private boolean mIsSwitchingState;
    private boolean mForceDrawAdjacentPages;
    private WorkspaceStateTransitionAnimation mStateTransitionAnimation;

    // State related to Launcher Overlay
    TestActivity.LauncherOverlay mLauncherOverlay;
    boolean mScrollInteractionBegan;
    boolean mStartedSendingScrollEvents;
    float mLastOverlayScroll = 0;
    boolean mOverlayShown = false;
    private Runnable mOnOverlayHiddenCallback;
    // Total over scrollX in the overlay direction.
    private float mOverlayTranslation;

    private Alarm wobbleExpireAlarm = new Alarm();
    private static final int WOBBLE_EXPIRATION_TIMEOUT = 25000;


    public LauncherPagedView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public LauncherPagedView(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);

        mLauncher = TestActivity.Companion.getLauncher(context);
        mStateTransitionAnimation = new WorkspaceStateTransitionAnimation(mLauncher, this);
        mWallpaperManager = WallpaperManager.getInstance(context);
        setHapticFeedbackEnabled(false);
        initWorkspace();

        setMotionEventSplittingEnabled(true);
        setOnTouchListener((v, event) -> false);

        wobbleExpireAlarm.setOnAlarmListener(this);
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
        mMaxDistanceForFolderCreation = (0.55f * grid.getIconSizePx());
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
            IconTextView appView = (IconTextView) LayoutInflater.from(getContext())
                .inflate(R.layout.app_icon, null, false);
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
                Log.d(
                    TAG,
                    "launcherItemCell " + launcherItem.cell % mLauncher.getDeviceProfile().getInv()
                        .getNumColumns() + " " + launcherItem.cell % mLauncher.getDeviceProfile()
                        .getInv().getNumRows() + " " + launcherItem.cell
                );
                GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.LayoutParams iconLayoutParams =
                    new GridLayout.LayoutParams(rowSpec, colSpec);
                iconLayoutParams.height = mLauncher.getDeviceProfile().getCellHeightPx();
                iconLayoutParams.width = mLauncher.getDeviceProfile().getCellWidthPx();
                iconLayoutParams.setGravity(Gravity.CENTER);
                appView.setLayoutParams(iconLayoutParams);
                appView.setTextVisibility(true);
                //appView.setWithText(true);
                Log.i(TAG, "bindItems: " + appView);
                workspaceScreen.addView(appView);
            } else if (launcherItem.container == Constants.CONTAINER_HOTSEAT) {
                //appView.findViewById(R.id.app_label).setVisibility(GONE);
                GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.LayoutParams iconLayoutParams =
                    new GridLayout.LayoutParams(rowSpec, colSpec);
                iconLayoutParams.setGravity(Gravity.CENTER);
                iconLayoutParams.height = mLauncher.getDeviceProfile().getHotseatCellHeightPx();
                iconLayoutParams.width = mLauncher.getDeviceProfile().getCellWidthPx();
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
                showPageIndicatorAtCurrentScroll();
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
        } else if (launcherItem.itemType == ITEM_TYPE_APPLICATION) {
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

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child    The child to add in one of the workspace's screens.
     * @param screenId The screen in which to add the child.
     * @param x        The X position of the child in the screen's grid.
     * @param y        The Y position of the child in the screen's grid.
     */
    private void addInScreen(View child, long container, long screenId, int x, int y) {
        Log.d(
            TAG,
            "addInScreen() called with: child = [" + child + "], container = [" + container + "], screenId = [" + screenId + "], x = [" + x + "], y = [" + y + "]"
        );
        addInScreen(
            child,
            container,
            screenId,
            y * mLauncher.getDeviceProfile().getInv().getNumColumns() + x
        );
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child    The child to add in one of the workspace's screens.
     * @param screenId The screen in which to add the child.
     * @param index    The index of the child in grid.
     */
    private void addInScreen(View child, long container, long screenId, int index) {
        if (container == Constants.CONTAINER_DESKTOP) {
            if (getScreenWithId(screenId) == null) {
                Log.e(TAG, "Skipping child, screenId " + screenId + " not found");
                // DEBUGGING - Print out the stack trace to see where we are adding from
                new Throwable().printStackTrace();
                return;
            }
        }
        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            // This should never happen
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }

        final CellLayout layout;
        if (container == Constants.CONTAINER_HOTSEAT) {
            layout = mLauncher.getHotseat().getLayout();

            // Hide folder title in the hotseat
            //TODO: Enable when supporting folders
            /*if (child instanceof FolderIcon) {
                ((FolderIcon) child).setTextVisible(false);
            }*/
        } else {
            // Show folder title if not in the hotseat
            //TODO: Enable when supporting folders
            /*if (child instanceof FolderIcon) {
                ((FolderIcon) child).setTextVisible(true);
            }*/
            layout = getScreenWithId(screenId);
        }

        ViewGroup.LayoutParams genericLp = child.getLayoutParams();
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
        CellLayout.LayoutParams lp;
        if (!(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new GridLayout.LayoutParams(rowSpec, colSpec);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED);
        }
        lp.setGravity(Gravity.CENTER);

        // Get the canonical child id to uniquely represent this view in this screen
        LauncherItem info = (LauncherItem) child.getTag();
        int childId = mLauncher.getViewIdForItem(info);

        //boolean markCellsAsOccupied = !(child instanceof Folder);
        boolean markCellsAsOccupied = true;
        if (!layout.addViewToCellLayout(
            child,
            index,
            childId,
            lp,
            markCellsAsOccupied
        )) {
            // TODO: This branch occurs when the workspace is adding views
            // outside of the defined grid
            // maybe we should be deleting these items from the LauncherModel?
        }

        child.setHapticFeedbackEnabled(false);
        child.setOnLongClickListener(ItemLongClickListener.INSTANCE_WORKSPACE);
        if (child instanceof DropTarget) {
            mDragController.addDropTarget((DropTarget) child);
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
        return shouldConsumeTouch(v);
    }

    private boolean shouldConsumeTouch(View v) {
        return !workspaceIconsCanBeDragged()
            || (!workspaceInModalState() && indexOfChild(v) != mCurrentPage);
    }

    public boolean isSwitchingState() {
        return mIsSwitchingState;
    }

    private boolean workspaceInModalState() {
        return !mLauncher.isInState(NORMAL);
    }

    /**
     * Returns whether a drag should be allowed to be started from the current workspace state.
     */
    public boolean workspaceIconsCanBeDragged() {
        return mLauncher.getStateManager().getState().workspaceIconsCanBeDragged;
    }

    /**
     * This differs from isSwitchingState in that we take into account how far the transition
     * has completed.
     */
    public boolean isFinishedSwitchingState() {
        return !mIsSwitchingState
            || (mTransitionProgress > FINISHED_SWITCHING_STATE_TRANSITION_PROGRESS);
    }

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

        if (mDragController.isDragging()) {
            /*if (workspaceInModalState()) {
                // If we are in springloaded mode, then force an event to check if the current touch
                // is under a new page (to scroll to)
                mDragController.forceTouchMove();
            }*/
        }

        if (mStripScreensOnPageStopMoving) {
            stripEmptyScreens();
            mStripScreensOnPageStopMoving = false;
        }
    }

    public void moveToDefaultScreen() {
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
        boolean enableChildrenLayers = mIsSwitchingState || isPageInTransition();

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

            if (mForceDrawAdjacentPages) {
                // In overview mode, make sure that the two side pages are visible.
                leftScreen = Utilities.boundToRange(getCurrentPage() - 1, 0, rightScreen);
                rightScreen = Utilities.boundToRange(getCurrentPage() + 1,
                    leftScreen, getPageCount() - 1
                );
            }

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

    protected void onScrollInteractionBegin() {
        super.onScrollInteractionBegin();
        mScrollInteractionBegan = true;
    }

    protected void onScrollInteractionEnd() {
        super.onScrollInteractionEnd();
        mScrollInteractionBegan = false;
        if (mStartedSendingScrollEvents) {
            mStartedSendingScrollEvents = false;
            mLauncherOverlay.onScrollInteractionEnd();
        }
    }

    public void setLauncherOverlay(TestActivity.LauncherOverlay overlay) {
        mLauncherOverlay = overlay;
        // A new overlay has been set. Reset event tracking
        mStartedSendingScrollEvents = false;
        onOverlayScrollChanged(0);
    }

    private boolean isScrollingOverlay() {
        return mLauncherOverlay != null &&
            ((mIsRtl && getUnboundedScrollX() > mMaxScroll)
                || (!mIsRtl && getUnboundedScrollX() < mMinScroll));
    }

    @Override
    protected void snapToDestination() {
        // If we're overscrolling the overlay, we make sure to immediately reset the PagedView
        // to it's baseline position instead of letting the overscroll settle. The overlay handles
        // it's own settling, and every gesture to the overlay should be self-contained and start
        // from 0, so we zero it out here.
        if (isScrollingOverlay()) {
            // We reset mWasInOverscroll so that PagedView doesn't zero out the overscroll
            // interaction when we call snapToPageImmediately.
            mWasInOverscroll = false;
            snapToPageImmediately(0);
        } else {
            super.snapToDestination();
        }
    }

    @Override
    public boolean scrollLeft() {
        boolean result = false;
        if (!workspaceInModalState() && !mIsSwitchingState) {
            result = super.scrollLeft();
        }
        // TODO: Fix this asap
        /*Folder openFolder = Folder.getOpen(mLauncher);
        if (openFolder != null) {
            openFolder.completeDragExit();
        }*/
        return result;
    }

    @Override
    public boolean scrollRight() {
        boolean result = false;
        if (!workspaceInModalState() && !mIsSwitchingState) {
            result = super.scrollRight();
        }
        // TODO: Fix this asap
        /*Folder openFolder = Folder.getOpen(mLauncher);
        if (openFolder != null) {
            openFolder.completeDragExit();
        }*/
        return result;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        // Update the page indicator progress.
        boolean isTransitioning =
            mIsSwitchingState || (getLayoutTransition() != null && getLayoutTransition()
                .isRunning());
        if (!isTransitioning) {
            showPageIndicatorAtCurrentScroll();
        }

        //updatePageAlphaValues();
        enableHwLayersOnVisiblePages();
    }

    public void showPageIndicatorAtCurrentScroll() {
        if (mPageIndicator != null) {
            mPageIndicator.setScroll(getScrollX(), computeMaxScrollX());
        }
    }

    @Override
    protected void overScroll(int amount) {
        boolean shouldScrollOverlay = mLauncherOverlay != null && !mScroller.isSpringing() &&
            ((amount <= 0 && !mIsRtl) || (amount >= 0 && mIsRtl));

        boolean shouldZeroOverlay = mLauncherOverlay != null && mLastOverlayScroll != 0 &&
            ((amount >= 0 && !mIsRtl) || (amount <= 0 && mIsRtl));

        if (shouldScrollOverlay) {
            if (!mStartedSendingScrollEvents && mScrollInteractionBegan) {
                mStartedSendingScrollEvents = true;
                mLauncherOverlay.onScrollInteractionBegin();
            }

            mLastOverlayScroll = Math.abs(((float) amount) / getMeasuredWidth());
            mLauncherOverlay.onScrollChange(mLastOverlayScroll, mIsRtl);
        } else {
            dampedOverScroll(amount);
        }

        if (shouldZeroOverlay) {
            mLauncherOverlay.onScrollChange(0, mIsRtl);
        }
    }

    @Override
    protected boolean shouldFlingForVelocity(int velocityX) {
        // When the overlay is moving, the fling or settle transition is controlled by the overlay.
        return Float.compare(Math.abs(mOverlayTranslation), 0) == 0 &&
            super.shouldFlingForVelocity(velocityX);
    }

    @Override
    public void onDragStart(
        DragObject dragObject, DragOptions options
    ) {
        if (mDragInfo != null && mDragInfo.getCell() != null) {
            CellLayout layout = (CellLayout) mDragInfo.getCell().getParent();
            layout.markCellsAsUnoccupiedForView(mDragInfo.getCell());
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
        }
    }

    @Override
    public void onDragEnd() {
        if (!mDeferRemoveExtraEmptyScreen) {
            //removeExtraEmptyScreen(true, mDragSourceInternal != null);
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

    /**
     * @return false if the callback is still pending
     */
    private boolean tryRunOverlayCallback() {
        if (mOnOverlayHiddenCallback == null) {
            // Return true as no callback is pending. This is used by OnWindowFocusChangeListener
            // to remove itself if multiple focus handles were added.
            return true;
        }
        if (mOverlayShown || !hasWindowFocus()) {
            return false;
        }

        mOnOverlayHiddenCallback.run();
        mOnOverlayHiddenCallback = null;
        return true;
    }

    /**
     * Runs the given callback when the minus one overlay is hidden. Specifically, it is run
     * when launcher's window has focus and the overlay is no longer being shown. If a callback
     * is already present, the new callback will chain off it so both are run.
     *
     * @return Whether the callback was deferred.
     */
    public boolean runOnOverlayHidden(Runnable callback) {
        if (mOnOverlayHiddenCallback == null) {
            mOnOverlayHiddenCallback = callback;
        } else {
            // Chain the new callback onto the previous callback(s).
            Runnable oldCallback = mOnOverlayHiddenCallback;
            mOnOverlayHiddenCallback = () -> {
                oldCallback.run();
                callback.run();
            };
        }
        if (!tryRunOverlayCallback()) {
            ViewTreeObserver observer = getViewTreeObserver();
            if (observer != null && observer.isAlive()) {
                observer.addOnWindowFocusChangeListener(
                    new ViewTreeObserver.OnWindowFocusChangeListener() {
                        @Override
                        public void onWindowFocusChanged(boolean hasFocus) {
                            if (tryRunOverlayCallback() && observer.isAlive()) {
                                observer.removeOnWindowFocusChangeListener(this);
                            }
                        }
                    });
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDrop(DragObject d, DragOptions options) {
        mDragViewVisualCenter = d.getVisualCenter(mDragViewVisualCenter);
        CellLayout dropTargetLayout = mDropToLayout;

        // We want the point to be mapped to the dragTarget.
        if (dropTargetLayout != null) {
            if (mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter);
            }
        }

        boolean droppedOnOriginalCell = false;

        int snapScreen = -1;
        boolean resizeOnDrop = false;
        if (d.dragSource != this || mDragInfo == null) {
            final int[] touchXY = new int[]{(int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1]};
            //onDropExternal(touchXY, dropTargetLayout, d);
        } else {
            final View cell = mDragInfo.getCell();
            boolean droppedOnOriginalCellDuringTransition = false;
            Runnable onCompleteRunnable = null;

            if (dropTargetLayout != null && !d.cancelled) {
                // Move internally
                boolean hasMovedLayouts = (getParentCellLayoutForView(cell) != dropTargetLayout);
                boolean hasMovedIntoHotseat = mLauncher.isHotseatLayout(dropTargetLayout);
                long container = hasMovedIntoHotseat ?
                    Constants.CONTAINER_HOTSEAT :
                    Constants.CONTAINER_DESKTOP;
                long screenId = (mTargetCell[0] < 0) ?
                    mDragInfo.getScreenId() : getIdForScreen(dropTargetLayout);
                int spanX = 1;
                int spanY = 1;
                // First we find the cell nearest to point at which the item is
                // dropped, without any consideration to whether there is an item there.

                mTargetCell = findNearestArea((int) mDragViewVisualCenter[0], (int)
                    mDragViewVisualCenter[1], dropTargetLayout, mTargetCell);
                float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                    mDragViewVisualCenter[1], mTargetCell
                );

                // If the item being dropped is a shortcut and the nearest drop
                // cell also contains a shortcut, then create a folder with the two shortcuts.
                if (createUserFolderIfNecessary(cell, container,
                    dropTargetLayout, mTargetCell, distance, false, d.dragView
                ) ||
                    addToExistingFolderIfNecessary(cell, dropTargetLayout, mTargetCell,
                        distance, d, false
                    )) {
                    return;
                }

                // Aside from the special case where we're dropping a shortcut onto a shortcut,
                // we need to find the nearest cell location that is vacant
                LauncherItem item = d.dragInfo;
                int cellX = item.cell % mLauncher.getDeviceProfile().getInv().getNumColumns();
                int cellY = item.cell % mLauncher.getDeviceProfile().getInv().getNumRows();

                droppedOnOriginalCell = item.screenId == screenId && item.container == container
                    && cellX == mTargetCell[0] && cellY == mTargetCell[1];
                //droppedOnOriginalCellDuringTransition = droppedOnOriginalCell && mIsSwitchingState;

                // When quickly moving an item, a user may accidentally rearrange their
                // workspace. So instead we move the icon back safely to its original position.

                //TODO: may consider later
               /* boolean returnToOriginalCellToPreventShuffling = !isFinishedSwitchingState()
                    && !droppedOnOriginalCell && !dropTargetLayout
                    .isRegionVacant(mTargetCell[0], mTargetCell[1], spanX, spanY);

                if (returnToOriginalCellToPreventShuffling) {
                    mTargetCell[0] = mTargetCell[1] = -1;
                } else {
                    mTargetCell = dropTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], 1, 1, spanX, spanY, cell,
                        mTargetCell, resultSpan, CellLayout.MODE_ON_DROP
                    );
                }*/
                int[] resultSpan = new int[2];
                mTargetCell = dropTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], 1, 1, spanX, spanY, cell,
                    mTargetCell, resultSpan, CellLayout.MODE_ON_DROP
                );

                boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;

                Log.d(TAG, "Found cell " + foundCell + " " + mTargetCell[0] + " " + mTargetCell[0]);

                if (foundCell) {
                    if (getScreenIdForPageIndex(mCurrentPage) != screenId && !hasMovedIntoHotseat) {
                        snapScreen = getPageIndexForScreenId(screenId);
                        snapToPage(snapScreen);
                    }

                    final LauncherItem info = (LauncherItem) cell.getTag();
                    if (hasMovedLayouts) {
                        // Reparent the view
                        CellLayout parentCell = getParentCellLayoutForView(cell);
                        if (parentCell != null) {
                            parentCell.removeView(cell);
                        } else if (BuildConfig.DEBUG) {
                            throw new NullPointerException("mDragInfo.cell has null parent");
                        }

                        addInScreen(cell, container, screenId, mTargetCell[0], mTargetCell[1]);
                    }

                    // update the item's position after drop
                    /*CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    lp.cellX = lp.tmpCellX = mTargetCell[0];
                    lp.cellY = lp.tmpCellY = mTargetCell[1];
                    lp.cellHSpan = item.spanX;
                    lp.cellVSpan = item.spanY;
                    lp.isLockedToGrid = true;*/

                    //TODO: Update the launcher database here.
                } else {
                    onNoCellFound(dropTargetLayout);

                    // If we can't find a drop location, we return the item to its original position
                    addInScreen(
                        cell,
                        mDragInfo.getContainer(),
                        mDragInfo.getScreenId(),
                        mDragInfo.getRank()
                    );
                }
            }

            final CellLayout parent = (CellLayout) cell.getParent();
            if (d.dragView.hasDrawn()) {
                if (droppedOnOriginalCellDuringTransition) {
                    // Animate the item to its original position, while simultaneously exiting
                    // spring-loaded mode so the page meets the icon where it was picked up.
                    mLauncher.getDragController().animateDragViewToOriginalPosition(
                        onCompleteRunnable, cell, SPRING_LOADED_TRANSITION_MS);
                    //mLauncher.getStateManager().goToState(NORMAL);
                    parent.onDropChild(cell);
                    return;
                }
                final LauncherItem info = (LauncherItem) cell.getTag();
                int duration = snapScreen < 0 ? -1 : ADJACENT_SCREEN_DROP_DURATION;

                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, cell, duration,
                    this
                );
            } else {
                d.deferDragViewCleanupPostAnimation = false;
                cell.setVisibility(VISIBLE);
            }
            parent.onDropChild(cell);

            /*mLauncher.getStateManager().goToState(
                NORMAL, SPRING_LOADED_EXIT_DELAY, onCompleteRunnable);*/
        }
    }

    boolean createUserFolderIfNecessary(
        View newView, long container, CellLayout target,
        int[] targetCell, float distance, boolean external, DragView dragView
    ) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View v = target.getChildAt(targetCell[0], targetCell[1]);

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            CellLayout cellParent = getParentCellLayoutForView(mDragInfo.getCell());
            hasntMoved = (mDragInfo.getRank() % cellParent.getMCountX() == targetCell[0] &&
                mDragInfo.getRank() / cellParent
                    .getMCountX() == targetCell[1]) && (cellParent == target);
        }

        if (v == null || hasntMoved || !mCreateUserFolderOnDrop) return false;
        mCreateUserFolderOnDrop = false;
        final long screenId = getIdForScreen(target);

        boolean aboveShortcut =
            (v.getTag() instanceof ApplicationItem) || (v.getTag() instanceof ShortcutItem);
        boolean willBecomeShortcut =
            (v.getTag() instanceof ApplicationItem) || (v.getTag() instanceof ShortcutItem);

        if (aboveShortcut && willBecomeShortcut) {
            LauncherItem sourceItem = (LauncherItem) newView.getTag();
            LauncherItem destItem = (LauncherItem) v.getTag();

            Rect folderLocation = new Rect();
            target.removeView(v);
            FolderItem fi = new FolderItem();
            fi.title = getResources().getString(R.string.untitled);
            fi.id = String.valueOf(System.currentTimeMillis());
            fi.items = new ArrayList<>();
            sourceItem.container = Long.parseLong(fi.id);
            destItem.container = Long.parseLong(fi.id);
            sourceItem.screenId = -1;
            destItem.screenId = -1;
            sourceItem.cell = fi.items.size();
            fi.items.add(sourceItem);
            destItem.cell = fi.items.size();
            fi.items.add(destItem);
            Drawable folderIcon = new GraphicsUtil(getContext()).generateFolderIcon(getContext(),
                sourceItem.icon, destItem.icon
            );
            fi.icon = folderIcon;
            fi.container = container;
            fi.screenId = screenId;
            fi.cell = targetCell[1] * mLauncher.getDeviceProfile().getInv()
                .getNumColumns() + targetCell[0];
            IconTextView folderView = (IconTextView) LayoutInflater.from(getContext())
                .inflate(R.layout.app_icon, null, false);
            folderView.applyFromShortcutItem(fi);
            folderView.setOnClickListener(ItemClickHandler.INSTANCE);
            folderView.setOnLongClickListener(ItemLongClickListener.INSTANCE_WORKSPACE);
            addInScreen(folderView, fi);
            // if the drag started here, we need to remove it from the workspace
            if (!external) {
                getParentCellLayoutForView(mDragInfo.getCell()).removeView(mDragInfo.getCell());
            }
            //Add animation here.
            dragView.remove();
            dragView = null;
            invalidate();
            return true;
        }
        return false;
    }

    private void addInScreen(IconTextView child, LauncherItem item) {
        addInScreen(child, item.container, item.screenId, item.cell);
    }

    boolean addToExistingFolderIfNecessary(
        View newView, CellLayout target, int[] targetCell,
        float distance, DragObject d, boolean external
    ) {
        if (distance > mMaxDistanceForFolderCreation) return false;

        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (!mAddToExistingFolderOnDrop) return false;
        mAddToExistingFolderOnDrop = false;

        if ((dropOverView instanceof IconTextView) && (dropOverView
            .getTag() instanceof FolderItem)) {
            FolderItem fi = (FolderItem) dropOverView.getTag();
            LauncherItem sourceItem = (LauncherItem) newView.getTag();
            sourceItem.container = Long.parseLong(fi.id);
            sourceItem.screenId = -1;
            sourceItem.cell = fi.items.size();
            fi.items.add(sourceItem);
            fi.icon = new GraphicsUtil(getContext()).generateFolderIcon(getContext(), fi);
            ((IconTextView) dropOverView).applyFromShortcutItem(fi);
            // if the drag started here, we need to remove it from the workspace
            if (!external) {
                getParentCellLayoutForView(mDragInfo.getCell()).removeView(mDragInfo.getCell());
            }
            //Add animation here.
            d.dragView.remove();
            d.dragView = null;
            invalidate();
            return true;
        }
        return false;
    }

    public void onNoCellFound(View dropTargetLayout) {
        Log.d(TAG, "onNoCellFound() called with: dropTargetLayout = [" + dropTargetLayout + "]");
        if (mLauncher.isHotseatLayout(dropTargetLayout)) {
            showOutOfSpaceMessage(true);
        } else {
            showOutOfSpaceMessage(false);
        }
    }

    private void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = (isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space);
        Toast.makeText(mLauncher, mLauncher.getString(strId), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
        Log.d(TAG, "onDragEnter() called with: dragObject = [" + dragObject + "]");
        mCreateUserFolderOnDrop = false;
        mAddToExistingFolderOnDrop = false;

        mDropToLayout = null;
        mDragViewVisualCenter = dragObject.getVisualCenter(mDragViewVisualCenter);
        setDropLayoutForDragObject(dragObject, mDragViewVisualCenter[0], mDragViewVisualCenter[1]);
    }

    /**
     * Updates {@link #mDragTargetLayout} and {@link #mDragOverlappingLayout}
     * based on the DragObject's position.
     * <p>
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
        if (parentFolderCell != null) {
            parentFolderCell.setScaleX(1f);
            parentFolderCell.setScaleY(1f);
        }

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
    private CellLayout verifyInsidePage(int pageNo, float[] touchXy) {
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
                Log.d(
                    TAG,
                    "Before mapping: " + mDragViewVisualCenter[0] + " " + mDragViewVisualCenter[1]
                );
                mapPointFromSelfToChild(mDragTargetLayout, mDragViewVisualCenter);
                Log.d(
                    TAG,
                    "After mapping: " + mDragViewVisualCenter[0] + " " + mDragViewVisualCenter[1]
                );
            }

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1],
                mDragTargetLayout, mTargetCell
            );
            int reorderX = mTargetCell[0];
            int reorderY = mTargetCell[1];

            setCurrentDropOverCell(mTargetCell[0], mTargetCell[1]);

            float targetCellDistance = mDragTargetLayout.getDistanceFromCell(
                mDragViewVisualCenter[0], mDragViewVisualCenter[1], mTargetCell);

            //TODO: Enable when supporting foler
            manageFolderFeedback(mDragTargetLayout, mTargetCell, targetCellDistance, d);

            boolean nearestDropOccupied = mDragTargetLayout.isNearestDropLocationOccupied((int)
                mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], child, mTargetCell);

            Log.d(
                TAG,
                "Reorder [" + mLastReorderX + ", " + mLastReorderY + "] [" + reorderX + ", " + reorderY + "] "
            );
            Log.d(
                TAG,
                "Reorder " + mDragMode + " " + mReorderAlarm
                    .alarmPending() + " " + nearestDropOccupied
            );

            if (!nearestDropOccupied) {
                mDragTargetLayout.visualizeDropLocation(child, mOutlineProvider,
                    mTargetCell[0], mTargetCell[1], false, d
                );
            } else if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
                && (mLastReorderX != reorderX || mLastReorderY != reorderY)) {
                Log.d(
                    TAG,
                    "Reorder  Setting reorder now"
                );
                int[] resultSpan = new int[2];
                /*mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], 1, 1, 1, 1,
                    child, mTargetCell, resultSpan, CellLayout.MODE_SHOW_REORDER_HINT);*/

                // Otherwise, if we aren't adding to or creating a folder and there's no pending
                // reorder, then we schedule a reorder
                ReorderAlarmListener listener =
                    new ReorderAlarmListener(mDragViewVisualCenter, d, child);
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
        // Here we store the final page that will be dropped to, if the workspace in fact
        // receives the drop
        mDropToLayout = mDragTargetLayout;
        if (mDragMode == DRAG_MODE_CREATE_FOLDER) {
            mCreateUserFolderOnDrop = true;
        } else if (mDragMode == DRAG_MODE_ADD_TO_FOLDER) {
            mAddToExistingFolderOnDrop = true;
        }

        // Reset the previous drag target
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);

        mSpringLoadedDragController.cancel();

        // Reset the grid state by stopping the animation and removing uninstall icon after 25 seconds
        wobbleExpireAlarm.setAlarm(WOBBLE_EXPIRATION_TIMEOUT);
    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        CellLayout dropTargetLayout = mDropToLayout;
        long screenId = getIdForScreen(dropTargetLayout);
        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            commitExtraEmptyScreen();
        }
        return true;
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
        View child = cellInfo.getCell();
        mDragInfo = cellInfo;
        // Clear any running animation.
        child.clearAnimation();
        child.setVisibility(GONE);
        beginDragShared(child, this, dragOptions);
    }

    public void beginDragShared(View child, DragSource source, DragOptions options) {
        Object dragObject = child.getTag();
        if (!(dragObject instanceof LauncherItem)) {
            String msg = "Drag started with a view that has no tag set. This "
                + "will cause a crash down the line. "
                + "View: " + child + "  tag: " + child.getTag();
            throw new IllegalStateException(msg);
        }

        beginDragShared(
            child,
            source,
            (LauncherItem) dragObject,
            new DragPreviewProvider(child),
            options
        );
    }

    private DragView beginDragShared(
        View child,
        DragSource source,
        LauncherItem dragObject,
        DragPreviewProvider previewProvider,
        DragOptions dragOptions
    ) {
        float iconScale = 1f;
        if (child instanceof IconTextView) {
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

        Log.i(TAG, "beginDragShared: " + dragLayerY);

        VariantDeviceProfile grid = mLauncher.getDeviceProfile();
        Point dragVisualizeOffset = null;
        Rect dragRect = null;
        if (child instanceof IconTextView) {
            dragRect =
                ((IconTextView) child).getIconBounds();
            dragLayerY += dragRect.top;
            Log.i(TAG, "beginDragShared: " + dragLayerY + " " + dragRect);

            // Note: The dragRect is used to calculate drag layer offsets, but the
            // dragVisualizeOffset in addition to the dragRect (the size) to position the outline.
            dragVisualizeOffset = new Point(-halfPadding, halfPadding);
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
            dragObject, dragVisualizeOffset, dragRect, scale * iconScale, scale, dragOptions
        );
        dv.setIntrinsicIconScaleFactor(dragOptions.intrinsicIconScaleFactor);
        return dv;
    }

    /**
     * Calculate the nearest cell where the given object would be dropped.
     * <p>
     * pixelX and pixelY should be in the coordinate system of layout
     */
    int[] findNearestArea(int pixelX, int pixelY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(
            pixelX, pixelY, recycle);
    }

    boolean willCreateUserFolder(
        LauncherItem info, CellLayout target, int[] targetCell,
        float distance, boolean considerTimeout
    ) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        return willCreateUserFolder(info, dropOverView, considerTimeout);
    }

    boolean willCreateUserFolder(LauncherItem info, View dropOverView, boolean considerTimeout) {

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            hasntMoved = dropOverView == mDragInfo.getCell();
        }

        if (dropOverView == null || hasntMoved || (considerTimeout && !mCreateUserFolderOnDrop)) {
            return false;
        }

        boolean aboveShortcut = (dropOverView.getTag() instanceof ShortcutItem || dropOverView
            .getTag() instanceof ApplicationItem);
        boolean willBecomeShortcut =
            (info.itemType == ITEM_TYPE_APPLICATION ||
                info.itemType == Constants.ITEM_TYPE_SHORTCUT);

        return (aboveShortcut && willBecomeShortcut);
    }

    boolean willAddToExistingUserFolder(
        LauncherItem dragInfo, CellLayout target, int[] targetCell,
        float distance
    ) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        return willAddToExistingUserFolder(dragInfo, dropOverView);
    }

    boolean willAddToExistingUserFolder(LauncherItem dragInfo, View dropOverView) {
        if (dropOverView.getTag() instanceof FolderItem) {
            return true;
        }
        return false;
    }

    private void manageFolderFeedback(
        CellLayout targetLayout,
        int[] targetCell, float distance, DragObject dragObject
    ) {
        Log.d(
            TAG,
            "manageFolderFeedback() called with: targetLayout = [" + targetLayout + "], targetCell = [" + targetCell + "], distance = [" + distance + "], dragObject = [" + dragObject + "]"
        );
        if (distance > mMaxDistanceForFolderCreation) return;

        final View dragOverView = mDragTargetLayout.getChildAt(mTargetCell[0], mTargetCell[1]);
        LauncherItem info = dragObject.dragInfo;
        boolean userFolderPending = willCreateUserFolder(info, dragOverView, false);
        Log.i(TAG, "manageFolderFeedback: userFolderPending: " + userFolderPending);
        if (mDragMode == DRAG_MODE_NONE && userFolderPending &&
            !mFolderCreationAlarm.alarmPending()) {

            FolderCreationAlarmListener listener = new
                FolderCreationAlarmListener(targetLayout, targetCell[0], targetCell[1], true);

            if (!dragObject.accessibleDrag) {
                mFolderCreationAlarm.setOnAlarmListener(listener);
                mFolderCreationAlarm.setAlarm(FOLDER_CREATION_TIMEOUT);
            } else {
                listener.onAlarm(mFolderCreationAlarm);
            }

            //TODO: Enable when supporting accessibility
            /*if (dragObject.stateAnnouncer != null) {
                dragObject.stateAnnouncer.announce(WorkspaceAccessibilityHelper
                    .getDescriptionForDropOver(dragOverView, getContext()));
            }*/
            return;
        }

        boolean willAddToFolder = willAddToExistingUserFolder(info, dragOverView);
        if (willAddToFolder && mDragMode == DRAG_MODE_NONE && !mFolderCreationAlarm
            .alarmPending()) {
            FolderCreationAlarmListener listener = new
                FolderCreationAlarmListener(targetLayout, targetCell[0], targetCell[1], false);

            if (!dragObject.accessibleDrag) {
                mFolderCreationAlarm.setOnAlarmListener(listener);
                mFolderCreationAlarm.setAlarm(FOLDER_CREATION_TIMEOUT);
            } else {
                listener.onAlarm(mFolderCreationAlarm);
            }
            setDragMode(DRAG_MODE_ADD_TO_FOLDER);
            return;
        }

        if (mDragMode == DRAG_MODE_ADD_TO_FOLDER && !willAddToFolder) {
            setDragMode(DRAG_MODE_NONE);
        }
        if (mDragMode == DRAG_MODE_CREATE_FOLDER && !userFolderPending) {
            setDragMode(DRAG_MODE_NONE);
        }
    }

    private void onStartStateTransition(LauncherState state) {
        mIsSwitchingState = true;
        mTransitionProgress = 0;

        updateChildrenLayersEnabled();
    }

    private void onEndStateTransition() {
        mIsSwitchingState = false;
        mForceDrawAdjacentPages = false;
        mTransitionProgress = 1;

        updateChildrenLayersEnabled();
    }

    /**
     * Sets the current workspace {@link LauncherState} and updates the UI without any animations
     */
    @Override
    public void setState(LauncherState toState) {
        onStartStateTransition(toState);
        mStateTransitionAnimation.setState(toState);
        onEndStateTransition();
    }

    /**
     * Sets the current workspace {@link LauncherState}, then animates the UI
     */
    @Override
    public void setStateWithAnimation(
        LauncherState toState,
        AnimatorSetBuilder builder, LauncherStateManager.AnimationConfig config
    ) {
        StateTransitionListener listener = new StateTransitionListener(toState);
        mStateTransitionAnimation.setStateWithAnimation(toState, builder, config);

        // Invalidate the pages now, so that we have the visible pages before the
        // animation is started
        if (toState.hasMultipleVisiblePages) {
            mForceDrawAdjacentPages = true;
        }
        invalidate(); // This will call dispatchDraw(), which calls getVisiblePages().

        ValueAnimator stepAnimator = ValueAnimator.ofFloat(0, 1);
        stepAnimator.addUpdateListener(listener);
        stepAnimator.setDuration(config.duration);
        stepAnimator.addListener(listener);
        builder.play(stepAnimator);
    }

    /**
     * Similar to {@link #getFirstMatch} but optimized to finding a suitable view for the app close
     * animation.
     *
     * @param packageName The package name of the app to match.
     * @param user        The user of the app to match.
     */
    public View getFirstMatchForAppClose(String packageName, UserHandle user) {
        final int curPage = getCurrentPage();
        final CellLayout currentPage = (CellLayout) getPageAt(curPage);
        final LauncherPagedView.ItemOperator packageAndUser =
            (LauncherItem info, View view, int index) -> info != null
                && info.getTargetComponent() != null
                && TextUtils.equals(info.getTargetComponent().getPackageName(), packageName)
                && info.user.equals(user);
        final LauncherPagedView.ItemOperator packageAndUserAndApp =
            (LauncherItem info, View view, int index) ->
                packageAndUser.evaluate(info, view, index) && info.itemType == ITEM_TYPE_APPLICATION;

        return getFirstMatch(
            new CellLayout[]{mLauncher.getHotseat(), currentPage},
            packageAndUserAndApp
        );
    }

    /**
     * @param cellLayouts List of CellLayouts to scan, in order of preference.
     * @param operators   List of operators, in order starting from best matching operator.
     * @return
     */
    private View getFirstMatch(CellLayout[] cellLayouts, final ItemOperator... operators) {
        // This array is filled with the first match for each operator.
        final View[] matches = new View[operators.length];
        // For efficiency, the outer loop should be CellLayout.
        for (CellLayout cellLayout : cellLayouts) {
            mapOverCellLayout(MAP_NO_RECURSE, cellLayout, (info, v, idx) -> {
                for (int i = 0; i < operators.length; ++i) {
                    if (matches[i] == null && operators[i].evaluate(info, v, idx)) {
                        matches[i] = v;
                        if (i == 0) {
                            // We can return since this is the best match possible.
                            return true;
                        }
                    }
                }
                return false;
            });
            if (matches[0] != null) {
                break;
            }
        }
        for (View match : matches) {
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * Map the operator over the shortcuts and widgets, return the first-non-null value.
     *
     * @param recurse true: iterate over folder children. false: op get the folders themselves.
     * @param op      the operator to map over the shortcuts
     */
    public void mapOverItems(boolean recurse, ItemOperator op) {
        for (CellLayout layout : getWorkspaceAndHotseatCellLayouts()) {
            if (mapOverCellLayout(recurse, layout, op)) {
                return;
            }
        }
    }

    private boolean mapOverCellLayout(boolean recurse, CellLayout layout, ItemOperator op) {
        // TODO(b/128460496) Potential race condition where layout is not yet loaded
        if (layout == null) {
            return false;
        }
        // map over all the shortcuts on the workspace
        final int itemCount = layout.getChildCount();
        for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
            View item = layout.getChildAt(itemIdx);
            LauncherItem info = (LauncherItem) item.getTag();
            if (recurse && info instanceof FolderItem) {
                FolderItem folder = (FolderItem) info;
                List<LauncherItem> folderChildren = folder.items;
                // map over all the children in the folder
                final int childCount = folder.items.size();
                for (int childIdx = 0; childIdx < childCount; childIdx++) {
                    LauncherItem childItem = folderChildren.get(childIdx);
                    if (op.evaluate(info, item, itemIdx)) {
                        return true;
                    }
                }
            } else {
                if (op.evaluate(info, item, itemIdx)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateNotificationBadge(Predicate<PackageUserKey> updatedDots) {
        final PackageUserKey packageUserKey = new PackageUserKey(null, null);
        final Set<String> folderIds = new HashSet<>();
        mapOverItems(MAP_RECURSE, (info, v, itemIdx) -> {
            if ((info instanceof ApplicationItem || info instanceof ShortcutItem) && v instanceof IconTextView) {
                if (!packageUserKey.updateFromItemInfo(info)
                    || updatedDots.test(packageUserKey)) {
                    ((IconTextView) v).applyDotState(info, true /* animate */);
                    folderIds.add(String.valueOf(info.container));
                }
            }
            // process all the shortcuts
            return false;
        });

        // Update folder icons
        mapOverItems(MAP_NO_RECURSE, (info, v, itemIdx) -> {
            if (info instanceof FolderItem && folderIds.contains(info.id)
                && v instanceof IconTextView) {
                FolderDotInfo folderDotInfo = new FolderDotInfo();
                for (LauncherItem si : ((FolderItem) info).items) {
                    folderDotInfo.addDotInfo(mLauncher.getDotInfoForItem(si));
                }
                ((IconTextView) v).applyDotState(info, true /* animate */);
            }
            // process all the shortcuts
            return false;
        });
    }

    /**
     * The overlay scroll is being controlled locally, just update our overlay effect
     */
    public void onOverlayScrollChanged(float scroll) {
        if (Float.compare(scroll, 1f) == 0) {
            mOverlayShown = true;
            // Not announcing the overlay page for accessibility since it announces itself.
        } else if (Float.compare(scroll, 0f) == 0) {
            if (Float.compare(mOverlayTranslation, 0f) != 0) {
                // When arriving to 0 overscroll from non-zero overscroll, announce page for
                // accessibility since default announcements were disabled while in overscroll
                // state.
                // Not doing this if mOverlayShown because in that case the accessibility service
                // will announce the launcher window description upon regaining focus after
                // switching from the overlay screen.
                announcePageForAccessibility();
            }
            mOverlayShown = false;
            tryRunOverlayCallback();
        }

        float offset = 0f;

        scroll = Math.max(scroll - offset, 0);
        scroll = Math.min(1, scroll / (1 - offset));

        float alpha = 1 - Interpolators.DEACCEL_3.getInterpolation(scroll);
        float transX = mLauncher.getDragLayer().getMeasuredWidth() * scroll;

        if (mIsRtl) {
            transX = -transX;
        }
        mOverlayTranslation = transX;
        Log.d(TAG, "onOverlayScrollChanged() called with: scroll = [" + scroll + "]");

        // TODO(adamcohen): figure out a final effect here. We may need to recommend
        // different effects based on device performance. On at least one relatively high-end
        // device I've tried, translating the launcher causes things to get quite laggy.
        mLauncher.getDragLayer().setTranslationX(transX);
        mLauncher.getDragLayer().getAlphaProperty(ALPHA_INDEX_OVERLAY).setValue(alpha);
    }

    protected void announcePageForAccessibility() {
        // TODO: Enable it when supporting accessibility.
        /*if (isAccessibilityEnabled(getContext())) {
            // Notify the user when the page changes
            announceForAccessibility(getCurrentPageDescription());
        }*/
    }

    public Hotseat getHotseat() {
        return mLauncher.getHotseat();
    }

    /**
     * It starts to animate all the grid item and also add uninstall button to each item if the item supports it.
     */
    public void wobbleLayouts() {
        // Adds uninstall icon.
        Animation wobbleAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.wobble);
        Animation reverseWobbleAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.wobble_reverse);
        mapOverItems(MAP_NO_RECURSE, (info, v, itemIdx) -> {
            if ((info instanceof ApplicationItem || info instanceof ShortcutItem)
                && v instanceof IconTextView
                && !UninstallHelper.INSTANCE
                .isUninstallDisabled(info.user.getRealHandle(), getContext())) {

                // Return early if this app is system app
                if(info instanceof ApplicationItem) {
                    ApplicationItem applicationItem = (ApplicationItem) info;
                    if (applicationItem.isSystemApp != ApplicationItem.FLAG_SYSTEM_UNKNOWN) {
                        if ((applicationItem.isSystemApp & ApplicationItem.FLAG_SYSTEM_NO) != 0) {
                            ((IconTextView)v).applyUninstallIconState(true);
                        }
                    } else {
                        ((IconTextView)v).applyUninstallIconState(true);
                    }
                } else {
                    ((IconTextView)v).applyUninstallIconState(true);
                }
            }

            if(itemIdx % 2 == 0) {
                v.startAnimation(wobbleAnimation);
            } else {
                v.startAnimation(reverseWobbleAnimation);
            }
            // process all the items
            return false;
        });
    }

    /**
     * Triggered when wobble animation expire after timeout.
     * @param alarm
     */
    @Override
    public void onAlarm(Alarm alarm) {
        // Adds uninstall icon.
        mapOverItems(MAP_NO_RECURSE, (info, v, idx) -> {
            if (v instanceof IconTextView) {
                ((IconTextView)v).applyUninstallIconState(false);
            }

            // Clears if there is any running animation on the view.
            v.clearAnimation();

            // process all the items
            return false;
        });
    }

    public interface ItemOperator {
        /**
         * Process the next itemInfo, possibly with side-effect on the next item.
         *
         * @param info info for the shortcut
         * @param view view for the shortcut
         * @param index index of the view in the parent layout.
         * @return true if done, false to continue the map
         */
        boolean evaluate(LauncherItem info, View view, int index);
    }

    class FolderCreationAlarmListener implements OnAlarmListener {
        final CellLayout layout;
        final IconTextView cell;
        final int cellX;
        final int cellY;
        final boolean createFolder;

        public FolderCreationAlarmListener(
            CellLayout layout,
            int cellX,
            int cellY,
            boolean createFolder
        ) {
            this.layout = layout;
            this.cellX = cellX;
            this.cellY = cellY;
            this.cell = (IconTextView) layout.getChildAt(cellX, cellY);
            this.createFolder = createFolder;
        }

        public void onAlarm(Alarm alarm) {
            Log.d(TAG, "onAlarm() called with: alarm = [" + alarm + "]");
            parentFolderCell = cell;
            parentFolderCell.setScaleX(1.2f);
            parentFolderCell.setScaleY(1.2f);
            if (createFolder) {
                setDragMode(DRAG_MODE_CREATE_FOLDER);
            } else {
                setDragMode(DRAG_MODE_ADD_TO_FOLDER);
            }
        }
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
                (int) mDragViewVisualCenter[1], mDragTargetLayout,
                mTargetCell
            );
            mLastReorderX = mTargetCell[0];
            mLastReorderY = mTargetCell[1];

            mTargetCell = mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], 1, 1, 1, 1,
                child, mTargetCell, resultSpan, CellLayout.MODE_DRAG_OVER
            );

            if (mTargetCell[0] < 0 || mTargetCell[1] < 0) {
                mDragTargetLayout.revertTempState();
            } else {
                setDragMode(DRAG_MODE_REORDER);
            }
            mDragTargetLayout.visualizeDropLocation(child, mOutlineProvider,
                mTargetCell[0], mTargetCell[1], false, dragObject
            );
        }
    }

    private class StateTransitionListener extends AnimatorListenerAdapter
        implements ValueAnimator.AnimatorUpdateListener {

        private final LauncherState mToState;

        StateTransitionListener(LauncherState toState) {
            mToState = toState;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator anim) {
            mTransitionProgress = anim.getAnimatedFraction();
        }

        @Override
        public void onAnimationStart(Animator animation) {
            onStartStateTransition(mToState);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            onEndStateTransition();
        }
    }
}
