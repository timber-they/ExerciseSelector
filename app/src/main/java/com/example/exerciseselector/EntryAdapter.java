package com.example.exerciseselector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class EntryAdapter extends ArrayAdapter<Entry> {
    public EntryAdapter(Context context, List<Entry> entries){
        super(context, 0, entries);
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent){
        Entry entry = getItem(position);
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.entry_view, parent, false);

        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvDifficulty = convertView.findViewById(R.id.tvDifficulty);

        if (entry != null) {
            tvTitle.setText(entry.getTitle());
            tvDifficulty.setText(entry.getDifficulty().toString());
        }

        return convertView;
    }
}
