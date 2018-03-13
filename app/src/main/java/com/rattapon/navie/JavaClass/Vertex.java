package com.rattapon.navie.JavaClass;

/**
 * Created by ratta on 2/10/2018.
 */

public class Vertex {
    final private int id;
    final private String name;
    private double x;
    private double y;


    public Vertex(int id, String name,double x, double y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + id.hashCode();
//        return result;
//    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vertex other = (Vertex) obj;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

}
