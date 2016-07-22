package com.unimelb.angry_io.Entity;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.unimelb.angry_io.Activities.MultipleGameActivity;
import com.unimelb.angry_io.Cmd.Cmd;
import com.unimelb.angry_io.Cmd.CmdFactory;
import com.unimelb.angry_io.Cmd.HandshakeCmd;
import com.unimelb.angry_io.Cmd.InitMasterCmd;
import com.unimelb.angry_io.Cmd.PROTOCOL;
import com.unimelb.angry_io.Network.BluetoothService;
import com.unimelb.angry_io.System.CONFIG;
import com.unimelb.angry_io.System.WorldView;

import java.util.ArrayList;
import java.util.Random;

/**
 * This class is created to manage all the entities.
 * Created by lizy on 3/09/15.
 */
public class EntityManager {
    private String TAG = "Entity Manager";
    private String TAGP = "Protocol";
    protected WorldView worldView;
    protected int scrn_width, scrn_height;
    public float init_GameMap_pos_x;
    public float init_GameMap_pos_y;
    int last_ballsize;

    public final ArrayList<Food> foods = new ArrayList<Food>();
    public final ArrayList<Spiker> spikers = new ArrayList<Spiker>();
    private final ArrayList<AI_Ball> ai_balls = new ArrayList<AI_Ball>();
    private final ArrayList<Collision_Detector> collision_detectors = new ArrayList<Collision_Detector>();
    private Paint layer_Paint = new Paint();


    public Player local_player = null;
    private PlayerInfo mLocalPlayerInfo = null;

    private ArrayList<OnlinePlayer> onlinePlayers = new ArrayList<OnlinePlayer>();

    private ArrayList<PlayerInfo> onlinePlayerInfos = new ArrayList<PlayerInfo>();
    private ArrayList<PlayerInfo> allPlayerInfos = new ArrayList<PlayerInfo>();
    public static boolean touched = false;

    // Initializion
    private CmdFactory cmdFactory = new CmdFactory();
    private int cmdseq = 0;

    public EntityManager(WorldView worldView, int width, int height) {
        this.worldView = worldView;
        layer_Paint.setColor(Color.WHITE);

        scrn_width = width;
        scrn_height = height;

        //Initialize the player positions
        //TODO get info random generated and distribute between players.
        PlayerInfo localinfo = new PlayerInfo("id", "nickname", width / 2, height / 2);
        local_player = new Player(localinfo, this, worldView.getContext());
        last_ballsize = local_player.balls.size();

        //randomly create food in the map
        generateFoods();
        generateSpiker();
        generate_positions(worldView.getPng_width(), worldView.getPng_height());
        worldView.getScoreBoard().setStart_time(System.currentTimeMillis());
    }

