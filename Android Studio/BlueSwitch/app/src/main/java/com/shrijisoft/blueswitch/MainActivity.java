package com.shrijisoft.blueswitch;

import android.app.Activity;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.preference.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

//import android.support.v7.app.AppCompatActivity;
import androidx.annotation.RequiresApi;

import java.io.*;
import java.util.*;

public class MainActivity extends Activity {

    private BluetoothAdapter bAdapter;
    private BluetoothDevice bDev;
    private BluetoothSocket bSocket;

    private InputStream bInStream;
    private OutputStream bOutStream;

    private ListView list;

    private Button btnSearch;
    private Button btnConnect;


    private final int BT_ENABLE_REQUEST = 10;
    private final int SETTINGS = 20;

    private UUID mDeviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private int mBufferSize = 50000; //Default

    public static final String DEVICE_EXTRA = "com.shrijisoft.blueswitch.SOCKET";
    public static final String DEVICE_UUID = "com.shrijisoft.blueswitch.uuid";
    private static final String DEVICE_LIST = "com.shrijisoft.blueswitch.devicelist";
    private static final String DEVICE_LIST_SELECTED = "com.shrijisoft.blueswitch.devicelistselected";
    public static final String BUFFER_SIZE = "com.shrijisoft.blueswitch.buffersize";
    private static final String TAG = "BlueSwitch-MainActivity";

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSearch = (Button) findViewById(R.id.search);
        btnConnect = (Button) findViewById(R.id.connect);

        list = (ListView) findViewById(R.id.blueList);

        if (savedInstanceState != null){
            ArrayList<BluetoothDevice> bList = savedInstanceState.getParcelableArrayList(DEVICE_LIST);
            if(bList != null){
                initList(bList);
                MyAdapter adapter = (MyAdapter) list.getAdapter();
                int selectedIndex = savedInstanceState.getInt(DEVICE_LIST_SELECTED);
                if(selectedIndex != -1) {
                    adapter.setSelectedIndex(selectedIndex);
                    btnConnect.setEnabled(true);
                }
            } else {
                initList(new ArrayList<BluetoothDevice>());
            }
        } else {
            initList(new ArrayList<BluetoothDevice>());
        }

        btnSearch.setOnClickListener((View v) -> {
            bAdapter = BluetoothAdapter.getDefaultAdapter();

            if(bAdapter == null) {
                Toast.makeText(getApplicationContext(), "Bluetooth not found!", Toast.LENGTH_SHORT).show();
            } else if(!bAdapter.isEnabled()){
                Intent enableBT =  new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, BT_ENABLE_REQUEST);
            } else {
                new SearchDevices().execute();
            }
        });

        btnConnect.setOnClickListener((View v) -> {
            bDev = ((MyAdapter) (list.getAdapter())).getSelectedItem();

            Intent intent = new Intent(getApplicationContext(), Controlling.class);
            intent.putExtra(DEVICE_EXTRA, bDev);
            intent.putExtra(DEVICE_UUID, mDeviceUUID.toString());
            intent.putExtra(BUFFER_SIZE, mBufferSize);
            startActivity(intent);

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_ENABLE_REQUEST:

                if(resultCode == RESULT_OK) {
                    msg("Bluetooth Enabled successfully!");
                    new SearchDevices().execute();
                } else {
                    msg("Bluetooth couldn't be enabled!");
                }
                break;
            case SETTINGS:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String uuid = prefs.getString("prefUuid", "Null");
                mDeviceUUID = UUID.fromString(uuid);
                Log.d(TAG, "UUID: " + uuid);
                String bufSize = prefs.getString("prefTextBuffer", "Null");
                mBufferSize = Integer.parseInt(bufSize);

                String orientation = prefs.getString("prefOrientation", "Null");
                Log.d(TAG, "Orientation: " + orientation);
                if (orientation.equals("Landscape")) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (orientation.equals("Portrait")) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else if (orientation.equals("Auto")) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                }
                break;
            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Quick way to call the Toast
     * @param str
     */
    private void msg(String str) {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }



    /**
     * Initialize the List adapter
     * @param objects
     */
    private void initList(List<BluetoothDevice> objects) {
        final MyAdapter adapter = new MyAdapter(getApplicationContext(), R.layout.list_item, R.id.lstContent, objects);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.setSelectedIndex(position);
                btnConnect.setEnabled(true);
            }
        });
    }

    /**
     * Searches for paired devices. Doesn't do a scan! Only devices which are paired through Settings->Bluetooth
     * will show up with this. I didn't see any need to re-build the wheel over here
     * @author ryder
     *
     */
    private class SearchDevices extends AsyncTask<Void, Void, List<BluetoothDevice>> {

        @Override
        protected List<BluetoothDevice> doInBackground(Void... params) {
            Set<BluetoothDevice> pairedDevices = bAdapter.getBondedDevices();
            List<BluetoothDevice> listDevices = new ArrayList<BluetoothDevice>();
            for (BluetoothDevice device : pairedDevices) {
                listDevices.add(device);
            }
            return listDevices;

        }

        @Override
        protected void onPostExecute(List<BluetoothDevice> listDevices) {
            super.onPostExecute(listDevices);
            if (listDevices.size() > 0) {
                MyAdapter adapter = (MyAdapter) list.getAdapter();
                adapter.replaceItems(listDevices);
            } else {
                msg("No paired devices found, please pair your serial BT device and try again");
            }
        }

    }
    /**
     * Custom adapter to show the current devices in the list. This is a bit of an overkill for this
     * project, but I figured it would be good learning
     * Most of the code is lifted from somewhere but I can't find the link anymore
     * @author ryder
     *
     */
    private class MyAdapter extends ArrayAdapter<BluetoothDevice> {
        private int selectedIndex;
        private Context context;
        private int selectedColor = Color.parseColor("#abcdef");
        private List<BluetoothDevice> myList;

        public MyAdapter(Context ctx, int resource, int textViewResourceId, List<BluetoothDevice> objects) {
            super(ctx, resource, textViewResourceId, objects);
            context = ctx;
            myList = objects;
            selectedIndex = -1;
        }

        public void setSelectedIndex(int position) {
            selectedIndex = position;
            notifyDataSetChanged();
        }

        public BluetoothDevice getSelectedItem() {
            return myList.get(selectedIndex);
        }

        @Override
        public int getCount() {
            return myList.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return myList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class ViewHolder {
            TextView tv;
        }

        public void replaceItems(List<BluetoothDevice> list) {
            myList = list;
            notifyDataSetChanged();
        }

        public List<BluetoothDevice> getEntireList() {
            return myList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            ViewHolder holder;
            if (convertView == null) {
                vi = LayoutInflater.from(context).inflate(R.layout.list_item, null);
                holder = new ViewHolder();

                holder.tv = (TextView) vi.findViewById(R.id.lstContent);

                vi.setTag(holder);
            } else {
                holder = (ViewHolder) vi.getTag();
            }

            if (selectedIndex != -1 && position == selectedIndex) {
                holder.tv.setBackgroundColor(selectedColor);
            } else {
                holder.tv.setBackgroundColor(Color.WHITE);
            }
            BluetoothDevice device = myList.get(position);
            holder.tv.setText(device.getName() + "\n " + device.getAddress());

            return vi;
        }

    }
}