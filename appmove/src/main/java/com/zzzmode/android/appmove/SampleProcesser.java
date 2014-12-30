package com.zzzmode.android.appmove;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by zl on 14/12/29.
 */
public class SampleProcesser extends Processer {
    private static final String TAG="SampleProcesser";


    @Override
    public void process(String... cmd) {
        try {
            Log.i(TAG, "shell execCommand begin");
            final Process process = new ProcessBuilder("sh").redirectErrorStream(false).start();
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            Thread thread= new Thread() {
                @Override
                public void run() {
                    String tmp;
                    try {
                        while ((tmp = reader.readLine()) != null) {
                            Log.i(TAG, "shell return --> " + tmp);
                            if ("exit".equals(tmp)) {
                                Log.i(TAG, "shell execCommand end");
                                try {
                                    writer.close();
                                } catch (Exception e) {
                                }
                                try {
                                    process.destroy();
                                } catch (Exception e) {
                                }
                                break;
                            }else {
                                stringBuffer.append(tmp);
                            }
                        }
                    } catch (IOException e) {
                        Log.i(TAG, Log.getStackTraceString(e));
                    } finally {
                        try {
                            reader.close();
                            Log.i(TAG, "shell reader close");
                        } catch (Exception ex) {
                        }
                    }
                }
            };
            thread.start();

            execWriter(writer, "echo shell start--");
            for(String str:cmd){
                execWriter(writer, str);
            }
            execWriter(writer,"echo exit");
            thread.join();
        } catch (Exception e) {
            Log.i(TAG, Log.getStackTraceString(e));
        }
    }

    @Override
    public String getOutput() {
        return stringBuffer.toString();
    }


    private void execWriter(BufferedWriter writer,String cmd){
        try{
            if(writer != null){
                writer.write(cmd);
                writer.newLine();
                writer.flush();
            }
        }catch (Exception e){
        }
    }

}
