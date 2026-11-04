package com.eventbrief;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class MainActivity extends Activity {
  LinearLayout feed;TextView status;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    ScrollView sv=new ScrollView(this);LinearLayout root=new LinearLayout(this);
    root.setPadding(25,43,25,25);root.setOrientation(1);root.setBackgroundColor(0xFF121D2C);sv.addView(root);
    label(root,"☀ Event Brief",28,Color.WHITE);
    label(root,"Сегодня и завтра: что будет на интересных мероприятиях. Без лекций и адресов.",15,0xFFCCDEEA);
    status=label(root,"",14,0xFFA6EBC9);
    Button permit=new Button(this);permit.setText("Разрешить чтение календаря");
    permit.setOnClickListener(v->{if(checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},100); else reload();});root.addView(permit);
    Button pin=new Button(this);pin.setText("Добавить виджет на главный экран");
    pin.setOnClickListener(v->{AppWidgetManager m=AppWidgetManager.getInstance(this);
      if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,BriefWidgetProvider.class),null,null);
      else Toast.makeText(this,"Главный экран → Виджеты → Event Brief",Toast.LENGTH_LONG).show();});root.addView(pin);
    Button update=new Button(this);update.setText("Обновить");update.setOnClickListener(v->reload());root.addView(update);
    feed=new LinearLayout(this);feed.setOrientation(1);root.addView(feed);
    label(root,"Данные читаются из Google Calendar на телефоне. Только мероприятия с меткой EVENTBRIEF_V1, которую добавляет Opportunity Scout.",12,0xFFB0C7DC);
    setContentView(sv);reload();
  }
  TextView label(LinearLayout root,String s,int size,int color){
    TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setPadding(0,0,0,16);root.addView(t);
    return t;
  }
  @Override protected void onResume(){super.onResume();if(feed!=null)reload();}
  void reload(){
    BriefWidgetProvider.refreshAll(this);
    List<EventBriefData.Item> events=EventBriefData.load(this);
    boolean allowed=checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED;
    status.setText(allowed?"✓ Календарь подключён · "+events.size()+" мероприятий":"Требуется доступ к календарю");
    feed.removeAllViews();
    for(int day=0;day<2;day++){
      label(feed,day==0?"СЕГОДНЯ":"ЗАВТРА",17,0xFF9DE5B8);
      int count=0;
      for(EventBriefData.Item e:events)if(e.day==day){
        count++;label(feed,e.title,20,Color.WHITE);
        label(feed,EventBriefData.statusLabel(e.status),12,0xFFB0E5CE);
        label(feed,e.summary,16,0xFFE3ECF7);
        if(!e.why.isEmpty())label(feed,"Почему интересно: "+e.why,14,0xFFBFD0E6);
      }
      if(count==0)label(feed,"Подходящих мероприятий пока нет",14,0xFFB0C4D5);
    }
  }
  @Override public void onRequestPermissionsResult(int req,String[] p,int[] g){super.onRequestPermissionsResult(req,p,g);if(req==100)reload();}
}
