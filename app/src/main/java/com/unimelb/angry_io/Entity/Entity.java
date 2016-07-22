package com.unimelb.angry_io.Entity;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.unimelb.angry_io.System.WorldView;

/**
 * This class is used to unify all the common methods used in entities classes.
 * Created by lizy on 3/09/15.
 */
public abstract class Entity {
    // the bit map texture of entities
    protected Bitmap texture;

    // The position of entities
    protected Coord scrn_pos;
    protected Coord map_pos;

    // The speed of entities
    protected float dir_x, dir_y;


    // !! Right now, we only have circle and radius is enough
    // But what if new object need to be added? and how to handle them?
    protected float radius;

    // used for when there should be something done by the entity themselves
    public abstract void update();

    // Each entity should have a method to draw itself
    public abstract void onDraw(Canvas canvas);

    //!! This should return a field that represent the entity,
    // Is there an existing java class can represent them all in one?
    //public abstract void getFiled();

    // This method should involve the getFiled() method, and then can determine whether
    // current entity collide with the input one
    //public  boolean CheckCollision(Entity AnotherOne);

    // !! This construction method is meant to be used by Food.
    // Therefore it does not have texture & direction speed inputs.
    public Entity(float pos_x, float pos_y, float radius, float dir_x, float dir_y, Bitmap texture) {
        this.scrn_pos = new Coord();
        this.map_pos = new Coord();
        this.scrn_pos.x = pos_x;
        this.scrn_pos.y = pos_y;
        this.radius = radius;
        this.dir_x = dir_x;
        this.dir_y = dir_y;
        this.texture = texture;
    }

    // Getters and Setters
    public Coord map2scrn(Coord map_pos) {
        Coord temp_scrn_pos = new Coord();
        temp_scrn_pos.x = map_pos.x + WorldView.GameMapX;
        temp_scrn_pos.y = map_pos.y + WorldView.GameMapY;
        return temp_scrn_pos;
    }

    public Coord scrn2map(Coord scrn_pos) {
        Coord temp_map_pos = new Coord();
        temp_map_pos.x = this.scrn_pos.x - WorldView.GameMapX;
        temp_map_pos.y = this.scrn_pos.y - WorldView.GameMapY;
        return temp_map_pos;
    }

    public float getDir_y() {
        return dir_y;
    }

    public void setDir_y(float dir_y) {
        this.dir_y = dir_y;
    }

    public float getDir_x() {
        return dir_x;
    }

    public void setDir_x(float dir_x) {
        this.dir_x = dir_x;
    }

    public float getRadius() {
        return radius;
    }

    public Coord getScrn_pos() {
        return scrn_pos;
    }

    public void setScrn_pos(Coord scrn_pos) {
        this.scrn_pos = scrn_pos;
    }

    public Coord getMap_pos() {
        return map_pos;
    }

    public void setMap_pos(Coord map_pos) {
        this.map_pos = map_pos;
    }


}
