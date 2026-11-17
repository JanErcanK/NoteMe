package de.noteme.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

final class Ui {
    static final int PAPER = 0xfffaf8f3, INK = 0xff292921, MUTED = 0xff64645a, RULE = 0xffdedbd2, PURPLE = 0xff625185, LILAC = 0xffe9e2f2;
    static int dp(Context c, float v) { return Math.round(c.getResources().getDisplayMetrics().density*v); }
    static GradientDrawable shape(Context c,int fill,int radius,int stroke) {
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(c,radius));
        if(stroke!=0) g.setStroke(dp(c,1),stroke); return g;
    }
    static void pad(View v,int x,int y) { v.setPadding(dp(v.getContext(),x),dp(v.getContext(),y),dp(v.getContext(),x),dp(v.getContext(),y)); }
    static TextView label(Context c,String value,int size,int color) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setIncludeFontPadding(false); return t;
    }
    static TextView title(Context c,String value,int size) {
        TextView t=label(c,value,size,INK); t.setTypeface(Typeface.SERIF); return t;
    }
    static TextView small(Context c,String value) {
        TextView t=label(c,value,11,MUTED); t.setLetterSpacing(.10f); t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); return t;
    }
    static TextView button(Context c,String value,boolean primary,Runnable action) {
        TextView t=label(c,value,15,primary?Color.WHITE:INK); t.setGravity(Gravity.CENTER); pad(t,14,8);
        t.setMinHeight(dp(c,48)); t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22696477),shape(c,primary?PURPLE:PAPER,15,primary?0:RULE),null));
        t.setFocusable(true); t.setOnClickListener(v->action.run()); return t;
    }
    static LinearLayout column(Context c) { LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c) { LinearLayout l=new LinearLayout(c); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    static void gap(LinearLayout l,int h) { l.addView(new Space(l.getContext()),1,dp(l.getContext(),h)); }
    static void flex(LinearLayout l,View v) { l.addView(v,new LinearLayout.LayoutParams(0,-2,1)); }
    static void line(LinearLayout l) { View v=new View(l.getContext()); v.setBackgroundColor(RULE); l.addView(v,-1,dp(l.getContext(),1)); }
}

