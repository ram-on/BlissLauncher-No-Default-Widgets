package foundation.e.blisslauncher.core.customviews;


import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import foundation.e.blisslauncher.features.test.TestActivity;
import foundation.e.blisslauncher.features.test.VariantDeviceProfile;

/**
 * Created by falcon on 16/2/18.
 */

public class SquareImageView extends AppCompatImageView {

    private VariantDeviceProfile deviceProfile;

    public SquareImageView(Context context) {
        this(context, null);
    }

    public SquareImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TestActivity launcher = TestActivity.Companion.getLauncher(context);
        deviceProfile = launcher.getDeviceProfile();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int size = Math.min(width, height);
        setMeasuredDimension(deviceProfile.getIconSizePx(), deviceProfile.getIconSizePx());
    }
}