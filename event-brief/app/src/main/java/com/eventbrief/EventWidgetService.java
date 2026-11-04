package com.eventbrief;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventWidgetService extends RemoteViewsService {
  @Override public RemoteViewsFactory onGetViewFactory(Intent intent){return new Factory(getApplicationContext());}
  private static final class Factory implements RemoteViewsFactory {
    final Context c;final List<EventBriefData.Item> items=new ArrayList<>();final List<Integer> positions=new ArrayList<>();
    Factory(Context c){this.c=c;}
    @Override public void onCreate(){onDataSetChanged();}
    @Override public void onDestroy(){items.clear();positions.clear();}
    @Override public void onDataSetChanged(){
      items.clear();items.addAll(EventBriefData.load(c));positions.clear();
      for(int d=0;d<2;d++){
        positions.add(d==0?-1:-2);
        boolean any=false;
        for(int i=0;i<items.size();i++)if(items.get(i).day==d){positions.add(i);any=true;}
        if(!any)positions.add(-3);
      }
    }
    @Override public int getCount(){return positions.size();}
    @Override public RemoteViews getLoadingView(){return null;}
    @Override public int getViewTypeCount(){return 2;}
    @Override public long getItemId(int p){if(p<0||p>=positions.size())return p;
      int k=positions.get(p);return k<0?k:items.get(k).id*37+items.get(k).start;}
    @Override public boolean hasStableIds(){return false;}
    @Override public RemoteViews getViewAt(int p){
      if(p<0||p>=positions.size())return null;
      int n=positions.get(p);
      if(n<0){
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.row_header);
        v.setTextViewText(R.id.header_text,n==-1?"СЕГОДНЯ":n==-2?"ЗАВТРА":"Пока нет подходящих мероприятий");
        return v;
      }
      EventBriefData.Item e=items.get(n);
      RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.row_event);
      v.setTextViewText(R.id.event_title,e.title);
      String time=e.allDay?"Весь день":new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(e.start));
      v.setTextViewText(R.id.event_meta,time+" · "+EventBriefData.statusLabel(e.status));
      v.setTextColor(R.id.event_meta,"social".equals(e.category)?0xFFFFCA92:0xFF9DE5B8);
      v.setTextViewText(R.id.event_summary,e.summary);
      v.setTextViewText(R.id.event_why,e.why.isEmpty()?"":"Почему интересно: "+e.why);
      Intent detail=new Intent();detail.putExtra("event_id",e.id);detail.putExtra("event_start",e.start);
      v.setOnClickFillInIntent(R.id.event_row,detail);
      return v;
    }
  }
}
