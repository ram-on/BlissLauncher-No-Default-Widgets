package foundation.e.blisslauncher.core.customviews;

import static foundation.e.blisslauncher.core.utils.Constants.ITEM_TYPE_APPLICATION;
import static foundation.e.blisslauncher.features.test.LauncherState.NORMAL;
import static foundation.e.blisslauncher.features.test.anim.LauncherAnimUtils.SPRING_LOADED_TRANSITION_MS;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.app.usage.UsageStats;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MutableInt;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jakewharton.rxbinding3.widget.RxTextView;
import foundation.e.blisslauncher.BuildConfig;
import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.Preferences;
import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.customviews.pageindicators.PageIndicatorDots;
import foundation.e.blisslauncher.core.database.DatabaseManager;
import foundation.e.blisslauncher.core.database.model.ApplicationItem;
import foundation.e.blisslauncher.core.database.model.FolderItem;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.core.executors.AppExecutors;
import foundation.e.blisslauncher.core.touch.ItemClickHandler;
import foundation.e.blisslauncher.core.touch.ItemLongClickListener;
import foundation.e.blisslauncher.core.touch.WorkspaceTouchListener;
import foundation.e.blisslauncher.core.utils.AppUtils;
import foundation.e.blisslauncher.core.utils.Constants;
import foundation.e.blisslauncher.core.utils.GraphicsUtil;
import foundation.e.blisslauncher.core.utils.IntSparseArrayMap;
import foundation.e.blisslauncher.core.utils.IntegerArray;
import foundation.e.blisslauncher.core.utils.ListUtil;
import foundation.e.blisslauncher.core.utils.PackageUserKey;
import foundation.e.blisslauncher.features.folder.FolderIcon;
import foundation.e.blisslauncher.features.launcher.Hotseat;
import foundation.e.blisslauncher.features.launcher.SearchInputDisposableObserver;
import foundation.e.blisslauncher.features.notification.FolderDotInfo;
import foundation.e.blisslauncher.features.shortcuts.DeepShortcutManager;
import foundation.e.blisslauncher.features.shortcuts.InstallShortcutReceiver;
import foundation.e.blisslauncher.features.shortcuts.ShortcutKey;
import foundation.e.blisslauncher.features.suggestions.AutoCompleteAdapter;
import foundation.e.blisslauncher.features.suggestions.SearchSuggestionUtil;
import foundation.e.blisslauncher.features.suggestions.SuggestionProvider;
import foundation.e.blisslauncher.features.suggestions.SuggestionsResult;
import foundation.e.blisslauncher.features.test.Alarm;
import foundation.e.blisslauncher.features.test.CellLayout;
import foundation.e.blisslauncher.features.test.IconTextView;
import foundation.e.blisslauncher.features.test.LauncherItemMatcher;
import foundation.e.blisslauncher.features.test.LauncherState;
import foundation.e.blisslauncher.features.test.LauncherStateManager;
import foundation.e.blisslauncher.features.test.OnAlarmListener;
import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;
import foundation.e.blisslauncher.features.test.WorkspaceStateTransitionAnimation;
import foundation.e.blisslauncher.features.test.anim.AnimatorSetBuilder;
import foundation.e.blisslauncher.features.test.anim.Interpolators;
import foundation.e.blisslauncher.features.test.anim.PropertyListBuilder;
import foundation.e.blisslauncher.features.test.dragndrop.DragController;
import foundation.e.blisslauncher.features.test.dragndrop.DragOptions;
import foundation.e.blisslauncher.features.test.dragndrop.DragSource;
import foundation.e.blisslauncher.features.test.dragndrop.DragView;
import foundation.e.blisslauncher.features.test.dragndrop.DropTarget;
import foundation.e.blisslauncher.features.test.dragndrop.SpringLoadedDragController;
import foundation.e.blisslauncher.features.test.graphics.DragPreviewProvider;
import foundation.e.blisslauncher.features.test.uninstall.UninstallHelper;
import foundation.e.blisslauncher.features.usagestats.AppUsageStats;
import foundation.e.blisslauncher.features.weather.DeviceStatusService;
import foundation.e.blisslauncher.features.weather.ForecastBuilder;
import foundation.e.blisslauncher.features.weather.WeatherPreferences;
import foundation.e.blisslauncher.features.weather.WeatherSourceListenerService;
import foundation.e.blisslauncher.features.weather.WeatherUpdateService;
import foundation.e.blisslauncher.features.weather.WeatherUtils;
import foundation.e.blisslauncher.features.widgets.WidgetManager;
import foundation.e.blisslauncher.features.widgets.WidgetPageLayer;
import foundation.e.blisslauncher.features.widgets.WidgetViewBuilder;
import foundation.e.blisslauncher.features.widgets.WidgetsActivity;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

