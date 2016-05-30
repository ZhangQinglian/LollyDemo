package com.zqlite.android.lollydemo.artphelper;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhangqnglian 2016/03/09
 */
public class ARTPHelper {


    public static final int PERMISSION_REQ_CODE = 99;

    public static final int REQUEST_WRITE_SETTINGS_CODE = 0x101010;

    public static final int REQUEST_DRAW_OVERLAY_CODE = 0x101011;

    public boolean mIsForceRequest = true ;

    private List<String> mPermissions =new ArrayList<String>();


    public ARTPHelper(boolean isForceRequest){
        mIsForceRequest = isForceRequest ;
    }

    private boolean isAllPermissionGranted(Context context,String[] permissions){

        for(String permission : permissions){
            if(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED){
                return false ;
            }
        }
        return true ;
    }


    public void requestPermissions(Activity activity){

        String[] permissions =(String[])(mPermissions.toArray(new String[mPermissions.size()]));
        if(isAllPermissionGranted(activity,permissions)){
            return ;
        }
        for(String permission : permissions){
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)){
                //TODO tell user what permission we should
                if(!mIsForceRequest){
                    return ;
                }
            }
        }
        ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQ_CODE);
    }


    @TargetApi(Build.VERSION_CODES.M)
    public void tryToRequestWriteSettings(Context context){
        if (!Settings.System.canWrite(context)) {
            Intent goToSettings = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            goToSettings.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(goToSettings);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    public void tryToDropZone(Context context) {
        if (!Settings.canDrawOverlays(context)) {
            Intent goToSettings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            goToSettings.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(goToSettings);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode) {
            case PERMISSION_REQ_CODE: {
                if (grantResults.length == permissions.length) {
                } else {
                   //TODO not all permission we get
                }
                return;
            }
        }
    }

    public ARTPHelper writeExternalStorage(){
        mPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return this;
    }

    public ARTPHelper readExternalStorage(){
        mPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        return this;
    }


    public ARTPHelper readCalendar(){
        mPermissions.add(Manifest.permission.READ_CALENDAR);
        return this;
    }

    public ARTPHelper writeCalendar(){
        mPermissions.add(Manifest.permission.WRITE_CALENDAR);
        return this;
    }

    public ARTPHelper useCamera(){
        mPermissions.add(Manifest.permission.CAMERA);
        return this;
    }

    public ARTPHelper readContacts(){
        mPermissions.add(Manifest.permission.READ_CONTACTS);
        return this;
    }

    public ARTPHelper writeContacts(){
        mPermissions.add(Manifest.permission.WRITE_CONTACTS);
        return this;
    }

    public ARTPHelper getContacts(){
        mPermissions.add(Manifest.permission.GET_ACCOUNTS);
        return this;
    }

    public ARTPHelper accessFineLocation(){
        mPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        return this;
    }

    public ARTPHelper accessCoarseLocation(){
        mPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        return this;
    }

    public ARTPHelper recordAudio(){
        mPermissions.add(Manifest.permission.RECORD_AUDIO);
        return this;
    }

    public ARTPHelper readPhoneState(){
        mPermissions.add(Manifest.permission.READ_PHONE_STATE);
        return this;
    }

    public ARTPHelper callPhone(){
        mPermissions.add(Manifest.permission.CALL_PHONE);
        return this;
    }

    public ARTPHelper readCallLog(){
        mPermissions.add(Manifest.permission.READ_CALL_LOG);
        return this;
    }

    public ARTPHelper writeCallLog(){
        mPermissions.add(Manifest.permission.WRITE_CALL_LOG);
        return this;
    }

    public ARTPHelper addVoiceMail(){
        mPermissions.add(Manifest.permission.ADD_VOICEMAIL);
        return this;
    }

    public ARTPHelper useSip(){
        mPermissions.add(Manifest.permission.USE_SIP);
        return this;
    }

    public ARTPHelper processOutgoingCalls(){
        mPermissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
        return this;
    }

    public ARTPHelper bodySensors(){
        mPermissions.add(Manifest.permission.BODY_SENSORS);
        return this;
    }

    public ARTPHelper sendSMS(){
        mPermissions.add(Manifest.permission.SEND_SMS);
        return this;
    }

    public ARTPHelper receiveSMS(){
        mPermissions.add(Manifest.permission.RECEIVE_SMS);
        return this;
    }

    public ARTPHelper readSMS(){
        mPermissions.add(Manifest.permission.READ_SMS);
        return this;
    }

    public ARTPHelper receiveWapPush(){
        mPermissions.add(Manifest.permission.RECEIVE_WAP_PUSH);
        return this;
    }

    public ARTPHelper receiveMSM(){
        mPermissions.add(Manifest.permission.RECEIVE_MMS);
        return this;
    }
}
