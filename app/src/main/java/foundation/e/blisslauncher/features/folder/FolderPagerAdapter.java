package foundation.e.blisslauncher.features.folder;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.customviews.BlissFrameLayout;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.touch.ItemClickHandler;
import foundation.e.blisslauncher.core.touch.ItemLongClickListener;
import foundation.e.blisslauncher.features.test.CellLayout;
import foundation.e.blisslauncher.features.test.IconTextView;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;

public class FolderPagerAdapter extends PagerAdapter {

    private Context mContext;
    private List<LauncherItem> mFolderAppItems;
    private VariantDeviceProfile mDeviceProfile;
    private List<GridLayout> grids = new ArrayList<>();

    public FolderPagerAdapter(
        Context context,
        List<LauncherItem> items,
        VariantDeviceProfile mDeviceProfile
    ) {
        this.mContext = context;
        this.mFolderAppItems = items;
        this.mDeviceProfile = mDeviceProfile;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        GridLayout viewGroup = (GridLayout) LayoutInflater.from(mContext).inflate(
            R.layout.apps_page, container, false);
        viewGroup.setRowCount(3);
        viewGroup.setColumnCount(3);
        viewGroup.setPadding(
            mContext.getResources().getDimensionPixelSize(R.dimen.folder_padding) ,
            mContext.getResources().getDimensionPixelSize(R.dimen.folder_padding),
            mContext.getResources().getDimensionPixelSize(R.dimen.folder_padding),
            mContext.getResources().getDimensionPixelSize(R.dimen.folder_padding)
        );
        ViewPager.LayoutParams params = (ViewPager.LayoutParams) viewGroup.getLayoutParams();
        params.width = GridLayout.LayoutParams.WRAP_CONTENT;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        int i = 0;
        while (9 * position + i < mFolderAppItems.size() && i < 9) {
            LauncherItem appItem = mFolderAppItems.get(9 * position + i);
            IconTextView appView = (IconTextView) LayoutInflater.from(mContext)
                .inflate(R.layout.app_icon, null, false);
            appView.applyFromShortcutItem(appItem);
            appView.setTextVisibility(true);
            appView.setOnClickListener(ItemClickHandler.INSTANCE);
            appView.setOnLongClickListener(ItemLongClickListener.INSTANCE_WORKSPACE);
            GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams();
            iconLayoutParams.height = mDeviceProfile.getCellHeightPx() + mDeviceProfile.getIconDrawablePaddingPx()*2;
            iconLayoutParams.width = mDeviceProfile.getCellHeightPx();
            iconLayoutParams.setGravity(Gravity.CENTER);
            appView.setLayoutParams(iconLayoutParams);
            viewGroup.addView(appView);
            i++;
        }
        grids.add(viewGroup);
        container.addView(viewGroup);
        return viewGroup;
    }

    @Override
    public int getCount() {
        return (int) Math.ceil((float) mFolderAppItems.size() / 9);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(
        @NonNull ViewGroup container, int position,
        @NonNull Object object
    ) {
        container.removeView((View) object);
    }
}
