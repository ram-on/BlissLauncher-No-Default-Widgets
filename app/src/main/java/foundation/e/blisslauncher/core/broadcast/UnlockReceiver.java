package foundation.e.blisslauncher.core.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import foundation.e.blisslauncher.features.weather.DeviceStatusService;
import foundation.e.blisslauncher.features.weather.WeatherSourceListenerService;
import foundation.e.blisslauncher.features.weather.WeatherUpdateService;
import foundation.e.blisslauncher.features.weather.WeatherUtils;

public class UnlockReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            launchServices(context);
        }
    }

    private void launchServices(Context context) {
        if (WeatherUtils.isWeatherServiceAvailable(context)) {
            context.startService(new Intent(context, WeatherSourceListenerService.class));
            context.startService(new Intent(context, DeviceStatusService.class));
            context.startService(
                    new Intent(context, WeatherUpdateService.class)
                            .setAction(WeatherUpdateService.ACTION_FORCE_UPDATE));
        }
    }
}
