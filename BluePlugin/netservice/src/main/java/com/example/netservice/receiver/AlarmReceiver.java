package com.example.netservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.netservice.service.GPSService;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("LOCATION_CLOCK")) {
            Log.e("ggb", "--->>>   onReceive  LOCATION_CLOCK");
            Intent locationIntent = new Intent(context, GPSService.class);
            context.startService(locationIntent);
        }
    }
}
