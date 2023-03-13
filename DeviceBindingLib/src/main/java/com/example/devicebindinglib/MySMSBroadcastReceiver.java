package com.example.devicebindinglib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * BroadcastReceiver to wait for SMS messages. This can be registered either
 * in the AndroidManifest or at runtime.  Should filter Intents on
 * SmsRetriever.SMS_RETRIEVED_ACTION.
 */
public class MySMSBroadcastReceiver extends BroadcastReceiver {

    private OTPReceiveListener otpReceiveListener;

    public MySMSBroadcastReceiver() {
    }

    public void init(OTPReceiveListener otpReceiveListener) {
        this.otpReceiveListener = otpReceiveListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
                if (status != null)
                    switch (status.getStatusCode()) {
                        case CommonStatusCodes.SUCCESS:
                            // Get SMS message contents
                            String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                            Log.d("onReceive message----------->", "onReceive :  " + message);
                            if (message != null) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    Optional<String> max = Arrays.stream(message
                                                    .split(" "))
                                            .max((a, b) -> a.length() - b.length());
                                    Log.d("max", max.get());
                                    Log.d("max", String.valueOf(max.get().length()));
                                    String val = max.get();
                                    if(val.length() >= 35){
                                        Pattern pattern = Pattern.compile("^[-@.\\/#&+\\w\\s]*$");
                                        Matcher matcher = pattern.matcher(val);
                                        if(matcher.matches()) {
                                            // yay! alphanumeric!
                                            if (this.otpReceiveListener != null)
                                                this.otpReceiveListener.onOTPReceived(val);
                                        }
                                        else {
                                            Toast.makeText(context, "Not Alphanumeric", Toast.LENGTH_LONG).show();
                                        }
                                    }else{
                                        Toast.makeText(context, "Token length is less than 35 chars", Toast.LENGTH_LONG).show();
                                    }
                                }



                               /* Pattern pattern = Pattern.compile("^[-@.\\/#&+\\w\\s]*$");

                                Matcher matcher = pattern.matcher(message);
                                String val = "";
                                if (matcher.find()) {
                                    val = matcher.group(0);  // 4 digit number
                                    if (this.otpReceiveListener != null)
                                        this.otpReceiveListener.onOTPReceived(val);
                                } else {
                                    if (this.otpReceiveListener != null)
                                        this.otpReceiveListener.onOTPReceived(null);
                                }*/
                            }
                            break;
                        case CommonStatusCodes.TIMEOUT:
                            if (this.otpReceiveListener != null)
                                this.otpReceiveListener.onOTPTimeOut();
                            break;
                    }
            }
        }
    }

    interface OTPReceiveListener {
        void onOTPReceived(String otp);

        void onOTPTimeOut();
    }
}