public class LauncherPagedView extends PagedView<PageIndicatorDots> implements View.OnTouchListener,
    Insettable, DropTarget, DragSource, DragController.DragListener,
    LauncherStateManager.StateHandler, OnAlarmListener,
    AutoCompleteAdapter.OnSuggestionClickListener {

    private static final String TAG = "LauncherPagedView";
    private static final int DEFAULT_PAGE = 1;
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
    public static final int EXTRA_EMPTY_SCREEN_ID = -201;
    // The is the first screen. It is always present, even if its empty.
    public static final long FIRST_SCREEN_ID = 0;
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;

    private final TestActivity mLauncher;

    private LayoutTransition mLayoutTransition;
    final WallpaperManager mWallpaperManager;

    public final IntSparseArrayMap<CellLayout> mWorkspaceScreens = new IntSparseArrayMap<>();
    final IntegerArray mScreenOrder = new IntegerArray();

    // Variables relating to touch disambiguation (scrolling workspace vs. scrolling a widget)
    private float mXDown;
    private float mYDown;
    final static float START_DAMPING_TOUCH_SLOP_ANGLE = (float) Math.PI / 6;
    final static float MAX_SWIPE_ANGLE = (float) Math.PI / 3;
    final static float TOUCH_SLOP_DAMPING_FACTOR = 4;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static final int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    static final int NEW_APPS_ANIMATION_DELAY = 500;
    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;

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

    private static final int FOLDER_CREATION_TIMEOUT = 100;
    public static final int REORDER_TIMEOUT = 350;
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

    /**
     * Map of ShortcutKey to the number of times it is pinned.
     */
    public final Map<ShortcutKey, MutableInt> pinnedShortcutCounts = new HashMap<>();

    /**
     * The value that {@link #mTransitionProgress} must be greater than for
     * {@link #transitionStateShouldAllowDrop()} to return true.
     */
    private static final float ALLOW_DROP_TRANSITION_PROGRESS = 0.25f;
    private WidgetPageLayer widgetPage;
    private LinearLayout widgetContainer;
    private BlissInput mSearchInput;
    private View mWeatherPanel;
    private View mWeatherSetupTextView;
    public RoundedWidgetView activeRoundedWidgetView;
    private List<UsageStats> mUsageStats;
    private AnimatorSet currentAnimator;

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
        setOnTouchListener(new WorkspaceTouchListener(mLauncher, this));

        wobbleExpireAlarm.setOnAlarmListener(this);
    }

    private void initWorkspace() {
        mCurrentPage = 0;
        setClipChildren(true);
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
        // GridLayout grid = (GridLayout) child;
        //grid.setOnInterceptOnTouchListener(this);
        super.onViewAdded(child);
    }

    public boolean isTouchActive() {
        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);

        VariantDeviceProfile grid = mLauncher.getDeviceProfile();
        mMaxDistanceForFolderCreation = (0.35f * grid.getIconSizePx());
        Rect padding = grid.getWorkspacePadding();
        setPadding(padding.left, padding.top, padding.right, 0);
        int paddingLeftRight = grid.getCellLayoutPaddingLeftRightPx();
        int paddingBottom = grid.getCellLayoutBottomPaddingPx();
        for (int i = mWorkspaceScreens.size() - 1; i >= 0; i--) {
            mWorkspaceScreens.valueAt(i)
                .setPadding(paddingLeftRight, 0, paddingLeftRight, paddingBottom + padding.bottom);
        }
    }

    public void deferRemoveExtraEmptyScreen() {
        mDeferRemoveExtraEmptyScreen = true;
    }

    public void bindAndInitFirstScreen(View view) {
        setupWidgetPage();
    }

    public void removeAllWorkspaceScreens() {
        // Disable all layout transitions before removing all pages to ensure that we don't get the
        // transition animations competing with us changing the scroll when we add pages
        disableLayoutTransitions();

        // Remove the pages and clear the screen models
        removeFolderListeners();
        removeAllViews();
        mScreenOrder.clear();
        mWorkspaceScreens.clear();

        // Ensure that the first page is always present
        bindAndInitFirstScreen(null);

        // Re-enable the layout transitions
        enableLayoutTransitions();
    }

    public void insertNewWorkspaceScreenBeforeEmptyScreen(int screenId) {
        // Find the index to insert this view into.  If the empty screen exists, then
        // insert it before that.
        int insertIndex = mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID);
        if (insertIndex < 0) {
            insertIndex = mScreenOrder.size();
        }
        insertNewWorkspaceScreen(screenId, insertIndex + 1);
    }

    public void bindScreens(@NotNull IntegerArray orderedScreenIds) {
        if (orderedScreenIds.isEmpty()) {
            addExtraEmptyScreen();
        }

        for (int i = 0; i < orderedScreenIds.size(); i++) {
            int screenId = orderedScreenIds.get(i);
            insertNewWorkspaceScreenBeforeEmptyScreen(screenId);
        }
    }

    public void bindItems(
        @NotNull List<? extends LauncherItem> launcherItems,
        boolean animateIcons
    ) {
        final Collection<Animator> bounceAnims = new ArrayList<>();
        int newItemsScreenId = -1;
        List<LauncherItem> unhandledItems = new ArrayList<>();
        for (int i = 0; i < launcherItems.size(); i++) {
            LauncherItem launcherItem = launcherItems.get(i);
            View appView;
            if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                FolderIcon folderIcon = FolderIcon.Companion.fromXml(
                    R.layout.folder_icon,
                    getScreenWithId(launcherItem.screenId),
                    (FolderItem) launcherItem
                );
                folderIcon.applyFromFolderItem((FolderItem) launcherItem);
                appView = folderIcon;
            } else {
                IconTextView appIcon = (IconTextView) LayoutInflater.from(getContext())
                    .inflate(R.layout.app_icon, null, false);
                appIcon.applyFromShortcutItem(launcherItem);
                appView = appIcon;
            }
            appView.setOnClickListener(ItemClickHandler.INSTANCE);
            appView.setOnLongClickListener(ItemLongClickListener.INSTANCE_WORKSPACE);
            if (launcherItem.container == Constants.CONTAINER_DESKTOP) {
                CellLayout cl = getScreenWithId(launcherItem.screenId);
                if ((cl != null && cl.isOccupied(launcherItem.cell))) {
                    // We add this item to unhandled and handle them later.
                    unhandledItems.add(launcherItem);
                    continue;
                }
                GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.LayoutParams iconLayoutParams =
                    new GridLayout.LayoutParams(rowSpec, colSpec);
                iconLayoutParams.height = mLauncher.getDeviceProfile().getCellHeightPx();
                iconLayoutParams.width = mLauncher.getDeviceProfile().getCellWidthPx();
                appView.setLayoutParams(iconLayoutParams);
            } else if (launcherItem.container == Constants.CONTAINER_HOTSEAT) {
                GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
                GridLayout.LayoutParams iconLayoutParams =
                    new GridLayout.LayoutParams(rowSpec, colSpec);
                iconLayoutParams.height = mLauncher.getDeviceProfile().getHotseatCellHeightPx();
                iconLayoutParams.width = mLauncher.getDeviceProfile().getCellWidthPx();
                appView.setLayoutParams(iconLayoutParams);
            }
            addInScreenFromBind(appView, launcherItem);
            if (animateIcons) {
                // Animate all the applications up now
                appView.setAlpha(0f);
                appView.setScaleX(0f);
                appView.setScaleY(0f);
                bounceAnims.add(createNewAppBounceAnimation(appView, i));
                newItemsScreenId = launcherItem.screenId;
            }
        }

        // Handle unhandled items
        if (!unhandledItems.isEmpty()) {
            List<LauncherItem> tempItems = new ArrayList<>(unhandledItems);
            bindItemsAdded(tempItems);
            unhandledItems.clear();
        }

        // Animate to the correct page
        if (animateIcons && newItemsScreenId > -1) {
            AnimatorSet anim = new AnimatorSet();
            anim.playTogether(bounceAnims);
            int currentScreenId = getScreenIdForPageIndex(getNextPage());
            final int newScreenIndex = getPageIndexForScreenId(newItemsScreenId);
            final Runnable startBounceAnimRunnable = anim::start;

            if (newItemsScreenId != currentScreenId) {
                // We post the animation slightly delayed to prevent slowdowns
                // when we are loading right after we return to launcher.
                this.postDelayed(() -> {
                    AbstractFloatingView.closeAllOpenViews(mLauncher, false);

                    snapToPage(newScreenIndex);
                    postDelayed(
                        startBounceAnimRunnable,
                        NEW_APPS_ANIMATION_DELAY
                    );
                }, NEW_APPS_PAGE_MOVE_DELAY);
            } else {
                postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
            }
        }
        requestLayout();
    }

    public void bindItemsAdded(@NotNull List<? extends LauncherItem> items) {
        final ArrayList<LauncherItem> addedItemsFinal = new ArrayList<>();
        final IntegerArray addedWorkspaceScreensFinal = new IntegerArray();
        List<LauncherItem> filteredItems = new ArrayList<>();
        for (LauncherItem item : items) {
            if (item.itemType == Constants.ITEM_TYPE_APPLICATION ||
                item.itemType == Constants.ITEM_TYPE_SHORTCUT
            ) {
                // Short-circuit this logic if the icon exists somewhere on the workspace
                if (shortcutExists(item.getIntent(), item.user.getRealHandle())) {
                    continue;
                }
            }
            if (item != null) {
                filteredItems.add(item);
            }
        }

        for (LauncherItem item : filteredItems) {
            // Find appropriate space for the item.
            int[] coords = findSpaceForItem(addedWorkspaceScreensFinal);
            int screenId = coords[0];

            LauncherItem itemInfo;
            if (item instanceof ApplicationItem || item instanceof ShortcutItem ||
                item instanceof FolderItem) {
                itemInfo = item;
                itemInfo.screenId = screenId;
                itemInfo.cell = coords[1];
                itemInfo.container = Constants.CONTAINER_DESKTOP;
            } else {
                throw new RuntimeException("Unexpected info type");
            }

            if (item.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                // Increment the count for the given shortcut
                ShortcutKey pinnedShortcut = ShortcutKey.fromItem((ShortcutItem) item);
                MutableInt count = pinnedShortcutCounts.get(pinnedShortcut);
                if (count == null) {
                    count = new MutableInt(1);
                    pinnedShortcutCounts.put(pinnedShortcut, count);
                } else {
                    count.value++;
                }

                // Since this is a new item, pin the shortcut in the system server.
                if (count.value == 1) {
                    DeepShortcutManager.getInstance(getContext()).pinShortcut(pinnedShortcut);
                }
            }
            // Save the WorkspaceItemInfo for binding in the workspace
            addedItemsFinal.add(itemInfo);
        }

        if (!addedItemsFinal.isEmpty()) {
            final ArrayList<LauncherItem> addAnimated = new ArrayList<>();
            final ArrayList<LauncherItem> addNotAnimated = new ArrayList<>();
            if (!addedItemsFinal.isEmpty()) {
                LauncherItem info = addedItemsFinal.get(addedItemsFinal.size() - 1);
                int lastScreenId = info.screenId;
                for (LauncherItem i : addedItemsFinal) {
                    if (i.screenId == lastScreenId) {
                        addAnimated.add(i);
                    } else {
                        addNotAnimated.add(i);
                    }
                }
            }

            if (!addedWorkspaceScreensFinal.isEmpty()) {
                bindScreens(addedWorkspaceScreensFinal);
            }

            // We add the items without animation on non-visible pages, and with
            // animations on the new page (which we will try and snap to).
            if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
                bindItems(addNotAnimated, false);
            }
            if (addAnimated != null && !addAnimated.isEmpty()) {
                bindItems(addAnimated, true);
            }

            // Remove the extra empty screen
            removeExtraEmptyScreen(false, false);
            updateDatabase(getWorkspaceAndHotseatCellLayouts());
        }
    }

    public void updateDatabase() {
        updateDatabase(getWorkspaceAndHotseatCellLayouts());
    }

    private void setupWidgetPage() {
        widgetPage =
            (WidgetPageLayer) LayoutInflater.from(getContext())
                .inflate(R.layout.widgets_page, this, false);
        this.addView(widgetPage, 0);
        InsettableScrollLayout scrollView = widgetPage.findViewById(R.id.widgets_scroll_container);
        scrollView.setOnTouchListener((v, event) -> {
            if (widgetPage.findViewById(R.id.widget_resizer_container).getVisibility()
                == VISIBLE) {
                hideWidgetResizeContainer();
            }
            return false;
        });

        widgetContainer = widgetPage.findViewById(R.id.widget_container);

        widgetPage.setVisibility(View.VISIBLE);
//        widgetPage.post {
//            widgetPage.translationX = -(widgetPage.measuredWidth * 1.00f)
//        }
        widgetPage.findViewById(R.id.used_apps_layout).setClipToOutline(true);
        widgetPage.setTag("Widget page");

        // TODO: replace with app predictions
        // Prepare app suggestions view
        // [[BEGIN]]
        widgetPage.findViewById(R.id.openUsageAccessSettings)
            .setOnClickListener(v -> mLauncher.startActivity(
                new Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            ));

        // divided by 2 because of left and right padding.
        final float emptySpace = mLauncher.getDeviceProfile().getAvailableWidthPx() - 4 *
            mLauncher.getDeviceProfile().getCellWidthPx();
        int padding = (int) (emptySpace / 10);
        widgetPage.findViewById(R.id.suggestedAppGrid)
            .setPadding(padding, 0, padding, 0);
        // [[END]]

        // Prepare search suggestion view
        // [[BEGIN]]
        mSearchInput = widgetPage.findViewById(R.id.search_input);
        ImageView clearSuggestions =
            widgetPage.findViewById(R.id.clearSuggestionImageView);
        clearSuggestions.setOnClickListener(v -> {
            mSearchInput.setText("");
            mSearchInput.clearFocus();
        });

        mSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    clearSuggestions.setVisibility(GONE);
                } else {
                    clearSuggestions.setVisibility(VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        RecyclerView suggestionRecyclerView = widgetPage.findViewById(R.id.suggestionRecyclerView);
        AutoCompleteAdapter suggestionAdapter = new AutoCompleteAdapter(getContext(), this);
        suggestionRecyclerView.setHasFixedSize(true);
        suggestionRecyclerView.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        suggestionRecyclerView.setAdapter(suggestionAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
            getContext(),
            DividerItemDecoration.VERTICAL
        );
        suggestionRecyclerView.addItemDecoration(dividerItemDecoration);
        mLauncher.getCompositeDisposable().add(
            RxTextView.textChanges(mSearchInput)
                .debounce(300, TimeUnit.MILLISECONDS)
                .map(CharSequence::toString)
                .distinctUntilChanged()
                .switchMap(charSequence -> {
                    if (charSequence != null && charSequence.length() > 0) {
                        return searchForQuery(charSequence);
                    } else {
                        return Observable.just(
                            new SuggestionsResult(charSequence));
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    new SearchInputDisposableObserver(mLauncher, suggestionAdapter, widgetPage)
                )
        );

        mSearchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                mLauncher.hideKeyboard(v);
            }
        });

        mSearchInput.setOnEditorActionListener((textView, action, keyEvent) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                mLauncher.hideKeyboard(mSearchInput);
                mLauncher.runSearch(mSearchInput.getText().toString());
                mSearchInput.setText("");
                mSearchInput.clearFocus();
                return true;
            }
            return false;
        });
        // [[END]]

        // Prepare edit widgets button
        findViewById(R.id.edit_widgets_button).setOnClickListener(v -> mLauncher.startActivity(
            new Intent(
                mLauncher,
                WidgetsActivity.class
            )
        ));

        // Prepare weather widget view
        // [[BEGIN]]
        findViewById(R.id.weather_setting_imageview)
            .setOnClickListener(v -> mLauncher.startActivity(
                new Intent(
                    mLauncher,
                    WeatherPreferences.class
                )
            ));

        mWeatherSetupTextView = findViewById(R.id.weather_setup_textview);
        mWeatherPanel = findViewById(R.id.weather_panel);
        mWeatherPanel.setOnClickListener(v -> {
            Intent launchIntent = mLauncher.getPackageManager().getLaunchIntentForPackage(
                "foundation.e.weather");
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mLauncher.startActivity(launchIntent);
            }
        });
        updateWeatherPanel();

        if (WeatherUtils.isWeatherServiceAvailable(mLauncher)) {
            mLauncher.startService(new Intent(mLauncher, WeatherSourceListenerService.class));
            mLauncher.startService(new Intent(mLauncher, DeviceStatusService.class));
        }

        LocalBroadcastManager.getInstance(mLauncher)
            .registerReceiver(mWeatherReceiver, new IntentFilter(
                WeatherUpdateService.ACTION_UPDATE_FINISHED));

        if (!Preferences.useCustomWeatherLocation(mLauncher)) {
            if (!WeatherPreferences.hasLocationPermission(mLauncher)) {
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                mLauncher.requestPermissions(
                    permissions,
                    WeatherPreferences.LOCATION_PERMISSION_REQUEST_CODE
                );
            } else {
                LocationManager lm =
                    (LocationManager) mLauncher.getSystemService(Context.LOCATION_SERVICE);
                if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    && Preferences.getEnableLocation(mLauncher)) {
                    showLocationEnableDialog();
                    Preferences.setEnableLocation(mLauncher);
                } else {
                    mLauncher.startService(new Intent(mLauncher, WeatherUpdateService.class)
                        .setAction(WeatherUpdateService.ACTION_FORCE_UPDATE));
                }
            }
        } else {
            mLauncher.startService(new Intent(mLauncher, WeatherUpdateService.class)
                .setAction(WeatherUpdateService.ACTION_FORCE_UPDATE));
        }
        // [[END]]

        int[] widgetIds = mLauncher.mAppWidgetHost.getAppWidgetIds();
        Arrays.sort(widgetIds);
        for (int id : widgetIds) {
            AppWidgetProviderInfo appWidgetInfo = mLauncher.mAppWidgetManager.getAppWidgetInfo(id);
            if (appWidgetInfo != null) {
                mLauncher.getCompositeDisposable()
                    .add(DatabaseManager.getManager(mLauncher).getHeightOfWidget(id)
                        .subscribeOn(Schedulers.from(AppExecutors.getInstance().diskIO()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(height -> {
                            RoundedWidgetView hostView =
                                (RoundedWidgetView) mLauncher.getMAppWidgetHost().createView(
                                    mLauncher.getApplicationContext(), id,
                                    appWidgetInfo
                                );
                            hostView.setAppWidget(id, appWidgetInfo);
                            addWidgetToContainer(hostView);
                            if (height != 0) {
                                int minHeight = hostView.getAppWidgetInfo().minResizeHeight;
                                int maxHeight =
                                    mLauncher.getDeviceProfile().getAvailableHeightPx() * 3 / 4;
                                int normalisedDifference = (maxHeight - minHeight) / 100;
                                hostView.getLayoutParams().height =
                                    minHeight + (normalisedDifference * height);
                            }
                        }, Throwable::printStackTrace));
            }
        }
    }

    private void addWidgetToContainer(RoundedWidgetView widgetView) {
        widgetView.setPadding(0, 0, 0, 0);
        widgetContainer.addView(widgetView);
    }

    private ObservableSource<SuggestionsResult> searchForQuery(
        CharSequence charSequence
    ) {
        Observable<SuggestionsResult> launcherItems = searchForLauncherItems(
            charSequence.toString()
        ).subscribeOn(Schedulers.io());
        Observable<SuggestionsResult> networkItems = searchForNetworkItems(
            charSequence
        ).subscribeOn(Schedulers.io());
        return launcherItems.mergeWith(networkItems);
    }

    private Observable<SuggestionsResult> searchForLauncherItems(
        CharSequence charSequence
    ) {
        String query = charSequence.toString().toLowerCase();
        SuggestionsResult suggestionsResult = new SuggestionsResult(
            query
        );
        List<LauncherItem> launcherItems = new ArrayList();
        mapOverItems(true, new ItemOperator() {
            @Override
            public boolean evaluate(LauncherItem item, View view, int index) {
                Log.i(TAG, "searchForLauncherItems: ${item.title}");
                if (item.title.toString().toLowerCase(Locale.getDefault()).contains(query)) {
                    launcherItems.add(item);
                }
                return false;
            }
        });
        launcherItems.sort(Comparator.comparing(launcherItem ->
            launcherItem.title.toString().toLowerCase().indexOf(query)
        ));

        if (launcherItems.size() > 4) {
            suggestionsResult.setLauncherItems(launcherItems.subList(0, 4));
        } else {
            suggestionsResult.setLauncherItems(launcherItems);
        }
        return Observable.just(suggestionsResult)
            .onErrorReturn(throwable -> {
                suggestionsResult.setLauncherItems(new ArrayList<>());
                return suggestionsResult;
            });
    }

    private Observable<SuggestionsResult> searchForNetworkItems(CharSequence charSequence) {
        String query = charSequence.toString().toLowerCase(Locale.getDefault()).trim();
        SuggestionProvider suggestionProvider = new SearchSuggestionUtil().getSuggestionProvider(
            mLauncher
        );
        return suggestionProvider.query(query).toObservable();
    }

    @Override
    public void onClick(String suggestion) {
        mSearchInput.setText(suggestion);
        mLauncher.runSearch(suggestion);
        mSearchInput.clearFocus();
        mSearchInput.setText("");
    }

    private BroadcastReceiver mWeatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getBooleanExtra(WeatherUpdateService.EXTRA_UPDATE_CANCELLED, false)) {
                updateWeatherPanel();
            }
        }
    };

    private static final int REQUEST_LOCATION_SOURCE_SETTING = 267;
    private AlertDialog enableLocationDialog;

    public void updateWeatherPanel() {
        if (mWeatherPanel != null) {
            if (Preferences.getCachedWeatherInfo(mLauncher) == null) {
                mWeatherSetupTextView.setVisibility(VISIBLE);
                mWeatherPanel.setVisibility(GONE);
                mWeatherSetupTextView.setOnClickListener(
                    v -> mLauncher.startActivity(
                        new Intent(mLauncher, WeatherPreferences.class)));
                return;
            }
            mWeatherSetupTextView.setVisibility(GONE);
            mWeatherPanel.setVisibility(VISIBLE);
            ForecastBuilder.buildLargePanel(mLauncher, mWeatherPanel,
                Preferences.getCachedWeatherInfo(mLauncher)
            );
        }
    }

    public void showLocationEnableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mLauncher);
        // Build and show the dialog
        builder.setTitle(R.string.weather_retrieve_location_dialog_title);
        builder.setMessage(R.string.weather_retrieve_location_dialog_message);
        builder.setCancelable(false);
        builder.setPositiveButton(
            R.string.weather_retrieve_location_dialog_enable_button,
            (dialog1, whichButton) -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mLauncher.startActivityForResult(intent, REQUEST_LOCATION_SOURCE_SETTING);
            }
        );
        builder.setNegativeButton(R.string.cancel, null);
        enableLocationDialog = builder.create();
        enableLocationDialog.show();
    }

    public void refreshSuggestedApps(boolean forceRefresh) {
        if (widgetPage != null) {
            refreshSuggestedApps(widgetPage, forceRefresh);
        }
    }

    public void refreshSuggestedApps(ViewGroup viewGroup, boolean forceRefresh) {
        TextView openUsageAccessSettingsTv = viewGroup.findViewById(R.id.openUsageAccessSettings);
        GridLayout suggestedAppsGridLayout = viewGroup.findViewById(R.id.suggestedAppGrid);
        AppUsageStats appUsageStats = new AppUsageStats(getContext());
        List<UsageStats> usageStats = appUsageStats.getUsageStats();
        if (usageStats.size() > 0) {
            openUsageAccessSettingsTv.setVisibility(GONE);
            suggestedAppsGridLayout.setVisibility(VISIBLE);

            // Check if usage stats have been changed or not to avoid unnecessary flickering
            if (forceRefresh || mUsageStats == null || mUsageStats.size() != usageStats.size()
                || !ListUtil.areEqualLists(mUsageStats, usageStats)) {
                mUsageStats = usageStats;
                if (suggestedAppsGridLayout.getChildCount() > 0) {
                    suggestedAppsGridLayout.removeAllViews();
                }
                int i = 0;
                while (suggestedAppsGridLayout.getChildCount() < 4 && i < mUsageStats.size()) {
                    ApplicationItem appItem = AppUtils.createAppItem(
                        getContext(),
                        mUsageStats.get(i).getPackageName(),
                        new foundation.e.blisslauncher.core.utils.UserHandle()
                    );
                    if (appItem != null) {
                        BlissFrameLayout view = mLauncher.prepareSuggestedApp(appItem);
                        mLauncher.addAppToGrid(suggestedAppsGridLayout, view);
                    }
                    i++;
                }
            }
        } else {
            openUsageAccessSettingsTv.setVisibility(VISIBLE);
            suggestedAppsGridLayout.setVisibility(GONE);
        }
    }

    public void showWidgetResizeContainer(RoundedWidgetView roundedWidgetView) {
        RelativeLayout widgetResizeContainer = widgetPage.findViewById(
            R.id.widget_resizer_container);
        if (widgetResizeContainer.getVisibility() != VISIBLE) {
            activeRoundedWidgetView = roundedWidgetView;

            SeekBar seekBar = widgetResizeContainer.findViewById(R.id.widget_resizer_seekbar);
            if (currentAnimator != null) {
                currentAnimator.cancel();
            }

            seekBar.setOnTouchListener((v, event) -> {
                seekBar.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });

            AnimatorSet set = new AnimatorSet();
            set.play(ObjectAnimator.ofFloat(widgetResizeContainer, View.Y,
                mLauncher.getDeviceProfile().getAvailableHeightPx(),
                mLauncher.getDeviceProfile().getAvailableHeightPx() - Utilities
                    .pxFromDp(48, getContext())
            ));
            set.setDuration(200);
            set.setInterpolator(new LinearInterpolator());
            set.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    super.onAnimationStart(animation);
                                    widgetResizeContainer.setVisibility(VISIBLE);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    super.onAnimationCancel(animation);
                                    currentAnimator = null;
                                    widgetResizeContainer.setVisibility(GONE);
                                    roundedWidgetView.removeBorder();
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    currentAnimator = null;
                                    prepareWidgetResizeSeekBar(seekBar);
                                    roundedWidgetView.addBorder();
                                }
                            }
            );
            set.start();
            currentAnimator = set;
        }
    }

    private void prepareWidgetResizeSeekBar(SeekBar seekBar) {
        int minHeight = activeRoundedWidgetView.getAppWidgetInfo().minResizeHeight;
        int maxHeight = mLauncher.getDeviceProfile().getAvailableHeightPx() * 3 / 4;
        int normalisedDifference = (maxHeight - minHeight) / 100;
        int defaultHeight = activeRoundedWidgetView.getHeight();
        int currentProgress = (defaultHeight - minHeight) * 100 / (maxHeight - minHeight);

        seekBar.setMax(100);
        seekBar.setProgress(currentProgress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newHeight = minHeight + (normalisedDifference * progress);
                LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) activeRoundedWidgetView.getLayoutParams();
                layoutParams.height = newHeight;
                activeRoundedWidgetView.setLayoutParams(layoutParams);

                Bundle newOps = new Bundle();
                newOps.putInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                    mLauncher.getDeviceProfile().getMaxWidgetWidth()
                );
                newOps.putInt(
                    AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                    mLauncher.getDeviceProfile().getMaxWidgetWidth()
                );
                newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, newHeight);
                newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, newHeight);
                activeRoundedWidgetView.updateAppWidgetOptions(newOps);
                activeRoundedWidgetView.requestLayout();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DatabaseManager.getManager(getContext()).saveWidget(
                    activeRoundedWidgetView.getAppWidgetId(), seekBar.getProgress());
            }
        });
    }

    public void hideWidgetResizeContainer() {
        if(widgetPage != null) {
            RelativeLayout widgetResizeContainer = widgetPage.findViewById(
                R.id.widget_resizer_container);
            if (widgetResizeContainer.getVisibility() == VISIBLE) {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator.ofFloat(widgetResizeContainer, View.Y,
                    mLauncher.getDeviceProfile().getAvailableHeightPx()
                ));
                set.setDuration(200);
                set.setInterpolator(new LinearInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                        super.onAnimationStart(animation);
                                        ((SeekBar) widgetPage.findViewById(
                                            R.id.widget_resizer_seekbar)).setOnSeekBarChangeListener(null);
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                        super.onAnimationCancel(animation);
                                        currentAnimator = null;
                                        widgetResizeContainer.setVisibility(VISIBLE);
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        currentAnimator = null;
                                        widgetResizeContainer.setVisibility(GONE);
                                        activeRoundedWidgetView.removeBorder();
                                    }
                                }
                );
                set.start();
                currentAnimator = set;
            }
        }

    }

    private int[] findSpaceForItem(IntegerArray addedWorkspaceScreensFinal) {
        // Find appropriate space for the item.
        int screenId = 0;
        int cell = 0;
        boolean found = false;

        int screenCount = getChildCount();
        for (int screen = 1; screen < screenCount; screen++) {
            View child = getChildAt(screen);
            if (child instanceof CellLayout) {
                CellLayout cellLayout = (CellLayout) child;
                int index = mWorkspaceScreens.indexOfValue(cellLayout);
                screenId = mWorkspaceScreens.keyAt(index);
                if (cellLayout.getChildCount() < cellLayout.getMaxChildCount()) {
                    found = true;
                    cell = cellLayout.getChildCount();
                    break;
                }
            }
        }

        if (!found) {
            screenId = screenId + 1;
            addedWorkspaceScreensFinal.add(screenId);
            cell = 0;
        }
        return new int[]{screenId, cell};
    }

    /**
     * Removes all folder listeners
     */
    public void removeFolderListeners() {
        mapOverItems(false, (info, view, index) -> {
            if (view instanceof FolderIcon) {
                ((FolderIcon) view).removeListeners();
            }
            return false;
        });
    }

    /**
     * Returns true if the shortcuts already exists on the workspace. This must be called after
     * the workspace has been loaded. We identify a shortcut by its intent.
     */
    protected boolean shortcutExists(Intent intent, UserHandle user) {
        final String compPkgName, intentWithPkg, intentWithoutPkg;
        if (intent == null) {
            // Skip items with null intents
            return true;
        }
        if (intent.getComponent() != null) {
            // If component is not null, an intent with null package will produce
            // the same result and should also be a match.
            compPkgName = intent.getComponent().getPackageName();
            if (intent.getPackage() != null) {
                intentWithPkg = intent.toUri(0);
                intentWithoutPkg = new Intent(intent).setPackage(null).toUri(0);
            } else {
                intentWithPkg = new Intent(intent).setPackage(compPkgName).toUri(0);
                intentWithoutPkg = intent.toUri(0);
            }
        } else {
            compPkgName = null;
            intentWithPkg = intent.toUri(0);
            intentWithoutPkg = intent.toUri(0);
        }

        boolean isLauncherAppTarget = Utilities.isLauncherAppTarget(intent);

        for (CellLayout layout : getWorkspaceAndHotseatCellLayouts()) {
            // map over all the shortcuts on the workspace
            final int itemCount = layout.getChildCount();
            for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
                View item = layout.getChildAt(itemIdx);
                LauncherItem info = (LauncherItem) item.getTag();
                if (info instanceof FolderItem) {
                    FolderItem folder = (FolderItem) info;
                    List<LauncherItem> folderChildren = folder.items;
                    // map over all the children in the folder
                    final int childCount = folder.items.size();
                    for (int childIdx = 0; childIdx < childCount; childIdx++) {
                        LauncherItem childItem = folderChildren.get(childIdx);
                        if (childItem.getIntent() != null && childItem.user.equals(user)) {
                            Intent copyIntent = new Intent(childItem.getIntent());
                            copyIntent.setSourceBounds(intent.getSourceBounds());
                            String s = copyIntent.toUri(0);
                            if (intentWithPkg.equals(s) || intentWithoutPkg.equals(s)) {
                                return true;
                            }

                            // checking for existing promise icon with same package name
                            if (isLauncherAppTarget
                                && childItem.getTargetComponent() != null
                                && compPkgName != null
                                && compPkgName
                                .equals(childItem.getTargetComponent().getPackageName())) {
                                return true;
                            }
                        }
                    }
                } else {
                    if (info.getIntent() != null && info.user.equals(user)) {
                        Intent copyIntent = new Intent(info.getIntent());
                        copyIntent.setSourceBounds(intent.getSourceBounds());
                        String s = copyIntent.toUri(0);
                        if (intentWithPkg.equals(s) || intentWithoutPkg.equals(s)) {
                            return true;
                        }

                        // checking for existing promise icon with same package name
                        if (isLauncherAppTarget
                            && info.getTargetComponent() != null
                            && compPkgName != null
                            && compPkgName.equals(info.getTargetComponent().getPackageName())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = new PropertyListBuilder().alpha(1).scale(1).build(v)
            .setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
        bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        return bounceAnim;
    }

    public GridLayout insertNewWorkspaceScreen(int screenId) {
        return insertNewWorkspaceScreen(screenId, getChildCount());
    }

    public GridLayout insertNewWorkspaceScreen(int screenId, int insertIndex) {
        if (mWorkspaceScreens.containsKey(screenId)) {
            throw new RuntimeException("Screen id " + screenId + " already exists!");
        }

        // Inflate the cell layout, but do not add it automatically so that we can get the newly
        // created CellLayout.
        CellLayout newScreen = (CellLayout) LayoutInflater.from(getContext()).inflate(
            R.layout.workspace_screen, this, false /* attachToRoot */);
        int paddingLeftRight = mLauncher.getDeviceProfile().getCellLayoutPaddingLeftRightPx();
        int paddingBottom = mLauncher.getDeviceProfile().getCellLayoutBottomPaddingPx() + mLauncher
            .getDeviceProfile().getWorkspacePadding().bottom;
        newScreen.setPadding(paddingLeftRight, 0, paddingLeftRight, paddingBottom);
        newScreen.setRowCount(mLauncher.getDeviceProfile().getInv().getNumRows());
        newScreen.setColumnCount(mLauncher.getDeviceProfile().getInv().getNumColumns());

        mWorkspaceScreens.put(screenId, newScreen);
        mScreenOrder.add(insertIndex - 1, screenId);
        addView(newScreen, insertIndex);

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
        int finalScreenId = mScreenOrder.get(mScreenOrder.size() - 1);

        CellLayout finalScreen = mWorkspaceScreens.get(finalScreenId);

        // If the final screen is empty, convert it to the extra empty screen
        if (finalScreen.getChildCount() == 0) {
            mWorkspaceScreens.remove(finalScreenId);
            mScreenOrder.removeValue(finalScreenId);

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
                mScreenOrder.removeValue(EXTRA_EMPTY_SCREEN_ID);
                removeView(cl);
                if (stripEmptyScreens) {
                    stripEmptyScreens();
                }
                // Update the page indicator to reflect the removed page.
                showPageIndicatorAtCurrentScroll();
                handleWidgetPageTransition();
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
        mScreenOrder.removeValue(EXTRA_EMPTY_SCREEN_ID);

        int newId = mScreenOrder.size();
        mWorkspaceScreens.put(newId, cl);
        mScreenOrder.add(newId);

        // Update the model for the new screen
        //TODO: LauncherModel.updateWorkspaceScreenOrder(mLauncher, mScreenOrder);

        return newId;
    }

    public CellLayout getScreenWithId(int screenId) {
        return mWorkspaceScreens.get(screenId);
    }

    public int getIdForScreen(CellLayout layout) {
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

    public int getPageIndexForScreenId(int screenId) {
        return indexOfChild(mWorkspaceScreens.get(screenId));
    }

    public int getScreenIdForPageIndex(int index) {
        if (0 <= index && index < mScreenOrder.size()) {
            return mScreenOrder.get(index);
        }
        return -1;
    }

    public IntegerArray getScreenOrder() {
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
        IntegerArray removeScreens = new IntegerArray();
        int total = mWorkspaceScreens.size();
        for (int i = 0; i < total; i++) {
            int id = mWorkspaceScreens.keyAt(i);
            GridLayout cl = mWorkspaceScreens.valueAt(i);
            if (id > FIRST_SCREEN_ID && cl.getChildCount() == 0) {
                removeScreens.add(id);
            }
        }

        // We enforce at least one page to add new items to. In the case that we remove the last
        // such screen, we convert the last screen to the empty screen
        int minScreens = 1;

        int pageShift = 0;
        for (int i = 0; i < removeScreens.size(); i++) {
            int id = removeScreens.get(i);
            CellLayout cl = mWorkspaceScreens.get(id);
            mWorkspaceScreens.remove(id);
            mScreenOrder.removeValue(id);

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
            updateDatabase();
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
     * @param animate  If the view should start to animate after drag and drop.
     */
    private void addInScreen(
        View child,
        long container,
        int screenId,
        int x,
        int y,
        boolean animate
    ) {
        int index = y * mLauncher.getDeviceProfile().getInv().getNumColumns() + x;
        addInScreen(
            child,
            container,
            screenId,
            index
        );

        post(() -> {
            if (animate) {
                if (index % 2 == 0) {
                    child.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.wobble));
                } else {
                    child.startAnimation(AnimationUtils
                        .loadAnimation(getContext(), R.anim.wobble_reverse));
                }

                LauncherItem info = (LauncherItem) child.getTag();
                if (child instanceof IconTextView && (info instanceof ApplicationItem || info instanceof ShortcutItem) && !UninstallHelper.INSTANCE
                    .isUninstallDisabled(info.user.getRealHandle(), getContext())) {
                    // Return early if this app is system app
                    if (info instanceof ApplicationItem) {
                        ApplicationItem applicationItem = (ApplicationItem) info;
                        if (applicationItem.isSystemApp != ApplicationItem.FLAG_SYSTEM_UNKNOWN) {
                            if ((applicationItem.isSystemApp & ApplicationItem.FLAG_SYSTEM_NO) != 0) {
                                ((IconTextView) child).applyUninstallIconState(true);
                            }
                        } else {
                            ((IconTextView) child).applyUninstallIconState(true);
                        }
                    } else if (info instanceof ShortcutItem) {
                        ((IconTextView) child).applyUninstallIconState(true);
                    }
                }
            }
        });
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child    The child to add in one of the workspace's screens.
     * @param screenId The screen in which to add the child.
     * @param index    The index of the child in grid.
     */
    private void addInScreen(View child, long container, int screenId, int index) {
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
        } else {
            layout = getScreenWithId(screenId);
        }

        // It helps in recovering from situation when a layout is not saved correctly.
        // TODO: Figure out when it can happen.
        if (index > layout.getChildCount()) {
            index = layout.getChildCount();
        }

        ViewGroup.LayoutParams genericLp = child.getLayoutParams();
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
        CellLayout.LayoutParams lp;
        if (!(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new GridLayout.LayoutParams(rowSpec, colSpec);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.rowSpec = rowSpec;
            lp.columnSpec = colSpec;
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
        addInScreen(child, info.container, info.screenId, info.cell);
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
    protected void notifyPageSwitchListener(int prevPage) {
        super.notifyPageSwitchListener(prevPage);
        handleWidgetPageTransition();
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
                    final View grid = getChildAt(i);
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
                final View layout = getPageAt(i);
                // enable layers between left and right screen inclusive.
                boolean enableLayer = leftScreen <= i && i <= rightScreen;
                layout.setLayerType(enableLayer ? LAYER_TYPE_HARDWARE : LAYER_TYPE_NONE, sPaint);
            }
        }
    }

    public void onWallpaperTap(MotionEvent ev) {
        setWobbleExpirationAlarm(0); // Dismiss any animation if running.
        final int[] position = mTempXY;
        getLocationOnScreen(position);

        int pointerIndex = ev.getActionIndex();
        position[0] += (int) ev.getX(pointerIndex);
        position[1] += (int) ev.getY(pointerIndex);

        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
            ev.getAction() == MotionEvent.ACTION_UP
                ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP,
            position[0], position[1], 0, null
        );
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
        Folder openFolder = Folder.Companion.getOpen(mLauncher);
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
        return result;
    }

    @Override
    public boolean scrollRight() {
        boolean result = false;
        if (!workspaceInModalState() && !mIsSwitchingState) {
            result = super.scrollRight();
        }
        Folder openFolder = Folder.Companion.getOpen(mLauncher);
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
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
            int currentScrollX = getScrollX();
            int firstPageScroll = getScrollForPage(1);
            if (currentScrollX <= firstPageScroll) {
                handleWidgetPageTransition(currentScrollX, firstPageScroll);
            }
        }

        //updatePageAlphaValues();
        enableHwLayersOnVisiblePages();
    }

    private void handleWidgetPageTransition() {
        handleWidgetPageTransition(getScrollX(), getScrollForPage(1));
    }

    private void handleWidgetPageTransition(int currentScrollX, int firstPageScroll) {
        if (currentScrollX > firstPageScroll) {
            currentScrollX = firstPageScroll;
        }
        float scroll = 1 - (float) currentScrollX / firstPageScroll;
        float offset = 0f;

        scroll = Math.max(scroll - offset, 0);
        scroll = Math.min(1, scroll / (1 - offset));

        float transX = getHotseat().getMeasuredWidth() * scroll;

        if (mIsRtl) {
            transX = -transX;
        }
        //mOverlayTranslation = transX;

        // TODO(adamcohen): figure out a final effect here. We may need to recommend
        // different effects based on device performance. On at least one relatively high-end
        // device I've tried, translating the launcher causes things to get quite laggy.
        float finalTransX = transX;
        float finalScroll = scroll;
        post(() -> {
            getHotseat().setTranslationX(finalTransX);
            getHotseat().changeBlurBounds(finalScroll, true);
            getPageIndicator().setTranslationX(finalTransX);
        });
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
            mLauncherOverlay.onScrollChange(mLastOverlayScroll, true, mIsRtl);
        } else {
            dampedOverScroll(amount);
        }

        if (shouldZeroOverlay) {
            mLauncherOverlay.onScrollChange(0, true, mIsRtl);
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
        if (mDragInfo != null) {
            ViewParent parent = mDragInfo.getCell().getParent();
            if (parent instanceof CellLayout) {
                ((CellLayout) parent).markCellsAsUnoccupiedForView(mDragInfo.getCell());
            }
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
     * Unbinds the view for the specified item, and removes the item and all its children.
     *
     * @param v        the view being removed.
     * @param itemInfo the {@link LauncherItem} for this view.
     */
    public boolean removeItem(View v, final LauncherItem itemInfo) {
        if (itemInfo instanceof ApplicationItem || itemInfo instanceof ShortcutItem) {
            // Remove the shortcut from the folder before removing it from launcher
            View folderIcon = getHomescreenIconByItemId(String.valueOf(itemInfo.container));
            if (folderIcon instanceof FolderIcon) {
                ((FolderItem) folderIcon.getTag()).remove(itemInfo, true);
            } else {
                removeWorkspaceItem(v);
            }
            updateDatabase();
        } else if (itemInfo instanceof FolderItem) {
            if (v instanceof FolderIcon) {
                ((FolderIcon) v).removeListeners();
            }
            removeWorkspaceItem(v);
            updateDatabase();
        } else {
            return false;
        }
        return true;
    }

    public View getHomescreenIconByItemId(final String id) {
        return getFirstMatch((info, v, idx) -> info != null && info.id == id);
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
        for (int screen = 1; screen < screenCount; screen++) {
            layouts.add(((CellLayout) getChildAt(screen)));
        }
        if (mLauncher.getHotseat() != null) {
            layouts.add(mLauncher.getHotseat().getLayout());
        }
        return layouts;
    }

    @Override
    public boolean isDropEnabled() {
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
            // onDropExternal(touchXY, dropTargetLayout, d);
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
                int screenId = (mTargetCell[0] < 0) ?
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
                            cell.clearAnimation();
                            parentCell.removeView(cell);
                        } else if (BuildConfig.DEBUG) {
                            throw new NullPointerException("mDragInfo.cell has null parent");
                        }

                        addInScreen(
                            cell,
                            container,
                            screenId,
                            mTargetCell[0],
                            mTargetCell[1],
                            true
                        );
                    }

                    // update the item's position after drop
                    /*CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    lp.cellX = lp.tmpCellX = mTargetCell[0];
                    lp.cellY = lp.tmpCellY = mTargetCell[1];
                    lp.cellHSpan = item.spanX;
                    lp.cellVSpan = item.spanY;
                    lp.isLockedToGrid = true;*/

                    updateDatabase(getWorkspaceAndHotseatCellLayouts());
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
                    mLauncher.getStateManager().goToState(NORMAL);
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

    private void updateDatabase(ArrayList<CellLayout> layouts) {
        List<LauncherItem> items = new ArrayList<>();
        for (int i = 0; i < layouts.size(); i++) {
            CellLayout cellLayout = layouts.get(i);
            int screenId = getIdForScreen(cellLayout);
            long container;
            if (cellLayout.getContainerType() == CellLayout.WORKSPACE) {
                container = Constants.CONTAINER_DESKTOP;
            } else if (cellLayout.getContainerType() == CellLayout.HOTSEAT) {
                container = Constants.CONTAINER_HOTSEAT;
            } else {
                throw new IllegalArgumentException(
                    "Container type can only be CellLayout.WORKSPACE or CellLayout.HOTSEAT");
            }
            for (int j = 0; j < cellLayout.getChildCount(); j++) {
                LauncherItem launcherItem = (LauncherItem) cellLayout.getChildAt(
                    j).getTag();
                launcherItem.cell = j;
                launcherItem.screenId = screenId;
                launcherItem.container = container;
                if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                    FolderItem folderItem = (FolderItem) launcherItem;
                    items.add(folderItem);
                    for (int k = 0; k < folderItem.items.size(); k++) {
                        LauncherItem item = folderItem.items.get(k);
                        item.container = Long.parseLong(folderItem.id);
                        item.cell = k;
                        items.add(item);
                    }
                } else {
                    items.add(launcherItem);
                }
            }
        }
        DatabaseManager.getManager(getContext()).saveItems(items);
    }

    boolean createUserFolderIfNecessary(
        View newView, long container, CellLayout target,
        int[] targetCell, float distance, boolean external, DragView dragView
    ) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View targetView = target.getChildAt(targetCell[0], targetCell[1]);

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            CellLayout cellParent = getParentCellLayoutForView(mDragInfo.getCell());
            hasntMoved = (mDragInfo.getRank() % cellParent.getMCountX() == targetCell[0] &&
                mDragInfo.getRank() / cellParent
                    .getMCountX() == targetCell[1]) && (cellParent == target);
        }

        if (targetView == null || hasntMoved || !mCreateUserFolderOnDrop) return false;
        mCreateUserFolderOnDrop = false;
        final int screenId = getIdForScreen(target);

        boolean aboveShortcut =
            (targetView.getTag() instanceof ApplicationItem) || (targetView
                .getTag() instanceof ShortcutItem);
        boolean willBecomeShortcut =
            (targetView.getTag() instanceof ApplicationItem) || (targetView
                .getTag() instanceof ShortcutItem);

        if (aboveShortcut && willBecomeShortcut) {
            LauncherItem sourceItem = (LauncherItem) newView.getTag();
            LauncherItem destItem = (LauncherItem) targetView.getTag();

            // if the drag started here, we need to remove it from the workspace
            if (!external) {
                getParentCellLayoutForView(mDragInfo.getCell()).removeView(mDragInfo.getCell());
            }
            targetView.clearAnimation();
            target.removeView(targetView);

            final FolderItem fi = new FolderItem();
            fi.title = getResources().getString(R.string.untitled);
            fi.id = String.valueOf(System.currentTimeMillis());
            fi.items = new ArrayList<>();
            fi.container = destItem.container;
            fi.screenId = destItem.screenId;
            fi.cell = destItem.cell;

            // Create the view
            FolderIcon newFolder = FolderIcon.Companion.fromXml(R.layout.folder_icon, target, fi);
            addInScreen(newFolder, fi);
            // Force measure the new folder icon
            CellLayout parent = getParentCellLayoutForView(newFolder);
            parent.measureChild(newFolder);
            sourceItem.container = Long.parseLong(fi.id);
            destItem.container = Long.parseLong(fi.id);
            sourceItem.screenId = -1;
            destItem.screenId = -1;
            destItem.cell = 0;
            sourceItem.cell = 1;
            newFolder.addItem(destItem);
            newFolder.addItem(sourceItem);

            updateDatabase(getWorkspaceAndHotseatCellLayouts());

            // Clear drag view
            dragView.remove();
            dragView = null;
            invalidate();
            post(() -> {
                if (fi.cell % 2 == 0) {
                    newFolder
                        .startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.wobble));
                } else {
                    newFolder.startAnimation(AnimationUtils
                        .loadAnimation(getContext(), R.anim.wobble_reverse));
                }
                newFolder.applyUninstallIconState(false);
            });
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

        if (dropOverView instanceof FolderIcon) {
            FolderIcon folderIcon = (FolderIcon) dropOverView;
            if (folderIcon.acceptDrop()) {
                LauncherItem item = d.dragInfo;
                folderIcon.addItem(item);
            }
            // if the drag started here, we need to remove it from the workspace
            if (!external) {
                getParentCellLayoutForView(mDragInfo.getCell()).removeView(mDragInfo.getCell());
            }
            //Add animation here.
            d.dragView.remove();
            d.dragView = null;
            updateDatabase(getWorkspaceAndHotseatCellLayouts());
            return true;
        }
        return false;
    }

    public void onNoCellFound(View dropTargetLayout) {
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
        if (layout == null && nextPage >= 1 && nextPage < getPageCount()) {
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
                //cleanupFolderCreation();
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
        if (pageNo >= 1 && pageNo < getPageCount()) {
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
        if (mDragTargetLayout != null && child != null) {
            // We want the point to be mapped to the dragTarget.
            if (mLauncher.isHotseatLayout(mDragTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(mDragTargetLayout, mDragViewVisualCenter);
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

            manageFolderFeedback(mDragTargetLayout, mTargetCell, targetCellDistance, d);

            boolean nearestDropOccupied = mDragTargetLayout.isNearestDropLocationOccupied((int)
                mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], child, mTargetCell);

            if (!nearestDropOccupied) {
                mDragTargetLayout.visualizeDropLocation(child, mOutlineProvider,
                    mTargetCell[0], mTargetCell[1], false, d
                );
            } else if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
                && (mLastReorderX != reorderX || mLastReorderY != reorderY)) {
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
        setWobbleExpirationAlarm(WOBBLE_EXPIRATION_TIMEOUT);
    }

    /**
     * @param timeoutInMillis Expire wobble animation after the given timeout
     * @return true if there was any pending alarm otherwise false.
     */
    public boolean setWobbleExpirationAlarm(long timeoutInMillis) {
        boolean alarmPending = wobbleExpireAlarm.alarmPending();
        wobbleExpireAlarm.cancelAlarm();
        wobbleExpireAlarm.setAlarm(timeoutInMillis);
        return alarmPending;
    }

    public boolean isWobbling() {
        return wobbleExpireAlarm.alarmPending();
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        CellLayout dropTargetLayout = mDropToLayout;
        if (d.dragSource != this) {
            if (dropTargetLayout == null) {
                return false;
            }
            if (!transitionStateShouldAllowDrop()) return false;

            mDragViewVisualCenter = d.getVisualCenter(mDragViewVisualCenter);

            // We want the point to be mapped to the dragTarget.
            mapPointFromDropLayout(dropTargetLayout, mDragViewVisualCenter);

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], dropTargetLayout,
                mTargetCell
            );
            float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                mDragViewVisualCenter[1], mTargetCell
            );
            if (mCreateUserFolderOnDrop && willCreateUserFolder(d.dragInfo,
                dropTargetLayout, mTargetCell, distance, true
            )) {
                return true;
            }

            if (mAddToExistingFolderOnDrop && willAddToExistingUserFolder(d.dragInfo,
                dropTargetLayout, mTargetCell, distance
            )) {
                return true;
            }

            int[] resultSpan = new int[2];
            mTargetCell = dropTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], 1, 1, 1, 1,
                null, mTargetCell, resultSpan, CellLayout.MODE_ACCEPT_DROP
            );
            boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;

            // Don't accept the drop if there's no room for the item
            if (!foundCell) {
                onNoCellFound(dropTargetLayout);
                return false;
            }
        }
        long screenId = getIdForScreen(dropTargetLayout);
        if (screenId == EXTRA_EMPTY_SCREEN_ID) {
            commitExtraEmptyScreen();
        }
        return true;
    }

    /**
     * Updates the point in {@param xy} to point to the co-ordinate space of {@param layout}
     *
     * @param layout either hotseat of a page in workspace
     * @param xy     the point location in workspace co-ordinate space
     */
    private void mapPointFromDropLayout(CellLayout layout, float[] xy) {
        if (mLauncher.isHotseatLayout(layout)) {
            mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, xy, true);
            mLauncher.getDragLayer().mapCoordInSelfToDescendant(layout, xy);
        } else {
            mapPointFromSelfToChild(layout, xy);
        }
    }

    private boolean transitionStateShouldAllowDrop() {
        return (!isSwitchingState() || mTransitionProgress > ALLOW_DROP_TRANSITION_PROGRESS) &&
            workspaceIconsCanBeDragged();
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

        VariantDeviceProfile grid = mLauncher.getDeviceProfile();
        Point dragVisualizeOffset = null;
        Rect dragRect = null;
        if (child instanceof IconTextView) {
            dragRect =
                ((IconTextView) child).getIconBounds();
            dragLayerY += dragRect.top;

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
        if (dropOverView != null && dropOverView.getTag() instanceof FolderItem) {
            return true;
        }
        return false;
    }

    private void manageFolderFeedback(
        CellLayout targetLayout,
        int[] targetCell, float distance, DragObject dragObject
    ) {
        if (distance > mMaxDistanceForFolderCreation) return;

        final View dragOverView = mDragTargetLayout.getChildAt(mTargetCell[0], mTargetCell[1]);
        LauncherItem info = dragObject.dragInfo;

        // Return early in case of dragged item is a folder because we don't support nested folders.
        if (info instanceof FolderItem) return;

        boolean userFolderPending = willCreateUserFolder(info, dragOverView, false);
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
                packageAndUser
                    .evaluate(info, view, index) && info.itemType == ITEM_TYPE_APPLICATION;

        return getFirstMatch(
            new CellLayout[]{mLauncher.getHotseat(), currentPage},
            packageAndUserAndApp
        );
    }

    public View getFirstMatch(final ItemOperator operator) {
        final View[] value = new View[1];
        mapOverItems(MAP_NO_RECURSE, (info, v, index) -> {
            if (operator.evaluate(info, v, index)) {
                value[0] = v;
                return true;
            }
            return false;
        });
        return value[0];
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
        Folder folder = Folder.Companion.getOpen(mLauncher);
        if (folder != null && !folder.isDestroyed()) {
            for (int i = 0; i < folder.getContent().getChildCount(); i++) {
                GridLayout grid = (GridLayout) folder.getContent().getChildAt(i);
                if (mapOverCellLayout(recurse, grid, op)) {
                    return;
                }
            }
        }
    }

    private boolean mapOverCellLayout(boolean recurse, GridLayout layout, ItemOperator op) {
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
                    if (op.evaluate(childItem, item, itemIdx)) {
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
                    ((IconTextView) v).applyDotState(info, true);
                    folderIds.add(String.valueOf(info.container));
                }
            }
            // process all the shortcuts
            return false;
        });

        // Update folder icons
        mapOverItems(MAP_NO_RECURSE, (info, v, itemIdx) -> {
            if (info instanceof FolderItem && folderIds.contains(info.id)
                && v instanceof FolderIcon) {
                FolderDotInfo folderDotInfo = new FolderDotInfo();
                for (LauncherItem si : ((FolderItem) info).items) {
                    folderDotInfo.addDotInfo(mLauncher.getDotInfoForItem(si));
                }
                ((FolderIcon) v).setDotInfo(folderDotInfo);
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

        // TODO(adamcohen): figure out a final effect here. We may need to recommend
        // different effects based on device performance. On at least one relatively high-end
        // device I've tried, translating the launcher causes things to get quite laggy.
        getHotseat().setTranslationX(transX);
        getHotseat().getAlphaProperty(0).setValue(alpha);
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
        Animation reverseWobbleAnimation =
            AnimationUtils.loadAnimation(getContext(), R.anim.wobble_reverse);
        mapOverItems(MAP_NO_RECURSE, (info, v, itemIdx) -> {
            if ((info instanceof ApplicationItem || info instanceof ShortcutItem)
                && v instanceof IconTextView
                && !UninstallHelper.INSTANCE
                .isUninstallDisabled(info.user.getRealHandle(), getContext())) {

                // Return early if this app is system app
                if (info instanceof ApplicationItem) {
                    ApplicationItem applicationItem = (ApplicationItem) info;
                    if (applicationItem.isSystemApp != ApplicationItem.FLAG_SYSTEM_UNKNOWN) {
                        if ((applicationItem.isSystemApp & ApplicationItem.FLAG_SYSTEM_NO) != 0) {
                            ((IconTextView) v).applyUninstallIconState(true);
                        }
                    } else {
                        ((IconTextView) v).applyUninstallIconState(true);
                    }
                } else {
                    ((IconTextView) v).applyUninstallIconState(true);
                }
            }

            if (itemIdx % 2 == 0) {
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
     *
     * @param alarm
     */
    @Override
    public void onAlarm(Alarm alarm) {
        // Adds uninstall icon.
        mapOverItems(MAP_NO_RECURSE, (info, v, idx) -> {
            if (v instanceof IconTextView) {
                ((IconTextView) v).applyUninstallIconState(false);
            }

            // Clears if there is any running animation on the view.
            v.clearAnimation();

            // process all the items
            return false;
        });
    }

    public void computeScrollWithoutInvalidation() {
        computeScrollHelper(false);
    }

    /**
     * Removes items that match the {@param matcher}. When applications are removed
     * as a part of an update, this is called to ensure that other widgets and application
     * shortcuts are not removed.
     */
    public void removeItemsByMatcher(@NotNull LauncherItemMatcher matcher) {
        for (final CellLayout layout : getWorkspaceAndHotseatCellLayouts()) {

            HashMap<String, View> idToViewMap = new HashMap<>();
            ArrayList<LauncherItem> items = new ArrayList<>();
            for (int j = 0; j < layout.getChildCount(); j++) {
                final View view = layout.getChildAt(j);
                if (view.getTag() instanceof LauncherItem) {
                    LauncherItem item = (LauncherItem) view.getTag();
                    items.add(item);
                    idToViewMap.put(item.id, view);
                }
            }

            for (LauncherItem itemToRemove : matcher.filterItemInfos(items)) {
                View child = idToViewMap.get(itemToRemove.id);

                if (child != null) {
                    // Note: We can not remove the view directly from CellLayoutChildren as this
                    // does not re-mark the spaces as unoccupied.
                    child.clearAnimation();
                    layout.removeViewInLayout(child);
                    if (child instanceof DropTarget) {
                        mDragController.removeDropTarget((DropTarget) child);
                    }
                    // Remove item from database
                    DatabaseManager.getManager(getContext()).removeItem(itemToRemove.id);
                } else if (itemToRemove.container >= 0) {
                    // The item may belong to a folder.
                    View parent = idToViewMap.get(String.valueOf(itemToRemove.container));
                    if (parent != null) {
                        FolderItem folder = (FolderItem) parent.getTag();
                        parent.clearAnimation();
                        // Close folder before making any changes
                        // mLauncher.closeFolder();
                        folder.items.remove(itemToRemove);
                        DatabaseManager.getManager(getContext()).removeItem(itemToRemove.id);
                        if (folder.items.size() == 0) {
                            layout.removeView(parent);
                            if (parent instanceof DropTarget) {
                                mDragController.removeDropTarget((DropTarget) child);
                            }
                        } else if (folder.items.size() == 1) {
                            LauncherItem lastFolderItem = folder.items.get(0);
                            layout.removeViewInLayout(parent);
                            if (parent instanceof DropTarget) {
                                mDragController.removeDropTarget((DropTarget) parent);
                            }
                            lastFolderItem.container = folder.container;
                            lastFolderItem.cell = folder.cell;
                            lastFolderItem.screenId = folder.screenId;
                            bindItems(Collections.singletonList(lastFolderItem), true);
                        } else {
                            folder.icon = new GraphicsUtil(getContext())
                                .generateFolderIcon(getContext(), folder);
                            layout.removeViewInLayout(parent);
                            if (parent instanceof DropTarget) {
                                mDragController.removeDropTarget((DropTarget) parent);
                            }
                            bindItems(Collections.singletonList(folder), true);
                        }
                    }
                }
            }
        }

        // Strip all the empty screens
        stripEmptyScreens();
        updateDatabase(getWorkspaceAndHotseatCellLayouts());
    }

    public void clearWidgetState() {
        if (activeRoundedWidgetView != null && activeRoundedWidgetView.isWidgetActivated()) {
            hideWidgetResizeContainer();
        }

        if (mSearchInput != null) {
            mSearchInput.setText("");
        }
    }

    public void updateWidgets() {
        if (widgetContainer != null) {
            WidgetManager widgetManager = WidgetManager.getInstance();
            Integer id = widgetManager.dequeRemoveId();
            while (id != null) {
                for (int i = 0; i < widgetContainer.getChildCount(); i++) {
                    if (widgetContainer.getChildAt(i) instanceof RoundedWidgetView) {
                        RoundedWidgetView appWidgetHostView =
                            (RoundedWidgetView) widgetContainer.getChildAt(i);
                        if (appWidgetHostView.getAppWidgetId() == id) {
                            widgetContainer.removeViewAt(i);
                            DatabaseManager.getManager(mLauncher).removeWidget(id);
                            break;
                        }
                    }
                }
                id = widgetManager.dequeRemoveId();
            }

            RoundedWidgetView widgetView = widgetManager.dequeAddWidgetView();
            while (widgetView != null) {
                widgetView = WidgetViewBuilder.create(mLauncher, widgetView);
                addWidgetToContainer(widgetView);
                widgetView = widgetManager.dequeAddWidgetView();
            }
        }
    }

    public interface ItemOperator {
        /**
         * Process the next itemInfo, possibly with side-effect on the next item.
         *
         * @param info  info for the shortcut
         * @param view  view for the shortcut
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
