package com.unimelb.angry_io.Cmd;

import android.util.Log;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



/*
 * Use the MsgFactory to convert a JSON String to an appropriate
 * Msg class.
 */

public class CmdFactory {

    private final String TAG = "Cmd";
    private static final JSONParser parser = new JSONParser();

    public CmdFactory() {

    }

    // returns null on any problems
    public Cmd FromJSON(String msg) {
        JSONObject obj;
        try {
            obj = (JSONObject) parser.parse(msg);

        } catch (ParseException e) {
            // alert the user
            Log.d(TAG, "FromJSON the message is not parsed correctly");
            return null;
        }
        if (obj != null) {
            Log.d("Cmd", "FromJSON obj has something");
            Cmd cmd = null;
            if (obj.get("type").equals(PROTOCOL.CMD_INITIALIZATION)) {
                Log.d("Cmd", "FromJSON get an initialization");
                cmd = new InitMasterCmd();
                cmd.FromJSON(msg);
            } else if (obj.get("type").equals(PROTOCOL.CMD_HANDSHAKE)) {
                Log.d(TAG, "FromJSON get a handshake");
                cmd = new HandshakeCmd(msg);
            }


            return cmd;
        } else {
            Log.d("Cmd", "FromJSON jsonObj is parsed as a Null");
            return null;
        }
    }

}
