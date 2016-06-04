package com.fowler.tinyfileserver;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ServerManagementActivity extends AppCompatActivity {

    private static final String TAG = ServerManagementActivity.class.getSimpleName();

    private boolean started = false;

    private ServerStartReceiver serverStartReceiver = new ServerStartReceiver();
    private ServerStopReceiver serverStopReceiver = new ServerStopReceiver();
    private WifiReceiver wifiReceiver = new WifiReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_management);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        final Button serverBtn = (Button)findViewById(R.id.startServerBtn);
        serverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleServerState();
            }
        });

        if(!isWifiAvailable()) {
            serverBtn.setEnabled(false);
            setStatus("Connect to WIFI to start server.", true);
        }

        registerReceiver(wifiReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

//        Intent intent = getIntent();
//        onRestoreInstanceState(intent.getExtras());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver);
    }

    private void toggleServerState() {
        if (started) {
            stopHttpService();
        } else {
            startHttpService();
        }
    }

    private void addNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle("Tiny File Server");
        builder.setContentText("Tiny File Server is running.");

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ServerManagementActivity.class);
        Intent intent = new Intent(this, ServerManagementActivity.class);
        intent.putExtra("started", started);
        TextView statusView = (TextView)findViewById(R.id.textView);
        intent.putExtra("status", statusView.getText().toString());
        intent.putExtra("statusColor", statusView.getCurrentTextColor());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("started", started);

        TextView statusView = (TextView)findViewById(R.id.textView);
        editor.putString("status", statusView.getText().toString());
        editor.putInt("statusColor", statusView.getCurrentTextColor());

        TextView passwordView = (TextView)findViewById(R.id.passwordView);
        editor.putString("password", passwordView.getText().toString());

        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        started = prefs.getBoolean("started", false);

        Button serverBtn = (Button) findViewById(R.id.startServerBtn);
        serverBtn.setText(started ? R.string.stop_server_text : R.string.start_server_text);

        TextView statusView = (TextView) findViewById(R.id.textView);
        statusView.setText(prefs.getString("status", ""));
        statusView.setTextColor(prefs.getInt("statusColor", Color.BLACK));

        TextView passwordView = (TextView)findViewById(R.id.passwordView);
        passwordView.setText(prefs.getString("password", ""));
    }

//    @Override
//    protected void onSaveInstanceState(Bundle savedInstanceState) {
//        savedInstanceState.putBoolean("started", started);
//        TextView statusView = (TextView)findViewById(R.id.textView);
//        savedInstanceState.putString("status", statusView.getText().toString());
//        savedInstanceState.putInt("statusColor", statusView.getCurrentTextColor());
//        super.onSaveInstanceState(savedInstanceState);
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        if(savedInstanceState != null) {
//            super.onRestoreInstanceState(savedInstanceState);
//            started = savedInstanceState.getBoolean("started", false);
//            Button serverBtn = (Button) findViewById(R.id.startServerBtn);
//            serverBtn.setText(started ? R.string.stop_server_text : R.string.start_server_text);
//            TextView statusView = (TextView) findViewById(R.id.textView);
//            statusView.setText(savedInstanceState.getString("status", ""));
//            statusView.setTextColor(savedInstanceState.getInt("statusColor", Color.BLACK));
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server_management, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setStatus(int resid, boolean error) {
        setStatus(getResources().getText(resid), error);
    }

    private void setStatus(CharSequence text, boolean error) {
        TextView statusView = (TextView)findViewById(R.id.textView);
        statusView.setText(text);
        statusView.setTextColor(error ? Color.RED : Color.BLACK);
    }

    private void setPassword(CharSequence pw) {
        TextView pwView = (TextView)findViewById(R.id.passwordView);
        if(pw != null && pw.length() > 0)
            pwView.setText("Password: " + pw);
        else
            pwView.setText("");
    }

    private void startHttpService() {
        Intent intent = new Intent(this, WebServerIntentService.class);
        intent.putExtra(Constants.START, true);
        intent.putExtra(Constants.RECEIVER, serverStartReceiver);
        startService(intent);
    }

    private void stopHttpService() {
        Intent intent = new Intent(this, WebServerIntentService.class);
        intent.putExtra(Constants.START, false);
        intent.putExtra(Constants.RECEIVER, serverStopReceiver);
        startService(intent);
    }

    private boolean isWifiAvailable() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public class WifiReceiver extends BroadcastReceiver {

        private ServerManagementActivity activity;

        public WifiReceiver() {
            this.activity = ServerManagementActivity.this;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                Button serverBtn = (Button)activity.findViewById(R.id.startServerBtn);

                if(activity.isWifiAvailable()) {
                    serverBtn.setEnabled(true);
                    if(!activity.started)
                        activity.setStatus("", false);
                } else {
                    serverBtn.setEnabled(false);
                    if(activity.started)
                        activity.toggleServerState();
                    activity.setStatus("Connect to WIFI to start server.", true);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to receive WIFI update", e);
            }
        }
    }

    @SuppressLint("ParcelCreator")
    class ServerStartReceiver extends ResultReceiver {
        public ServerStartReceiver() {
            super(null);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(resultCode == Constants.SUCCESS) {
                started = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addNotification();
                        Button serverBtn = (Button)findViewById(R.id.startServerBtn);
                        serverBtn.setText(R.string.stop_server_text);
                        String serverUrl = "http://%s:%d";
                        WifiManager wm = (WifiManager) ServerManagementActivity.this.getSystemService(Context.WIFI_SERVICE);
                        String ip = android.text.format.Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                        HttpServer server = HttpServer.getHttpServer();
                        serverUrl = String.format(serverUrl, ip, server.getPort());
                        setStatus(String.format("Server has been started at %s", serverUrl), false);
                        setPassword(server.getPassword());
                    }
                });
            } else if(resultCode == Constants.FAILURE) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setStatus("Server failed to start.", true);
                        setPassword(null);
                    }
                });
            }
        }
    }

    @SuppressLint("ParcelCreator")
    class ServerStopReceiver extends ResultReceiver {
        public ServerStopReceiver() {
            super(null);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(resultCode == Constants.SUCCESS) {
                started = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        removeNotification();
                        Button serverBtn = (Button)findViewById(R.id.startServerBtn);
                        serverBtn.setText(R.string.start_server_text);
                        setStatus(R.string.server_stopped_text, false);
                        setPassword(null);
                    }
                });
            } else if(resultCode == Constants.FAILURE) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setStatus("Server failed to stop.", true);
                    }
                });
            }
        }
    }
}
