package com.unimelb.angry_io.Cmd;

import com.unimelb.angry_io.Entity.Food;
import com.unimelb.angry_io.Entity.PlayerInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;

/**
 * Master distribute the positions of foods and players to all other players
 * Created by lizy on 24/09/15.
 */
public class InitMasterCmd extends Cmd {

    String type = PROTOCOL.CMD_INITIALIZATION;
    private ArrayList<PlayerInfo> players = new ArrayList<PlayerInfo>();
    private String player_id;
    private int cmd_seq;

    // used by sender, initialize the command message
    public InitMasterCmd() {

    }

    // initialize the command from sender side
    public InitMasterCmd(String player_id, ArrayList<PlayerInfo> players, ArrayList<Food> foods, int cmd_seq) {
        this.players = players;
        this.player_id = player_id;
        this.cmd_seq = cmd_seq;
    }

    // Rebuild the command in the receiver side
    public InitMasterCmd(String jst) {
        final JSONParser parser = new JSONParser();
        JSONObject jsonObj = null;
        try {
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
        JSONArray jsonPlayers = new JSONArray();

        // format players as JsonArr
        for (PlayerInfo player : players) {
            JSONObject jsonPlayer = new JSONObject();
            try {
                jsonPlayer.put("player_id", player.getPlayer_id());
                jsonPlayer.put("nick_name", player.getNick_name());
                jsonPlayer.put("pos_x", player.getPos_x());
                jsonPlayer.put("pos_y", player.getPos_y());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonPlayers.put(jsonPlayer);
        }

        // Sum up the final json message
        try {
            jsonObject.put("type", Type());
            jsonObject.put("player_id", player_id);
            jsonObject.put("cmd_seq", cmd_seq);
            jsonObject.put("playersArr", jsonPlayers);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }

    @Override
    public void FromJSON(String jst) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jst);
        } catch (JSONException je) {
            je.printStackTrace();
        }

        if (jsonObject != null) {
            try {
                this.type = (String) jsonObject.get("type");
                this.cmd_seq = ((Number) jsonObject.get("cmd_seq")).intValue();
                this.player_id = (String) jsonObject.get("player_id");

                JSONArray jsonPlayers = new JSONArray();
                jsonPlayers = jsonObject.getJSONArray("playersArr");

                for (int i = 0; i < jsonPlayers.length(); i++) {
                    JSONObject jsonPlayer = jsonPlayers.getJSONObject(i);

                    float pos_x, pos_y;
                    String nick_name, player_id;
                    pos_x = ((Number) jsonPlayer.get("pos_x")).floatValue();
                    pos_y = ((Number) jsonPlayer.get("pos_y")).floatValue();
                    nick_name = (String) jsonPlayer.get("nick_name");
                    player_id = (String) jsonPlayer.get("player_id");

                    PlayerInfo temp_player = new PlayerInfo(player_id, nick_name, pos_x, pos_y);
                    players.add(temp_player);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<PlayerInfo> getPlayers() {
        return players;
    }

}
