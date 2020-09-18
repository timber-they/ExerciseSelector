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

    private TextView availableLabel;

    private TextView doneLabel;

    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        availableEntriesLv = findViewById(R.id.availableEntries);
        doneEntriesLv = findViewById(R.id.doneEntries);

        availableLabel = findViewById(R.id.availableLabel);
        doneLabel = findViewById(R.id.doneLabel);

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

    private List<Entry> getWeighedEntriesList() {
        Map<Entry, Integer> entriesToEntries = getEntriesToEntries();
        List<Entry> weighedList = new LinkedList<>();

        for (final Entry entry : entriesToEntries.keySet()) {
            for (int i = 0; i < entriesToEntries.get(entry); i++)
                weighedList.add(entry);
        }

        return weighedList;
    }

    private Map<Entry, Integer> getEntriesToEntries() {
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

    private Map<String, Integer> getEntriesToSheets() {
        Map<String, Difficulty> difficulties = getDifficultiesToSheets();
        Map<String, Integer> entries = new HashMap<>(difficulties.size());

        for (final String sheet : difficulties.keySet())
            entries.put(sheet, getEntriesToDifficulty(difficulties.get(sheet)));

        return entries;
    }

    private int getEntriesToDifficulty(Difficulty difficulty) {
        switch (difficulty) {
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

    private Map<String, Difficulty> getDifficultiesToSheets() {
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
                if (counts.get(difficulty) > count) {
                    count = counts.get(difficulty);
                    maxDifficulty = difficulty;
                }
            }

            res.put(sheet, maxDifficulty != null ? maxDifficulty : Difficulty.Unknown);
        }

        return res;
    }

    private Map<String, List<Difficulty>> mapDifficulties() {
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

    private String getSheet(Entry entry) {
        return getSheet(entry.getTitle());
    }

    private String getSheet(String title) {
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

            if (availableEntries.isEmpty() && doneEntries.isEmpty())
                initialize(availableEntries);

            int availableSize = availableEntries.size();
            int doneSize = doneEntries.size();
            int totalSize = availableSize + doneSize;

            availableLabel.setText(getString(R.string.available_text, availableSize, availableSize * 100 / totalSize));
            doneLabel.setText(getString(R.string.done_text, doneSize, doneSize * 100 / totalSize));

            if (availableAdapter == null || doneAdapter == null)
                return;

            availableAdapter.notifyDataSetChanged();
            doneAdapter.notifyDataSetChanged();

            availableEntriesLv.invalidateViews();
            doneEntriesLv.invalidateViews();

            availableEntriesLv.refreshDrawableState();
            doneEntriesLv.refreshDrawableState();
        });
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
        return getAllEntries().stream().filter(entry -> !entry.isDone()).sorted().collect(Collectors.toList());
    }

    private List<Entry> getDoneEntries() {
        return getAllEntries().stream().filter(Entry::isDone).sorted().collect(Collectors.toList());
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
            "P0.1",
            "P0.2",
            "P0.3",
            "P1.1",
            "P1.2",
            "P1.3",
            "P1.4",
            "P2.1",
            "P2.2",
            "P2.3",
            "P3.1",
            "P3.2",
            "P3.3",
            "P4.1",
            "P4.2",
            "P4.3",
            "P5.1",
            "P5.2",
            "P6.1",
            "P6.2",
            "P6.3",
            "P2.1",
            "P2.2",
            "P8.1",
            "P8.2",
            "P9.1",
            "P9.2",
            "P10.1",
            "P11.1",
            "P11.2",
            "H1.1",
            "H1.2",
            "H1.3",
            "H2.1",
            "H2.2",
            "H2.3",
            "H3.1",
            "H3.2",
            "H3.3",
            "H4.1",
            "H4.2",
            "H4.3",
            "H5.1",
            "H5.2",
            "H6.1",
            "H6.2",
            "H6.3",
            "H7.1",
            "H7.2",
            "H8.1",
            "H8.2",
            "H8.3",
            "H9.1",
            "H9.2",
            "H10.1",
            "H10.2"
    )
            .map(s -> new Entry(s, Difficulty.Unknown, false))
            .collect(Collectors.toList());
}
