package de.noteme.app;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MainActivity extends Activity {
    private static final Locale DE=Locale.GERMAN;
    private static final DateTimeFormatter MONTH=DateTimeFormatter.ofPattern("MMMM yyyy",DE);
    private Notes notes;
    private YearMonth month=YearMonth.now();
    private boolean oldest, stars, picking, awaitingPermission, initialResume=true;
    private final LinkedHashSet<Long> selected=new LinkedHashSet<>();
    private List<Notes.Entry> all, visible;
    private LinearLayout root, header, footer;
    private ListView list;
    private int rememberedPosition, rememberedTop;
    private android.content.SharedPreferences prefs;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener bearChanged=(p,k)->{
        if("enabled".equals(k) && hasWindowFocus()) render(true);
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        notes=new Notes(this); prefs=getSharedPreferences("bear",MODE_PRIVATE);
        if(state!=null) {
            String m=state.getString("month"); month=m==null?null:YearMonth.parse(m);
            oldest=state.getBoolean("old"); stars=state.getBoolean("stars"); picking=state.getBoolean("pick");
            awaitingPermission=state.getBoolean("request");
            long[] ids=state.getLongArray("ids"); if(ids!=null) for(long id:ids) selected.add(id);
        }
        prefs.registerOnSharedPreferenceChangeListener(bearChanged);
    }
    @Override protected void onResume() {
        super.onResume();
        if(awaitingPermission) {
            awaitingPermission=false;
            if(Settings.canDrawOverlays(this)) BearService.enable(this,true);
            else Toast.makeText(this,"Der Randbär braucht die Freigabe „Über anderen Apps einblenden“. ",Toast.LENGTH_LONG).show();
        } else if(initialResume && BearService.enabled(this)) BearService.enable(this,true);
        initialResume=false; render(true);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("month",month==null?null:month.toString()); out.putBoolean("old",oldest); out.putBoolean("stars",stars);
        out.putBoolean("pick",picking); out.putBoolean("request",awaitingPermission);
        out.putLongArray("ids",selected.stream().mapToLong(Long::longValue).toArray());
    }
    private void toggleBear() {
        if(BearService.enabled(this)) { BearService.enable(this,false); render(true); return; }
        if(Settings.canDrawOverlays(this)) { BearService.enable(this,true); render(true); return; }
        new AlertDialog.Builder(this).setTitle("Randbär aktivieren")
            .setMessage("Damit der Bär am rechten Bildschirmrand auf dich warten kann, erlaube NoteMe in den Android-Einstellungen das Einblenden über anderen Apps.")
            .setNegativeButton("Später",null).setPositiveButton("Einstellungen öffnen",(d,w)->{
                try {
                    awaitingPermission=true;
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
                } catch(ActivityNotFoundException e) {
                    awaitingPermission=false; Toast.makeText(this,"Die Overlay-Einstellungen sind auf diesem Gerät nicht verfügbar.",Toast.LENGTH_LONG).show();
                }
            }).show();
    }
    private ZonedDateTime date(long millis) { return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()); }
    private void render(boolean retainScroll) {
        if(retainScroll && list!=null) {
            rememberedPosition=list.getFirstVisiblePosition();
            rememberedTop=list.getChildCount()==0?0:list.getChildAt(0).getTop();
        } else { rememberedPosition=0; rememberedTop=0; }
        all=notes.all(oldest); visible=new ArrayList<>();
        Set<Long> valid=new HashSet<>();
        for(Notes.Entry e:all) {
            if((month==null || month.equals(YearMonth.from(date(e.created)))) && (!stars || e.important)) {
                visible.add(e); valid.add(e.id);
            }
        }
        selected.retainAll(valid);
        root=Ui.column(this); root.setBackgroundColor(Ui.PAPER);
        root.setOnApplyWindowInsetsListener((v,i)->{
            v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());
            return i.consumeSystemWindowInsets();
        });
        if(android.os.Build.VERSION.SDK_INT<35) root.setOnApplyWindowInsetsListener(null);
        setContentView(root);
        list=new ListView(this); list.setDivider(null); list.setVerticalScrollBarEnabled(false);
        header=Ui.column(this); Ui.pad(header,20,0); Ui.gap(header,16);
        LinearLayout mast=Ui.row(this);
        TextView wordmark=Ui.title(this,"NoteMe",26); wordmark.setTypeface(Typeface.create("serif",Typeface.BOLD_ITALIC)); Ui.flex(mast,wordmark);
        mast.addView(Ui.small(this,LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd. MMM",DE)).toUpperCase(DE)));
        header.addView(mast); Ui.gap(header,14); Ui.line(header); Ui.gap(header,22);
        header.addView(Ui.title(this,"Mein Notizbuch",38)); Ui.gap(header,16);
        LinearLayout hero=Ui.row(this); Ui.pad(hero,16,14); hero.setBackground(Ui.shape(this,Ui.LILAC,20,0));
        LinearLayout copy=Ui.column(this); boolean on=BearService.enabled(this);
        copy.addView(Ui.small(this,on?"●  AKTIV":"SCHNELLZUGRIFF")); Ui.gap(copy,5);
        copy.addView(Ui.title(this,"Dein Randbär",25)); Ui.gap(copy,5);
        copy.addView(Ui.label(this,on?"Tippe rechts auf den Bären, um zu schreiben.":"Öffne eine Notiz über jeder App.",14,Ui.MUTED)); Ui.gap(copy,10);
        TextView toggle=Ui.button(this,on?"Bär ausblenden":"Bär aktivieren",false,this::toggleBear);
        toggle.setBackground(Ui.shape(this,0xfff8f5fa,12,0xffcbbfd5)); copy.addView(toggle,-2,-2); Ui.flex(hero,copy);
        hero.addView(new BearArt(this,BearArt.Scene.NOTEBOOK),Ui.dp(this,107),Ui.dp(this,125)); header.addView(hero); Ui.gap(header,24);
        LinearLayout section=Ui.row(this);
        Ui.flex(section,Ui.title(this,"Notizen",27));
        TextView options=Ui.button(this,picking?"Abbrechen":"Optionen",false,()->{ if(picking) clearSelection(); else options(); });
        section.addView(options);
        if(on) section.setPadding(0,0,Ui.dp(this,36),0);
        header.addView(section); Ui.gap(header,10);
        LinearLayout filter=Ui.row(this);
        TextView period=Ui.button(this,(month==null?"Alle Monate":month.format(MONTH))+"  ▾",false,this::chooseMonth);
        period.setTextSize(14); filter.addView(period);
        if(stars) { TextView badge=Ui.label(this,"   ★ Wichtig",14,Ui.PURPLE); filter.addView(badge); }
        header.addView(filter); Ui.gap(header,12); Ui.line(header);
        if(visible.isEmpty()) {
            Ui.gap(header,16); LinearLayout empty=Ui.row(this);
            empty.addView(new BearArt(this,BearArt.Scene.PAGES),Ui.dp(this,65),Ui.dp(this,72));
            LinearLayout hint=Ui.column(this);
            hint.addView(Ui.title(this,all.isEmpty()?"Deine erste Notiz":"Keine passenden Notizen",20)); Ui.gap(hint,4);
            hint.addView(Ui.label(this,all.isEmpty()?"Halte einen Gedanken mit „Neue Notiz“ fest.":"Wähle einen anderen Monat oder Filter.",14,Ui.MUTED));
            Ui.flex(empty,hint); header.addView(empty);
        }
        list.addHeaderView(header,null,false);
        list.setAdapter(new EntryAdapter());
        list.setOnItemClickListener((a,v,pos,itemId)->{
            int i=pos-list.getHeaderViewsCount(); if(i<0 || i>=visible.size()) return;
            Notes.Entry e=visible.get(i); if(picking) select(e.id); else edit(e.id);
        });
        list.setOnItemLongClickListener((a,v,pos,itemId)->{
            int i=pos-list.getHeaderViewsCount(); if(i<0 || i>=visible.size()) return false;
            picking=true; select(visible.get(i).id); return true;
        });
        root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        footer=Ui.column(this); Ui.pad(footer,20,10); root.addView(footer);
        if(!picking) footer.addView(Ui.button(this,"＋   Neue Notiz",true,()->edit(0)));
        else footer.addView(Ui.button(this,selected.size()+" ausgewählt  ·  Aktionen",true,this::actions));
        list.setSelectionFromTop(rememberedPosition,rememberedTop);
    }
    private class EntryAdapter extends BaseAdapter {
        public int getCount(){return visible.size();}
        public Object getItem(int i){return visible.get(i);}
        public long getItemId(int i){return visible.get(i).id;}
        public View getView(int i,View old,ViewGroup parent){
            Notes.Entry e=visible.get(i); boolean checked=selected.contains(e.id);
            LinearLayout item=Ui.column(MainActivity.this); Ui.pad(item,20,0); item.setBackgroundColor(checked?Ui.LILAC:Ui.PAPER);
            Ui.gap(item,13); String stamp=date(e.created).format(DateTimeFormatter.ofPattern("dd. MMM yyyy  ·  HH:mm",DE));
            item.addView(Ui.small(MainActivity.this,(picking?(checked?"☑  ":"☐  "):"")+(e.important?"★  ":"")+stamp.toUpperCase(DE)));
            Ui.gap(item,7); TextView body=Ui.label(MainActivity.this,e.body.trim(),18,Ui.INK); body.setMaxLines(3); body.setEllipsize(TextUtils.TruncateAt.END);
            item.addView(body); Ui.gap(item,13); Ui.line(item); return item;
        }
    }
    private void edit(long id) { startActivity(new Intent(this,CaptureActivity.class).putExtra(CaptureActivity.NOTE_ID,id)); }
    private void select(long id) { if(!selected.remove(id)) selected.add(id); render(true); }
    private void clearSelection() { selected.clear(); picking=false; render(true); }
    private void options() {
        String[] labels={oldest?"Neueste zuerst":"Älteste zuerst",stars?"Alle Wichtigkeitsstufen":"Nur wichtige Notizen","Notizen auswählen"};
        new AlertDialog.Builder(this).setTitle("Notizen ordnen").setItems(labels,(d,i)->{
            if(i==0) oldest=!oldest; if(i==1) stars=!stars; if(i==2) picking=true;
            render(false);
        }).show();
    }
    private void chooseMonth() {
        TreeSet<YearMonth> months=new TreeSet<>(Comparator.reverseOrder()); months.add(YearMonth.now());
        for(Notes.Entry e:all) months.add(YearMonth.from(date(e.created)));
        ArrayList<YearMonth> choices=new ArrayList<>(months); choices.add(0,null);
        String[] labels=new String[choices.size()];
        for(int i=0;i<labels.length;i++) labels[i]=i==0?"Alle Monate":choices.get(i).format(MONTH);
        new AlertDialog.Builder(this).setTitle("Monatsarchiv").setSingleChoiceItems(labels,choices.indexOf(month),(d,i)->{
            month=choices.get(i); selected.clear(); picking=false; d.dismiss(); render(false);
        }).show();
    }
    private void actions() {
        if(selected.isEmpty()) { Toast.makeText(this,"Tippe auf die gewünschten Notizen.",Toast.LENGTH_SHORT).show(); return; }
        boolean onlyStars=true; for(Notes.Entry e:visible) if(selected.contains(e.id) && !e.important) onlyStars=false;
        final boolean mark=!onlyStars;
        ArrayList<String> labels=new ArrayList<>();
        labels.add(mark?"Als wichtig markieren":"Wichtig-Markierung entfernen");
        if(selected.size()>1) labels.add("Zusammenführen");
        labels.add("Löschen");
        new AlertDialog.Builder(this).setTitle("Auswahl bearbeiten").setItems(labels.toArray(new String[0]),(d,i)->{
            if(i==0) { notes.mark(selected,mark); clearSelection(); }
            else if(i==labels.size()-1) confirm(false);
            else confirm(true);
        }).show();
    }
    private void confirm(boolean merge) {
        String verb=merge?"Zusammenführen":"Löschen";
        String detail=merge?"Die Texte werden chronologisch in der ältesten Notiz verbunden.":"Die ausgewählten Notizen werden dauerhaft gelöscht.";
        new AlertDialog.Builder(this).setTitle(selected.size()+" Notizen bearbeiten").setMessage(detail)
            .setNegativeButton("Abbrechen",null).setPositiveButton(verb,(d,w)->{
                if(merge) notes.merge(selected); else notes.delete(selected); clearSelection();
            }).show();
    }
    @Override public void onBackPressed() { if(picking) clearSelection(); else super.onBackPressed(); }
    @Override protected void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(bearChanged); notes.close(); super.onDestroy();
    }
}

