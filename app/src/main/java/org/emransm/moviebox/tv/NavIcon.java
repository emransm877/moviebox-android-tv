package org.emransm.moviebox.tv;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Consistent outline icons, drawn natively at the current display density. */
final class NavIcon extends Drawable {
    private final String name;private final Paint pen=new Paint(3);private final int size;
    NavIcon(String name,int color,int size){this.name=name;this.size=size;pen.setColor(color);pen.setStyle(Paint.Style.STROKE);pen.setStrokeWidth(1.6f);pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);setBounds(0,0,size,size);}
    public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
    @Override public void draw(Canvas canvas){canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);Path path=new Path();
        switch(name){
            case "Discover":path.moveTo(3,10);path.lineTo(12,3);path.lineTo(21,10);path.lineTo(21,21);path.lineTo(15,21);path.lineTo(15,14);path.lineTo(9,14);path.lineTo(9,21);path.lineTo(3,21);path.close();break;
            case "Movies":canvas.drawRoundRect(3,4,21,21,2,2,pen);canvas.drawLine(3,10,21,10,pen);canvas.drawLine(7,4,10,10,pen);canvas.drawLine(14,4,17,10,pen);break;
            case "TV Shows":canvas.drawRoundRect(3,7,21,21,2,2,pen);canvas.drawLine(8,2,12,7,pen);canvas.drawLine(17,2,12,7,pen);break;
            case "My list":path.moveTo(6,3);path.lineTo(18,3);path.lineTo(18,22);path.lineTo(12,17);path.lineTo(6,22);path.close();break;
            case "History":canvas.drawCircle(12,12,9,pen);canvas.drawLine(12,6,12,12,pen);canvas.drawLine(12,12,17,15,pen);break;
            case "Search":canvas.drawCircle(10,10,6.5f,pen);canvas.drawLine(15,15,21,21,pen);break;
            case "Settings":canvas.drawCircle(12,12,7,pen);canvas.drawCircle(12,12,2.5f,pen);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,pen);}break;
        }
        canvas.drawPath(path,pen);canvas.restore();
    }
    @Override public void setAlpha(int alpha){pen.setAlpha(alpha);}@Override public void setColorFilter(ColorFilter filter){pen.setColorFilter(filter);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
