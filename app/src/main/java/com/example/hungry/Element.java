package com.example.hungry;

public class Element {
    private String name;
    private double latitude;
    private double longitude;


    public Element(String name) {
        this.name = name;
    }

    public Element(String name, double lt, double lg) {
        this.name = name;
        this.latitude=lt;
        this.longitude=lg;

    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }
}

