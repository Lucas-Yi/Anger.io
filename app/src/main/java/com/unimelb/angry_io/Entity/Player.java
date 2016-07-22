package com.unimelb.angry_io.Entity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.FloatMath;
import android.util.Log;

import com.unimelb.angry_io.R;
import com.unimelb.angry_io.System.CONFIG;
import com.unimelb.angry_io.System.WorldView;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Player are an abstraction for player.
 * It contains several balls. And the screen focus should follow Player rather than individual balls.
 * It is also responsible for splitting the balls under double click
 * And merge balls as time flows.
 * Created by lizy on 9/09/15.
 */
public class Player extends Entity implements Comparable<Player> {
    private final String TAG = "Player";
    // player_id is used for multi-player mode, used to identify which device has which player
    private String player_id;

    private String nick_name;
    private Bitmap userIcon = null;

    // use pos_x and y to determine the relative position of screen

    private Coord scrn_pos = null;
    private Coord map_pos = null;

    private float touch_dir_x, touch_dir_y;
    private float sensor_dir_x;
    private float sensor_dir_y;
    private float last_dir_x = 0, last_dir_y = 0;

    private float total_score;
    private int foods_eaten;
    private int ball_size;


    private EntityManager entityManager;

    // Contains the balls of this player
    ArrayList<Ball> balls = new ArrayList<Ball>();

    Context mContext;

    Bitmap tmp;

    // initialize the player with a ball for local player

    public Player(PlayerInfo playerInfo, EntityManager entityManager, Context ctx) {
        super(playerInfo.getPos_x() + entityManager.worldView.GameMapX, playerInfo.getPos_y() + entityManager.worldView.GameMapY,
                0, 0, 0, null);
        Log.d("Randy", "Player: x+MapX = " + playerInfo.getPos_x() + "+" + entityManager.worldView.GameMapX);
        Log.d("Randy", "Player: y+MapY = " + playerInfo.getPos_y() + "+" + entityManager.worldView.GameMapY);
        this.mContext = ctx;
        this.player_id = playerInfo.getPlayer_id();
        this.nick_name = CONFIG.player_nickname;
        this.entityManager = entityManager;

        this.map_pos = new Coord(playerInfo.getPos_x() - entityManager.worldView.GameMapX, playerInfo.getPos_y() - entityManager.worldView.GameMapY);
        this.scrn_pos = map2scrn(this.map_pos);

        userIcon = select_texture(nick_name, CONFIG.BALL_INI_RADIUS * 2);

        Ball init_ball = new Ball(userIcon, this.scrn_pos.x, this.scrn_pos.y, CONFIG.BALL_INI_RADIUS);
        balls.add(init_ball);
        ball_size = balls.size();
        for (int i = 0; i < ball_size; i++) {
            balls.get(i).setCharacter(i);
        }
        total_score = CONFIG.BALL_INI_RADIUS / 2;
    }

