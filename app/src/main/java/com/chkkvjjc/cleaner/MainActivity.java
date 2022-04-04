package com.chkkvjjc.cleaner;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    {
        System.loadLibrary("cleaner");
    }
    Handler showmemory;
    ProgressBar membar;
    TextView avail,total;//54977d4007f2b44bf2d284c93abc9a30 //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
    long totalMemory;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        membar=findViewById(R.id.progressBar);
        avail=findViewById(R.id.avail);
        total=findViewById(R.id.total);
        totalMemory=getTotalMemory(this);
        total.setText("总"+(int)(totalMemory/1024/1024)+"MB");
        showmemory=new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                switch (msg.what){
                    case 0:
                        membar.setProgress((int)(1000.0*(totalMemory-getAvailMemory(MainActivity.this))/totalMemory));
                        avail.setText((int)((totalMemory-getAvailMemory(MainActivity.this))/1024/1024)+"MB已用");
                        break;
                    case 1:
                        findViewById(R.id.clean_button).setEnabled(true);
                        break;
                }
            }
        };
        new Thread(){
            @Override
            public void run(){
                try {
                    while(true) {
                        showmemory.sendEmptyMessage(0);
                        sleep(6);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        if(read("autoclean")>0){
            ((CheckBox)findViewById(R.id.auto)).setChecked(true);
            clean();
        };
    }
    public long getTotalMemory(Context context) {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();
            arrayOfString = str2.split("\\s+");
            for (String num : arrayOfString) {
                Log.i(str2, num + "\t");
            }
            int i = Integer.valueOf(arrayOfString[1]).intValue();
            initial_memory = new Long((long) i * 1024);
            localBufferedReader.close();
        } catch (IOException e) {
        }
        return initial_memory;
    }
    public long getAvailMemory(Context context){
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    public void clean(){
        double threadnum=read("threadnum")>0?read("threadnum"):1,
                eatpercent=read("eatpercent")>0?read("eatpercent")*0.01:0.6;
        for(int i=0;i<threadnum;i++){
            if(read("rootmode")<1){
                new Thread(){
                    @Override
                    public void run(){
                        if(read("shellmode")<1){
                            cleaner((long)(totalMemory*eatpercent/threadnum));
                            showmemory.sendEmptyMessage(1);
                        }
                        else try {
                            Runtime runtime=Runtime.getRuntime();
                            runtime.exec(getApplicationInfo().nativeLibraryDir+"/libcleaner.so "+(long)((totalMemory*eatpercent/threadnum)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }else{
                new Thread(){
                    @Override
                    public void run(){
                        try {
                            Runtime runtime=Runtime.getRuntime();
                            runtime.exec("su -c "+getApplicationInfo().nativeLibraryDir+"/libcleaner.so "+(long)((totalMemory*eatpercent/threadnum)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    }
    public void clean(View button){
        button.setEnabled(false);
        clean();
        new Thread(){
            @Override
            public void run(){
                try {
                    if(read("shellmode")>0||read("rootmode")>0){
                        sleep(6000);
                        showmemory.sendEmptyMessage(1);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void setting(View view){
        LinearLayout layout=(LinearLayout) View.inflate(this,R.layout.setting,findViewById(R.id.setting));
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setView(layout);
        EditText threadnum=layout.findViewById(R.id.setting1),
                eatpercent=layout.findViewById(R.id.setting2);
        CheckBox rootmode=layout.findViewById(R.id.setting3),
                shellmode=layout.findViewById(R.id.setting4);
        threadnum.setText((read("threadnum")!=-1?read("threadnum"):1)+"");
        eatpercent.setText((read("eatpercent")!=-1?read("eatpercent"):60)+"");
        rootmode.setChecked(read("rootmode")>0);
        shellmode.setChecked(read("shellmode")>0);
        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int tnum=Integer.parseInt(threadnum.getText()+""),
                        eper=Integer.parseInt(eatpercent.getText()+"");
                if(tnum>0)save("threadnum",tnum);
                else{
                    Toast.makeText(MainActivity.this, "线程数异常，已重置", Toast.LENGTH_SHORT).show();
                    save("threadnum",1);
                }
                if(eper>0&&eper<=100)save("eatpercent",eper);
                else{
                    Toast.makeText(MainActivity.this, "挤压量异常，已重置", Toast.LENGTH_SHORT).show();
                    save("eatpercent",60);
                }
                save("rootmode",rootmode.isChecked()?1:0);
                save("shellmode",shellmode.isChecked()?1:0);
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }
    public void auto(View view){
        save("autoclean",((CheckBox)view).isChecked()?1:0);
    }
    public void save(String name,int num){
        getSharedPreferences("data", MODE_PRIVATE).edit().putInt(name,num).commit();
    }
    public int read(String name){
        return getSharedPreferences("data", MODE_PRIVATE).getInt(name,-1);
    }
    public native int cleaner(long l);
}