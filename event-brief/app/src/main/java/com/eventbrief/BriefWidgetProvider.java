package com.eventbrief;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class BriefWidgetProvider extends AppWidgetProvider {
  static final String REFRESH="com.eventbrief.ACTION_REFRESH";
  @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){
    for(int id:ids)update(c,m,id);
    m.notifyAppWidgetViewDataChanged(ids,R.id.event_list);
  }
  @Override public void onReceive(Context c,Intent i){
    super.onReceive(c,i);
    if(i!=null && (REFRESH.equals(i.getAction()) || Intent.ACTION_DATE_CHANGED.equals(i.getAction()) ||
       Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction()))) refreshAll(c);
  }
  static void refreshAll(Context c){
    AppWidgetManager m=AppWidgetManager.getInstance(c);
    int[] ids=m.getAppWidgetIds(new ComponentName(c,BriefWidgetProvider.class));
    for(int id:ids)update(c,m,id);
    if(ids.length>0)m.notifyAppWidgetViewDataChanged(ids,R.id.event_list);
  }
  private static void update(Context c,AppWidgetManager m,int id){
    RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
    Intent service=new Intent(c,EventWidgetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
    service.setData(Uri.parse("eventbrief://widget/"+id));
    v.setRemoteAdapter(R.id.event_list,service);
    v.setEmptyView(R.id.event_list,R.id.empty_text);
    PendingIntent refresh=PendingIntent.getBroadcast(c,id,
      new Intent(c,BriefWidgetProvider.class).setAction(REFRESH),
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    v.setOnClickPendingIntent(R.id.widget_refresh,refresh);
    PendingIntent app=PendingIntent.getActivity(c,id+100000,new Intent(c,MainActivity.class),
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    v.setOnClickPendingIntent(R.id.widget_title,app);
    PendingIntent detail=PendingIntent.getActivity(c,id,new Intent(c,DetailActivity.class),
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
    v.setPendingIntentTemplate(R.id.event_list,detail);
    m.updateAppWidget(id,v);
  }
}
