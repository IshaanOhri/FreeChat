package com.ishaan.wifip2p;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button sendButton;
    TextView connectionStatusTextView;
    private EditText newMessageEditText;
    private ListView listView;
    private WifiManager wifiManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;
    private ArrayList<WifiP2pDevice> peers = new ArrayList<>();
    private String[] deviceName;
    private WifiP2pDevice[] devices;
    static final int MESSAGE_READ = 1;
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;
    final static int LOC_CODE = 100;
    private ArrayList<String> messages = new ArrayList<>();
    private ArrayList<Integer> identifier = new ArrayList<>();
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendButton = findViewById(R.id.sendButton);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        newMessageEditText = findViewById(R.id.newMessageEditText);
        listView = findViewById(R.id.listView);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        checkLocation();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);

        broadcastReceiver = new WifiDirect(wifiP2pManager,channel,this);

        checkWiFi();

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkWiFi();
                checkLocation();
                String message = newMessageEditText.getText().toString().trim();
                if (message.equals(""))
                {

                }
                else
                {
                    try {
                        sendReceive.write(message.getBytes());
                        String str = new String(message.getBytes(), "UTF-8");
                        messages.add(str);
                        identifier.add(0);
                        recyclerViewAdapter = new RecyclerViewAdapter(messages, identifier);
                        recyclerView.setAdapter(recyclerViewAdapter);
                        Log.i("INFO","Sent Message: " + messages);
                        newMessageEditText.setText("");
                    }
                    catch (Exception e)
                    {
                        Log.i("INFO","ERROR " + e.toString());
                        Toast.makeText(MainActivity.this, "Connection problem", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device = devices[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                
                wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Connecting to " + device.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(MainActivity.this, "Connection Failed :(", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void checkLocation() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOC_CODE);
        }
    }

    private void checkWiFi() {
        if(!wifiManager.isWifiEnabled())
        {
            new AlertDialog.Builder(this)
                    .setTitle("Turn on WiFi")
                    .setMessage("Please enable WiFi to continue")
                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            checkWiFi();
                        }
                    })
                    .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                            System.exit(0);
                        }
                    }).show();
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {

            switch (message.what)
            {
                case MESSAGE_READ:
                    byte[] readBuffer = (byte[]) message.obj;
                    String tempMessage = new String(readBuffer,0,message.arg1);
                    messages.add(tempMessage);
                    identifier.add(1);
                    recyclerViewAdapter = new RecyclerViewAdapter(messages, identifier);
                    recyclerView.setAdapter(recyclerViewAdapter);
                    Log.i("INFO","Received Messages: " + messages);
                    break;
            }
            return true;
        }
    });

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            if(!wifiP2pDeviceList.getDeviceList().equals(peers))
            {
                peers.clear();
                peers.addAll(wifiP2pDeviceList.getDeviceList());
                deviceName = new String[wifiP2pDeviceList.getDeviceList().size()];
                devices = new WifiP2pDevice[wifiP2pDeviceList.getDeviceList().size()];
                int i = 0;

                for(WifiP2pDevice wifiP2pDevice : wifiP2pDeviceList.getDeviceList())
                {
                    deviceName[i] = wifiP2pDevice.deviceName;
                    devices[i] = wifiP2pDevice;
                    i++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, deviceName);
                listView.setAdapter(adapter);
            }
            if (peers.size() == 0)
            {
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if(wifiP2pInfo.groupFormed)
            {
                if(wifiP2pInfo.isGroupOwner)
                {
                    serverClass = new ServerClass();
                    serverClass.start();
                }
                else
                {
                    clientClass = new ClientClass(groupOwnerAddress);
                    clientClass.start();
                }
                connectionStatusTextView.setText(R.string.successful_connection);
                recyclerView.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver,intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket socket1)
        {
            socket = socket1;
            try
            {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                Log.i("INFO","InputStream: " + inputStream.toString());
                Log.i("INFO","OutputStream: " + outputStream.toString());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket!=null)
            {
                try {
                    bytes = inputStream.read(buffer);
                    if(bytes>0)
                    {
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(final byte[] bytes)
        {
            new Thread(new Runnable(){

                @Override
                public void run()
                {
                    try
                    {
                        outputStream.write(bytes);
                    }
                    catch (IOException e)
                    {
                        Log.i("INFO", e.toString());
                        e.printStackTrace();
                    }
                }}).start();
        }
    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostAddress;

        public ClientClass(InetAddress hostAddress1)
        {
            hostAddress = hostAddress1.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAddress,8888),500);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0)
        {
            if(!(requestCode == LOC_CODE && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED))
            {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Denied")
                        .setMessage("Location permission is required to run application")
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOC_CODE);
                            }
                        })
                        .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                                System.exit(0);
                            }
                        }).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId())
        {
            case R.id.discoverButton:
                checkWiFi();
                checkLocation();
                listView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);

                wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatusTextView.setText("Discovering devices...");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatusTextView.setText("Failed to discover devices");
                    }
                });
                return true;

            default:
                return false;
        }

    }
}
