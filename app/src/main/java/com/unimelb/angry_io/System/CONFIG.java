package com.unimelb.angry_io.System;

import com.unimelb.angry_io.Entity.Coord;

import java.util.ArrayList;

/**
 * Storing all the global settings for the game.
 * Created by lizy on 3/09/15.
 */
public class CONFIG {
    //Ball related settings
    public static final float STDSPED=10;
    public static float speedScale = 1;
    public static final float BALL_INI_RADIUS=40;
    public static final float BALL_SPLIT_SHREHOLD=24;
    public static final int BALL_SPLIT_STEPS = 15;
    public static ArrayList<Coord> BALL_POSITIONS = new ArrayList<Coord>();

    // Moving Control setting
    public static final int CON_JUST_TOUCH=0;
    public static final int CON_JUST_SENSOR=1;
    public static final int CON_TOUCH_SENSOR=2;
    public static int CONTROL_MODE=2;

    // Food related settings
    public static final float FOOD_RADIUS=10;
    public static final int FOOD_WEIGHT=1;
    public static final int FOOD_NUMBER=300;

    // sensor related settings
    public static final float ACC_SENSOR_MAX=1.5f;
    // Actually, the maximum value of accmeter in my device is 19.6. But in practice,
    // the values typically fall in 0~10. And 10 only appears when mobile is "standing"
    // TODO, if maximum value varies from devices, I may want to set value to
    // mAccelerometer.getMaximumRange()/2. This may make sense.

    // Split icon related settings
    public static final float ICON_DIS_TO_EDGE=10;
    public static final float ICON_RADIUS=50;

    // Score icon related settings
    public static final float ICON_SCORE_Width=300;
    public static final float ICON_SCORE_Height = 80;
    public static final float ICON_BOARD_Height = 400;
    public static final float ICON_SCORE_TEXT_EDGE_DIS = 10;

    // Tags used for each developer (and game stage?)
    public static final String lizy="Lizy";
    public static final String randy="Randy";
    public static final String yi="Yi";
    public static final String TAG_UI="UI";

    //NETWORK
    static public boolean MASTER = false;
    static public int NW_BUFFER_SIZE=1024;
    static public int NW_FRAGMENT_SIZE=900;
    static public final String NW_LOG_DELIMITER="#*#";
    static public final String NW_FRA_DELIMITER="#frag#";

    // Game info
    static public String player_id="default";
    static public String id_just_server = "just_server";
    static public String player_nickname="default";

    // Mode information
    static public String SINGLE_MODE="single_player";
    static public String MULTI_MODE="multiple_players";
    static public String mode=SINGLE_MODE;

    // Spiker info
    static public int Spiker_Update_Frequency = 5;

    // Game Centre
    static public final String LEADERBOARD_ID = "CgkI9MDJgfEFEAIQBw";
}
