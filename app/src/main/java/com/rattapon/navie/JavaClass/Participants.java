package com.rattapon.navie.JavaClass;

public class Participants {
    private boolean active;
    private int x;
    private int y;

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Participants(boolean active, int x, int y) {
        this.active = active;
        this.x = x;
        this.y = y;
    }
}
