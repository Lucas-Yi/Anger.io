package com.unimelb.angry_io.Cmd;

import android.util.Log;

import com.unimelb.angry_io.Entity.PlayerInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOError;
import java.io.IOException;

/**
 * Created by lizy on 8/10/15.
 */
public class HandshakeCmd extends Cmd {
    private String type = PROTOCOL.CMD_HANDSHAKE;
    private PlayerInfo localPlayer;

    //initialize from sender side
    public HandshakeCmd(String player_ID, String nickName) {
        this.localPlayer = new PlayerInfo(player_ID, nickName, 0, 0);
    }

    // initialize from receiver side
    public HandshakeCmd(String jst) {
        //final JSONParser parser = new JSONParser();
        JSONObject jsonObj = null;
        try {
            Log.d("Cmd", "HandshakeCmd.class: HandshakeCmd ");
            jsonObj = (JSONObject) parser.parse(jst);
        } catch (org.json.simple.parser.ParseException e) {
            System.out.println(e.getMessage());
            System.err.println("The InitMasterCmd message is damaged.");
            e.printStackTrace();
        }
        if (jsonObj != null) {
            FromJSON(jst);
        }

    }

    @Override
    public String Type() {
        return type;
    }

    @Override
    public String ToJSON() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("player_id", localPlayer.getPlayer_id());
            jsonObject.put("nickname", localPlayer.getNick_name());
            jsonObject.put("type", Type());
        } catch (Error e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    @Override
    public void FromJSON(String jst) {
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) parser.parse(jst.trim());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (jsonObject != null) {
            try {
                String temp_player_id = (String) jsonObject.get("player_id");
                String temp_nickname = (String) jsonObject.get("nickname");
                this.type = (String) jsonObject.get("type");
                //Log.d("Cmd", "FromJSON this.type: "+ this.type +" vs "+PROTOCOL.CMD_HANDSHAKE);
                this.localPlayer = new PlayerInfo(temp_player_id, temp_nickname, 0, 0);
            } catch (Error e) {
                e.printStackTrace();
            }
        }
    }

    public PlayerInfo getPlayerInfo() {
        return this.localPlayer;
    }
}
