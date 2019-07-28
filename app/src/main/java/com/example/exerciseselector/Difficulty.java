package com.example.exerciseselector;

public enum Difficulty {
    Easy(1),
    Neutral(2),
    Hard(3),
    Unknown(0);

    private int code;

    Difficulty(int code){
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
