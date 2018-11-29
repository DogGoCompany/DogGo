package com.example.doggo;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class BTcontrol extends AppCompatActivity {

    Button btnDis;
    TextView txtHR;
    String address = null;
    private ProgressDialog progress;

    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    //Handler
    private static Handler mainUIHandler;

    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard/Base UUID

    StringBuilder sb = new StringBuilder();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btcontrol);

        Intent i = getIntent();
        address = i.getStringExtra(DisplayBTList.EXTRA_ADDRESS); // Receive the address of the bluetooth device

        new connectBT().execute();

        btnDis = (Button) findViewById(R.id.btnDisconnect);
        txtHR = (TextView) findViewById(R.id.txtHR);

        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectBT(); //close connection
            }
        });

        mainUIHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                txtHR.setText(msg.obj.toString());
            }
        };
    }

    private void disconnectBT()
    {
        if (btSocket != null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { displayMessage("Error");}
        }

        finish();
    }

    private void displayMessage(String msg)
    {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    private class connectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(BTcontrol.this, "Connecting...", "This may take a while!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    // Connect to the device's address and checks if it's available
                    BluetoothDevice myBluetoothRemoteDevice = myBluetooth.getRemoteDevice(address);

                    // Create a RFCOMM (SPP) connection
                    btSocket = myBluetoothRemoteDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
//                    btSocket = myBluetoothRemoteDevice.createRfcommSocketToServiceRecord(myUUID);

                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                displayMessage("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                displayMessage("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
            new MyTask().execute(btSocket);
        }
    }


    private class MyTask extends AsyncTask<BluetoothSocket, Void,StringBuilder > {

        @Override
        protected StringBuilder doInBackground(BluetoothSocket... params) {
            InputStream socketInputStream = null;
            try {
                socketInputStream = params[0].getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = socketInputStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    sb.append(readMessage);

                    int endOfLineIndex = sb.indexOf("\r\n");
                    if (endOfLineIndex > 0) {
                        String sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());

                        // log message to console
                        System.out.println(sbprint);
                        // pass message to handler
                        if (mainUIHandler != null){
                            Message message = mainUIHandler.obtainMessage();
                            message.what = 1;
                            message.obj = sbprint;
                            mainUIHandler.sendMessage(message);
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
            return sb;
        }

        @Override
        protected void onPostExecute(StringBuilder result) {
            super.onPostExecute(result);
        }
    }

}
