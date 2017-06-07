package com.example.zhou.identifier;

import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.lang.Runnable;
import java.util.logging.LogRecord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
//import android.support.v4.app.FragmentActivity;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import org.xmlpull.v1.XmlPullParser;

/**
 * 功能描述：通过手机信号获取基站信息
 * # 通过TelephonyManager 获取lac:mcc:mnc:cell-id
 * # MCC，Mobile Country Code，移动国家代码（中国的为460）；
 * # MNC，Mobile Network Code，移动网络号码（中国移动为0，中国联通为1，中国电信为2）；
 * # LAC，Location Area Code，位置区域码；
 * # CID，Cell Identity，基站编号；
 * # BSSS，Base station signal strength，基站信号强度。
 *
 * @author android_ls
 */
public class GSMCellLocationActivity extends Activity {
    private static final String TAG = "GSMCellLocationActivity";
    TextView MCCtext, MNCtext, LACtext, CIDtext, SBtext, numtext;
    ArrayList<CellLoc> neighborCellLoc;
    CellLoc ConnectedCell;

    TelephonyManager telephonyManager;
    MyPhoneStateListener MyListener;

    int lac;
    int cellId;
    int neighbornum;

    int mcc;
    int mnc;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;

    Handler handler = new Handler() {
        public void  handleMessage(android.os.Message msg){
            Log.i("handle.msg",""+msg.what);
            switch (msg.what)
            {
                case 1:
                    showNeighbor();
                    break;
                case 0:
                    SBtext.setText("get cell location error.");
                    break;
                default:
                    break;
            }
        }
    };



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsmcell_location);
        MCCtext = (TextView) findViewById(R.id.textViewMCC);
        MNCtext = (TextView) findViewById(R.id.textViewMNC);
        LACtext = (TextView) findViewById(R.id.textViewLAC);
        CIDtext = (TextView) findViewById(R.id.textViewCID);
        SBtext = (TextView) findViewById(R.id.textView7SB);// 获取基站信息
        Button SkipButton = (Button)findViewById(R.id.button2) ;

        telephonyManager= (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        MyListener = new MyPhoneStateListener();

        SkipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(GSMCellLocationActivity.this,SingleCellLocation.class);
                startActivity(intent);
            }
        });


        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                //设备是否装有sim卡
                if (mTelephonyManager.SIM_STATE_ABSENT == mTelephonyManager.getSimState()) {
                    Log.i(TAG, "SIM STATE ABSENT");
                    return;
                }
                // 返回值MCC + MNC
                String operator = mTelephonyManager.getNetworkOperator();
                mcc = Integer.parseInt(operator.substring(0, 3));
                mnc = Integer.parseInt(operator.substring(3));

                if (mTelephonyManager.PHONE_TYPE_GSM == mTelephonyManager.getPhoneType()) {
                    // 中国移动和中国联通获取LAC、CID的方式
                    GsmCellLocation location = (GsmCellLocation) mTelephonyManager.getCellLocation();
                    lac = location.getLac();
                    cellId = location.getCid();
                } else if (mTelephonyManager.PHONE_TYPE_CDMA == mTelephonyManager.getPhoneType()) {
                    // 中国电信获取LAC、CID的方式
                    CdmaCellLocation location1 = (CdmaCellLocation) mTelephonyManager.getCellLocation();
                    lac = location1.getNetworkId();
                    cellId = location1.getBaseStationId();
                    cellId /= 16;
                }
                Log.i(TAG, " MCC = " + mcc + "\t MNC = " + mnc + "\t LAC = " + lac + "\t CID = " + cellId);
                MCCtext.setText("" + mcc);
                MNCtext.setText("" + mnc);
                LACtext.setText("" + lac);
                CIDtext.setText("" + cellId);

                ConnectedCell = new CellLoc(lac,cellId,0);

                // 获取邻区基站信息
                List<NeighboringCellInfo> infos = mTelephonyManager.getNeighboringCellInfo();
                Log.d("neighbornum", Integer.toString(infos.size()));
                neighbornum = infos.size();
                neighborCellLoc = new ArrayList<CellLoc>(neighbornum);
                for (NeighboringCellInfo info1 : infos) { // 根据邻区总数进行循环
                    neighborCellLoc.add(new CellLoc(info1.getLac(),info1.getCid(),info1.getRssi()* 2 - 113));
                }
           //     Log.i(TAG, " 获取邻区基站信息:" + sb.toString());
                new Thread(){
                    @Override
                    public void run(){
                        try{
                            Message msg = new Message();
                            if (neighborCellLoc==null)
                            {
                                msg.what = 1;
                                handler.sendMessage(msg);
                            }
                            else{
                                for (CellLoc cellLoc:neighborCellLoc){
                                    String getstr = "http://api.cellocation.com/cell/?mcc="+mcc+"&mnc="+mnc+"&lac="+cellLoc.getLac()+"&ci="+cellLoc.getCid()+"&output=xml";
                                    URL url = new URL(getstr);
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setRequestMethod("GET");
                                    if (conn.getResponseCode()==200) {
                                        InputStream instream = conn.getInputStream();
                                        XmlPullParser parser = Xml.newPullParser();
                                        parser.setInput(instream, "UTF-8");
                                        int event = parser.getEventType();
                                        while (event != XmlPullParser.END_DOCUMENT) {
                                            Log.i("Start", "Start parsing");
                                            switch (event) {
                                                case XmlPullParser.START_TAG:
                                                    if ("lat".equals(parser.getName())) {
                                                        cellLoc.setLat(Double.parseDouble(parser.getText()));
                                                    } else if ("loc".equals(parser.getName())) {
                                                        cellLoc.setLon(Double.parseDouble(parser.getText()));
                                                    }
                                                    break;
                                                case XmlPullParser.END_TAG:
                                                    break;
                                                default:
                                                    break;
                                            }
                                            event = parser.next();
                                        }
                                    }

                                }

                                String getstr = "http://api.cellocation.com/cell/?mcc="+mcc+"&mnc="+mnc+"&lac="+ConnectedCell.getLac()+"&ci="+ConnectedCell.getCid()+"&output=xml";
                                URL url = new URL(getstr);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("GET");
                                if (conn.getResponseCode()==200) {
                                    InputStream instream = conn.getInputStream();
                                    XmlPullParser parser = Xml.newPullParser();
                                    parser.setInput(instream, "UTF-8");
                                    int event = parser.getEventType();
                                    while (event != XmlPullParser.END_DOCUMENT) {
                                        Log.i("Start", "Start parsing");
                                        switch (event) {
                                            case XmlPullParser.START_TAG:
                                                if ("lat".equals(parser.getName())) {
                                                    ConnectedCell.setLat(Double.parseDouble(parser.getText()));
                                                } else if ("loc".equals(parser.getName())) {
                                                    ConnectedCell.setLon(Double.parseDouble(parser.getText()));
                                                }
                                                break;
                                            case XmlPullParser.END_TAG:
                                                break;
                                            default:
                                                break;
                                        }
                                        event = parser.next();
                                    }
                                }

                                Log.i("info","get location end.");
                                msg.what = 1;
                                handler.sendMessage(msg);
                            }
                        }catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
       // client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        telephonyManager.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        // telephonyManager.listen(celllistener, PhoneStateListener.LISTEN_CELL_LOCATION); // 基站位置的变化
    }

    //show neighboring cell tower in textView
    public void showNeighbor()
    {
        Log.i("neiborCellLoc.SIZE",""+neighborCellLoc.size());
        if (neighborCellLoc==null)
        {
            SBtext.setText("cannot get neighboring cell location");
        }
        else
        {
            StringBuffer text = new StringBuffer("");
            if(neighborCellLoc.size()==0)
            {
                text.append("no neighboring cells.\n");
            }
            for (CellLoc cellloc:neighborCellLoc)
            {
                text.append("lac:"+cellloc.getLac());
                text.append("cid:"+cellloc.getCid());
                text.append("bsss:"+cellloc.getRSSI());
                text.append("lat:"+cellloc.getLat());
                text.append("lon:"+cellloc.getLon());
                text.append("\n");
            }
            SBtext.setText(text.toString());
        }

    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    public Action getIndexApiAction() {
//        Thing object = new Thing.Builder()
//                .setName("GSMCellLocation Page") // TODO: Define a title for the content shown.
//                // TODO: Make sure this auto-generated URL is correct.
//                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
//                .build();
//        return new Action.Builder(Action.TYPE_VIEW)
//                .setObject(object)
//                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
//                .build();
//    }

    @Override
    public void onStart() {
        super.onStart();

//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.connect();
//        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        AppIndex.AppIndexApi.end(client, getIndexApiAction());
//        client.disconnect();
    }

    private class MyPhoneStateListener extends PhoneStateListener
    {
        /* Get the Signal strength from the provider, each tiome there is an update  从得到的信号强度,每个tiome供应商有更新*/
        @Override

        public void onSignalStrengthsChanged(SignalStrength signalStrength)
        {
            super.onSignalStrengthsChanged(signalStrength);
            if (signalStrength.getGsmSignalStrength() != 99) {
                ConnectedCell.setRSSI(signalStrength.getGsmSignalStrength() * 2 - 113);
                Toast.makeText(getApplicationContext(),
                        "Go to Firstdroid!!! GSM Cinr = " + String.valueOf(signalStrength.getGsmSignalStrength() * 2 - 113) + "dbM", Toast.LENGTH_SHORT).show();
                System.out.println("****" + String.valueOf(signalStrength.getGsmSignalStrength() * 2 - 113));
            }
        }
    }
}
