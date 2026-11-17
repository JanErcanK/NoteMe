package de.noteme.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** An upright, jointed paper illustration. All geometry is in illustration coordinates. */
final class BearArt extends View {
    enum Scene { EDGE, NOTEBOOK, PAGES }
    private final Scene scene;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int INK=0xff523e35, FUR=0xffc7966d, LIGHT=0xffe7bd91, BLUSH=0xffd78f79, CREAM=0xffffe5c1;
    BearArt(Context c, Scene scene) { super(c); this.scene=scene; }
    private void fill(int col) { p.setStyle(Paint.Style.FILL); p.setColor(col); }
    private void stroke(int col,float width) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(width); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); p.setColor(col); }
    private void oval(Canvas c,float x,float y,float rx,float ry,int col,boolean outline) {
        fill(col); c.drawOval(x-rx,y-ry,x+rx,y+ry,p);
        if(outline) { stroke(INK,1.65f); c.drawOval(x-rx,y-ry,x+rx,y+ry,p); }
    }
    private void line(Canvas c,float x,float y,float a,float b,int color,float width) {
        stroke(color,width); c.drawLine(x,y,a,b,p);
    }
    private void round(Canvas c,float x,float y,float w,float h,float r,int col,int edge) {
        fill(col); c.drawRoundRect(x,y,x+w,y+h,r,r,p);
        if(edge!=0) { stroke(edge,1.5f); c.drawRoundRect(x,y,x+w,y+h,r,r,p); }
    }
    private void star(Canvas c,float x,float y,float size,int color) {
        Path a=new Path(); a.moveTo(x,y-size); a.quadTo(x+1,y-1,x+size,y); a.quadTo(x+1,y+1,x,y+size);
        a.quadTo(x-1,y+1,x-size,y); a.quadTo(x-1,y-1,x,y-size); a.close(); fill(color); c.drawPath(a,p);
    }
    private void bear(Canvas c,float lift) {
        // The gripping paw stays on the right-hand edge while the shoulders rise.
        c.save(); c.translate(0,-lift);
        c.save(); c.rotate(-28,75,86); oval(c,75,86,24,32,FUR,true); c.restore();
        oval(c,72,88,13,17,LIGHT,false);
        c.save(); c.rotate(-13,44,48);
        oval(c,22,27,11,12,FUR,true); oval(c,22,27,6,7,BLUSH,false);
        oval(c,65,24,11,12,FUR,true); oval(c,65,24,6,7,BLUSH,false);
        oval(c,45,49,31,29,FUR,true);
        oval(c,31,56,5,3,BLUSH,false); oval(c,61,54,5,3,BLUSH,false);
        oval(c,46,59,13,10,CREAM,false);
        oval(c,34,46,2.5f,3.1f,INK,false); oval(c,55,45,2.5f,3.1f,INK,false);
        oval(c,34.7f,45,.75f,.8f,0xffffffff,false); oval(c,55.7f,44,.75f,.8f,0xffffffff,false);
        oval(c,45,54,4.5f,3,INK,false);
        Path smile=new Path(); smile.moveTo(45,56); smile.lineTo(45,60); smile.quadTo(42,64,39,60);
        smile.moveTo(45,60); smile.quadTo(48,63,51,59); stroke(INK,1.3f); c.drawPath(smile,p);
        line(c,38,28,40,26,LIGHT,1.7f); line(c,43,27,45,25,LIGHT,1.7f);
        c.restore(); c.restore();
        c.save(); c.rotate(-24,77,61); oval(c,77,61,10,13,FUR,true);
        line(c,73,56,73,60,INK,1); line(c,77,55,77,59,INK,1); c.restore();
    }
    private void notebook(Canvas c) {
        c.save(); c.rotate(-10,58,73);
        round(c,21,21,75,105,6,0xffaf9dc6,INK);
        round(c,17,17,75,105,6,0xfffcf4df,INK);
        round(c,16,14,75,104,6,0xffc2b0d4,INK);
        line(c,28,15,28,116,INK,1.4f);
        round(c,35,42,46,24,3,0xfff9f1dc,INK);
        line(c,42,51,73,51,INK,1.4f); line(c,46,57,69,57,INK,1);
        for(int j=0;j<3;j++) line(c,25,120+j*1.6f,85,120+j*1.6f,0xff9b8670,.6f);
        c.restore();
    }
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        boolean motion=ValueAnimator.areAnimatorsEnabled() && isShown();
        long phase=SystemClock.uptimeMillis()%6800L;
        float lift=motion && phase<2100 ? (float)(2.5*(1-Math.cos(phase*Math.PI*2/2100.0))) : 0;
        float w=scene==Scene.EDGE?82:155, h=scene==Scene.EDGE?108:146;
        float s=Math.min(getWidth()/w,getHeight()/h);
        c.save(); c.translate(scene==Scene.EDGE?getWidth()-w*s:(getWidth()-w*s)/2,(getHeight()-h*s)/2); c.scale(s,s);
        if(scene==Scene.EDGE) { bear(c,lift); }
        else if(scene==Scene.NOTEBOOK) {
            star(c,15,29,5,0xff85709e); star(c,138,86,6,0xff85709e);
            c.save(); c.translate(65,1); c.scale(.9f,.9f); bear(c,lift); c.restore();
            notebook(c);
            c.save(); c.rotate(16,95,71); oval(c,95,71,8,10,FUR,true);
            line(c,92,66,92,70,INK,1); line(c,96,66,96,70,INK,1); c.restore();
        } else {
            c.save(); c.translate(13,0); notebook(c); c.restore();
            c.save(); c.rotate(30,108,75); round(c,104,34,8,68,1,0xffd7ad65,INK);
            Path tip=new Path(); tip.moveTo(104,102);tip.lineTo(112,102);tip.lineTo(108,112);tip.close();
            fill(CREAM);c.drawPath(tip,p);stroke(INK,1.4f);c.drawPath(tip,p);c.restore();
        }
        c.restore();
        if(motion && scene!=Scene.PAGES) postInvalidateDelayed(phase<2100 ? 65 : 6800-phase);
    }
}

