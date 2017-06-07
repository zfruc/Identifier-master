package com.example.zhou.identifier;

/**
 * Created by Zhou on 2017/5/18.
 */

public class CellLoc implements Comparable{
    private int lac = 0;
    private int cid = 0;
    private double lat = 0.0;
    private double lon = 0.0;
    private int RSSI = 0;

    public  CellLoc(int givenlac,int givencid,int givenrssi){
        this.lac = givenlac;
        this.cid = givencid;
        this.RSSI = givenrssi;
    }

    public void setLat(double givenlat){
        this.lat = givenlat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setRSSI(int rssi){this.RSSI = rssi;}

    public double getLat()
    {
        return lat;
    }
    public double getLon(){return lon;}

    public double getRSSI() {
        return RSSI;
    }

    public int getCid() {
        return cid;
    }

    public int getLac() {
        return lac;
    }

    @Override
    public int compareTo(Object o ){
        CellLoc compare_o = (CellLoc) o;
        return  compare_o.RSSI - RSSI;
    }
}
