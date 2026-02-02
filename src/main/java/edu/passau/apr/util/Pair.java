package edu.passau.apr.util;

public class Pair<U, V> {
    private final U first;
    private final V second;

    public Pair(U first, V second) {
        this.first = first;
        this.second = second;
    }

    public U first() {
        return first;
    }

    public V second() {
        return second;
    }
}
