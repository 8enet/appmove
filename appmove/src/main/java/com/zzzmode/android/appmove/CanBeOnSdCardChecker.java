package com.zzzmode.android.appmove;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CanBeOnSdCardChecker {


    private static final String TAG = "CanBeOnSdCardChecker";
    private IPackageManager mPm;
    private int mInstallLocation;

    CanBeOnSdCardChecker() {
        try {
            Class<?> serviceManager = Class
                    .forName("android.os.ServiceManager");
            Method getService = serviceManager.getMethod("getService",
                    java.lang.String.class);
            IBinder service = (IBinder) getService.invoke(null, "package");
            mPm = IPackageManager.Stub.asInterface(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
        init();
    }

    void init() {

        try {
            if (mPm != null) {
                mInstallLocation = mPm.getInstallLocation();
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        try {
            installLocationField = ApplicationInfo.class.getDeclaredField("installLocation");
            Class helper = Class.forName("com.android.internal.content.PackageHelper");
            externalValue = helper.getField("APP_INSTALL_EXTERNAL").getInt(helper);
        } catch (Exception e) {
            externalValue=2;
        }
    }

    private Field installLocationField = null;
    private int externalValue; //系统外部存储值

    public int getCurrInstallLocation(ApplicationInfo info) {
        try {
            if (installLocationField != null) {
                return installLocationField.getInt(info);
            }
        } catch (Exception e) {

        }
        return -1;
    }


    boolean check(ApplicationInfo info) {
        boolean canBe = false;
        if ((info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
            canBe = true;
        } else {
            try {
                int installLocation = getCurrInstallLocation(info);
                if ((info.flags & 1 << 29) == 0 &&
                        (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    if (installLocation == PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL ||
                            installLocation == PackageInfo.INSTALL_LOCATION_AUTO) {
                        canBe = true;
                    } else if (installLocation == -1) {
                        if (mInstallLocation == externalValue) {
                            canBe = true;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return canBe;
    }
}
