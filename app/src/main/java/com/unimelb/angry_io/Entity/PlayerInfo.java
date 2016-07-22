package com.unimelb.angry_io.Entity;

/**
 * Created by lizy on 24/09/15.
 */
public class PlayerInfo {
    public float getPos_x() {
        return pos_x;
    }

    public void setPos_x(float pos_x) {
        this.pos_x = pos_x;
    }

    public float getPos_y() {
        return pos_y;
    }

    public void setPos_y(float pos_y) {
        this.pos_y = pos_y;
    }

    public String getNick_name() {
        return nick_name;
    }


    public String getPlayer_id() {
        return player_id;
    }


    float pos_x, pos_y;
    String nick_name;
    String player_id;

    public PlayerInfo(String player_id, String nickName, float pos_x, float pos_y) {
        this.pos_x = pos_x;
        this.pos_y = pos_y;
        this.nick_name = nickName;
        this.player_id = player_id;
    }

    public PlayerInfo() {

    }
}
