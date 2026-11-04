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
    if ("registered".equals(status)) return "✓ Регистрация подтверждена";
    if ("open".equals(status)) return "✓ Свободный вход";
    return "◇ Рекомендация · регистрация не подтверждена";
  }
  private static String field(String block, String name) {
    Pattern p = Pattern.compile("(?mi)^\\s*" + name + "\\s*:\\s*(.*?)\\s*$");
    Matcher m = p.matcher(block);
    return m.find() ? m.group(1).trim() : "";
  }
  private static String clean(String s) {
    return s.replaceAll("(?i)https?://\\S+","").replaceAll("<[^>]*>","").replaceAll("\\s+"," ").trim();
  }
  public static List<Item> load(Context ctx) {
    List<Item> result = new ArrayList<>();
    if (ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return result;
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0);
    cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
    long today=cal.getTimeInMillis();
    cal.add(Calendar.DAY_OF_MONTH,1);
    long tomorrow=cal.getTimeInMillis();
    cal.add(Calendar.DAY_OF_MONTH,1);
    long end=cal.getTimeInMillis();
    Uri.Builder b=CalendarContract.Instances.CONTENT_URI.buildUpon();
    ContentUris.appendId(b,today); ContentUris.appendId(b,end);
    String[] columns={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,
      CalendarContract.Instances.DESCRIPTION,CalendarContract.Instances.BEGIN,CalendarContract.Instances.ALL_DAY};
    Set<String> seen=new HashSet<>();
    try(Cursor c=ctx.getContentResolver().query(b.build(),columns,null,null,CalendarContract.Instances.BEGIN+" ASC")) {
      if(c==null)return result;
      while(c.moveToNext()){
        long id=c.getLong(0),start=c.getLong(3);
        if(start<today || start>=end)continue;
        String title=c.getString(1),raw=c.getString(2);
        if(raw==null)continue;
        String lower=title==null ? "" : title.toLowerCase(Locale.ROOT);
        if(lower.matches("(?s).*\\bcomp[0-9]{3}\\b.*") ||
           lower.matches("(?s)^(lecture|tutorial|lab|exam|test|лекция|экзамен|лабораторная)\\b.*"))continue;
        int i=raw.indexOf("[EVENTBRIEF_V1]");
        int j=raw.indexOf("[/EVENTBRIEF_V1]",i<0?0:i);
        if(i<0 || j<=i)continue;
        // Google Calendar may fold the newlines inside description metadata.
        // Parse labels using whitespace-independent delimiters.
        String block=raw.substring(i+15,j).replaceAll("(?i)<br\\s*/?>"," ")
          .replaceAll("\\s+"," ").trim();
        Matcher metadata=Pattern.compile(
          "(?i)^STATUS\\s*:\\s*(registered|open|suggested)\\s+"+
          "CATEGORY\\s*:\\s*(career|social)\\s+"+
          "SUMMARY\\s*:\\s*(.*?)\\s+WHY\\s*:\\s*(.+)$"
        ).matcher(block);
        if(!metadata.matches())continue;
        String status=metadata.group(1).toLowerCase(Locale.ROOT);
        String category=metadata.group(2).toLowerCase(Locale.ROOT);
        String summary=clean(metadata.group(3));
        String why=clean(metadata.group(4));
        if(!status.equals("suggested") && !status.equals("registered") && !status.equals("open"))continue;
        if(!category.equals("career") && !category.equals("social"))continue;
        if(summary.isEmpty() || !seen.add(id+":"+start))continue;
        Item e=new Item();e.id=id;e.start=start;e.title=title==null?"Мероприятие":title;
        e.summary=summary;e.why=why;e.status=status;e.category=category;
        e.day=start<tomorrow?0:1;e.allDay=c.getInt(4)==1;
        result.add(e);
      }
    }catch(Exception ignored){return new ArrayList<>();}
    result.sort(Comparator.comparingInt((Item x)->x.day).thenComparingLong(x->x.start));
    return result;
  }
}
