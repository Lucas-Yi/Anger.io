package com.unimelb.angry_io.Entity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.unimelb.angry_io.System.WorldView;

/**
 * Created by Yi on 9/1/2015.
 */
public class AI_Ball extends Entity {

    private final String TAG = "AI_Ball";
    private EntityManager entityManager;

    private float invisibleRidius = 300;
    private float detectRadius = 600;
    private int weight;
    private float png_width, png_height;
    private float last_GameX, last_GameY;
    // Used for onDraw();
    private Paint paint = new Paint();

    public AI_Ball(EntityManager entityManager, Bitmap texture, int png_width, int png_height, int radius) {
        super(0, 0, radius, 0, 0, null);
        this.png_width = png_width;
        this.png_height = png_height;
        setScrn_pos(new Coord((float) Math.random() * png_width, (float) Math.random() * png_height));

        last_GameX = WorldView.GameMapX;
        last_GameY = WorldView.GameMapY;

        setDir_x((float) Math.random());
        setDir_y((float) Math.random());

        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        weight = radius / 20;
        updatePosition(scrn_pos.x, scrn_pos.y);
    }

    public void updatePosition(float x, float y) {
        this.scrn_pos.x = x;
        this.scrn_pos.y = y;
    }

    public float getBallRadius() {
        return radius;
    }

    public void setBallRadius(float ballRadius) {
        this.radius = ballRadius;
    }

    public float getInvisibleRadius() {
        return invisibleRidius;
    }

    public float getDetectRadius() {
        return detectRadius;
    }

    public void moveBall() {
        scrn_pos.x += dir_x;
        scrn_pos.y += dir_y;
    }

    public void relativeMove() {
        if (WorldView.GameMapX != last_GameX) {
            scrn_pos.x = scrn_pos.x + WorldView.GameMapX - last_GameX;
            last_GameX = WorldView.GameMapX;
        }
        if (WorldView.GameMapY != last_GameY) {
            scrn_pos.y = scrn_pos.y + WorldView.GameMapY - last_GameY;
            last_GameY = WorldView.GameMapY;
        }
    }

    public void updatePhysics() {
        if (scrn_pos.x <= WorldView.GameMapX + getBallRadius() || scrn_pos.x >= WorldView.GameMapX + png_width - getBallRadius()) {
            dir_x = -dir_x;
            //Reverse direction and slow down ball
        }
        if (scrn_pos.y <= WorldView.GameMapY + getBallRadius() || scrn_pos.y >= WorldView.GameMapY + png_height - getBallRadius()) {
            //Reverse direction and slow down ball
            dir_y = -dir_y;
        }
    }

    // The update function for ai_ball
    public void update() {
        updatePhysics();
        relativeMove();
        moveBall();
    }

    public void onDraw(Canvas canvas) {
        if (canvas != null)
            canvas.drawCircle(scrn_pos.x, scrn_pos.y, radius, paint);
    }

    public int getWeight() {
        return weight;
    }
}