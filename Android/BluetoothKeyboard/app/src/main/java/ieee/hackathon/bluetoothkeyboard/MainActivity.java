package ieee.hackathon.bluetoothkeyboard;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;



public class MainActivity extends Activity {
    static int REQUEST_ENABLE_BT = 1;
    BluetoothFragment mFrag;
    WriteFragment mWriteFrag;
    static ListView mView;
    static ArrayAdapter<String> mArrayAdapter;
    static BluetoothAdapter mBluetoothAdapter;
    public static final UUID MY_UUID = UUID.fromString("37407000-8cf0-11bd-b23e-10b75c30d20a");
    private static BluetoothSocket mmSocket;
    private static BluetoothDevice mmDevice;
    private static ConnectThread connectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFrag = new BluetoothFragment();
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFrag)
                    .commit();
        }

    }

    public void onClickOn(View v) {
     mFrag.onClickOn(v);
    }
    public void onClickScan(View v) { mFrag.onClickScan(v); }
    public void onClickPair(View v) { mFrag.onClickPair(v); }


    public void onClickWrite(View v) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        mWriteFrag = new WriteFragment();
        ft.replace(R.id.container, mWriteFrag, "WriteFrag");
        ft.addToBackStack(null);
        ft.commit();
    }

    //public void startWrite(View v) { mWriteFrag.startWrite(v); }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class WriteFragment extends Fragment {
        View rootView;
        EditText text;
        public WriteFragment() {

        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_write, container, false);
            text = (EditText) rootView.findViewById(R.id.edit_text);
            TextWatcher watch = new TextWatcher(){
                @Override
                public void afterTextChanged(Editable arg0) {
                    // TODO Auto-generated method stub
                }
                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                              int arg3) {
                    // TODO Auto-generated method stub
                }
                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    if(s.length() != 0) {
                        Log.v("Hello", Integer.toString(s.length()));
                        write(s.charAt(s.length() - 1));
                    }
                }};
            text.addTextChangedListener(watch);
            return rootView;
        }


        public void write(char t) {
            Log.v("Hello", Character.toString(t));

            OutputStream mmOutStream = null;
            try {
                mmOutStream = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mmOutStream != null) {
                DataOutputStream stream = new DataOutputStream(mmOutStream);
                try {
                    stream.writeChar(t);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            text.setText("");
        }
    }

    /*
     * A placeholder fragment containing a simple view.
     */
    public static class BluetoothFragment extends Fragment {


        public BluetoothFragment() {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mArrayAdapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.list_view);

            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mView = (ListView) rootView.findViewById(R.id.list_view);
            mView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    String t = mArrayAdapter.getItem(position);
                    String lines[] = t.split("\\r?\\n");
                    Toast.makeText(getActivity(), lines[1], Toast.LENGTH_SHORT).show();
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(lines[1]);
                    connectThread = new ConnectThread(device);
                    connectThread.start();
                }
            });
            return rootView;
        }

        public void onClickOn(View v) {
            if (mBluetoothAdapter == null) {
                Toast.makeText(getActivity(), "No Bluetooth", Toast.LENGTH_LONG);
            }

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            if(mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
            Toast.makeText(getActivity(), "Bluetooth is on", Toast.LENGTH_LONG);

        }

        public void onClickScan(View v) {
            // Create a BroadcastReceiver for ACTION_FOUND
            final BroadcastReceiver mReceiver;
            mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    // When discovery finds a device
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // Add the name and address to an array adapter to show in a ListView
                        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }
            };
            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            getActivity().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
            mBluetoothAdapter.startDiscovery();
            mView.setAdapter(mArrayAdapter);
            Toast.makeText(getActivity(), "Showing all scanned devices", Toast.LENGTH_LONG);
        }

        public void onClickPair(View v) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            mView.setAdapter(mArrayAdapter);
            Toast.makeText(getActivity(), "Showing all paired devices", Toast.LENGTH_LONG);
           }
    }

    //public static void showConnected(String device) {
    //    Toast.makeText(thisActivity, "Connected to: " + device + "", Toast.LENGTH_LONG);
    //}

    private static class ConnectThread extends Thread {


        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            Log.v("Main", "Main");

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                Log.v("Try", "Try");
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.v("Created", "Created");
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
            //showConnected(mmSocket.getRemoteDevice().getName());
            // Do work to manage the connection (in a separate thread)
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
