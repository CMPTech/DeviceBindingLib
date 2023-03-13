package com.example.devicebindinglib

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.devicebindinglib.Models.*
import com.example.devicedetails.DeviceDetails
import com.google.android.gms.auth.api.phone.SmsRetriever
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


object MainActivity : AppCompatActivity() {
    private val totalTimerCount: Long = 45000
    private var countdownTimer: CountDownTimer? = null

    private val mySMSBroadcastReceiver = MySMSBroadcastReceiver()

    private val RECORD_REQUEST_CODE = 101

    var details: String? = null
    private val MY_REQUEST_CODE = 123

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askPermission()
        generateAppSignKey()
        registerSMSReceiver()
        requestSmsPermission()
        initEventListeners()

        val isSimSupport = isSimSupport(this)
        Log.d("Is sim Available -------",isSimSupport.toString())

        val isAirplaneModeOn = isAirplaneModeOn(this)
        Log.d("Device is in airplane mode ---------- ",isAirplaneModeOn.toString())

        val isOnline = isOnline(this)
        Log.d("Is device connected to wifi ---------- ",isOnline.toString())

        val versionInfo = getVersionInfo(this)
        Log.d("version info ---------- ",versionInfo.toString())

        fetchToken()
    }

    override fun onResume() {
        super.onResume()
        // put your code here...
//        askPermission()
    }

    fun askPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 23
            val permission1 =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            // Check for permissions
            if (permission1 != PackageManager.PERMISSION_GRANTED) {
                Log.d("", "Requesting Permissions")
                // Request permissions
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_PHONE_STATE
                    ),MY_REQUEST_CODE
                )
                return
            }
            Log.d("", "permission already granted")
        }
        getDevice()
    }

    fun getDevice() {
        details = DeviceDetails.getDeviceId(this)
        Log.d("Device Details - ", details!!)
        postDevice(details);
    }


    private fun generateAppSignKey() {
        val appSignatureHelper = AppSignatureHelper(applicationContext)
        val hashKey =  appSignatureHelper.appSignatures // This will give you the key.
        Log.d("hashKey----------->", "hashKey: $hashKey")
    }

    private fun registerSMSReceiver() {

        this.registerReceiver(
            mySMSBroadcastReceiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        )

        mySMSBroadcastReceiver.init(object : MySMSBroadcastReceiver.OTPReceiveListener {
            override fun onOTPReceived(otp: String) {
                Log.d("onOTPReceived ------------>", "onOTPReceived ---> $otp ")
                // OTP Received

            }

            override fun onOTPTimeOut() {
                Log.d("onOTPTimeOut----------->", "onOTPTimeOut")
            }
        })
    }

    private fun requestSmsPermission() {
        val permission = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.SEND_SMS)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i("requestSmsPermission", "Permission to record denied")
            makeRequest()
        }
        startSMSRetrieverClient()

    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.SEND_SMS),
            RECORD_REQUEST_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (mySMSBroadcastReceiver != null) applicationContext.unregisterReceiver(mySMSBroadcastReceiver)
        }catch (e : Exception){
            Log.e("onDestroy", "error --->  $e")
        }

    }

    private fun startSMSRetrieverClient() {
        val client = SmsRetriever.getClient(this)
        val task = client.startSmsRetriever()
        task.addOnSuccessListener { aVoid: Void? ->
            Log.d("startSMSRetrieverClient -------->", "success: $aVoid ")
        }
        task.addOnFailureListener { e: Exception? ->
            Log.d("startSMSRetrieverClient -------->", "failure: $e ")
        }
    }


    private fun fetchToken() {
        Log.d("fetchToken", "fetchToken: ")
        startTimer()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun initEventListeners() {
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
                Log.d("LifeCycle", "onActivityStopped: ")
                stopTimer()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }

        })
    }

    private fun startTimer(){
        Log.d("startTimer", "startTimer: ")
        countdownTimer = object : CountDownTimer(totalTimerCount, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d("onTick", "onTick in secs: " + millisUntilFinished/1000)
            }

            override fun onFinish() {
                Log.d("timer", "onFinish: ")
                stopTimer()
            }
        }.start()
    }

    fun stopTimer(){
        Log.d("stopTimer", "onFinish: ")
        countdownTimer?.cancel()
        countdownTimer = null
    }

    private fun getVersionInfo(mainActivity: MainActivity): Any {
        val manager = this.packageManager
        val info = manager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)


        Log.d("version", "VersionCode = "
                + info.versionCode + "\nVersionName = "
                + info.versionName)

        verify(info.versionCode, info.versionName)
        return  info;

    }

    private fun isSimSupport(context: Context): Boolean {
        val tm: TelephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager //gets the current TelephonyManager
        return !(tm.getSimState() === TelephonyManager.SIM_STATE_ABSENT)
    }


    fun isAirplaneModeOn(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.AIRPLANE_MODE_ON, 0
            ) != 0
        } else {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        }
    }

    @SuppressLint("MissingPermission")
    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    fun postDevice(device: String?) {
        val methods: Methods = RetrofitClient.getRetrofitInstance().create(Methods::class.java)
        val modal = Model(device)
        val call: Call<Model> = methods.getUserData(modal)
        call.enqueue(object : Callback<Model> {
            override fun onResponse(call: Call<Model>, response: Response<Model>) {
                if (response.isSuccessful()) {
                    Log.d(
                        "",
                        "Response while sending Device ID" + (response.body()?.getResponseData())
                    )
                    val obj = JSONObject(response.body()?.getResponseData() as Map<*, *>)
                    try {
                        val apiRes = JSONObject(obj.getString("response"))
                        val token = apiRes.getString("token")
                        //                        Log.d("----------","Token "+ token);
                        val sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
                        val myEdit = sharedPreferences.edit()
                        myEdit.putString("DeviceID", device)
                        myEdit.putString("Token", token)
                        myEdit.apply()
                        postVerify()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onFailure(call: Call<Model>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error Occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun postVerify() {
        val methods: Methods = RetrofitClient.getRetrofitInstance().create(Methods::class.java)
        val sh = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        val s1 = sh.getString("DeviceID", "")
        val s2 = sh.getString("Token", "")
        val modal = DataModel(s1, s2)
        val call: Call<DataModel> = methods.verifyDevice(modal)
        call.enqueue(object : Callback<DataModel> {
            override fun onResponse(call: Call<DataModel>, response: Response<DataModel>) {
                if (response.isSuccessful()) {
                    Toast.makeText(this@MainActivity, "Data updated to API", Toast.LENGTH_SHORT)
                        .show()
                    val responseFromAPI: DataModel? = response.body()
                    Log.d(
                        "",
                        "Response after successful match of ID and token" + (response.body()
                            ?.getResponseData())
                    )
                }
            }

            override fun onFailure(call: Call<DataModel>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error Occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun verify(versionCode: Int, versionName: String) {
        val methods = RetrofitClient.getRetrofitInstance().create(Methods::class.java)
        val modal = Version(versionCode, versionName)
        val call: Call<Version> = methods.verifyVersion(modal)
        call.enqueue(object : Callback<Version> {
            override fun onResponse(call: Call<Version>, response: Response<Version>) {
                if (response.isSuccessful()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Verification done successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(
                        "",
                        "Response after successfull version match" + (response.body()
                            ?.getResponseData())
                    )
                }
            }

            override fun onFailure(call: Call<Version>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error Occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

}