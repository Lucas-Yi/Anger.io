package com.unimelb.angry_io.Entity;

import android.content.Context;
import android.graphics.Canvas;

/**
 * Created by lizy on 10/10/15.
 */
public class OnlinePlayer extends Player {
    private String TAG = "OnlinePlayer";

    private String Player_id;
    private String nick_name;

    public OnlinePlayer(PlayerInfo playerInfo, EntityManager entityManager, Context ctx) {
        super(playerInfo,entityManager, ctx);


    }

    // use playerInfo to get  messages about that player
    // update all the balls
    public void updatePlayerInfo(PlayerInfo playerInfo){

    }

    @Override
    public void onDraw(Canvas canvas){
        for(Ball ball:balls){
            ball.onDraw(canvas);
        }
    }
}
