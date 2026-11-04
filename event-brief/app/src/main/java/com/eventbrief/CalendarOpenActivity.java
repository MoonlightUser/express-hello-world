package com.eventbrief;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;

public class CalendarOpenActivity extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    Intent incoming=getIntent();
    long id=incoming.getLongExtra("event_id",-1L);
    long begin=incoming.getLongExtra("event_start",-1L);
    if(id>=0) {
      Uri uri=ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,id);
      Intent calendar=new Intent(Intent.ACTION_VIEW,uri);
      calendar.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if(begin>0)calendar.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,begin);
      try {
        startActivity(calendar);
      }catch(Exception ex) {
        try {
          Intent fallback=new Intent(Intent.ACTION_MAIN);
          fallback.addCategory(Intent.CATEGORY_APP_CALENDAR);
          startActivity(fallback);
        }catch(Exception ignored) { }
      }
    }
    finish();
  }
}
