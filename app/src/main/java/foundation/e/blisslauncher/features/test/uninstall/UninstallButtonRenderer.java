package foundation.e.blisslauncher.features.test.uninstall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import foundation.e.blisslauncher.R;

/**
 * Created by falcon on 20/3/18.
 */
public class UninstallButtonRenderer {

    private static final float SIZE_PERCENTAGE = 0.3375f;

    private final Context mContext;
    private final int mSize;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG |
            Paint.FILTER_BITMAP_FLAG);

    public UninstallButtonRenderer(Context context, int iconSizePx) {
        mContext = context;
        this.mSize = (int) (SIZE_PERCENTAGE * iconSizePx);
    }

    public void draw(Canvas canvas, Rect iconBounds) {
        Bitmap myBitmap = BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.ic_minus);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(myBitmap, mSize, mSize, true);

        canvas.drawBitmap(scaledBitmap, iconBounds.right - scaledBitmap.getWidth() / 2,
                iconBounds.top - scaledBitmap.getHeight() / 2, mPaint);
        //canvas.drawCircle(badgeCenterX, badgeCenterY, mSize/2, mPaint);
    }

    /**
     * We double the icons bounds here to increase the touch area of uninstall icon size.
     * @param iconBounds
     * @return Doubled bounds for uninstall icon click.
     */
    public Rect getBoundsScaled(Rect iconBounds) {
        Rect uninstallIconBounds = new Rect();
        uninstallIconBounds.left = iconBounds.right - mSize;
        uninstallIconBounds.top = iconBounds.top - mSize;
        uninstallIconBounds.right = uninstallIconBounds.left + 2 * mSize;
        uninstallIconBounds.bottom = uninstallIconBounds.top + 2 * mSize;
        return uninstallIconBounds;
    }
}
