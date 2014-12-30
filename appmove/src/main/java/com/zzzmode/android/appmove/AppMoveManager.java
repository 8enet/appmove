package com.zzzmode.android.appmove;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;



import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;


public class AppMoveManager {

    public static final int PACKAGE_LOCATION_UNSPECIFIED = -1; //unspecified install location

    public static final int STORE_APP_AUTO_LOCALTION = 0;     //auto
    /**
     * Internal Store
     */
    public static final int STORE_APP_INTERNAL_LOCALTION = 1;
    /**
     * External Store
     */
    public static final int STORE_APP_EXTERNAL_LOCALTION = 2;


    public static final String FORK_PACKAGE_MANAGER_CLASSNAME = "com.zzzmode.android.util.move.PackageMove";

    private static final String TAG = "appmove";

    public static final String JAR_NAME= "appmove.jar";

    private static boolean isExternalStorageEmulated = Environment.isExternalStorageEmulated();

    private AppMoveCallback mOnAppMoveCallback;
    private Context mContext;
    private PackageManager mPackageManager;
    private String workPath;
    private CanBeOnSdCardChecker mCanBeOnSdCardChecker = new CanBeOnSdCardChecker();

    private Processer mProcesser;

    public AppMoveManager(Context context,Processer processer) {
        this.mContext = context;
        mPackageManager = mContext.getPackageManager();
        workPath = mContext.getFilesDir().getAbsolutePath() + File.separator;
        this.mProcesser=processer;
    }

    public void setOnAppMoveCallback(AppMoveCallback onAppMoveCallback) {
        this.mOnAppMoveCallback = onAppMoveCallback;
    }

    public void setProcesser(Processer processer){
        this.mProcesser=processer;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };


