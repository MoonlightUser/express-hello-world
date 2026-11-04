package com.eventbrief;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EventBriefData {
  private EventBriefData() {}

  public static final class Item {
    public long id, start;
    public String title, summary, why, status, category;
    public boolean allDay;
    public int day;
  }
  public static String statusLabel(String status) {
    switch(status) {
      case "registered": return "✓ Вы записаны";
      case "open": return "✓ Свободный вход";
      default: return "◇ Можно записаться";
    }
  }
  private static String clean(String s) {
    return s.replaceAll("(?i)https?://\\S+","")
      .replaceAll("<[^>]*>","").replaceAll("\\s+"," ").trim();
  }
  private static final Pattern BLOCK=Pattern.compile(
    "(?i)^STATUS\\s*:\\s*(registered|open|suggested)\\s+"+
    "CATEGORY\\s*:\\s*(career|social)\\s+"+
    "SUMMARY\\s*:\\s*(.*?)\\s+WHY\\s*:\\s*(.+)$"
  );

  private static void add(List<Item> out, Set<String> seen, long id, long start,
      String title, String description, boolean allDay, long today, long tomorrow, long end) {
    if(start<today || start>=end || description==null || title==null) return;
    String lower=title.toLowerCase(Locale.ROOT);
    if(lower.matches("(?s).*\\bcomp[0-9]{3}\\b.*") ||
       lower.matches("(?s)^(lecture|tutorial|lab|exam|test|лекция|экзамен|лабораторная)\\b.*")) return;

    int i=description.indexOf("[EVENTBRIEF_V1]");
    int j=i<0?-1:description.indexOf("[/EVENTBRIEF_V1]",i+15);
    if(i<0 || j<=i) return;
    // Google Calendar may fold arbitrary newlines in descriptions.
    String metadata=description.substring(i+15,j)
      .replaceAll("(?i)<br\\s*/?>"," ").replaceAll("\\s+"," ").trim();
    Matcher m=BLOCK.matcher(metadata);
    if(!m.matches())return;
    String category=m.group(2).toLowerCase(Locale.ROOT);
    String summary=clean(m.group(3)), why=clean(m.group(4));
    if(summary.isEmpty() || !seen.add(id+":"+start)) return;

    Item e=new Item();e.id=id;e.start=start;e.title=title;e.summary=summary;
    e.why=why;e.status=m.group(1).toLowerCase(Locale.ROOT);
    e.category=category;e.day=start<tomorrow?0:1;e.allDay=allDay;
    out.add(e);
  }

  public static List<Item> load(Context context) {
    List<Item> result=new ArrayList<>();
    if(context.checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)
      return result;
    Calendar cal=Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY,0);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);
    cal.set(Calendar.MILLISECOND,0);
    long today=cal.getTimeInMillis();
    cal.add(Calendar.DAY_OF_MONTH,1);
    long tomorrow=cal.getTimeInMillis();
    cal.add(Calendar.DAY_OF_MONTH,1);
    long end=cal.getTimeInMillis();
    Set<String> seen=new HashSet<>();

    // Instance query: includes occurrences of recurring events.
    Uri.Builder b=CalendarContract.Instances.CONTENT_URI.buildUpon();
    ContentUris.appendId(b,today);ContentUris.appendId(b,end);
    String[] instanceCols={
      CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,
      CalendarContract.Instances.DESCRIPTION,CalendarContract.Instances.BEGIN,
      CalendarContract.Instances.ALL_DAY
    };
    try(Cursor c=context.getContentResolver().query(b.build(),instanceCols,null,null,
        CalendarContract.Instances.BEGIN+" ASC")) {
      if(c!=null)while(c.moveToNext()) {
        add(result,seen,c.getLong(0),c.getLong(3),c.getString(1),c.getString(2),
            c.getInt(4)==1,today,tomorrow,end);
      }
    } catch(Exception ignored) { /* Event provider fallback below. */ }

    // Query non-recurring events directly as well. Some device calendar providers
    // return incomplete instance rows for recently synchronized events.
    String[] eventCols={
      CalendarContract.Events._ID,CalendarContract.Events.TITLE,
      CalendarContract.Events.DESCRIPTION,CalendarContract.Events.DTSTART,
      CalendarContract.Events.ALL_DAY,CalendarContract.Events.RRULE
    };
    try(Cursor c=context.getContentResolver().query(
        CalendarContract.Events.CONTENT_URI,eventCols,
        CalendarContract.Events.DTSTART+" >= ? AND "+CalendarContract.Events.DTSTART+" < ?",
        new String[]{String.valueOf(today),String.valueOf(end)},
        CalendarContract.Events.DTSTART+" ASC")) {
      if(c!=null)while(c.moveToNext()) {
        if(c.getString(5)!=null && !c.getString(5).trim().isEmpty())continue;
        add(result,seen,c.getLong(0),c.getLong(3),c.getString(1),c.getString(2),
            c.getInt(4)==1,today,tomorrow,end);
      }
    } catch(Exception ignored) { /* Provider may not expose Events directly. */ }
    result.sort(Comparator.comparingInt((Item x)->x.day).thenComparingLong(x->x.start));
    return result;
  }
}
