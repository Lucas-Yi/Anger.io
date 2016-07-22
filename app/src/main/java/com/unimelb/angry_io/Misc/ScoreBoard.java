package com.unimelb.angry_io.Misc;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.unimelb.angry_io.Entity.EntityManager;
import com.unimelb.angry_io.Entity.OnlinePlayer;
import com.unimelb.angry_io.Entity.Player;
import com.unimelb.angry_io.System.CONFIG;
import com.unimelb.angry_io.System.WorldView;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by lizy on 13/10/15.
 */
public class ScoreBoard {

    //
    private WorldView worldView = null;
    private EntityManager entityManager = null;
    //
    private static boolean score_board_isOn = false;
    // Constant paint for score board in top right corner
    private Paint score_icon = new Paint();
    private Paint score_text = new Paint();

    // Rank related
    private int local_rank = 0;
    private int local_score=0;
    private int highest_score=0;
    private ArrayList<OnlinePlayer> onlinePlayers = new ArrayList<OnlinePlayer>();

    // start time
    private long start_time;

    public ScoreBoard(WorldView worldView){
        this.worldView = worldView;

        //initialization for the icons
        score_icon.setAntiAlias(true);
        score_icon.setStyle(Paint.Style.FILL);

        score_text.setAntiAlias(true);
        score_text.setColor(Color.BLUE);
        score_text.setStyle(Paint.Style.FILL_AND_STROKE);
        score_text.setTextSize(50);

    }

