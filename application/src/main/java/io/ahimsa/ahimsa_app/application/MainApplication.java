package io.ahimsa.ahimsa_app.application;

import android.app.Application;
import android.content.Intent;
import android.preference.PreferenceManager;


import io.ahimsa.ahimsa_app.application.service.NodeService;
import io.ahimsa.ahimsa_app.application.service.OldNodeService;

/**
 * Created by askuck on 6/9/14.
 */
public class MainApplication extends Application {

    private Configuration config;
    private Intent nodeServiceIntent;

    @Override
    public void onCreate(){

//        I do not believe StrictMode is necessary, but ThrowOnLockCycles
//        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().permitDiskWrites().penaltyLog().build());
//        Threading.throwOnLockCycles();

        super.onCreate();

        //Use to store basic information
        config = new Configuration(PreferenceManager.getDefaultSharedPreferences(this));


        //Set up intent for service receiver
        nodeServiceIntent = new Intent(this, OldNodeService.class);


    }

    //start node service
    public void enableNodeService(){
        startService(nodeServiceIntent);
    }

    //stop node service
    public void disableNodeService(){
        stopService(nodeServiceIntent);
    }

    //send raw transaction via node intent service
    public void broadcast(byte[] rawTx){
        NodeService.startActionBroadcast(this, rawTx);

    }



}