    /**
     *  !!! TEST METHOD !!!
     */
    @Deprecated
    public void testMoveApp() {
        try {
            Log.d(TAG, "  -----   testMoveApp  -----   ");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        perResources();
                        List<PackageInfo> installedPackages = mPackageManager.getInstalledPackages(0);
                        if (installedPackages != null && !installedPackages.isEmpty()) {
                            for (PackageInfo pInfo : installedPackages) {
                                if (!mContext.getPackageName().equals(pInfo.packageName)) {

                                    if (checkMove2SD(pInfo)) {
                                        Log.d(TAG, pInfo.packageName + "  ----------   ");
                                        Log.d(TAG, pInfo.packageName + "  can move to sd!");
                                        moveApp(pInfo.packageName, STORE_APP_EXTERNAL_LOCALTION);
                                    }
                                    if (checkMove2Internal(pInfo)) {
                                        Log.d(TAG, pInfo.packageName + "  ----------   ");
                                        Log.d(TAG, pInfo.packageName + "  can move to internal !");
                                        moveApp(pInfo.packageName, STORE_APP_INTERNAL_LOCALTION);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {

                    }
                }
            }).start();
        } catch (Exception e) {

        }

    }


    /**
     * move app
     *
     * @param pkgName
     * @param location want move to location
     */
    public void moveApp(final String pkgName, final int location) {
        if (mOnAppMoveCallback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mOnAppMoveCallback.onMoveStart(pkgName, location);
                }
            });
        }

        String move = "." + workPath + "moveshell  " + pkgName + "  " + location;

        final boolean moveResult = executeMove(move);

        if (mOnAppMoveCallback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mOnAppMoveCallback.onMoveEnd(pkgName, moveResult, location);
                }
            });
        }
    }

    /**
     * move to external store
     *
     * @param pkgName
     */
    public void move2External(String pkgName) {
        moveApp(pkgName, STORE_APP_EXTERNAL_LOCALTION);
    }


    /**
     * move to internal store
     *
     * @param pkgName
     */
    public void move2Internal(String pkgName) {
        moveApp(pkgName, STORE_APP_INTERNAL_LOCALTION);
    }


    private boolean executeMove(final String cmd) {
        String root="su";
        String c1="chmod 711 "+workPath+JAR_NAME;
        String c2="export LD_LIBRARY_PATH=/vendor/lib:/system/lib";
        c2.intern();
        String c3="export CLASSPATH="+workPath+JAR_NAME;
        mProcesser.process(root,c1,c2,c3,cmd);
        return mProcesser.checkResult();
    }

    private void perResources() {
        copyAssetsFile("amove.jar", workPath + JAR_NAME);
        copyAssetsFile("moveshell", workPath + "moveshell");
    }


    private void copyAssetsFile(String fileName, String diskPath) {
        try {
            File file = new File(diskPath);
            if (file.exists()) {
                return;
            }
            AssetManager.AssetInputStream ais = null;
            FileOutputStream fos = null;
            try {
                ais = (AssetManager.AssetInputStream) mContext.getAssets().open(fileName);
                fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024 * 10];
                int len = 0;
                while ((len = ais.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                closeQuietly(ais, fos);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private void closeQuietly(final Closeable... closeable) {
        if (closeable != null) {
            for (Closeable cls : closeable) {
                try {
                    if (cls != null)
                        cls.close();
                } catch (Exception e) {

                }
            }
        }

    }


    /**
     * check the device support move app
     *
     * @return
     */
    public boolean deviceCanBeMove() {
        return isExternalStorageEmulated;
    }


    public boolean appCanBeMove(String pkgName) {
        return checkMove(pkgName, STORE_APP_AUTO_LOCALTION);
    }


    public boolean checkMove(String pkgName, int location) {
        PackageInfo packageInfo = getPackageInfo(pkgName, 0);
        if (packageInfo != null) {
            return checkMove(packageInfo, location);
        }
        return false;
    }

    private PackageInfo getPackageInfo(String pkgName, int flag) {
        try {
            return mPackageManager.getPackageInfo(pkgName, flag);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    private boolean isSystemApp(ApplicationInfo appInfo) {
        return (appInfo.flags & appInfo.FLAG_SYSTEM) > 0;
    }

    private boolean doChecker(ApplicationInfo appInfo, int location) {

        if ((appInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
            Log.d(TAG, appInfo.packageName + " can be move to inretnal storage");
            if (location == STORE_APP_EXTERNAL_LOCALTION) {
                return false;
            }
            return true;
        } else {

            boolean sd = mCanBeOnSdCardChecker.check(appInfo);
            if (sd && location == STORE_APP_INTERNAL_LOCALTION) {
                return false;
            }
            if (sd) {
                Log.d(TAG, appInfo.packageName + " can be move to external storage");
            }
            return sd;
        }
    }

    public boolean checkMove(PackageInfo packageInfo, int location) {
        try {
            if (isExternalStorageEmulated) {
                Log.d(TAG, " the device  unsupport move app !!");
                return false;
            }
            if (packageInfo != null) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                if (!isSystemApp(appInfo) && doChecker(appInfo, location)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }



    public boolean checkMove2SD(String pkgName) {
        return checkMove(pkgName, STORE_APP_EXTERNAL_LOCALTION);
    }

    public boolean checkMove2SD(PackageInfo packageInfo) {
        return checkMove(packageInfo, STORE_APP_EXTERNAL_LOCALTION);
    }



    public boolean checkMove2Internal(String pkgName) {
        return checkMove(pkgName, STORE_APP_INTERNAL_LOCALTION);
    }

    public boolean checkMove2Internal(PackageInfo packageInfo) {
        return checkMove(packageInfo, STORE_APP_INTERNAL_LOCALTION);
    }


    public int getPackageCurrLocation(PackageInfo packageInfo) {
        int location = PACKAGE_LOCATION_UNSPECIFIED;
        if (packageInfo != null) {
            location = mCanBeOnSdCardChecker.getCurrInstallLocation(packageInfo.applicationInfo);
        }
        return location;
    }

    /**
     * get current package install location
     *
     * @param pkgName
     * @return flags
     * {@link #PACKAGE_LOCATION_UNSPECIFIED}
     * {@link #STORE_APP_INTERNAL_LOCALTION},
     * {@link #STORE_APP_EXTERNAL_LOCALTION}
     */
    public int getPackageCurrLocation(String pkgName) {
        PackageInfo packageInfo = getPackageInfo(pkgName, 0);
        if (packageInfo != null) {
            return getPackageCurrLocation(packageInfo);
        }
        return PACKAGE_LOCATION_UNSPECIFIED;
    }


    public static interface AppMoveCallback {
        void onMoveStart(String pkgName, int flag);

        void onMoveEnd(String pkgName, boolean moveResult, int flag);
    }
}
