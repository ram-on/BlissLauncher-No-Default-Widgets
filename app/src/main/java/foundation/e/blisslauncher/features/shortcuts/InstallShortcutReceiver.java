package foundation.e.blisslauncher.features.shortcuts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.core.IconsHandler;
import foundation.e.blisslauncher.core.UserManagerCompat;
import foundation.e.blisslauncher.core.Utilities;
import foundation.e.blisslauncher.core.database.model.ApplicationItem;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.ShortcutItem;
import foundation.e.blisslauncher.core.events.EventRelay;
import foundation.e.blisslauncher.core.events.ShortcutAddEvent;
import foundation.e.blisslauncher.core.utils.Constants;
import foundation.e.blisslauncher.core.utils.PackageManagerHelper;
import foundation.e.blisslauncher.core.utils.Preconditions;
import foundation.e.blisslauncher.core.utils.UserHandle;
import foundation.e.blisslauncher.features.test.InvariantDeviceProfile;
import foundation.e.blisslauncher.features.test.LauncherAppState;
import foundation.e.blisslauncher.features.test.LauncherModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final String TAG = "InstallShortcutReceiver";

    private static final String ACTION_INSTALL_SHORTCUT =
        "com.android.launcher.action.INSTALL_SHORTCUT";

    private static final int MSG_ADD_TO_QUEUE = 1;
    private static final int MSG_FLUSH_QUEUE = 2;

    // Determines whether to defer installing shortcuts immediately until
    // processAllPendingInstalls() is called.
    private static int sInstallQueueDisabledFlags = 0;

    private static final String LAUNCH_INTENT_KEY = "intent.launch";
    private static final String DEEPSHORTCUT_TYPE_KEY = "isDeepShortcut";
    private static final String APP_SHORTCUT_TYPE_KEY = "isAppShortcut";
    private static final String USER_HANDLE_KEY = "userHandle";
    private static final String NAME_KEY = "name";
    private static final String ICON_KEY = "icon";
    private static final String ICON_RESOURCE_NAME_KEY = "iconResource";
    private static final String ICON_RESOURCE_PACKAGE_NAME_KEY = "iconResourcePackage";

    // The set of shortcuts that are pending install
    private static final String APPS_PENDING_INSTALL = "apps_to_install";

    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    public static final int NEW_SHORTCUT_STAGGER_DELAY = 85;

    private static final Object sLock = new Object();

    private static final Handler sHandler = new Handler(LauncherModel.getWorkerLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD_TO_QUEUE: {
                    Pair<Context, PendingInstallShortcutInfo> pair =
                        (Pair<Context, PendingInstallShortcutInfo>) msg.obj;
                    String encoded = pair.second.encodeToString();
                    SharedPreferences prefs = Utilities.getPrefs(pair.first);
                    Set<String> strings = prefs.getStringSet(APPS_PENDING_INSTALL, null);
                    strings = (strings != null) ? new HashSet<>(strings) : new HashSet<String>(1);
                    strings.add(encoded);
                    prefs.edit().putStringSet(APPS_PENDING_INSTALL, strings).apply();
                    return;
                }
                case MSG_FLUSH_QUEUE: {
                    Context context = (Context) msg.obj;
                    LauncherModel model = LauncherAppState.getInstance(context).getModel();
                    if (model.getCallback() == null) {
                        // Launcher not loaded
                        return;
                    }

                    ArrayList<Pair<LauncherItem, Object>> installQueue = new ArrayList<>();
                    SharedPreferences prefs = Utilities.getPrefs(context);
                    Set<String> strings = prefs.getStringSet(APPS_PENDING_INSTALL, null);
                    if (strings == null) {
                        return;
                    }

                    LauncherApps launcherApps =
                        (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                    for (String encoded : strings) {
                        PendingInstallShortcutInfo info = decode(encoded, context);
                        if (info == null) {
                            continue;
                        }

                        String pkg = getIntentPackage(info.launchIntent);
                        if (!TextUtils.isEmpty(pkg)
                            && !launcherApps.isPackageEnabled(pkg, info.user)) {
                            Log.d(TAG, "Ignoring shortcut for absent package: "
                                + info.launchIntent);
                            continue;
                        }

                        // Generate a shortcut info to add into the model
                        installQueue.add(info.getItemInfo(context));
                    }
                    prefs.edit().remove(APPS_PENDING_INSTALL).apply();
                    if (!installQueue.isEmpty()) {
                        model.addAndBindAddedWorkspaceItems(installQueue);
                    }
                    return;
                }
            }
        }
    };

    public static void removeFromInstallQueue(
        Context context, HashSet<String> packageNames,
        android.os.UserHandle user
    ) {
        if (packageNames.isEmpty()) {
            return;
        }
        Preconditions.assertWorkerThread();

        SharedPreferences sp = Utilities.getPrefs(context);
        Set<String> strings = sp.getStringSet(APPS_PENDING_INSTALL, null);
        Log.d(TAG, "APPS_PENDING_INSTALL: " + strings
            + ", removing packages: " + packageNames);
        if (strings == null || ((Collection) strings).isEmpty()) {
            return;
        }
        Set<String> newStrings = new HashSet<>(strings);
        Iterator<String> newStringsIter = newStrings.iterator();
        while (newStringsIter.hasNext()) {
            String encoded = newStringsIter.next();
            try {
                Decoder decoder = new Decoder(encoded, context);
                if (packageNames.contains(getIntentPackage(decoder.launcherIntent)) &&
                    user.equals(decoder.user)) {
                    newStringsIter.remove();
                }
            } catch (JSONException | URISyntaxException e) {
                Log.d(TAG, "Exception reading shortcut to add: " + e);
                newStringsIter.remove();
            }
        }
        sp.edit().putStringSet(APPS_PENDING_INSTALL, newStrings).apply();
    }

    @Override
    public void onReceive(Context context, Intent data) {
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }
        PendingInstallShortcutInfo info = createPendingInfo(context, data);
        if (info != null) {
            if (!info.isLauncherActivity()) {
                // Since its a custom shortcut, verify that it is safe to launch.
                if (!new PackageManagerHelper(context).hasPermissionForActivity(
                    info.launchIntent, null)) {
                    // Target cannot be launched, or requires some special permission to launch
                    Log.e(TAG, "Ignoring malicious intent " + info.launchIntent.toUri(0));
                    return;
                }
            }
            queuePendingShortcutInfo(info, context);
        }
        //EventRelay.getInstance().push(new ShortcutAddEvent(shortcutItem));
        /**/
    }

    /**
     * @return true is the extra is either null or is of type {@param type}
     */
    private static boolean isValidExtraType(Intent intent, String key, Class type) {
        Object extra = intent.getParcelableExtra(key);
        return extra == null || type.isInstance(extra);
    }

    /**
     * Verifies the intent and creates a {@link PendingInstallShortcutInfo}
     */
    private static PendingInstallShortcutInfo createPendingInfo(Context context, Intent data) {
        if (!isValidExtraType(data, Intent.EXTRA_SHORTCUT_INTENT, Intent.class) ||
            !(isValidExtraType(data, Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.class
            )) ||
            !(isValidExtraType(data, Intent.EXTRA_SHORTCUT_ICON, Bitmap.class))) {

            return null;
        }

        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(
            data, Process.myUserHandle(), context);
        if (info.launchIntent == null || info.label == null) {
            return null;
        }

        return convertToLauncherActivityIfPossible(info);
    }

    public static LauncherItem fromShortcutIntent(Context context, Intent data) {
        PendingInstallShortcutInfo info = createPendingInfo(context, data);
        return info == null ? null : (LauncherItem) info.getItemInfo(context).first;
    }

    public static void queueShortcut(ShortcutInfo info, Context context) {
        queuePendingShortcutInfo(new PendingInstallShortcutInfo(info, context), context);
    }

    public static void queueShortcut(ShortcutInfoCompat info, Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        ShortcutItem shortcutItem = new ShortcutItem();
        shortcutItem.id = info.getId();
        shortcutItem.user = new UserHandle(
            userManager.getSerialNumberForUser(info.getUserHandle()),
            info.getUserHandle()
        );
        shortcutItem.packageName = info.getPackage();
        shortcutItem.title = info.getShortLabel().toString();
        shortcutItem.container = Constants.CONTAINER_DESKTOP;
        Drawable icon = DeepShortcutManager.getInstance(context).getShortcutIconDrawable(
            info,
            context.getResources().getDisplayMetrics().densityDpi
        );
        shortcutItem.icon = BlissLauncher.getApplication(context).getIconsHandler().convertIcon(
            icon);
        shortcutItem.launchIntent = info.makeIntent();
        EventRelay.getInstance().push(new ShortcutAddEvent(shortcutItem));
        /*context.startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(context.getPackageName())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));*/
    }

    private static ShortcutItem createShortcutItem(Intent data, Context context) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        if (intent == null) {
            // If the intent is null, we can't construct a valid ShortcutInfo, so we return null
            Log.e(TAG, "Can't construct ShortcutInfo with null intent");
            return null;
        }

        final ShortcutItem item = new ShortcutItem();

        // Only support intents for current user for now. Intents sent from other
        // users wouldn't get here without intent forwarding anyway.
        item.user = new UserHandle();

        Drawable icon = null;
        if (bitmap instanceof Bitmap) {
            icon = IconsHandler.createIconDrawable((Bitmap) bitmap, context);
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra instanceof Intent.ShortcutIconResource) {
                icon = IconsHandler.createIconDrawable(
                    (Intent.ShortcutIconResource) extra, context);
            }
        }
        if (icon == null) {
            icon = BlissLauncher.getApplication(
                context).getIconsHandler().getFullResDefaultActivityIcon();
        }
        item.packageName = intent.getPackage();
        item.container = Constants.CONTAINER_DESKTOP;
        item.title = Utilities.trim(name);
        item.icon = BlissLauncher.getApplication(context).getIconsHandler().convertIcon(icon);
        if (item.icon != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            convertToBitmap(item.icon).compress(Bitmap.CompressFormat.PNG, 100, baos);
            item.icon_blob = baos.toByteArray();
        }
        item.launchIntent = intent;
        item.launchIntentUri = item.launchIntent.toUri(0);
        item.id = item.packageName + "/" + item.launchIntentUri;
        return item;
    }

    private static Bitmap convertToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
            drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static void queuePendingShortcutInfo(PendingInstallShortcutInfo info, Context context) {
        // Queue the item up for adding if launcher has not loaded properly yet
        Message.obtain(sHandler, MSG_ADD_TO_QUEUE, Pair.create(context, info)).sendToTarget();
        flushInstallQueue(context);
    }

    public static void enableInstallQueue(int flag) {
        sInstallQueueDisabledFlags |= flag;
    }

    public static void disableAndFlushInstallQueue(int flag, Context context) {
        sInstallQueueDisabledFlags &= ~flag;
        flushInstallQueue(context);
    }

    static void flushInstallQueue(Context context) {
        if (sInstallQueueDisabledFlags != 0) {
            return;
        }
        Message.obtain(sHandler, MSG_FLUSH_QUEUE, context.getApplicationContext()).sendToTarget();
    }

    /**
     * Ensures that we have a valid, non-null name.  If the provided name is null, we will return
     * the application name instead.
     */
    static CharSequence ensureValidName(Context context, Intent intent, CharSequence name) {
        if (name == null) {
            try {
                PackageManager pm = context.getPackageManager();
                ActivityInfo info = pm.getActivityInfo(intent.getComponent(), 0);
                name = info.loadLabel(pm);
            } catch (PackageManager.NameNotFoundException nnfe) {
                return "";
            }
        }
        return name;
    }

    private static class PendingInstallShortcutInfo {

        final LauncherActivityInfo activityInfo;
        final ShortcutInfo shortcutInfo;

        final Intent data;
        final Context mContext;
        final Intent launchIntent;
        final String label;
        final android.os.UserHandle user;

        /**
         * Initializes a PendingInstallShortcutInfo received from a different app.
         */
        public PendingInstallShortcutInfo(
            Intent data,
            android.os.UserHandle user,
            Context context
        ) {
            activityInfo = null;
            shortcutInfo = null;

            this.data = data;
            this.user = user;
            mContext = context;

            launchIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            label = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        }

        /**
         * Initializes a PendingInstallShortcutInfo to represent a launcher target.
         */
        public PendingInstallShortcutInfo(LauncherActivityInfo info, Context context) {
            activityInfo = info;
            shortcutInfo = null;

            data = null;
            user = info.getUser();
            mContext = context;

            launchIntent = ApplicationItem.makeLaunchIntent(info);
            label = info.getLabel().toString();
        }

        /**
         * Initializes a PendingInstallShortcutInfo to represent a launcher target.
         */
        public PendingInstallShortcutInfo(ShortcutInfo info, Context context) {
            activityInfo = null;
            shortcutInfo = info;

            data = null;
            mContext = context;
            user = info.getUserHandle();

            launchIntent = ShortcutKey.makeIntent(info);
            label = info.getShortLabel().toString();
        }

        public String encodeToString() {
            try {
                if (activityInfo != null) {
                    // If it a launcher target, we only need component name, and user to
                    // recreate this.
                    return new JSONStringer()
                        .object()
                        .key(LAUNCH_INTENT_KEY).value(launchIntent.toUri(0))
                        .key(APP_SHORTCUT_TYPE_KEY).value(true)
                        .key(USER_HANDLE_KEY).value(UserManagerCompat.getInstance(mContext)
                            .getSerialNumberForUser(user))
                        .endObject().toString();
                } else if (shortcutInfo != null) {
                    // If it a launcher target, we only need component name, and user to
                    // recreate this.
                    return new JSONStringer()
                        .object()
                        .key(LAUNCH_INTENT_KEY).value(launchIntent.toUri(0))
                        .key(DEEPSHORTCUT_TYPE_KEY).value(true)
                        .key(USER_HANDLE_KEY).value(UserManagerCompat.getInstance(mContext)
                            .getSerialNumberForUser(user))
                        .endObject().toString();
                }

                if (launchIntent.getAction() == null) {
                    launchIntent.setAction(Intent.ACTION_VIEW);
                } else if (launchIntent.getAction().equals(Intent.ACTION_MAIN) &&
                    launchIntent.getCategories() != null &&
                    launchIntent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                }

                // This name is only used for comparisons and notifications, so fall back to activity
                // name if not supplied
                String name = ensureValidName(mContext, launchIntent, label).toString();
                Bitmap icon = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                Intent.ShortcutIconResource iconResource =
                    data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);

                // Only encode the parameters which are supported by the API.
                JSONStringer json = new JSONStringer()
                    .object()
                    .key(LAUNCH_INTENT_KEY).value(launchIntent.toUri(0))
                    .key(NAME_KEY).value(name);
                if (icon != null) {
                    byte[] iconByteArray = flattenBitmap(icon);
                    json = json.key(ICON_KEY).value(
                        Base64.encodeToString(
                            iconByteArray, 0, iconByteArray.length, Base64.DEFAULT));
                }
                if (iconResource != null) {
                    json = json.key(ICON_RESOURCE_NAME_KEY).value(iconResource.resourceName);
                    json = json.key(ICON_RESOURCE_PACKAGE_NAME_KEY)
                        .value(iconResource.packageName);
                }
                return json.endObject().toString();
            } catch (JSONException e) {
                Log.d(TAG, "Exception when adding shortcut: " + e);
                return null;
            }
        }

        public Pair<LauncherItem, Object> getItemInfo(Context context) {
            /*if (activityInfo != null) {
                ApplicationItem appInfo = new ApplicationItem(mContext, activityInfo, user);
                final LauncherAppState app = LauncherAppState.getInstance(mContext);
                // Set default values until proper values is loaded.
                appInfo.title = "";
                appInfo.icon = (BlissLauncher.getApplication(mContext).getIconsHandler()
                    .getFullResDefaultActivityIcon());
                final WorkspaceItemInfo si = appInfo.makeWorkspaceItem();
                if (Looper.myLooper() == LauncherModel.getWorkerLooper()) {
                    app.getIconCache().getTitleAndIcon(si, activityInfo, false *//* useLowResIcon *//*);
                } else {
                    app.getModel().updateAndBindWorkspaceItem(() -> {
                        app.getIconCache().getTitleAndIcon(
                            si, activityInfo, false *//* useLowResIcon *//*);
                        return si;
                    });
                }
                return Pair.create((ItemInfo) si, (Object) activityInfo);
            } else*/
            if (shortcutInfo != null) {
                final ShortcutItem si = new ShortcutItem();

                // Only support intents for current user for now. Intents sent from other
                // users wouldn't get here without intent forwarding anyway.
                si.user = new UserHandle(
                    UserManagerCompat.getInstance(context)
                        .getSerialNumberForUser(shortcutInfo.getUserHandle()),
                    shortcutInfo.getUserHandle()
                );

                int fillResIconDpi =
                    BlissLauncher.getApplication(context).getInvariantDeviceProfile()
                        .getFillResIconDpi();
                Drawable icon = DeepShortcutManager.getInstance(context)
                    .getShortcutIconDrawable(shortcutInfo, fillResIconDpi);

                if (icon == null) {
                    Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (extra instanceof Intent.ShortcutIconResource) {
                        icon = IconsHandler.createIconDrawable(
                            (Intent.ShortcutIconResource) extra, context);
                    }
                }

                IconsHandler iconsHandler = BlissLauncher.getApplication(
                    context).getIconsHandler();
                if (icon == null) {
                    icon = iconsHandler.getFullResDefaultActivityIcon();
                }
                si.packageName = shortcutInfo.getPackage();
                si.container = Constants.CONTAINER_DESKTOP;
                si.title = shortcutInfo.getShortLabel();
                si.icon = iconsHandler.convertIcon(icon);
                if (si.icon != null) {
                    si.icon_blob = flattenBitmap(convertToBitmap(si.icon));
                }
                si.launchIntent = ShortcutKey.makeIntent(shortcutInfo);
                si.launchIntentUri = si.launchIntent.toUri(0);
                si.id = shortcutInfo.getId();
                return Pair.create((LauncherItem) si, (Object) shortcutInfo);
            } else {
                ShortcutItem si = createLauncherItem(data, LauncherAppState.getInstance(mContext));
                return Pair.create((LauncherItem) si, null);
            }
        }

        public boolean isLauncherActivity() {
            return activityInfo != null;
        }
    }

    /**
     * Compresses the bitmap to a byte array for serialization.
     */
    public static byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write (4 bytes per pixel).
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Could not write bitmap");
            return null;
        }
    }

    private static String getIntentPackage(Intent intent) {
        return intent.getComponent() == null
            ? intent.getPackage() : intent.getComponent().getPackageName();
    }

    /**
     * Tries to create a new PendingInstallShortcutInfo which represents the same target,
     * but is an app target and not a shortcut.
     *
     * @return the newly created info or the original one.
     */
    private static PendingInstallShortcutInfo convertToLauncherActivityIfPossible(
        PendingInstallShortcutInfo original
    ) {
        if (original.isLauncherActivity()) {
            // Already an activity target
            return original;
        }
        if (!Utilities.isLauncherAppTarget(original.launchIntent)) {
            return original;
        }

        LauncherApps launcherApps =
            (LauncherApps) original.mContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        LauncherActivityInfo info =
            launcherApps.resolveActivity(original.launchIntent, original.user);
        if (info == null) {
            return original;
        }
        // Ignore any conflicts in the label name, as that can change based on locale.
        return new PendingInstallShortcutInfo(info, original.mContext);
    }

    private static PendingInstallShortcutInfo decode(String encoded, Context context) {
        try {
            Decoder decoder = new Decoder(encoded, context);
            if (decoder.optBoolean(APP_SHORTCUT_TYPE_KEY)) {
                LauncherActivityInfo info =
                    ((LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE))
                        .resolveActivity(decoder.launcherIntent, decoder.user);
                return info == null ? null : new PendingInstallShortcutInfo(info, context);
            } else if (decoder.optBoolean(DEEPSHORTCUT_TYPE_KEY)) {
                DeepShortcutManager sm = DeepShortcutManager.getInstance(context);
                List<ShortcutInfo> si = sm.queryForFullDetails(
                    decoder.launcherIntent.getPackage(),
                    Arrays.asList(decoder.launcherIntent.getStringExtra(
                        ShortcutKey.EXTRA_SHORTCUT_ID)),
                    decoder.user
                );
                if (si.isEmpty()) {
                    return null;
                } else {
                    return new PendingInstallShortcutInfo(si.get(0), context);
                }
            }

            Intent data = new Intent();
            data.putExtra(Intent.EXTRA_SHORTCUT_INTENT, decoder.launcherIntent);
            data.putExtra(Intent.EXTRA_SHORTCUT_NAME, decoder.getString(NAME_KEY));

            String iconBase64 = decoder.optString(ICON_KEY);
            String iconResourceName = decoder.optString(ICON_RESOURCE_NAME_KEY);
            String iconResourcePackageName = decoder.optString(ICON_RESOURCE_PACKAGE_NAME_KEY);
            if (!iconBase64.isEmpty()) {
                byte[] iconArray = Base64.decode(iconBase64, Base64.DEFAULT);
                Bitmap b = BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length);
                data.putExtra(Intent.EXTRA_SHORTCUT_ICON, b);
            } else if (!iconResourceName.isEmpty()) {
                Intent.ShortcutIconResource iconResource =
                    new Intent.ShortcutIconResource();
                iconResource.resourceName = iconResourceName;
                iconResource.packageName = iconResourcePackageName;
                data.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
            }

            return new PendingInstallShortcutInfo(data, decoder.user, context);
        } catch (JSONException | URISyntaxException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
        }
        return null;
    }

    private static class Decoder extends JSONObject {
        public final Intent launcherIntent;
        public final android.os.UserHandle user;

        private Decoder(String encoded, Context context) throws JSONException, URISyntaxException {
            super(encoded);
            launcherIntent = Intent.parseUri(getString(LAUNCH_INTENT_KEY), 0);
            user = has(USER_HANDLE_KEY) ? UserManagerCompat.getInstance(context)
                .getUserForSerialNumber(getLong(USER_HANDLE_KEY))
                : Process.myUserHandle();
            if (user == null) {
                throw new JSONException("Invalid user");
            }
        }
    }

    private static ShortcutItem createLauncherItem(Intent data, LauncherAppState app) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        if (intent == null) {
            // If the intent is null, return null as we can't construct a valid WorkspaceItemInfo
            Log.e(TAG, "Can't construct WorkspaceItemInfo with null intent");
            return null;
        }

        final ShortcutItem info = new ShortcutItem();

        // Only support intents for current user for now. Intents sent from other
        // users wouldn't get here without intent forwarding anyway.
        info.user = new UserHandle(UserManagerCompat.getInstance(app.getContext())
            .getSerialNumberForUser(Process.myUserHandle()), Process.myUserHandle());

        Drawable icon = null;
        if (bitmap instanceof Bitmap) {
            icon = IconsHandler.createIconDrawable((Bitmap) bitmap, app.getContext());
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra instanceof Intent.ShortcutIconResource) {
                icon = IconsHandler.createIconDrawable(
                    (Intent.ShortcutIconResource) extra, app.getContext());
            }
        }
        if (icon == null) {
            icon = BlissLauncher.getApplication(
                app.getContext()).getIconsHandler().getFullResDefaultActivityIcon();
        }
        info.packageName = intent.getPackage();
        info.container = Constants.CONTAINER_DESKTOP;
        info.title = Utilities.trim(name);
        info.icon =
            BlissLauncher.getApplication(app.getContext()).getIconsHandler().convertIcon(icon);
        if (info.icon != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            convertToBitmap(info.icon).compress(Bitmap.CompressFormat.PNG, 100, baos);
            info.icon_blob = baos.toByteArray();
        }
        info.launchIntent = intent;
        info.launchIntentUri = info.launchIntent.toUri(0);
        info.id = info.packageName + "/" + info.launchIntentUri;
        return info;
    }
}
