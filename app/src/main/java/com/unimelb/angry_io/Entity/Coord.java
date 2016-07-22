package com.unimelb.angry_io.Entity;

/**
 * Create a new Point just because the one provided by andorid only use int
 * Created by lizy on 24/09/15.
 */
public class Coord {
    public float x, y;

    public Coord(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Coord() {
        this.x = 0;
        this.y = 0;
    }

    public String toString() {
        return "(" + this.x + "," + this.y + ")";
    }
}
