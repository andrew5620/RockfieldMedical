package com.rockfieldmd.andrew.rockfieldmedical;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity {

    ImageButton btnOn, btnOff,btnFlow,btnSpeed,btnInvisible;
    Button LeftButton, RightButton;
    TextView txtString, txtStringLength, sensorView0, sensorView1, sensorView2;
    Handler bluetoothIn;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;


    int previousSensor0 = 0;
    int currentSensor0 = 0;
    int averageFlow = 0;
    int averageAmount = 1;
    int currentFlow = 0;
    int tareValue = 0;
    int alarmCount = 0;

    String sensor0;            //get sensor value from string between indices 1-5
    String sensor2;            //same again...
    String sensor1;

    boolean averageFlowrate = false;
    boolean tare = false;
    boolean alarmEnable = false;
    boolean alarmActive = false;

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerList = (ListView)findViewById(R.id.navList);

        addDrawerItems();


        //Link the buttons and textViews to respective views
        btnOn = (ImageButton) findViewById(R.id.buttonOn);
        btnOff = (ImageButton) findViewById(R.id.buttonOff);
        btnFlow = (ImageButton) findViewById(R.id.flow);
        btnInvisible = (ImageButton) findViewById(R.id.invisible);
        btnSpeed = (ImageButton) findViewById(R.id.speed);

        LeftButton = (Button) findViewById(R.id.button);
        RightButton = (Button) findViewById(R.id.button2);

        txtString = (TextView) findViewById(R.id.txtString);                //String that reads in data from arduino at bottom of screen
        txtString.setVisibility(View.GONE);

        txtStringLength = (TextView) findViewById(R.id.txtstringlength);
        txtStringLength.setVisibility(View.GONE);

        sensorView0 = (TextView) findViewById(R.id.sensorView0);
        sensorView1 = (TextView) findViewById(R.id.sensorView1);
        sensorView2 = (TextView) findViewById(R.id.sensorView2);
        sensorView2.setVisibility(View.GONE);

        final ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 500);


        btnSpeed.setOnClickListener(new OnClickListener() {
            public void onClick(View c) {
                sensorView1.setVisibility(View.VISIBLE);
                if(averageFlowrate) averageFlowrate = false;
                else {
                    averageFlowrate = true;
                    averageFlow = 0;
                    averageAmount = 1;
                }

            }
        });

        btnFlow.setOnClickListener(new OnClickListener() {
            public void onClick(View c) {
                if(sensorView2.getVisibility() == View.INVISIBLE) sensorView2.setVisibility(View.VISIBLE);
                else  sensorView2.setVisibility(View.INVISIBLE);
            }
        });

        btnInvisible.setOnClickListener(new OnClickListener() {
            public void onClick(View c) {
                if(txtString.getVisibility() == View.INVISIBLE)
                {
                    txtString.setVisibility(View.VISIBLE);
                    txtStringLength.setVisibility(View.VISIBLE);
                }
                else
                {
                    txtString.setVisibility(View.INVISIBLE);
                    txtStringLength.setVisibility(View.INVISIBLE);
                }

            }
        });


        LeftButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

            }
        });

        RightButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                tare = true;
            }
        });


        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        txtString.setText("Data Received = " + dataInPrint);
                        int dataLength = dataInPrint.length();                          //get length of data received
                        txtStringLength.setText("String Length = " + String.valueOf(dataLength) + " Tare Value = " + tareValue + " AlarmCount = " + alarmCount);

                        if (recDataString.charAt(0) == '#')                             //if it starts with # we know it is what we are looking for
                        {
                            sensor0 = recDataString.substring(1, 5);             //get sensor value from string between indices 1-5
                            sensor2 = recDataString.substring(12, 16);            //same again...
                            sensor1 = recDataString.substring(17, 24);

                            currentFlow = Integer.parseInt(sensor2);

                            if(currentFlow <  400) {
                                if (averageFlowrate == true) {
                                    averageFlow += currentFlow;
                                    sensorView2.setText("Hourly Flow Rate: " + (averageFlow / averageAmount) + "mls/h");
                                    averageAmount++;
                                } else
                                    sensorView2.setText("Minute Flow Rate: " + sensor2 + "mls/h");
                            }
                            else sensorView2.setText("Calibrating");

                            currentSensor0 = Integer.parseInt(sensor0);

                            ValueAnimator va = ValueAnimator.ofInt(previousSensor0, currentSensor0);
                            va.setInterpolator(new AccelerateDecelerateInterpolator());
                            va.setDuration(700);
                            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                                public void onAnimationUpdate(ValueAnimator animation) {
                                    Integer value = (Integer) animation.getAnimatedValue();
                                    sensorView0.setText((value - tareValue) + "mls");
                                }
                            });
                            va.start();

                            previousSensor0 = Integer.parseInt(sensor0);

                            if(tare) {
                                tareValue = currentSensor0;
                                tare = false;
                            }

                            if(alarmEnable) {
                                if (currentFlow < 20 && !alarmActive)

                                {
                                    alarmCount++;
                                    if (alarmCount == 20) {
                                        alarmActive = true;
                                        alarmCount = 0;
                                    }
                                    sensorView1.setText("Alarm Count: " + alarmCount);
                                }

                                else if (currentFlow > 40 && alarmCount > 0) alarmCount = 0;

                                if (alarmActive) {
                                    mConnectedThread.write("0");
                                    sensorView1.setText("ALARM TRIGGERED");
                                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 250);
                                }
                                //else sensorView1.setText("Alarm Active");
                            }
                            else
                            {
                                sensorView1.setText("Time: " + sensor1); //TIME DISPLAY
                                mConnectedThread.write("L");
                            }

                        }
                        //clear all string data
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send "0" via Bluetooth
                if(alarmEnable) {
                    Toast.makeText(getBaseContext(), "Alarm Off", Toast.LENGTH_SHORT).show();
                    alarmEnable = false;
                }
                else{
                    Toast.makeText(getBaseContext(), "Alarm On", Toast.LENGTH_SHORT).show();
                    alarmEnable = true;
                    alarmActive = false;
                    alarmCount = 0;
                }
            }
        });

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, DeviceListActivity.class);
                i.putExtra(EXTRA_DEVICE_ADDRESS, address);
                startActivity(i);
            }
        });

    }

    private void addDrawerItems() {
        String[] osArray = { "Settings", "Help", "Rockfield Website", "Feed Type", "About" };
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    Intent i = new Intent(MainActivity.this, DeviceListActivity.class);             //CHANGED CODE FOR RETURNING BACK TO LIST SCREEN WHEN BLUETOOTH FAILS
                    startActivity(i);
                }
            }, 5000);
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

}