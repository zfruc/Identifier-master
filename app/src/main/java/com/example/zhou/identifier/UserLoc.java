package com.example.zhou.identifier;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Zhou on 2017/6/6.
 */

public class UserLoc {
    CellLoc[] neighborCell;
    private static final  double EARTH_RADIUS = 6378137; //meter

    public UserLoc(List<CellLoc> neighborCellLoc){
        this.neighborCell = neighborCellLoc.toArray(new CellLoc[neighborCellLoc.size()]);
    }

    private static double rad(double d)
    {
        return d * Math.PI / 180.0;
    }

    /**
     * 基于googleMap中的算法得到两经纬度之间的距离,计算精度与谷歌地图的距离精度差不多，相差范围在0.2米以下
     * @param lon1 第一点的精度
     * @param lat1 第一点的纬度
     * @param lon2 第二点的精度
     * @param lat2 第二点的纬度
     * @return 返回的距离，单位m
     * */
    public static double GetDistance(double lon1,double lat1,double lon2, double lat2)
    {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lon1) - rad(lon2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2)+Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * EARTH_RADIUS;
        //s = Math.round(s * 10000) / 10000;
        return s;
    }

    public boolean ifHijacked(CellLoc ConnectedC){
        Arrays.sort(this.neighborCell); //Arrays.sort() is ascending order,but for CellLoc is descending order
        if (this.neighborCell.length == 0)
        {
            return false;
        }
        if (this.neighborCell.length == 1)
        {
            double CenterDis = GetDistance(ConnectedC.getLon(),ConnectedC.getLat(),neighborCell[0].getLon(),neighborCell[0].getLat());
            double Radius = Math.pow(10,30 - 58.5 - neighborCell[0].getRSSI()/33);
            int MinRSSI = (int) (30 - 58.5 - 33*Math.log10(CenterDis + Radius));
            int MaxRSSI = (int) (30 - 58.5 - 33*Math.log10(CenterDis - Radius));
            if (ConnectedC.getRSSI()<=MaxRSSI && ConnectedC.getRSSI()>=MinRSSI){
                return false;
            }
            return true;
        }
        else {
            ArrayList x_coordinate = new ArrayList();
            ArrayList y_coordinate = new ArrayList();

            for (int i = 0; i < neighborCell.length; i++)
                for (int j = i + 1; j < neighborCell.length; j++) {
                    Circle c1 = new Circle(neighborCell[i].getLon(), neighborCell[i].getLat(), Math.pow(10, 30 - 58.5 - neighborCell[i].getRSSI() / 33));
                    Circle c2 = new Circle(neighborCell[j].getLon(), neighborCell[j].getLat(), Math.pow(10, 30 - 58.5 - neighborCell[j].getRSSI() / 33));
                    CirInterSect cir = new CirInterSect(c1, c2);

                    double[] dots = cir.intersect();
                    if (dots.length == 4) {
                        x_coordinate.add(dots[0]);
                        y_coordinate.add(dots[1]);
                        x_coordinate.add(dots[2]);
                        y_coordinate.add(dots[3]);
                    }
                }

            double SelfLon = 0,SelfLat = 0;
            for (int i = 0; i < x_coordinate.size(); i++)
            {
                SelfLon += (double)x_coordinate.get(i);
                SelfLat += (double)y_coordinate.get(i);
            }
            SelfLat = SelfLat/y_coordinate.size();
            SelfLon = SelfLon/x_coordinate.size();

            double SupposedRSSI = (30 - 58.5 - 33*Math.log10(GetDistance(SelfLon,SelfLat,ConnectedC.getLon(),ConnectedC.getLat())));
            if (Math.abs(ConnectedC.getRSSI()-SupposedRSSI)<5)
            {
                return false;
            }
            return true;
        }
    }

}
