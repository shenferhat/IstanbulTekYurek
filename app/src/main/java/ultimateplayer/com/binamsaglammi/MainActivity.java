package ultimateplayer.com.binamsaglammi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Debug;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import android.provider.Settings.Secure;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttCallback;

import android.Manifest;


import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Timer;


public class MainActivity extends AppCompatActivity implements SensorEventListener  {
    private String android_id ;
    TextView dataReceived;
    //MqttAndroidClient mqttAndroidClient;
    MqttAndroidClient mqttAndroidClient;
 //   String userName = "rwswearr";
 //   String password = "xo2KCHg2uKxb";
    String serverUri = "tcp://test.mosquitto.org";
    private String clientId = "testcihaz";
    private SensorManager sensorManager;
    private long lastUpdate;
    MqttConnectOptions mqttConnectOptions;
    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
    boolean workonce = false;
    boolean wasCalled = false;
    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dataReceived = (TextView) findViewById(R.id.dataReceived);
        dataReceived.setMovementMethod(new ScrollingMovementMethod());

        android_id =  Secure.getString(getBaseContext().getContentResolver(),
                Secure.ANDROID_ID);

        if(!wasCalled){
            mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, android_id);
            mqttAndroidClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        Log.d("MQTT", "Tekrar baglandi: " + serverURI);
                    } else {
                        Log.d("MQTT", "Bağlandı: " + serverURI);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "The Connection was lost.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {}

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            mqttConnectOptions= new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);
     //       mqttConnectOptions.setUserName(userName);
        //    mqttConnectOptions.setPassword(password.toCharArray());
            try {
                mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        try {
                            asyncActionToken.getSessionPresent();
                        } catch (Exception e) {
                            String message = e.getMessage();
                            Log.d("MQTT", "error message is null " + String.valueOf(message == null));
                        }
                        Log.d("MQTT", "connected to: " + serverUri);
                        Toast.makeText(MainActivity.this, "Bağlandı", Toast.LENGTH_SHORT).show();

                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        pub("Bağlandım:"+clientId,"topic");
                        wasCalled = true;
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d("MQTT", "Failed to connect to: " + serverUri + exception.toString());
                    }
                });
            } catch (MqttException ex) {
                ex.printStackTrace();
            }
            wasCalled = true;
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    private void getAccelerometer(SensorEvent event) {

        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        float accelationSquareRoot = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        long actualTime = event.timestamp;
        String oldData = dataReceived.getText().toString();
        String data = "X:" +Float.toString(x) + "Y:" +Float.toString(y) + "Z:" + Float.toString(z).concat("\n");
        dataReceived.setText(data);


       // pub(data,"topic");

        if (accelationSquareRoot >= 2) //
        {
            if (actualTime - lastUpdate < 200) {
                return;
            }
            lastUpdate = actualTime;
          //  pub("@","sallanancihaz");
           Location location = getLocationWithCheckNetworkAndGPS(this.getBaseContext());
            String json = "{\"lat\": "+String.format("%.6f", location.getLatitude())+ ",\"lon\":"+String.format("%.6f", location.getLongitude())  +",\"tst\": 1569988874}" ;
           // pub("@"+android_id +"@"+location.getLongitude()+"@"+location.getAltitude(),"sallanancihaz");
            pub(json,"isttest/devices");
           Toast.makeText(this, "Sarsıntı Tespit Edildi \nBoylam :"+location.getLongitude() + " Enlem:"+location.getLatitude() , Toast.LENGTH_SHORT).show();



            //Toast.makeText(this, "Cihaz sallandı", Toast.LENGTH_SHORT).show();



        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }



    public void pub(String message,String topic) {
         if(mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.publish(topic,new MqttMessage(message.getBytes()));
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    public static Location getLocationWithCheckNetworkAndGPS(Context mContext) {
        LocationManager lm = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        assert lm != null;
        boolean isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkLocationEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Location networkLoacation = null, gpsLocation = null, finalLoc = null;
        if (isGpsEnabled)
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return null;
            }gpsLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (isNetworkLocationEnabled)
            networkLoacation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (gpsLocation != null && networkLoacation != null) {

            //smaller the number more accurate result will
            if (gpsLocation.getAccuracy() > networkLoacation.getAccuracy())
                return finalLoc = networkLoacation;
            else
                return finalLoc = gpsLocation;

        } else {

            if (gpsLocation != null) {
                return finalLoc = gpsLocation;
            } else if (networkLoacation != null) {
                return finalLoc = networkLoacation;
            }
        }
        return finalLoc;
    }
}