    public void initialization() {

        // Local players info
        mLocalPlayerInfo = new PlayerInfo(CONFIG.player_id, CONFIG.player_nickname, 0, 0);

        if (CONFIG.mode.equals(CONFIG.MULTI_MODE)) {
            if (CONFIG.MASTER) {
                Log.d(TAG, "I am master ");
                while (onlinePlayerInfos.size() < MultipleGameActivity.getNumConnected()) {
                    if (BluetoothService.isRxMsg()) {
                        String msgJson = BluetoothService.getRxMsg();
                        Log.d(TAGP, "!!!!!!!initialization: msgJson: " + msgJson);
                        Cmd tmp_cmd = cmdFactory.FromJSON(msgJson);

                        if (tmp_cmd != null) {
                            if (tmp_cmd.Type().equals(PROTOCOL.CMD_HANDSHAKE)) {
                                HandshakeCmd hs_cmd = (HandshakeCmd) cmdFactory.FromJSON(msgJson);
                                onlinePlayerInfos.add(hs_cmd.getPlayerInfo());
                                Log.d(TAGP, "initialization: Get a new PlayerInfo");
                            } else {
                                Log.d(TAGP, "initialization error; Received a msg that is not Handshake");
                            }
                        } else {
                            Log.d(TAGP, "initialization get a null object");
                        }
                    }
                }
                Log.d(TAGP, "initialization collect all");

                mLocalPlayerInfo.setPos_x(CONFIG.BALL_POSITIONS.get(0).x);
                mLocalPlayerInfo.setPos_y(CONFIG.BALL_POSITIONS.get(0).y);

                for (int i = 1; i < onlinePlayerInfos.size() && i <= 8; i++) {
                    onlinePlayerInfos.get(i).setPos_x(CONFIG.BALL_POSITIONS.get(i).x);
                    onlinePlayerInfos.get(i).setPos_y(CONFIG.BALL_POSITIONS.get(i).y);
                }

                // put all player info together
                allPlayerInfos.addAll(onlinePlayerInfos);
                allPlayerInfos.add(mLocalPlayerInfo);

                //randomly create food in the map
                generateFoods();// randy test
                InitMasterCmd allInfoCmd = new InitMasterCmd(CONFIG.player_id, allPlayerInfos, foods, cmdseq);
                MultipleGameActivity.sendBTMessage(allInfoCmd.ToJSON());
                cmdseq++;

            } else {
                Log.d(TAGP, "initialization send message ");
                HandshakeCmd client_init_hs = new HandshakeCmd(mLocalPlayerInfo.getPlayer_id(), mLocalPlayerInfo.getNick_name());
                String client_init_json = client_init_hs.ToJSON();
                MultipleGameActivity.sendBTMessage(client_init_json);

                while (allPlayerInfos.size() == 0) {
                    if (BluetoothService.isRxMsg()) {


                        String msgJson = BluetoothService.getRxMsg();
                        Log.d(TAGP, "Should be initMasterCmd: msgJson: " + msgJson);
                        Cmd tmp_cmd = cmdFactory.FromJSON(msgJson);

                        if (tmp_cmd != null) {
                            if (tmp_cmd.Type().equals(PROTOCOL.CMD_INITIALIZATION)) {
                                InitMasterCmd init_cmd = (InitMasterCmd) cmdFactory.FromJSON(msgJson);
                                allPlayerInfos.addAll(init_cmd.getPlayers());

                                //foods.addAll(init_cmd.getFoods());
                                Log.d(TAGP, "initialization: get all playerInfo");
                            } else {
                                Log.d(TAGP, "initialization error; Received a msg that is not initialization");
                            }
                        } else {
                            Log.d(TAGP, "initialization get a null object");
                        }
                    }
                }
            }
            // first assign player positions.
            assignPlayers();
            // initialize the positions
            initPosition();
        }
        //single mode
        else {

            worldView.GameMapX = this.scrn_width / 2 - CONFIG.BALL_POSITIONS.get(1).x;
            worldView.GameMapY = this.scrn_height / 2 - CONFIG.BALL_POSITIONS.get(1).y;

            init_GameMap_pos_x = worldView.GameMapX;
            init_GameMap_pos_y = worldView.GameMapY;

            // TODO set single mode
            mLocalPlayerInfo.setPos_x(this.scrn_width / 2);
            mLocalPlayerInfo.setPos_y(this.scrn_height / 2);

            local_player = new Player(mLocalPlayerInfo, this, worldView.getContext());
            generateAI_Balls();// randy test
            collision_detectors.clear();
            for (AI_Ball ai_ball : ai_balls) {
                for (Ball player_ball : local_player.balls) {
                    collision_detectors.add(new Collision_Detector(player_ball, ai_ball));
                }
            }

        }

        // update once the information needed
        worldView.getScoreBoard().update();
    }

    // if collect all the playerinfos , extract the local player out, and rest goest to onlinePlayers
    public void assignPlayers() {
        if (allPlayerInfos.size() > 1) {
            for (PlayerInfo playerInfo : allPlayerInfos) {

                if (playerInfo.getPlayer_id().equals(CONFIG.player_id)) {
                    mLocalPlayerInfo = playerInfo;
                } else {
                    onlinePlayerInfos.add(playerInfo);
                }
            }
        }

    }

    // TODO
    public void initPosition() {

        // First get mlocalplayerinfo from
        worldView.GameMapX = this.scrn_width / 2 - mLocalPlayerInfo.getPos_x();
        worldView.GameMapY = this.scrn_height / 2 - mLocalPlayerInfo.getPos_y();
        init_GameMap_pos_x = worldView.GameMapX;
        init_GameMap_pos_y = worldView.GameMapY;

        Log.d(TAGP, "initPosition: local AB pos: " + mLocalPlayerInfo.getPos_x() + "," + mLocalPlayerInfo.getPos_y());
        Log.d(TAGP, "initPosition: gamemap pos: " + WorldView.GameMapX + "," + WorldView.GameMapY);
        mLocalPlayerInfo.setPos_x(scrn_width / 2);
        mLocalPlayerInfo.setPos_y(scrn_height / 2);
        local_player = new Player(mLocalPlayerInfo, this, worldView.getContext());

        for (PlayerInfo onlinePlayerInfo : onlinePlayerInfos) {
            float relative_x = onlinePlayerInfo.getPos_x() - mLocalPlayerInfo.getPos_x();
            float relative_y = onlinePlayerInfo.getPos_y() - mLocalPlayerInfo.getPos_y();
            onlinePlayerInfo.setPos_x(relative_x);
            onlinePlayerInfo.setPos_y(relative_y);

            OnlinePlayer onlinePlayer = new OnlinePlayer(onlinePlayerInfo, this, worldView.getContext());
            onlinePlayers.add(onlinePlayer);
        }

    }

