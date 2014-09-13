package com.kernel5.dotvpn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.graphics.Shader.TileMode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;

/**
 * GraphView creates a scaled line or bar graph with x and y axis labels. 
 * @author Arno den Hond
 *
 */
public class GraphView extends View {

    public static int BAR = 0;
    public static int LINE = 1;

    private Paint paint			= new Paint();
    private float[] valuesi		= new float[0];
    private float[] valueso		= new float[0];
    private String[] horlabels	= new String[0];
    private String[] verlabels	= new String[0];
    private String titlei		= "";
    private String titleo		= "";
    private Context mContext;

    public GraphView(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }
    
    public void setup(Context context, 
                     float[] valuesi, 
                     float[] valueso, 
                     String titlei, 
                     String titleo, 
                     String[] horlabels, 
                     String[] verlabels
                     ) {

        mContext = context;
        if (valuesi == null)
            valuesi = new float[0];
        else
            this.valuesi = valuesi;
        if (valueso == null)
            valueso = new float[0];
        else
            this.valueso = valueso;
        if (titlei == null)
            titlei = "";
        else
            this.titlei = titlei;
        if (titleo == null)
            titleo = "";
        else
            this.titleo = titleo;
        if (horlabels == null)
            this.horlabels = new String[0];
        else
            this.horlabels = horlabels;
        if (verlabels == null)
            this.verlabels = new String[0];
        else
            this.verlabels = verlabels;
//        paint = new Paint();
    }

    public void setData( 
         float[] valuesi, 
         float[] valueso, 
         String titlei, 
         String titleo, 
         String[] horlabels, 
         String[] verlabels
        ) {

        if (valuesi == null)
            valuesi = new float[0];
        else
            this.valuesi = valuesi;
        if (valueso == null)
            valueso = new float[0];
        else
            this.valueso = valueso;
        if (titlei == null)
            titlei = "";
        else
            this.titlei = titlei;
        if (titleo == null)
            titleo = "";
        else
            this.titleo = titleo;
        if (horlabels == null)
            this.horlabels = new String[0];
        else
            this.horlabels = horlabels;
        if (verlabels == null)
            this.verlabels = new String[0];
        else
            this.verlabels = verlabels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float border = 30;
        float horstart = border * 2;
        float horend = 10;
        float height = getHeight();
        float width = getWidth() - 1;
        float maxi = getMaxi();
        float mini = getMini();
        float maxo = getMaxo();
        float mino = getMino();
        float diffi = maxi - mini;
        float diffo = maxo - mino;
        float graphheight = height - (2 * border);
        float graphwidth = width-(2*border)-horend;

        // Shader shader = new LinearGradient(0, 0, getWidth()-1, getHeight(), 
        //                 Color.rgb(128,58,91), Color.rgb(58,5,30), TileMode.CLAMP);
        // Paint paints = new Paint();
        // paints.setShader(shader);
        // canvas.drawRect(new RectF(0, 0, getWidth()-1, getHeight()), paints);

        try {
           Bitmap bgbit = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.graph_bg); 
           canvas.drawBitmap(bgbit, new Rect(0, 0, bgbit.getWidth(), bgbit.getHeight()), 
                                    new RectF(0, 0, getWidth(), getHeight()), null);
        } catch ( Exception e ) {
           Log.v( Constants.TAG, "Couldn't set background image" );
        }

        paint.setTextAlign(Align.LEFT);
        int vers = verlabels.length - 1;
        for (int i = 0; i < verlabels.length; i++) {
            paint.setColor(Color.WHITE);
            float y = ((graphheight / vers) * i) + border;
            canvas.drawLine(horstart, y, width-horend, y, paint);
            paint.setColor(Color.WHITE);
            canvas.drawText(verlabels[i], 5, y, paint);
        }

        int hors = horlabels.length-1;
        for (int i = 0; i < horlabels.length; i++) {
            paint.setColor(Color.WHITE);
            float x = ((graphwidth / hors) * i) + horstart;
            canvas.drawLine(x, height-border, x, border, paint);
            paint.setTextAlign(Align.CENTER);
            if (i==horlabels.length-1)
                paint.setTextAlign(Align.RIGHT);
            if (i==0)
                paint.setTextAlign(Align.LEFT);
            paint.setColor(Color.WHITE);
            canvas.drawText(horlabels[i], x, height - 4, paint);
        }

