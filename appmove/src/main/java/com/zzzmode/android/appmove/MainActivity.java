package com.zzzmode.android.appmove;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements AppMoveManager.AppMoveCallback{
    
    private ListView listView;
    private AppMoveManager moveManager;
    
    private List<AppInfo> data=null;
    private Context context;
    private ProgressDialog progressDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        moveManager= new AppMoveManager(this,new SampleProcesser());

        this.context=this;

        if(!moveManager.deviceCanBeMove()){
            findViewById(R.id.tv_device_unsupport).setVisibility(View.VISIBLE);
            return;
        }

        initData();
        listView= (ListView) findViewById(R.id.listview);
        listView.setVisibility(View.VISIBLE);

        listView.setAdapter(new MyAdapter());
    }

    private void initData(){
        progressDialog=new ProgressDialog(this);
        progressDialog.setIndeterminate(false);
        progressDialog.setTitle(getString(R.string.wait));

        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        
        moveManager.setOnAppMoveCallback(this);
        data=new ArrayList<>();
        List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(0 | PackageManager.GET_CONFIGURATIONS);
        if (installedPackages != null && !installedPackages.isEmpty()) {
            for (PackageInfo pInfo : installedPackages) {
                ApplicationInfo app=pInfo.applicationInfo;
                if (!getPackageName().equals(pInfo.packageName) && (app.flags & app.FLAG_SYSTEM) <= 0) {
                     AppInfo appInfo=new AppInfo();
                    appInfo.pkgName=pInfo.packageName;
                    appInfo.icon=pInfo.applicationInfo.loadIcon(getPackageManager());
                    appInfo.check();
                    data.add(appInfo);
                }
            }
        }
    }

    @Override
    public void onMoveStart(String pkgName, int flag) {
        progressDialog.setMessage(getString(R.string.move_info,pkgName,AppMoveManager.getDescription(flag)));
        progressDialog.show();
    }

    @Override
    public void onMoveEnd(String pkgName, boolean moveResult, int flag) {
          progressDialog.dismiss();
        if(moveResult){
            Toast.makeText(context,"move "+pkgName+" success!",Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(context,"move "+pkgName+" fail !",Toast.LENGTH_LONG).show();
        }
    }

    class AppInfo{
        public String pkgName;
        public Drawable icon;
        public boolean canMove=false;
        public int location;
         
        public void check(){
            if(moveManager.appCanBeMove(pkgName)){
                if(moveManager.checkMove2SD(pkgName)){
                    location=AppMoveManager.STORE_APP_EXTERNAL_LOCALTION;
                    canMove=true;
                }else if(moveManager.checkMove2Internal(pkgName)){
                    location=AppMoveManager.STORE_APP_INTERNAL_LOCALTION;
                    canMove=true;
                }else {
                    location=AppMoveManager.PACKAGE_LOCATION_UNSPECIFIED;
                }
            }
        }
    }
    
    

    class MyAdapter extends BaseAdapter implements View.OnClickListener{


        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public AppInfo getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder=null;
            if(convertView == null){
                convertView= LayoutInflater.from(context).inflate(R.layout.app_move_item,null);
                holder=new ViewHolder();
                
                holder.imgIcon= (ImageView) convertView.findViewById(R.id.img_app_icon);
                holder.tvName= (TextView) convertView.findViewById(R.id.tv_app_name);
                holder.btnMove= (Button) convertView.findViewById(R.id.btn_move);
                convertView.setTag(holder);
            }else {
                holder= (ViewHolder) convertView.getTag();
            }
            AppInfo info=getItem(position);
            holder.imgIcon.setImageDrawable(info.icon);
            holder.tvName.setText(info.pkgName);
            if(info.canMove){
                holder.btnMove.setEnabled(true);
                holder.btnMove.setOnClickListener(this);
                holder.btnMove.setTag(info);
            }else {
                holder.btnMove.setEnabled(false);
            }
            
            return convertView;
        }

        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.btn_move && v.getTag() != null){
                AppInfo info= (AppInfo) v.getTag();
                moveManager.moveApp(info.pkgName,info.location);
            }
        }

        class ViewHolder{
            ImageView imgIcon;
             TextView tvName;
             Button btnMove;
            
        }
        
        
    }
    

}
