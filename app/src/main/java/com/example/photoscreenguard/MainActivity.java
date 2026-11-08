package com.example.photoscreenguard;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    EditText weekday,weekend,normal,dim,onMinutes,offMinutes;
    TextView status;
    int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
    @Override public void onCreate(Bundle b){super.onCreate(b); createUi();}
    TextView label(String str){ TextView t=new TextView(this);t.setText(str);t.setTextSize(15);t.setTextColor(Color.rgb(45,45,45));t.setPadding(0,dp(13),0,dp(4));return t; }
    EditText edit(LinearLayout root,String title,String initial,boolean number){
        root.addView(label(title));
        EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(16);e.setText(initial);
        if(number)e.setInputType(2);
        root.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));return e;
    }
    void button(LinearLayout layout,String name,View.OnClickListener listener){
        Button b=new Button(this);b.setText(name); b.setAllCaps(false);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.topMargin=dp(10);layout.addView(b,p);b.setOnClickListener(listener);
    }
    void createUi(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);setContentView(sc);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(18),dp(22),dp(28));sc.addView(root);
        TextView heading=new TextView(this);heading.setText("相册屏幕管家");heading.setTextSize(24);heading.setTextColor(Color.BLACK);root.addView(heading);
        TextView intro=label("Android 8+ · 无需 Root · 后台亮度调度\n时间段用英文逗号分隔，例如 07:00-08:30,18:00-23:00。未列出的时间自动保持低亮度。");
        intro.setTextSize(13);root.addView(intro);
        weekday=edit(root,"周一至周五 · 活跃时间段",com.example.photoscreenguard.Settings.getString(this,"weekday","07:00-08:30,12:00-13:30,18:00-23:00"),false);
        weekend=edit(root,"周六与周日 · 活跃时间段",com.example.photoscreenguard.Settings.getString(this,"weekend","08:00-23:00"),false);
        normal=edit(root,"正常亮度（1～100%）",""+com.example.photoscreenguard.Settings.getInt(this,"normal",60),true);
        dim=edit(root,"休息亮度（1～100%）",""+com.example.photoscreenguard.Settings.getInt(this,"dim",5),true);
        onMinutes=edit(root,"正常展示分钟数（1～1440）",""+com.example.photoscreenguard.Settings.getInt(this,"on_minutes",30),true);
        offMinutes=edit(root,"低亮度休息分钟数（1～1440）",""+com.example.photoscreenguard.Settings.getInt(this,"off_minutes",20),true);
        status=label("");root.addView(status);
        button(root,"保存并启动",v->saveStart());
        button(root,"暂停并恢复原亮度",v->stop());
        button(root,"授予修改系统亮度权限",v->grant());
        TextView footer=label("使用说明：首次需授权“修改系统设置”。如后台停止运行，请在华为系统设置中将本应用设为手动管理，允许自启动和后台运行。服务运行时通知栏会出现常驻通知。\n\n本应用仅调整系统亮度，不会锁屏或关闭充电。建议先以 5% 休息亮度测试，确认符合你的实际观感。");footer.setTextSize(13);root.addView(footer);
        updateStatus();
    }
    int readInt(EditText e,int min,int max){
        int n=Integer.parseInt(e.getText().toString().trim());
        if(n<min||n>max)throw new IllegalArgumentException("数值需在 "+min+"～"+max+" 之间");
        return n;
    }
    void saveStart(){
        try{
            String w=weekday.getText().toString().trim(),e=weekend.getText().toString().trim();
            if(!com.example.photoscreenguard.Settings.validPeriods(w) || !com.example.photoscreenguard.Settings.validPeriods(e)) throw new IllegalArgumentException("时间段格式错误，请使用 07:00-08:30,18:00-23:00");
            int a=readInt(normal,1,100),d=readInt(dim,1,100),n=readInt(onMinutes,1,1440),r=readInt(offMinutes,1,1440);
            if(!Settings.System.canWrite(this)){grant();Toast.makeText(this,"请授权后再点击保存并启动",Toast.LENGTH_LONG).show();return;}
            com.example.photoscreenguard.Settings.prefs(this).edit().putString("weekday",w).putString("weekend",e)
                .putInt("normal",a).putInt("dim",d).putInt("on_minutes",n).putInt("off_minutes",r).putBoolean("enabled",true).apply();
            // Restart to reset phase and apply changed settings immediately.
            stopService(new Intent(this,BrightnessService.class));BrightnessService.start(this);
            Toast.makeText(this,"已保存并启动",Toast.LENGTH_SHORT).show();updateStatus();
        }catch(Exception ex){new AlertDialog.Builder(this).setMessage(ex.getMessage()).setPositiveButton("知道了",null).show();}
    }
    void stop(){com.example.photoscreenguard.Settings.prefs(this).edit().putBoolean("enabled",false).apply();BrightnessService.stop(this);updateStatus();}
    void grant(){
        try{Intent i=new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()));startActivity(i);}
        catch(Exception ex){startActivity(new Intent(Settings.ACTION_SETTINGS));}
    }
    void updateStatus(){status.setText("当前状态："+(com.example.photoscreenguard.Settings.enabled(this)?"已启用":"未启用")+" · 修改系统设置权限："+(Settings.System.canWrite(this)?"已授权":"未授权"));}
    @Override protected void onResume(){super.onResume();if(status!=null)updateStatus();}
}
