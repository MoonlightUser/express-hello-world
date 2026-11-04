package com.eventbrief;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends Activity {
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    long id=getIntent().getLongExtra("event_id",-1),start=getIntent().getLongExtra("event_start",-1);
    EventBriefData.Item item=null;
    for(EventBriefData.Item x:EventBriefData.load(this))if(x.id==id&&x.start==start){item=x;break;}
    ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);
    root.setOrientation(1);root.setPadding(25,45,25,32);root.setBackgroundColor(0xFF121D2C);
    scroll.addView(root);
    txt(root,"EVENT BRIEF",14,0xFF9DE5B8);
    txt(root,item==null?"Мероприятие не найдено":item.title,25,Color.WHITE);
    if(item!=null){
      txt(root,(item.allDay?"Весь день":new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(item.start)))
        +" · "+EventBriefData.statusLabel(item.status),14,0xFFA8E5C6);
      txt(root,"Что будет",20,Color.WHITE);txt(root,item.summary,17,0xFFE4EDF7);
      if(!item.why.isEmpty()){txt(root,"Почему интересно",20,Color.WHITE);txt(root,item.why,16,0xFFC8D7E9);}
      Button go=new Button(this);go.setText("Подробнее и регистрация в календаре");
      long eventId=item.id;
      go.setOnClickListener(v->{
        Uri uri=ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,eventId);
        try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){}
      });
      root.addView(go);
    }
    setContentView(scroll);
  }
  private void txt(LinearLayout root,String s,int size,int color){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
    t.setPadding(0,0,0,15);root.addView(t,new LinearLayout.LayoutParams(-1,-2));
  }
}