    // Response to the touch event
    public void touchEvent(float touched_x, float touched_y) {
        //player.setTouchDirection(touched_x, touched_y);
        touched = true;
        local_player.setTouchDirection(touched_x, touched_y);
    }

    // response to untouch event
    // set all the touch direction to 0, use sensor direction instead.
    public void untouchEvent() {
        touched = false;
        // if just touch, do not clear the touch direction
        if (CONFIG.CONTROL_MODE != CONFIG.CON_JUST_TOUCH) {
            local_player.clearTouchDirection();
        }

    }

    public void sensorEvent(float sensor_x, float sensor_y) {
        if (touched) {

        } else {
            local_player.setSensorDirection(sensor_x, sensor_y);

        }

    }

    //TODO Response to the split icon touch event
    public void splitIconEvent() {
        // TODO the player should split itself.
        if (WorldView.hasSplit == false)
            local_player.split();
        local_player.split_move(WorldView.hasSplit);
    }


    private synchronized void setPlayerDie() {
        Intent i = new Intent("android.intent.action.GAME").putExtra("GAME_COMPLETE", worldView.getScoreBoard().getHighest_score());
        worldView.getContext().sendBroadcast(i);
    }


    // Get total player weight
    public float getTotalWeight() {
        float weight = 0;
        if (local_player != null) weight = local_player.getTotalWeight();
        return weight;
    }

    // All the entities update themself here and Manager would manage the interactions between them
    // TODO, the AI balls should have a update and integrated to this method
    public void update() {

        for (Food food : foods) {
            food.update();
        }
        regenerateFoods();

        for (AI_Ball ai_ball : ai_balls) {
            ai_ball.update();
        }


        // update spiker
        for (Spiker spiker : spikers) {
            spiker.update();
        }

        // spiker collisions are updated here
        for (Spiker spiker : spikers) {
            for (int i = local_player.balls.size() - 1; i >= 0; i--) {
                if (spiker.checkCanSpikeBall(local_player.balls.get(i))) {
                    local_player.loseBall(i);// lose a ball;
                }
            }
        }

        // TODO
        // if is message, we retrieve the players and foods and update them seperately
        //
        for (OnlinePlayer onlinePlayer : onlinePlayers) {
            onlinePlayer.updatePlayerInfo(null);
            onlinePlayer.update();
        }


        if (last_ballsize != local_player.balls.size()) {
            collision_detectors.clear();
            for (AI_Ball ai_ball : ai_balls) {
                for (Ball player_ball : local_player.balls) {
                    collision_detectors.add(new Collision_Detector(player_ball, ai_ball));
                }
            }
            last_ballsize = local_player.balls.size();
        }
        //check the collision detector for whether player's ball can eat the AI_ball
        for (Collision_Detector collision_detector : collision_detectors) {
            if (collision_detector.detect_detectRadius()) {
                if (collision_detector.detect_invisibleRadius()) {
                    collision_detector.getAIBall().setDir_x(collision_detector.getAIxSpeed(collision_detector.compare_radius(), 4));
                    collision_detector.getAIBall().setDir_y(collision_detector.getAIySpeed(collision_detector.compare_radius(), 4));
                } else {
                    collision_detector.getAIBall().setDir_x(collision_detector.getAIxSpeed(collision_detector.compare_radius(), 2));
                    collision_detector.getAIBall().setDir_y(collision_detector.getAIySpeed(collision_detector.compare_radius(), 2));
                }
            }
            collision_detector.getAIBall().update();
            if (collision_detector.detect_realRadius()) {
                if (collision_detector.compare_radius()) {
                    //u have successfully eatan smaller ai_ball  -->> grow larger
                    ai_balls.remove(collision_detector.getAIBall());
                    collision_detector.getPlayerBall().increase(collision_detector.getAIBall().getWeight());
                } else {
                    //u have been eaten by larger ai_ball  -->>  die
                    int loseIdx = local_player.balls.indexOf(collision_detector.getPlayerBall());
                    Log.d(TAG, "update : the loseball index is " + loseIdx);
                    if (loseIdx != -1) {
                        local_player.loseBall(loseIdx);
                    }
                }

            }
        }
        //
        //Log.d(CONFIG.lizy, "update : "+ worldView.getScoreBoard().getHighest_score());
        if (local_player.check_lose()) {
            //TODO player die here
            //Log.d(CONFIG.lizy, "update : "+ worldView.getScoreBoard().getHighest_score());
            setPlayerDie();
        }
        local_player.update();
    }

