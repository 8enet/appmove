package com.zzzmode.android.util.move;


import android.content.pm.IPackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.reflect.Method;

public final class PackageMove {
	public static final String COMMAND_MOVE_PACKAGE = "movePackage";

	public static final int RETURN_CODE_FAILED = -1;
	public static final int RETURN_CODE_SUCCESS = 0;
	public static final int STORE_APP_EXTERNAL_LOCALTION = 2;
	public static final int STORE_APP_INTERNAL_LOCALTION = 1;
	public static final int STORE_APP_AUTO_LOCALTION = 0;

	IPackageManager mPackageManager;
	private static final boolean debug=true;

	private static void returnCommand(int paramInt) {
		System.out.print(paramInt);
		System.exit(0);
		
	}

	public static void main(String[] args) {
		if(debug){
		for (String arg : args){
			log("options:" + arg);
		}	
		}
		new PackageMove().handleCommand(args);
	}

	private void handleCommand(String[] args) {
		try {
			String command = args[0];
			if (command == null || command.equals(""))
				return;

			Class<?> serviceManager = Class
					.forName("android.os.ServiceManager");
			Method getService = serviceManager.getMethod("getService",
					String.class);
			IBinder service = (IBinder) getService.invoke(null, "package");

            mPackageManager=IPackageManager.Stub.asInterface(service);
			if (mPackageManager == null) {
				log(getClass() + ":mPackageManager is null");
				returnCommand(RETURN_CODE_FAILED);
				return;
			}

			if (command.equalsIgnoreCase(COMMAND_MOVE_PACKAGE)) {
				if (args.length < 2){
                    log("params length is "+args.length +" fail!");
                    return;
                }

				runMovePackage(args[1], args[2]);
			}
		} catch (Exception e) {
			log(e);
			returnCommand(RETURN_CODE_FAILED);
		}
	}

	private void runMovePackage(String packageName, String location) {
		try {
			int install_location = Integer.parseInt(location);
			PackageMoveObserver localPackageMoveObserver = new PackageMoveObserver();
			mPackageManager.movePackage(packageName, localPackageMoveObserver, install_location);
			
				try{
					boolean bool = localPackageMoveObserver.finished;
					if (!bool) {	
						synchronized (localPackageMoveObserver) {
							localPackageMoveObserver.wait();
						}
					}
					}catch(Exception e){
                       log(e);
					}
	
			switch (localPackageMoveObserver.result) {
			case 1:
				returnCommand(RETURN_CODE_SUCCESS);
				return;
			default:
				returnCommand(RETURN_CODE_FAILED);
				return;
			}
		} catch (Exception e) {
			log(e);
			returnCommand(RETURN_CODE_FAILED);
		}
	}

    private static final void log(Exception e){
        if(debug && e != null){
            e.printStackTrace();
        }
    }
    private static final void  log(String msg){
        if(debug && msg != null ){
            System.out.println(msg);
        }
    }

	static class PackageMoveObserver extends
			android.content.pm.IPackageMoveObserver.Stub {

		boolean finished = false;
		int result;

		public void packageMoved(String paramString, int paramInt)
				throws RemoteException {
			finished = true;
			result = paramInt;
			synchronized (this) {
				notifyAll();
			}
		}
	}
}
