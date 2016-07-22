package com.unimelb.angry_io.Entity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.FloatMath;
import android.util.Log;

import com.unimelb.angry_io.System.CONFIG;
import com.unimelb.angry_io.System.WorldView;


public class Ball extends Entity {

    private final String TAG = "Ball";

    private float weight;// indicate how many food/player this player has eat. and decide its radius.

    private Bitmap PlayerIcon = null;
    private Bitmap OriPlayerIcon = null;

    private int png_height;
    private int png_width;

    private float splitPower;
    private float mergePower;
    private boolean move_back = false;
    private int character = 0;

    // Used for onDraw();
    private Paint paint = new Paint();


    public Ball(Bitmap bmp, float scrn_pos_x, float scrn_pos_y, float radius) {
        super(scrn_pos_x, scrn_pos_y, radius, 0, 0, bmp);
        this.png_width = WorldView.png_width;
        this.png_height = WorldView.png_height;

        // Set the color part, if there is bitmap texture then should use bitmap instead;
        if (bmp == null) {
            paint.setAntiAlias(true);
            paint.setColor(Color.RED);
        } else {
            // TODO use texture
            PlayerIcon = bmp;
            OriPlayerIcon = bmp;
        }

        weight = radius / 2;
        init_power();
    }


    public float getBallRadius() {
        return radius;
    }


    // TODO
    // might want to keep weight so player can eat player and
    // increase "appropriate" weight
    public void growUp() {

        radius = weight * 2;

        if (radius > 100)
            radius = 100;


        //Update speed scale
        CONFIG.speedScale = CONFIG.BALL_INI_RADIUS / radius;
        //For specific player icon

        if (OriPlayerIcon != null) {
            PlayerIcon = Tools.scaleCircle(OriPlayerIcon, radius * 2, false);
        }


    }


    public void init_power() {
        splitPower = 0;
        mergePower = 0;
        move_back = false;
    }

    public void check_power() {
        splitPower++;
        Log.d(TAG, "check_power :" + splitPower);
        if (splitPower < CONFIG.BALL_SPLIT_STEPS) {
            mergePower = getBallRadius();
            Log.d(TAG, "check_power00 :" + splitPower);
        } else if (splitPower >= CONFIG.BALL_SPLIT_STEPS && splitPower < CONFIG.BALL_SPLIT_STEPS * 2 - 3) {
            mergePower = -getBallRadius();

            Log.d(TAG, "check_power00 :" + splitPower);
        } else {
            Log.d(TAG, "check_power00 :" + splitPower);
            WorldView.split_icon_touched = false;
            WorldView.hasSplit = false;
            init_power();
        }

    }

    public void move_front(float dir_x, float dir_y) {
        float dir = FloatMath.sqrt(dir_x * dir_x + dir_y * dir_y);
        float unit_x = dir_x / dir;
        float unit_y = dir_y / dir;


        scrn_pos.x += mergePower * unit_x;
        scrn_pos.y += mergePower * unit_y;

        check_power();
    }

    public void move_behind(float dir_x, float dir_y) {
        float dir = FloatMath.sqrt(dir_x * dir_x + dir_y * dir_y);
        float unit_x = -dir_x / dir;
        float unit_y = -dir_y / dir;
        if (move_back == false) {
            scrn_pos.x += splitPower * unit_x;
            scrn_pos.y += splitPower * unit_y;
            move_back = true;
        }
    }


    // The update function for ball
    // TODO player tell ball its position
    // and might reset init power
    public void update() {
        growUp();
        this.setMap_pos(scrn2map(scrn_pos));
    }

    public void onDraw(Canvas canvas) {
        if (canvas != null)
            if (PlayerIcon == null)
                canvas.drawCircle(scrn_pos.x, scrn_pos.y, radius, paint);
            else canvas.drawBitmap(PlayerIcon, scrn_pos.x - radius, scrn_pos.y - radius, null);

    }

    // Probably used by player on food/player to check if it can it.
    public boolean checkCanEatFood(Entity entity) {
        float distance = FloatMath.sqrt(
                (this.scrn_pos.x - entity.scrn_pos.x) * (this.scrn_pos.x - entity.scrn_pos.x)
                        + (this.scrn_pos.y - entity.scrn_pos.y) * (this.scrn_pos.y - entity.scrn_pos.y));
        if (distance < this.radius) {
            return true;
        } else {
            return false;
        }
    }

    // increase the weight of ball and relative getter.
    public void increase(int eated) {
        weight += eated;
    }

    public float getWeight() {
        return weight;
    }


    // Getter and Setter for touch and sensor speed
    public void setCharacter(int character) {
        this.character = character;
    }

    public int getCharacter() {
        return character;
    }
}