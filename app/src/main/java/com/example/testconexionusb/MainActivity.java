package com.example.testconexionusb;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    UsbManager usbManager;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    //UsbDevice device;

    /*private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            Toast.makeText(context, "hay un USB conectado", Toast.LENGTH_SHORT).show();
                            Toast.makeText(context, device.getDeviceName(), Toast.LENGTH_SHORT).show();
                            //call method to set up device communication
                        }
                    }
                    else {
                        Log.d("permiso", "permission denied for device " + device);
                    }
                }
            }
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        Log.d("usb", deviceList.size()+"");
        System.out.println(deviceList.size());
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            Log.d("usb", device.getDeviceName());
        }



//        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
//        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//        registerReceiver(usbReceiver, filter);
        //usbManager.requestPermission(device, permissionIntent);




    }
}