    public void add_entityManager(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    public void get_one_press(){
        score_board_isOn = !score_board_isOn;
    }

    public void onDraw(Canvas canvas){
        if(score_board_isOn){
            onDrawScoreBoardIcon(canvas, Color.LTGRAY);
            onDrawScoreBoard(canvas);

            updateRank();
            onDrawRank(canvas);

        }else{
            onDrawScoreBoardIcon(canvas, Color.DKGRAY);
        }

        onDrawInfo(canvas);
    }

    // Show the scoreBoard icon in the top right  corner
    private void onDrawScoreBoardIcon(Canvas canvas,int score_icon_color){
        // initialize a rectangle for the score board icon
        score_icon.setColor(score_icon_color);
        if(canvas != null){
            canvas.drawRect(worldView.getWidth() - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_SCORE_Width, CONFIG.ICON_DIS_TO_EDGE
                    , worldView.getWidth()- CONFIG.ICON_DIS_TO_EDGE, CONFIG.ICON_DIS_TO_EDGE + CONFIG.ICON_SCORE_Height
                    , score_icon);
            canvas.drawText("ScoreBoard"
                    , worldView.getWidth() - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_SCORE_Width + CONFIG.ICON_SCORE_TEXT_EDGE_DIS
                    , CONFIG.ICON_SCORE_Height + CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_SCORE_TEXT_EDGE_DIS
                    , score_text);
        }
    }

    private void onDrawScoreBoard(Canvas canvas){
        if(canvas != null){
            canvas.drawRect(worldView.getWidth() - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_SCORE_Width
                    , CONFIG.ICON_DIS_TO_EDGE + CONFIG.ICON_SCORE_Height
                    , worldView.getWidth() - CONFIG.ICON_DIS_TO_EDGE
                    , CONFIG.ICON_DIS_TO_EDGE + CONFIG.ICON_SCORE_Height + CONFIG.ICON_BOARD_Height
                    , score_icon);
        }
    }

    private void onDrawRank(Canvas canvas){
        int onlinePlayers_idx = 0;
        boolean local_player_in = false;
        String rank_player = "null";
        if(canvas != null){
            for(int i=1; i <=5 ;i ++){

                if(onlinePlayers.isEmpty()){
                    if(!local_player_in){
                        rank_player = entityManager.getLocal_player().getNick_name() + " : "
                                        +entityManager.getLocal_player().getScore();
                        local_player_in = true;
                    }else{
                        rank_player = "None";
                    }
                }else{
                    if(onlinePlayers_idx<onlinePlayers.size() && !local_player_in){
                        if(entityManager.local_player.getScore() > onlinePlayers.get(onlinePlayers_idx).getScore()){
                            rank_player = entityManager.getLocal_player().getNick_name() + " : "
                                    +entityManager.getLocal_player().getScore() ;
                            local_player_in = true;
                        }else{
                            rank_player = onlinePlayers.get(onlinePlayers_idx).getNick_name() + ":"
                                    + onlinePlayers.get(onlinePlayers_idx).getScore();
                            onlinePlayers_idx++;
                        }
                    }else if(!local_player_in){
                        rank_player = entityManager.getLocal_player().getNick_name() + " : "
                                +entityManager.getLocal_player().getScore() ;
                        local_player_in = true;
                    }else if(onlinePlayers_idx<onlinePlayers.size()){
                        rank_player = onlinePlayers.get(onlinePlayers_idx).getNick_name() + ":"
                                + onlinePlayers.get(onlinePlayers_idx).getScore();
                        onlinePlayers_idx++;
                    }else{
                        rank_player = "None";
                    }
                }

                canvas.drawText("" +i+ "."+ rank_player
                        , worldView.getWidth() - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_SCORE_Width + CONFIG.ICON_SCORE_TEXT_EDGE_DIS
                        , CONFIG.ICON_SCORE_Height*(1+i) + CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_SCORE_TEXT_EDGE_DIS
                        , score_text);
            }

        }
    }

    public void onDrawInfo(Canvas canvas){

        canvas.drawText(worldView.getStrScheme()+"\n Position:"
                +"("+Math.round(worldView.GameMapX)+", "+Math.round(worldView.GameMapY)+")"
                , 30, 50, score_text);
        canvas.drawText("Rank:" + getLocal_rank()
                ,30,50+CONFIG.ICON_SCORE_Height,score_text);
        canvas.drawText("Time:" + getElapsed_time()
                ,30,50+CONFIG.ICON_SCORE_Height*2,score_text);
        canvas.drawText("Score:" + getLocal_score()
                ,30,50+CONFIG.ICON_SCORE_Height*3,score_text);
        canvas.drawText("Food Eat:" + getFood_Consumed()
                ,30,50+CONFIG.ICON_SCORE_Height*4,score_text);
        canvas.drawText("Highest Score:" + getHighest_score()
                ,30,50+CONFIG.ICON_SCORE_Height*5,score_text);

    }

    public void update(){
        updateRank();
    }
    private void updateRank(){
        this.onlinePlayers = entityManager.getOnline_Players();
        Collections.sort(onlinePlayers);

        // decide the rank of local player
        if(this.onlinePlayers.isEmpty()){
            local_rank = 1;
        }else{
            local_rank = 1;
            for(Player onlinePlayer: onlinePlayers){
                if(onlinePlayer.getScore() < entityManager.local_player.getScore()){
                    break;
                }else{
                    local_rank++;
                }
            }
        }
    }

    public int getLocal_rank(){
        return local_rank;
    }


    public int getLocal_score(){
        if(entityManager.getLocal_player()!=null){
            local_score = entityManager.getLocal_player().getScore();
            if(highest_score < local_score){
                highest_score = local_score;
            }
            return entityManager.getLocal_player().getScore();
        }else{
            return local_score;
        }
    }

    public int getFood_Consumed(){
        if(entityManager.getLocal_player() != null){
            return entityManager.getLocal_player().getFoods_eaten();
        }else{
            return 0;
        }
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public String getElapsed_time(){
        long elapsed_time= System.currentTimeMillis() - this.start_time;
        return formatMillis(elapsed_time);
    }
    public int getHighest_score(){
        return highest_score;
    }
    
    static public String formatMillis(long val) {
        StringBuilder                       buf=new StringBuilder(20);
        String                              sgn="";

        if(val<0) { sgn="-"; val=Math.abs(val); }

        append(buf,sgn,0,( val/3600000             ));
        append(buf,":",2,((val%3600000)/60000      ));
        append(buf,":",2,((val         %60000)/1000));
        //append(buf,".",3,( val                %1000));
        return buf.toString();
    }

    /** Append a right-aligned and zero-padded numeric value to a `StringBuilder`. */
    static private void append(StringBuilder tgt, String pfx, int dgt, long val) {
        tgt.append(pfx);
        if(dgt>1) {
            int pad=(dgt-1);
            for(long xa=val; xa>9 && pad>0; xa/=10) { pad--;           }
            for(int  xa=0;   xa<pad;        xa++  ) { tgt.append('0'); }
        }
        tgt.append(val);
    }


}
