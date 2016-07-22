package com.unimelb.angry_io.Entity;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.unimelb.angry_io.System.CONFIG;
import com.unimelb.angry_io.System.WorldView;

/**
 * Created by lizy on 7/09/15.
 */
public class Food extends Entity
{
    private final String TAG="Food";
    private Coord map_pos = new Coord();
    private Paint paint=new Paint();
    private int color;


    private int weight= CONFIG.FOOD_WEIGHT;

    public Food(float map_pos_x, float map_pos_y, int color, float radius) {
        super(map_pos_x+WorldView.GameMapX, map_pos_y+ WorldView.GameMapY, CONFIG.FOOD_RADIUS, 0, 0, null);

        paint.setAntiAlias(true);
	this.color = color;
        paint.setColor(color);
        map_pos.x = map_pos_x;
        map_pos.y = map_pos_y;
    }

    @Override
    public void update() {
        scrn_pos = map2scrn(this.map_pos);
    }


    @Override
    public void onDraw(Canvas canvas) {
        if(canvas != null)
            canvas.drawCircle(scrn_pos.x,scrn_pos.y, radius, paint);
    }

    // return the weight of this food, by default it should be 1.
    public int getWeight() {
        return weight;
    }
    public int getColor() {return color;}

}
