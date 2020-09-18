package com.example.exerciseselector;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class Entry implements Comparable<Entry> {
    @NonNull
    private String title;

    private Difficulty difficulty;

    private boolean done;

    public Entry(final String title, final Difficulty difficulty, final boolean done) {
        this.title = title;
        this.difficulty = difficulty;
        this.done = done;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(final Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(final boolean done) {
        this.done = done;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s%s%s", title, unitSeparator, difficulty, unitSeparator, done);
    }

    private static String unitSeparator = Character.toString((char) 31);

    public static Entry fromString(String s) {
        String[] parts = s.split(unitSeparator);
        if (parts.length != 3)
            return null;

        String title = parts[0];
        Difficulty difficulty = Difficulty.valueOf(parts[1]);
        boolean done = Boolean.parseBoolean(parts[2]);

        return new Entry(title, difficulty, done);
    }

    @Override
    public int compareTo(final Entry entry) {
        return entry == null
                ? 1
                : getValue().compareTo(entry.getValue());
    }

    public Integer getValue(){
        String[] splitted = title.split("\\.");
        if (splitted.length != 2)
            return null;

        String combined = splitted[0] + splitted[1];
        if (combined.charAt(0) > '9' || combined.charAt(0) < '0') {
            combined = (int) combined.charAt(0) + combined.substring(1);
        }
        try{
            return Integer.parseInt(combined);
        }
        catch (NumberFormatException e){
            return null;
        }
    }
}
