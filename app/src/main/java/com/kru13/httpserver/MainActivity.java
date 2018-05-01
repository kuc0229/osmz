package com.kru13.httpserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.kru13.httpserver.model.StatisticData;
import com.kru13.httpserver.service.HttpServerService;

public class MainActivity extends Activity implements OnClickListener, Handler.Callback {

    private Intent httpServerService;
    private StatisticManager statisticManager;
    private Handler handler;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_server);

        Button btn1 = (Button) findViewById(R.id.button1);
        Button btn2 = (Button) findViewById(R.id.button2);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj instanceof StatisticData) {
                    StatisticData data = (StatisticData) msg.obj;
                    updateStatistic(data);
//                    Log.d("MAIN ACTIVITY", "received message " + msg.obj);
                } else {
//                    Log.d("MAIN ACTIVITY", "received unknown message " + msg.obj);
                }
            }
        };
    }

    private void updateStatistic(StatisticData data) {
        TextView requestCountView = (TextView) findViewById(R.id.requestCountValue);
        TextView transferredBytesView = (TextView) findViewById(R.id.transferredBytesValue);
        TextView currentClientsValue = (TextView) findViewById(R.id.currentClientsValue);

        requestCountView.setText(String.valueOf(data.getCurrentRequestCount()));
        // convert to MB
        transferredBytesView.setText(String.valueOf(Math.round(data.getCurrentTransferredBytes() / 1024) / 1.0));
        currentClientsValue.setText(String.valueOf(data.getCurrentActiveClients()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.http_server, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button1) {
            this.httpServerService = new Intent(this, HttpServerService.class);
            startService(this.httpServerService);
            StatisticManager.initilizeStorage();
            statisticManager = new StatisticManager(handler);
            statisticManager.start();
        }

        if (v.getId() == R.id.button2) {
            if (httpServerService != null) {
                stopService(this.httpServerService);
                statisticManager.cancel();
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}