package com.unimelb.angry_io.Entity;

/**
 * Created by Yi on 9/1/2015.
 */
public class Collision_Detector {
    public Ball player;
    public AI_Ball ai_ball;

    public Collision_Detector(Ball player, AI_Ball ai_ball) {
        this.player = player;
        this.ai_ball = ai_ball;
    }

    public boolean detect_realRadius() {
        double xDiff = player.getScrn_pos().x - ai_ball.getScrn_pos().x;
        double yDiff = player.getScrn_pos().y - ai_ball.getScrn_pos().y;
        double distance = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));

        return distance < (player.getBallRadius() + ai_ball.getBallRadius());
    }

    public boolean detect_invisibleRadius() {
        double xDiff = player.getScrn_pos().x - ai_ball.getScrn_pos().x;
        double yDiff = player.getScrn_pos().y - ai_ball.getScrn_pos().y;
        double distance = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));

        return distance < (player.getBallRadius() + ai_ball.getInvisibleRadius());
    }

    public boolean detect_detectRadius() {
        double xDiff = player.getScrn_pos().x - ai_ball.getScrn_pos().x;
        double yDiff = player.getScrn_pos().y - ai_ball.getScrn_pos().y;
        double distance = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));

        return distance < (player.getBallRadius() + ai_ball.getDetectRadius());
    }

    public boolean compare_radius() {
        if (player.getBallRadius() - ai_ball.getBallRadius() > 0)
            return true;
        else
            return false;
    }

    public float getAIxSpeed(boolean result, int scale) {
        float xDiff;
        if (result)
            xDiff = ai_ball.getScrn_pos().x - player.getScrn_pos().x;
        else
            xDiff = player.getScrn_pos().x - ai_ball.getScrn_pos().x;

        return (scale * xDiff / (float) Math.sqrt(Math.pow(player.getScrn_pos().x - ai_ball.getScrn_pos().x, 2)
                + Math.pow(player.getScrn_pos().y - ai_ball.getScrn_pos().y, 2)));
    }

    public float getAIySpeed(boolean result, int scale) {
        float yDiff;
        if (result)
            yDiff = ai_ball.getScrn_pos().y - player.getScrn_pos().y;
        else
            yDiff = player.getScrn_pos().y - ai_ball.getScrn_pos().y;

        return (scale * yDiff / (float) Math.sqrt(Math.pow(player.getScrn_pos().x - ai_ball.getScrn_pos().x, 2) + Math.pow(player.getScrn_pos().y - ai_ball.getScrn_pos().y, 2)));
    }

    public AI_Ball getAIBall() {
        return ai_ball;
    }

    public Ball getPlayerBall() {
        return player;
    }
}
