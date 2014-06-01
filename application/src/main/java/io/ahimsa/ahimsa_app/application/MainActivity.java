package io.ahimsa.ahimsa_app.application;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity {

    //Constants----------------------------------------------
    public static final String ACTION_ENABLE_NODE_SERVICE = "enableNodeService";
    public static final String ACTION_DISABLE_NODE_SERVICE = "disableNodeService";

    //MainActivity-------------------------------------------
    private static final String TAG = "MainActivity";
    private Intent nodeServiceIntent;

    //Activity Overrides-------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, ".onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new txBroadcastFragment())
                    .commit();
        }

        //Set up intent for fragment receiver
        final IntentFilter intentFilter_fragment = new IntentFilter();
        intentFilter_fragment.addAction(ACTION_ENABLE_NODE_SERVICE);
        intentFilter_fragment.addAction(ACTION_DISABLE_NODE_SERVICE);
        intentFilter_fragment.addAction(NodeService.ACTION_TOAST_NUM_PEERS);
        intentFilter_fragment.addAction(NodeService.ACTION_BROADCAST_TRANSACTION);
        registerReceiver(fragmentReceiver, intentFilter_fragment);

        //Set up intent for service receiver
//        final IntentFilter intentFilter_service = new IntentFilter();
//        intentFilter_service.addAction();

        nodeServiceIntent = new Intent(this, NodeService.class);

    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(fragmentReceiver);
        super.onDestroy();
    }

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
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }


    //Receiver to communicate with fragment------------------
    private final BroadcastReceiver fragmentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (ACTION_ENABLE_NODE_SERVICE.equals(action)) {
                    enableNodeService();
                }
                else if (ACTION_DISABLE_NODE_SERVICE.equals(action)) {
                    disableNodeService();
                }
                else if (NodeService.ACTION_TOAST_NUM_PEERS.equals(action)){
                    toastNumberOfPeers();
                } else if (NodeService.ACTION_BROADCAST_TRANSACTION.equals(action)){
                    broadcastTx(intent);
                }
            }
        }
    };

    //-------------------------------------------------------
    //start node service
    private void enableNodeService(){
        startService(nodeServiceIntent);
        Toast.makeText(getApplicationContext(), "Enabled NodeService",
                Toast.LENGTH_SHORT).show();
    }

    //stop node service
    private void disableNodeService(){
        stopService(nodeServiceIntent);
        Toast.makeText(getApplicationContext(), "Disabled NodeService",
                Toast.LENGTH_SHORT).show();
    }

    //send byte array to node service for broadcast
    private void broadcastTx(Intent intent_from_fragment){
        final String tx_hex_string = intent_from_fragment.getStringExtra(NodeService.HEX_STRING_TRANSACTION);

        Intent intent_to_NodeService = new Intent(this, NodeService.class);
        intent_to_NodeService.setAction(NodeService.ACTION_BROADCAST_TRANSACTION);

        intent_to_NodeService.putExtra(NodeService.BYTE_ARRAY_TRANSACTION, hexStringToByteArray(tx_hex_string));

        startService(intent_to_NodeService);
    }

    //tell node service to toast number of pears if running. else toast negative statement.
    private void toastNumberOfPeers(){
        if(isNodeServiceRunning()) {
            Intent toastNumberOfPeersIntent = new Intent(this, NodeService.class);
            toastNumberOfPeersIntent.setAction(NodeService.ACTION_TOAST_NUM_PEERS);
            startService(toastNumberOfPeersIntent);
        } else {
            Toast.makeText(getApplicationContext(), "NodeService is not running",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //-------------------------------------------------------
    //returns boolean of whether node service is running. not recommended for production.
    private boolean isNodeServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NodeService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //good utility method. hex string to byte array. eventually will live within message library.
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }



}
