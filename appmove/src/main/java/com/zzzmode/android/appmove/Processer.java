package com.zzzmode.android.appmove;

/**
 * Created by zl on 14/12/29.
 */
public abstract class Processer {
    protected  StringBuffer stringBuffer=new StringBuffer();
    public abstract void process(String... cmd);
    public  String getOutput(){
        return stringBuffer.toString();
    }


    public boolean checkResult(){
        return stringBuffer.toString().endsWith("0\n");
    }
}
