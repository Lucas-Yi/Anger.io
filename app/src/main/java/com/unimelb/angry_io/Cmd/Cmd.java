package com.unimelb.angry_io.Cmd;

import org.json.simple.parser.JSONParser;


abstract public class Cmd {
    protected static final JSONParser parser = new JSONParser();

    abstract public String Type();

    abstract public String ToJSON();

    abstract public void FromJSON(String jst);

    public String player_id;


}
