package cz.raynet.raynetcrm.calllog;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;


public class CallLogPlugin extends CordovaPlugin {

    private final String TAG = "CallLogPlugin";

    private static boolean inBackground = true;
    private static boolean logEnabled = false;
    private static ArrayList<Bundle> notificationStack = null;
    private static CallbackContext notificationCallbackContext;

    @Override
    protected void pluginInitialize() {
        final Bundle extras = this.cordova.getActivity().getIntent().getExtras();
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Log.d(TAG, "Starting call log plugin");
                if (extras != null && extras.size() > 1) {
                    if (CallLogPlugin.notificationStack == null) {
                        CallLogPlugin.notificationStack = new ArrayList<Bundle>();
                    }
                    if (extras.containsKey("calllog.phoneNumber")) {
                        notificationStack.add(extras);
                    }
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("enableLogging")) {
            this.enableLogging(callbackContext);
            return true;
        } else if (action.equals("disableLogging")) {
            this.disableLogging(callbackContext);
            return true;
        } else if (action.equals("onLogCall")) {
            this.onLogCall(callbackContext);
            return true;
        }

        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
        CallLogPlugin.inBackground = true;
    }

    @Override
    public void onResume(boolean multitasking) {
        CallLogPlugin.inBackground = false;
    }

    @Override
    public void onReset() {
        CallLogPlugin.notificationCallbackContext = null;
    }

    private void enableLogging(final CallbackContext callbackContext) {
        CallLogPlugin.logEnabled = true;
    }

    private void disableLogging(final CallbackContext callbackContext) {
        CallLogPlugin.logEnabled = false;
        if (CallLogPlugin.notificationStack != null) {
            CallLogPlugin.notificationStack.clear();
        }
    }

    private void onLogCall(final CallbackContext callbackContext) {
        CallLogPlugin.notificationCallbackContext = callbackContext;
        if (CallLogPlugin.notificationStack != null) {
            for (Bundle bundle : CallLogPlugin.notificationStack) {
                CallLogPlugin.sendNotification(bundle);
            }
            CallLogPlugin.notificationStack.clear();
        }
    }

    public static void sendNotification(Bundle bundle) {
        if (!CallLogPlugin.hasNotificationsCallback()) {
            if (CallLogPlugin.notificationStack == null) {
                CallLogPlugin.notificationStack = new ArrayList<Bundle>();
            }
            notificationStack.add(bundle);
            return;
        }
        final CallbackContext callbackContext = CallLogPlugin.notificationCallbackContext;
        if (callbackContext != null && bundle != null) {
            JSONObject json = new JSONObject();
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                try {
                    json.put(key, bundle.get(key));
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                    return;
                }
            }

            PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, json);
            pluginresult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginresult);
        }
    }

    public static boolean inBackground() {
        return CallLogPlugin.inBackground;
    }

    public static boolean logEnabled() {
        return CallLogPlugin.logEnabled;
    }

    public static boolean hasNotificationsCallback() {
        return CallLogPlugin.notificationCallbackContext != null;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final Bundle data = intent.getExtras();
        if (data != null && data.containsKey("calllog.phoneNumber")) {
            CallLogPlugin.sendNotification(data);
        }
    }
}
