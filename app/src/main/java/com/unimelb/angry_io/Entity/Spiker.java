package com.unimelb.angry_io.Entity;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.FloatMath;

import com.unimelb.angry_io.R;
import com.unimelb.angry_io.System.CONFIG;
import com.unimelb.angry_io.System.WorldView;

/**
 * Created by lizy on 10/10/15.
 */
public class Spiker extends Entity {

    Paint paint = new Paint();
    Path path = new Path();
    Bitmap spiker_bmp1, spiker_bmp2 = null;
    float inner_half;
    int count = 0;
    Resources res;

    private Bitmap spiker_gph = null;

    public Spiker(float pos_x, float pos_y, float radius, Context mGameContext) {
        super(pos_x + WorldView.GameMapX, pos_y + WorldView.GameMapY, radius, 0, 0, null);
        inner_half = radius / 2;
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);
        map_pos.x = pos_x;
        map_pos.y = pos_y;

        res = mGameContext.getResources();
        try {
            spiker_bmp1 = BitmapFactory.decodeResource(res, R.drawable.spiker1);
            spiker_bmp1 = Bitmap.createScaledBitmap(spiker_bmp1, 300, 300, false);
            spiker_bmp2 = BitmapFactory.decodeResource(res, R.drawable.spiker2);
            spiker_bmp2 = Bitmap.createScaledBitmap(spiker_bmp2, 300, 300, false);
        } catch (OutOfMemoryError oe) {
        }
    }


    public void init_path() {
        float origin_x = this.map_pos.x + inner_half;
        float origin_y = this.map_pos.y;
        path.moveTo(origin_x, origin_y);

        Coord outter;
        Coord inner;

        for (int i = 0; i < 6; i++) {
            inner = new Coord(this.map_pos.x + (float) Math.cos(60 + i * 60) * inner_half
                    , this.map_pos.y + (float) Math.sin(60 + i * 60) * inner_half);
            outter = new Coord(this.map_pos.x + (float) Math.cos(30 + i * 60) * radius
                    , this.map_pos.y + (float) Math.cos(30 + i * 60) * radius);

            path.lineTo(outter.x, outter.y);
            path.lineTo(inner.x, inner.y);
        }
        path.close();
    }

    public boolean checkCanSpikeBall(Entity entity) {
        float distance = FloatMath.sqrt(
                (this.scrn_pos.x - entity.scrn_pos.x) * (this.scrn_pos.x - entity.scrn_pos.x)
                        + (this.scrn_pos.y - entity.scrn_pos.y) * (this.scrn_pos.y - entity.scrn_pos.y));
        if (distance < this.radius + entity.getRadius()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void update() {
        this.scrn_pos = map2scrn(this.map_pos);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (canvas != null)
            if (count < CONFIG.Spiker_Update_Frequency) {
                //TODO remove resource
                try {
                    canvas.drawBitmap(spiker_bmp1, this.scrn_pos.x - radius, scrn_pos.y - radius, null);
                } catch (NullPointerException ne) {
                }
                count++;
            } else {
                try {
                    canvas.drawBitmap(spiker_bmp2, this.scrn_pos.x - radius, scrn_pos.y - radius, null);
                } catch (NullPointerException ne) {
                }
                count++;
                if (count == 2 * CONFIG.Spiker_Update_Frequency - 1)
                    count = 0;
            }
    }
}