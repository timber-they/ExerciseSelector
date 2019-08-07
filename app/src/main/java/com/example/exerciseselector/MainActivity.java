package com.example.exerciseselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private List<Entry> availableEntries;

    private List<Entry> doneEntries;

    private EntryAdapter availableAdapter;

    private EntryAdapter doneAdapter;

    private ListView availableEntriesLv;

    private ListView doneEntriesLv;

    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        availableEntriesLv = findViewById(R.id.availableEntries);
        doneEntriesLv = findViewById(R.id.doneEntries);

        syncViews();

        availableAdapter = new EntryAdapter(this, availableEntries);
        doneAdapter = new EntryAdapter(this, doneEntries);

        availableEntriesLv.setAdapter(availableAdapter);
        doneEntriesLv.setAdapter(doneAdapter);

        availableEntriesLv.setOnItemClickListener((adapterView, view, i, l) -> {
            Entry nextEntry = getNextEntry();
            if (nextEntry != null)
                showDialog(nextEntry);
        });
        availableEntriesLv.setOnItemLongClickListener((adapterView, view, i, l) -> {
            Entry entry = availableEntries.get(i);
            entry.setDone(true);
            updatePreferences();
            syncViews();
            return true;
        });

        doneEntriesLv.setOnItemClickListener((adapterView, view, i, l) -> {
            Entry entry = doneEntries.get(i);
            if (entry != null)
                showDialog(entry);
        });

        doneEntriesLv.setOnItemLongClickListener((adapterView, view, i, l) -> {
            Entry entry = doneEntries.get(i);
            entry.setDone(false);
            updatePreferences();
            syncViews();
            return true;
        });
    }

    private Entry getNextEntry() {
        List<Entry> weighedEntriesList = getWeighedEntriesList();
        int rnd = random.nextInt(weighedEntriesList.size());

        return weighedEntriesList.get(rnd);
    }

    private List<Entry> getWeighedEntriesList(){
        Map<Entry, Integer> entriesToEntries = getEntriesToEntries();
        List<Entry> weighedList = new LinkedList<>();

        for (final Entry entry : entriesToEntries.keySet()) {
            for (int i = 0; i < entriesToEntries.get(entry); i++)
                weighedList.add(entry);
        }

        return weighedList;
    }

    private Map<Entry, Integer> getEntriesToEntries (){
        Map<String, Integer> entriesToSheets = getEntriesToSheets();
        List<Entry> allEntries = availableEntries;
        Map<Entry, Integer> entriesToEntries = new HashMap<>(allEntries.size());

        for (final Entry entry : allEntries) {
            String sheet = getSheet(entry);
            int entries = entriesToSheets.containsKey(sheet)
                    ? entriesToSheets.get(sheet)
                    : getEntriesToDifficulty(Difficulty.Unknown);

            entriesToEntries.put(entry, entries);
        }

        return entriesToEntries;
    }

    private Map<String, Integer> getEntriesToSheets (){
        Map<String, Difficulty> difficulties = getDifficultiesToSheets();
        Map<String, Integer> entries = new HashMap<>(difficulties.size());

        for (final String sheet : difficulties.keySet())
            entries.put(sheet, getEntriesToDifficulty(difficulties.get(sheet)));

        return entries;
    }

    private int getEntriesToDifficulty (Difficulty difficulty){
        switch (difficulty){
            case Easy:
                return 1;
            case Neutral:
                return 2;
            case Hard:
                return 3;
            case Unknown:
                return 2;
        }

        return 2;
    }

    private Map<String, Difficulty> getDifficultiesToSheets(){
        Map<String, List<Difficulty>> difficulties = mapDifficulties();
        Map<String, Difficulty> res = new HashMap<>(difficulties.size());

        for (final String sheet : difficulties.keySet()) {
            Map<Difficulty, Integer> counts = new HashMap<>();

            for (final Difficulty difficulty : difficulties.get(sheet)) {
                if (counts.containsKey(difficulty))
                    counts.put(difficulty, counts.get(difficulty) + 1);
                else
                    counts.put(difficulty, 1);
            }

            Difficulty maxDifficulty = null;
            int count = 0;

            for (final Difficulty difficulty : counts.keySet()) {
                if (counts.get(difficulty) > count){
                    count = counts.get(difficulty);
                    maxDifficulty = difficulty;
                }
            }

            res.put(sheet, maxDifficulty != null ? maxDifficulty : Difficulty.Unknown);
        }

        return res;
    }

    private Map<String, List<Difficulty>> mapDifficulties(){
        Map<String, List<Difficulty>> difficulties = new HashMap<>();

        for (final Entry entry : doneEntries) {
            if (entry.getDifficulty() == Difficulty.Unknown)
                continue;

            String sheet = getSheet(entry);
            if (difficulties.containsKey(sheet)) {
                List<Difficulty> coll = difficulties.get(sheet);
                if (coll == null)
                    continue;
                coll.add(entry.getDifficulty());
            } else
                difficulties.put(sheet, new ArrayList<>(Collections.singletonList(entry.getDifficulty())));
        }

        return difficulties;
    }

    private String getSheet(Entry entry){
        return getSheet(entry.getTitle());
    }

    private String getSheet(String title){
        return title.split("\\.")[0];
    }

    private void showDialog(final Entry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(entry.getTitle())
                .setItems(Arrays.stream(Difficulty.values()).map(Enum::toString).collect(Collectors.toList()).toArray(new String[]{}), (dialogInterface, i) -> {
                    entry.setDone(true);
                    entry.setDifficulty(Difficulty.values()[i]);

                    updatePreferences();

                    syncViews();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    entry.setDone(false);
                    entry.setDifficulty(Difficulty.Unknown);

                    updatePreferences();

                    syncViews();
                });


        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void syncViews() {
        runOnUiThread(() -> {
                    if (availableEntries == null)
                        availableEntries = new ArrayList<>();
                    if (doneEntries == null)
                        doneEntries = new ArrayList<>();

                    availableEntries.clear();
                    doneEntries.clear();

                    availableEntries.addAll(getAvailableEntries());
                    doneEntries.addAll(getDoneEntries());

                    if (availableEntries == null)
                        availableEntries = new ArrayList<>();
                    if (doneEntries == null)
                        doneEntries = new ArrayList<>();

                    if (availableEntries.isEmpty() && doneEntries.isEmpty())
                        initialize(availableEntries);

                    if (availableAdapter == null || doneAdapter == null)
                        return;

                    availableAdapter.notifyDataSetChanged();
                    doneAdapter.notifyDataSetChanged();

                    availableEntriesLv.invalidateViews();
                    doneEntriesLv.invalidateViews();

                    availableEntriesLv.refreshDrawableState();
                    doneEntriesLv.refreshDrawableState();
                }
        );
    }

    private void updatePreferences() {
        set(allEntries());
    }

    private List<Entry> allEntries() {
        List<Entry> n = new ArrayList<>(availableEntries);
        n.addAll(doneEntries);

        return n;
    }

    private List<Entry> getAllEntries() {
        SharedPreferences preferences = getSharedPreferences("entries", MODE_PRIVATE);
        Set<String> raw = preferences.getStringSet("entries", new HashSet<>());

        return raw.stream().map(Entry::fromString).collect(Collectors.toList());
    }

    private List<Entry> getAvailableEntries() {
        return getAllEntries().stream().filter(entry -> !entry.isDone()).collect(Collectors.toList());
    }

    private List<Entry> getDoneEntries() {
        return getAllEntries().stream().filter(Entry::isDone).collect(Collectors.toList());
    }

    private void set(List<Entry> entries) {
        SharedPreferences preferences = getSharedPreferences("entries", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putStringSet("entries", entries.stream().map(Entry::toString).collect(Collectors.toSet()));
        editor.commit();
    }

    private void initialize(List<Entry> availableEntries) {
        availableEntries.addAll(initialValues);
        set(availableEntries);
    }

    private List<Entry> initialValues = Stream.of(
            // 67 Exercises
            // 13 Days
            // ~5 exercises / day
            "1.1",
            "1.2",
            "1.3",
            "1.4",
            "2.1",
            "2.2",
            "2.3",
            "2.4",
            "2.5",
            "2.6",
            "2.7",
            "3.1",
            "3.2",
            "3.3",
            "3.4",
            "4.1",
            "4.2",
            "4.3",
            "4.4",
            "4.5",
            "5.1",
            "5.2",
            "5.3",
            "5.4",
            "5.5",
            "6.1",
            "6.2",
            "6.3",
            "6.4",
            "6.5",
            "7.1",
            "7.2",
            "7.3",
            "7.4",
            "7.5",
            "8.1",
            "8.2",
            "8.3",
            "8.4",
            "8.5",
            "8.6",
            "9.1",
            "9.2",
            "9.3",
            "9.4",
            "9.5",
            "W.1",
            "W.2",
            "W.3",
            "W.4",
            "W.5",
            "W.6",
            "10.1",
            "10.2",
            "10.3",
            "10.4",
            "11.1",
            "11.2",
            "11.3",
            "11.4",
            "11.5",
            "11.6",
            "12.1",
            "12.2",
            "12.3",
            "12.4",
            "12.5"
    )
            .map(s -> new Entry(s, Difficulty.Unknown, false))
            .collect(Collectors.toList());
}