    // TODO, this method would decide which entities should be draw on the canvas
    public void inScreenEntities() {

    }

    // TODO, this method will render all the entities calculated by the inScreenEntities() method
    public void onDraw(Canvas canvas) {
        // draw player
        for (Ball player_ball : local_player.balls) {
            player_ball.onDraw(canvas);
        }

        drawLayer(canvas);


        // draw foods first, in case player cover some food.
        for (Food food : foods) {
            food.onDraw(canvas);
        }

        for (OnlinePlayer onlinePlayer : onlinePlayers) {
            onlinePlayer.onDraw(canvas);
        }


        //draw the ai_balls
        for (AI_Ball ai_ball : ai_balls) {
            ai_ball.onDraw(canvas);
        }

        // draw spikers
        for (Spiker spiker : spikers) {
            spiker.onDraw(canvas);
        }
    }

    // TODO
    // This method will randomly generate foods,
    // 1. New foods should not overlap with existing foods and players (?)
    // 2. should generate foods evenly distributed on the map.
    public void generateFoods() {
        //initialize food
        for (int i = 0; i < CONFIG.FOOD_NUMBER; i++) {
            Random r = new Random();
            int x = r.nextInt(worldView.getPng_width());
            int y = r.nextInt(worldView.getPng_height());
            Random rnd = new Random();
            int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            foods.add(new Food(x, y, color, CONFIG.FOOD_RADIUS));
        }
    }

    public void generateAI_Balls() {
        ai_balls.add(new AI_Ball(this, null, worldView.getPng_width(), worldView.getPng_height(), 30));
        ai_balls.add(new AI_Ball(this, null, worldView.getPng_width(), worldView.getPng_height(), 50));

    }

    public void generateSpiker() {
        spikers.add(new Spiker(worldView.getPng_width() / 3, worldView.getPng_height() / 3, 150, worldView.getContext()));
        spikers.add(new Spiker(worldView.getPng_width() / 3 * 2, worldView.getPng_height() / 3, 150, worldView.getContext()));
        spikers.add(new Spiker(worldView.getPng_width() / 3, worldView.getPng_height() / 3 * 2, 150, worldView.getContext()));
        spikers.add(new Spiker(worldView.getPng_width() / 3 * 2, worldView.getPng_height() / 3 * 2, 150, worldView.getContext()));
    }

    public void generate_positions(float png_width, float png_height) {
        float unit_x = png_width / 4;
        float unit_y = png_height / 4;
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                Coord temp_c = new Coord(unit_x * i, unit_y * j);
                CONFIG.BALL_POSITIONS.add(temp_c);
            }
        }
    }

    public void regenerateFoods() {
        int food_size = foods.size();
        if (food_size < CONFIG.FOOD_NUMBER)
            for (int i = 0; i < CONFIG.FOOD_NUMBER - food_size; i++) {
                Random r = new Random();
                int x = r.nextInt(worldView.getPng_width());
                int y = r.nextInt(worldView.getPng_height());
                Random rnd = new Random();
                int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                foods.add(new Food(x, y, color, CONFIG.FOOD_RADIUS));
            }
    }

    public void drawLayer(Canvas canvas) {
        try {
            canvas.drawRect(worldView.GameMapX - 500, worldView.GameMapY - 500, worldView.GameMapX + worldView.getPng_width() + 500, worldView.GameMapY - 10, layer_Paint);
            canvas.drawRect(worldView.GameMapX - 500, worldView.GameMapY - 10, worldView.GameMapX - 10, worldView.GameMapY + worldView.getPng_height() + 10, layer_Paint);
            canvas.drawRect(worldView.GameMapX - 500, worldView.GameMapY + worldView.getPng_height() + 10, worldView.GameMapX + worldView.getPng_width() + 500, worldView.GameMapY + worldView.getPng_height() + 500, layer_Paint);
            canvas.drawRect(worldView.GameMapX + worldView.getPng_width() + 10, worldView.GameMapY - 10, worldView.GameMapX + worldView.getPng_width() + 500, worldView.GameMapY + worldView.getPng_height() + 10, layer_Paint);
        } catch (NullPointerException ne) {
        }
    }

    public Player getLocal_player() {
        return local_player;
    }

    public ArrayList<OnlinePlayer> getOnline_Players() {
        return onlinePlayers;
    }


}
