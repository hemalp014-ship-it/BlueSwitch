package com.shrijisoft.blueswitch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Paint;
import android.os.AsyncTask;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import android.view.*;

import org.w3c.dom.Text;

public class Controlling extends Activity {

    private static final String TAG = "BlueTest5-Controlling";
    private int mMaxChars = 50000;//Default//change this to string..........
    private UUID mDeviceUUID;
    private BluetoothSocket mBTSocket;
    private ReadInput mReadThread = null;

    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;

    private Button mBtnDisconnect;
    private BluetoothDevice mDevice;

    final static char Acquire = 'a';   //Switch 1 -> On

    final static char on  = 'o';   //Device -> On
    final static char off = 'f';   //Device -> Off

    private ProgressDialog progressDialog;
    private ScrollView scroll;

    TextView header;
    private CompoundButton switch1;
    private CompoundButton switch2;
    private CompoundButton switch3;
    private CompoundButton switch4;

    private boolean Acquiring = false;

    private OutputStream outputStream;

    private Date time;

    private class ReadInput implements Runnable {

        private boolean bStop = false;
        private Thread t;

        public ReadInput() {
            t = new Thread(this, "Input Thread");
            t.start();
        }

        public boolean isRunning() {
            return t.isAlive();
        }

        @Override
        public void run() {
            InputStream inputStream;

            try {
                inputStream = mBTSocket.getInputStream();
                while (!bStop) {
                    String data = "";
                    int temp;

                    while(inputStream.available() > 0) {
                        temp = inputStream.read();
                        if(temp == 0){
                            if(data.length() > 0) {
                                if (data.charAt(0) == 'd') {
                                    String finalData = data;
                                    runOnUiThread(() -> {SwitchTask(finalData);});
                                } else if(data.charAt(0) == 'a'){
                                    Acquiring = !(data.charAt(1) == off);
                                    float duration = (new Date().getTime() - time.getTime()) / 1000.0f;
                                    Log.i("Time Usage",duration  + " sec");
                                }
                                data = "";
                            }
                        }
                        else
                            data += (char) temp;
                    }
                    Thread.sleep(1);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            bStop = true;
        }
    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... params) {

            if (mReadThread != null) {
                mReadThread.stop();
                while (mReadThread.isRunning()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mReadThread = null;
            }

            try {
                mBTSocket.close();
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
            if (mIsUserInitiatedDisconnect) {
                finish();
            }
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(Controlling.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (mBTSocket == null || !mIsBluetoothConnected) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                }
            } catch (IOException e) {
                // Unable to connect to device
                // e.printStackTrace();
                mConnectSuccessful = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Could not connect to device.Please turn on your Hardware", Toast.LENGTH_LONG).show();
                finish();
            } else {
                msg("Connected to device");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }
            progressDialog.dismiss();
            new AcquireState().execute();
        }
    }

    private class AcquireState extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(Controlling.this, "Hold on", "Acquiring status of Devices");// http://stackoverflow.com/a/11130220/1287554
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {

                outputStream = mBTSocket.getOutputStream();

                Acquiring = true;

                time = new Date();

                outputStream.write(Acquire);
                outputStream.write(on);
                outputStream.write(0);
                outputStream.write('\r');
                outputStream.write('\n');

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
        }
    }

    private void SwitchTask(String str) {
        SwitchTask(str.charAt(1), str.charAt(2));
    }

    private void SwitchTask(int c1, int c2){
        boolean state = (c2 == (int) on);

        switch (c1) {
            case 1:
                switch1.setChecked(state);break;
            case 2:
                switch2.setChecked(state); break;
            case 3:
                switch3.setChecked(state); break;
            case 4:
                switch4.setChecked(state);break;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlling);

        findViewByIds();
        ActivityHelper.Initialize(this);

        Intent intent = getIntent();
        Bundle b = intent.getExtras();

        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));
        mMaxChars = b.getInt(MainActivity.BUFFER_SIZE);

        Log.d(TAG, "Ready");

        switch1.setOnCheckedChangeListener((CompoundButton btn, boolean isChecked) -> {
            try {
                if(!Acquiring) {
                    outputStream.write('d');
                    outputStream.write(1);
                    if (isChecked)
                        outputStream.write(on);
                    else
                        outputStream.write(off);
                    outputStream.write(0);
                    outputStream.write('\r');
                    outputStream.write('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        switch2.setOnCheckedChangeListener((CompoundButton btn, boolean isChecked) -> {
            try {
                if(!Acquiring) {
                    outputStream.write('d');
                    outputStream.write(2);
                    if (isChecked)
                        outputStream.write(on);
                    else
                        outputStream.write(off);
                    outputStream.write(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        switch3.setOnCheckedChangeListener((CompoundButton btn, boolean isChecked) -> {
            try {
                if(!Acquiring) {
                    outputStream.write('d');
                    outputStream.write(3);
                    if (isChecked)
                        outputStream.write(on);
                    else
                        outputStream.write(off);
                    outputStream.write(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        switch4.setOnCheckedChangeListener((CompoundButton btn, boolean isChecked) -> {
            try {
                if(!Acquiring) {
                    outputStream.write('d');
                    outputStream.write(4);
                    if (isChecked)
                        outputStream.write(on);
                    else
                        outputStream.write(off);
                    outputStream.write(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void findViewByIds() {
        switch1 = (Switch) findViewById(R.id.switch1);
        switch2 = (Switch) findViewById(R.id.switch2);
        switch3 = (Switch) findViewById(R.id.switch3);
        switch4 = (Switch) findViewById(R.id.switch4);

        scroll = (ScrollView) findViewById(R.id.scroll);
        header = (TextView) findViewById(R.id.header);
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
    private void msgLong(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        if (mBTSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mBTSocket == null || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        Log.d(TAG, "Resumed");

        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}