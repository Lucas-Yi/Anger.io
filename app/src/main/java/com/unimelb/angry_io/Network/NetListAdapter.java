package com.unimelb.angry_io.Network;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.unimelb.angry_io.R;

import java.util.ArrayList;

/**
 * Created by randy on 23/09/15.
 */
public class NetListAdapter extends ArrayAdapter<String>{

    private final Activity context;

    ArrayList<String> arrayListAvailable;
    ArrayList<String> arrayListAvailableMac;
    private ArrayList<Integer> arrayListAvailableType;

    Integer[] NetIconId={
            R.drawable.bluetooth,
            R.drawable.wifi,
    };

    public NetListAdapter(Activity context, ArrayList<String> arrayListAvailable
            ,ArrayList<String> arrayListAvailableMac, ArrayList<Integer> arrayListAvailableType) {
        super(context, R.layout.netlist, arrayListAvailable);

        this.context=context;
        this.arrayListAvailable = arrayListAvailable;
        this.arrayListAvailableMac = arrayListAvailableMac;
        this.arrayListAvailableType = arrayListAvailableType;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.netlist, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.item);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView extratxt = (TextView) rowView.findViewById(R.id.textView1);

        txtTitle.setText(arrayListAvailable.get(position));
        imageView.setImageResource(NetIconId[arrayListAvailableType.get(position)]);
        extratxt.setText(arrayListAvailableMac.get(position));
        return rowView;

    };
}
