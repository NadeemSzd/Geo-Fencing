package com.example.geofence;

public class Data_Model
{
    double Latitude;
    double Longitude;
    double radius;

    public Data_Model()
    {

    }

    public double getLatitude()
    {
        return Latitude;
    }

    public void setLatitude(double latitude)
    {
        Latitude = latitude;
    }

    public double getLongitude()
    {
        return Longitude;
    }

    public void setLongitude(double longitude)
    {
        Longitude = longitude;
    }

    public double getRadius()
    {
        return radius;
    }

    public void setRadius(double radius)
    {
        this.radius = radius;
    }
}