        paint.setTextAlign(Align.CENTER);
        paint.setColor(Color.GREEN);
        canvas.drawText(titlei, (graphwidth/4) + horstart, border - 10, paint);
        paint.setColor(Color.RED);
        canvas.drawText(titleo, 3*(graphwidth/4) + horstart, border - 10, paint);

/*
        if ( mini != maxi && mino != maxo )
        {
          float datalength = valuesi.length;
          float colwidth = (width - (2 * border)) / datalength;
          for (int i = 0; i < valuesi.length; i++) {
             float vali = valuesi[i] - mini;
             float rati = vali / diffi;
             float hi = graphheight * rati;
             float valo = valueso[i] - mino;
             float rato = valo / diffo;
             float ho = graphheight * rato;
             if ( vali >= valo )
             {
                paint.setColor(Color.argb(128,0,255,0));
                canvas.drawRect((i * colwidth) + horstart, 
                             (border - hi) + graphheight, 
                             ((i * colwidth) + horstart) + (colwidth - 1), 
                             height - (border - 1), paint);
                paint.setColor(Color.argb(128,255,0,0));
                canvas.drawRect((i * colwidth) + horstart, 
                               (border - ho) + graphheight, 
                               ((i * colwidth) + horstart) + (colwidth - 1), 
                               height - (border - 1), paint);
              }
              else
              {
                paint.setColor(Color.argb(128,255,0,0));
                canvas.drawRect((i * colwidth) + horstart, 
                               (border - ho) + graphheight, 
                               ((i * colwidth) + horstart) + (colwidth - 1), 
                               height - (border - 1), paint);
                paint.setColor(Color.argb(128,0,255,0));
                canvas.drawRect((i * colwidth) + horstart, 
                             (border - hi) + graphheight, 
                             ((i * colwidth) + horstart) + (colwidth - 1), 
                             height - (border - 1), paint);
              }

            }
        }
  
*/ 

        if ( mini != maxi )
        {
            paint.setColor(Color.RED);
            float datalength = valuesi.length;
            float colwidth = (width-(2*border)-horend) / datalength;
            float halfcol = colwidth / 2;
            float lasth = 0;
            for (int i = 0; i < valuesi.length; i++) {
              float val = valuesi[i] - mini;
              float rat = val / diffi;
              float h = graphheight * rat;
              if (i > 0)
              {
                 canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, 
                                 (border - lasth) + graphheight, 
                                 (i * colwidth) + (horstart + 1) + halfcol, 
                                 (border - h) + graphheight, paint);
              }
              lasth = h;
            }
        }
        if ( mino != maxo )
        {
            paint.setColor(Color.GREEN);
            float datalength = valueso.length;
            float colwidth = (width-(2*border)-horend) / datalength;
            float halfcol = colwidth / 2;
            float lasth = 0;
            for (int i = 0; i < valueso.length; i++) {
                float val = valueso[i] - mino;
                float rat = val / diffo;
                float h = graphheight * rat;
                if (i > 0)
                {
                   canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, 
                                    (border - lasth) + graphheight, 
                                    (i * colwidth) + (horstart + 1) + halfcol, 
                                    (border - h) + graphheight, paint);
                }
                lasth = h;
            }
         }
  
    }

    private float getMaxi() {
        float largest = Integer.MIN_VALUE;
        for (int i = 0; i < valuesi.length; i++)
            if (valuesi[i] > largest)
                largest = valuesi[i];
        return largest;
    }

    private float getMini() {
        float smallest = Integer.MAX_VALUE;
        for (int i = 0; i < valuesi.length; i++)
            if (valuesi[i] < smallest)
                smallest = valuesi[i];
        return smallest;
    }

    private float getMaxo() {
        float largest = Integer.MIN_VALUE;
        for (int i = 0; i < valueso.length; i++)
            if (valueso[i] > largest)
                largest = valueso[i];
        return largest;
    }

    private float getMino() {
        float smallest = Integer.MAX_VALUE;
        for (int i = 0; i < valueso.length; i++)
            if (valueso[i] < smallest)
                smallest = valueso[i];
        return smallest;
    }

}