    // TODO select corresponding  texture based on nick names
    private Bitmap select_texture(String nick_name, float size) {

        Field[] fields = R.raw.class.getFields();

        int res = -1;
        for (Field f : fields) {
            try {
                if (f.getName().contains(nick_name.toLowerCase())) {
                    Log.d(TAG, "!!!!!! Found icon: " + f.getName() + " for " + nick_name);
                    res = f.getInt(null);
                    break;
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException IAe) {
                IAe.printStackTrace();
            }
        }

        if (res != -1) {
            try {
                tmp = BitmapFactory.decodeResource(mContext.getResources(), res);
            } catch (OutOfMemoryError oe) {
            }
        }
        return tmp;
    }


    // update all the balls in this player
    // and determine the screen focus on the screen
    public void update() {
        for (Ball ball : balls) {
            ball.update();
            // update food and check whether each food can be eaten by a player ball
            for (int i = entityManager.foods.size() - 1; i >= 0; i--) {
                if (ball.checkCanEatFood(entityManager.foods.get(i))) {
                    foods_eaten++;
                    total_score += entityManager.foods.get(i).getWeight();
                    ball.increase(entityManager.foods.get(i).getWeight());
                    entityManager.foods.remove(i);

                }
            }


        }
        integrateDirectionSpeed();
        relativeMove();
        recordDir();
        checkCanMerge(WorldView.hasSplit);
    }


    // Get destination position value and calculate speed angle
    // Then standardize the new speed according to Standard Speed
    public void setTouchDirection(float desti_x, float desti_y) {
        float dist_x, dist_y;
        float dir_speed;

        dist_x = desti_x - scrn_pos.x;
        dist_y = desti_y - scrn_pos.y;
        dir_speed = FloatMath.sqrt(dist_x * dist_x + dist_y * dist_y);

        // after get the angle, set speed to that way.
        setTouch_dir_x((CONFIG.STDSPED / dir_speed) * dist_x);
        setTouch_dir_y((CONFIG.STDSPED / dir_speed) * dist_y);
        Log.d(TAG, "setTouchDirection x: " + touch_dir_x + "; y: " + touch_dir_y);
    }

    // used with untouch event.
    public void clearTouchDirection() {
        this.touch_dir_x = 0;
        this.touch_dir_y = 0;
    }

    // get sensor direction from sensor and modify total speed according to Standard Speed
    public void setSensorDirection(float sensor_x, float sensor_y) {
        float leveled_x, leveled_y;
        float dir_speed;

        leveled_x = sensor_x / CONFIG.ACC_SENSOR_MAX;
        leveled_y = sensor_y / CONFIG.ACC_SENSOR_MAX;

        dir_speed = FloatMath.sqrt(leveled_x * leveled_x + leveled_y * leveled_y);
        setSensor_dir_x((CONFIG.STDSPED / dir_speed) * leveled_x);
        setSensor_dir_y((CONFIG.STDSPED / dir_speed) * leveled_y);
    }

    // SUM up the speed value from touch and sensor
    // then modify the total value according to standard Speed
    public void integrateDirectionSpeed() {

        if (CONFIG.CONTROL_MODE == CONFIG.CON_TOUCH_SENSOR) {
            if (EntityManager.touched) {
                setDir_x(this.touch_dir_x * CONFIG.speedScale);
                setDir_y(this.touch_dir_y * CONFIG.speedScale);
            } else {
                setDir_x(this.sensor_dir_x * CONFIG.speedScale);
                setDir_y(this.sensor_dir_y * CONFIG.speedScale);
            }
        } else if (CONFIG.CONTROL_MODE == CONFIG.CON_JUST_TOUCH) {
            setDir_x(this.touch_dir_x * CONFIG.speedScale);
            setDir_y(this.touch_dir_y * CONFIG.speedScale);
        } else if (CONFIG.CONTROL_MODE == CONFIG.CON_JUST_SENSOR) {
            setDir_x(this.sensor_dir_x * CONFIG.speedScale);
            setDir_y(this.sensor_dir_y * CONFIG.speedScale);
        }
    }

    public boolean checkReachXLayer(float dir_x) {
        for (Ball ball : balls) {
            if (ball.getMap_pos().x >= 0 && ball.getMap_pos().x <= WorldView.png_width) {
                if (dir_x < 0) {
                    if (-dir_x > ball.getMap_pos().x) {
                        return false;
                    }
                } else if (dir_x > 0) {
                    if (dir_x > WorldView.png_width - ball.getMap_pos().x) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean checkReachYLayer(float dir_y) {
        for (Ball ball : balls) {
            if (ball.getMap_pos().y >= 0 && ball.getMap_pos().y <= WorldView.png_height) {
                if (dir_y < 0) {
                    if (-dir_y > ball.getMap_pos().y) {
                        return false;
                    }
                } else if (dir_y > 0) {
                    if (dir_y > WorldView.png_height - ball.getMap_pos().y) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void relativeMove() {
        while (checkReachXLayer(getDir_x()) == false) {
            dir_x = dir_x / 2;
        }
        WorldView.GameMapX -= dir_x;
        while (checkReachYLayer(getDir_y()) == false) {
            dir_y = dir_y / 2;
        }
        WorldView.GameMapY -= dir_y;
    }

    // draw all the balls out.
    public void onDraw(Canvas canvas) {
        for (Ball ball : balls) {
            ball.onDraw(canvas);
        }
    }

    public void recordDir() {
        if (WorldView.split_icon_touched == false) {
            last_dir_x = getDir_x();
            last_dir_y = getDir_y();
        }
    }

    // Return total Player weight
    public float getTotalWeight() {
        float sum = 0;
        for (Ball ball : balls) {
            sum += ball.getWeight();
        }
        return sum;
    }

    public Boolean checkCanSplit() {
        if (balls.size() == 1) {
            if ((balls.get(0).getMap_pos().x == 0 && balls.get(0).getMap_pos().y == 0) || (balls.get(0).getMap_pos().x == 0 && balls.get(0).getMap_pos().y == WorldView.png_height)
                    || (balls.get(0).getMap_pos().x == WorldView.png_width && balls.get(0).getMap_pos().y == 0) || (balls.get(0).getMap_pos().x == WorldView.png_width && balls.get(0).getMap_pos().y == WorldView.png_height))
                return false;
            if (balls.get(0).getWeight() < CONFIG.BALL_SPLIT_SHREHOLD)
                return false;
            return true;
        } else {
            for (Ball ball : balls) {
                if (ball.getWeight() < CONFIG.BALL_SPLIT_SHREHOLD)
                    return false;
            }
            return true;
        }
    }

    // TODO split the balls
    public void split() {
        if (checkCanSplit()) {
            ArrayList<Ball> balls_split = new ArrayList<Ball>();
            int size = balls.size();
            for (int i = 0; i < size; i++) {
                float radius = balls.get(i).getBallRadius();
                Coord scrn_pos = balls.get(i).getScrn_pos();
                if (userIcon != null) {
                    balls_split.add(new Ball(userIcon, scrn_pos.x, scrn_pos.y, radius / 2));
                    balls_split.add(new Ball(userIcon, scrn_pos.x, scrn_pos.y, radius / 2));
                } else {
                    balls_split.add(new Ball(null, scrn_pos.x, scrn_pos.y, radius / 2));
                    balls_split.add(new Ball(null, scrn_pos.x, scrn_pos.y, radius / 2));
                }
            }
            balls = balls_split;
            for (int i = 0; i < balls.size(); i++) {
                balls.get(i).setCharacter(i);
            }

            WorldView.hasSplit = true;
        } else
            WorldView.split_icon_touched = false;
    }

    public void split_move(Boolean flag) {
        if (flag) {
            for (int i = balls.size() - 1; i >= 0; i--) {
                Log.d(TAG, "BALL:" + i + ", ball's character:" + balls.get(i).getCharacter());
            }

            for (int i = balls.size() - 1; i >= 0; i -= 1) {
                if (ball_size != balls.size()) {
                    i -= balls.size() - ball_size;
                    ball_size = balls.size();
                } else {
                    if (balls.get(i).getCharacter() % 2 == 1)
                        balls.get(i).move_behind(last_dir_x, last_dir_y);
                    else
                        balls.get(i).move_front(last_dir_x, last_dir_y);
                }
            }
        }
    }

    // TODO merge the balls as time flows
    public void checkCanMerge(Boolean flag) {
        if (flag == false) {
            float m, n;
            for (int i = balls.size() - 1; i >= 1; i--) {
                for (int j = i - 1; j >= 0; j--) {
                    if (mergeTwoBalls(balls.get(i), balls.get(j))) {
                        m = balls.get(i).getBallRadius();
                        n = balls.get(j).getBallRadius();
                        Log.d(TAG, "merged");
                        if (userIcon != null) {
                            balls.add(new Ball(userIcon, (balls.get(i).getScrn_pos().x + balls.get(j).getScrn_pos().x) / 2,
                                    (balls.get(i).getScrn_pos().y + balls.get(j).getScrn_pos().y) / 2,
                                    (m + n)));
                        } else {
                            balls.add(new Ball(null, (balls.get(i).getScrn_pos().x + balls.get(j).getScrn_pos().x) / 2,
                                    (balls.get(i).getScrn_pos().y + balls.get(j).getScrn_pos().y) / 2,
                                    (m + n)));
                        }
                        balls.remove(balls.get(i));
                        balls.remove(balls.get(j));
                    }
                }
            }
        }
    }

    public boolean mergeTwoBalls(Ball a, Ball b) {
        float xDiff = a.getScrn_pos().x - b.getScrn_pos().x;
        float yDiff = a.getScrn_pos().y - b.getScrn_pos().y;
        float dis = (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);
        if (dis * 1.3 < (a.getBallRadius() + b.getBallRadius()))
            return true;
        else
            return false;
    }

    public void loseBall(int idx) {
        float loseWeight = balls.get(idx).getWeight();
        this.total_score = this.total_score - loseWeight;
        balls.remove(idx);
    }

    public boolean check_lose() {
        return balls.isEmpty();
    }


    // getters and setters for pos_x/y

    public String getPlayer_id() {
        return player_id;
    }

    public String getNick_name() {
        return nick_name;
    }
    // Getter and Setter for touch and sensor speed

    public void setSensor_dir_y(float sensor_dir_y) {
        this.sensor_dir_y = sensor_dir_y;
    }

    public void setSensor_dir_x(float sensor_dir_x) {
        this.sensor_dir_x = sensor_dir_x;
    }

    public void setTouch_dir_y(float touch_dir_y) {
        this.touch_dir_y = touch_dir_y;
    }

    public void setTouch_dir_x(float touch_dir_x) {
        this.touch_dir_x = touch_dir_x;
    }

    public int getScore() {
        return (int) this.total_score;
    }

    public int getFoods_eaten() {
        return foods_eaten;
    }

    @Override
    public int compareTo(Player player) {
        return this.getScore() - player.getScore();
    }
}
