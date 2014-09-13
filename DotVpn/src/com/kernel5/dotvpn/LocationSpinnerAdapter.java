package com.kernel5.dotvpn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class LocationSpinnerAdapter extends ArrayAdapter<String> {

	private int layout_id;
	private String[] names;
	private int[] flag_resource_ids;
	private LayoutInflater view_creator;
	
	public LocationSpinnerAdapter(Context context, int resource, String[] objects, int[] flag_ids) {
		
		super(context, resource, objects);
		
		this.layout_id = resource;
		this.flag_resource_ids = flag_ids;
		this.names = objects;
		this.view_creator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return this.getView(position, convertView, parent);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		TextView item_view;
		
		if (convertView != null)
			item_view = (TextView) convertView;
		else
			item_view = (TextView) this.view_creator.inflate(this.layout_id, parent, false);
		
		item_view.setCompoundDrawablesWithIntrinsicBounds(this.flag_resource_ids[position], 0, 0, 0);
		item_view.setText(this.names[position]);
		
		return item_view;
	}

}
