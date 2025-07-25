package com.example.telescopemotionsender;

import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MotionSensorManager motionSensorManager;
    private UdpSender udpSender;
    private TextView statusTextView;
    private EditText ipAddressEditText;
    private EditText portEditText;
    private Button startButton;
    private Button stopButton;

    // New TextViews for displaying AZ and ALT
    private TextView azimuthTextView;
    private TextView altitudeTextView;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        statusTextView = findViewById(R.id.statusTextView);
        ipAddressEditText = findViewById(R.id.ipAddressEditText);
        portEditText = findViewById(R.id.portEditText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        // Initialize new TextViews
        azimuthTextView = findViewById(R.id.azimuthTextView);
        altitudeTextView = findViewById(R.id.altitudeTextView);

        // Set default values
        ipAddressEditText.setText("192.168.1.105"); // Replace with your desktop's IP
        portEditText.setText("5001"); // Match the port on your desktop script

        motionSensorManager = new MotionSensorManager(this) {
            @Override
            public void onSensorChanged(SensorEvent event) {
                super.onSensorChanged(event);
                // This method is called frequently on sensor updates.
                // Avoid heavy processing here.

                float[] orientation = getOrientationAngles(); // azimuth, pitch, roll in radians

                if (udpSender != null) {
                    double azimuthRad = orientation[0]; // Azimuth in radians (-pi to +pi)
                    double altitudeRad = orientation[1]; // Pitch in radians

                    // Convert azimuth from -pi to +pi range to 0 to 2*pi range
                    if (azimuthRad < 0) {
                        azimuthRad += 2 * Math.PI;
                    }

                    String dataToSend = String.format(Locale.US, "AZ:%.4f,ALT:%.4f", azimuthRad, altitudeRad);
                    udpSender.sendData(dataToSend);

                    // Update UI on the main thread
                    double finalAzimuthRad = azimuthRad;
                    double finalAzimuthRad1 = azimuthRad;
                    runOnUiThread(() -> {
                        azimuthTextView.setText(String.format(Locale.US, "Azimuth: %.4f rad (%.2f°)", finalAzimuthRad, Math.toDegrees(finalAzimuthRad1)));
                        altitudeTextView.setText(String.format(Locale.US, "Altitude: %.4f rad (%.2f°)", altitudeRad, Math.toDegrees(altitudeRad)));
                    });
                }
            }
        };

        startButton.setOnClickListener(v -> {
            String desktopIp = ipAddressEditText.getText().toString();
            int desktopPort;
            try {
                desktopPort = Integer.parseInt(portEditText.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid Port Number", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid port number: " + e.getMessage());
                return;
            }

            if (udpSender != null) {
                udpSender.close(); // Close any existing sender
            }

            udpSender = new UdpSender(desktopIp, desktopPort);
            motionSensorManager.startListening();
            statusTextView.setText("Sending data to " + desktopIp + ":" + desktopPort);
            Toast.makeText(MainActivity.this, "Started sending data", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Started sending data to " + desktopIp + ":" + desktopPort);
        });

        stopButton.setOnClickListener(v -> {
            motionSensorManager.stopListening();
            if (udpSender != null) {
                udpSender.close();
            }
            statusTextView.setText("Stopped.");
            // Clear the displayed values when stopped
            azimuthTextView.setText("Azimuth: 0.0000");
            altitudeTextView.setText("Altitude: 0.0000");
            Toast.makeText(MainActivity.this, "Stopped sending data", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Stopped sending data.");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // You can choose to automatically start listening here, or only via button click.
        // If auto-start: motionSensorManager.startListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        motionSensorManager.stopListening();
        if (udpSender != null) {
            udpSender.close();
        }
        Log.d(TAG, "Activity paused, stopped listening and closed UDP sender.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (udpSender != null) {
            udpSender.close();
        }
        Log.d(TAG, "Activity destroyed, closed UDP sender.");
    }
}
