package com.example.telescopemotionsender;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class MotionSensorManager implements SensorEventListener {
    private static final String TAG = "MotionSensorManager";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magneticField;
    private Sensor gyroscope; // Although not directly used in getRotationMatrix, it's good practice to get it.

    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3]; // azimuth, pitch, roll

    public MotionSensorManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE); // For future use or more advanced fusion

            if (accelerometer == null) {
                Log.w(TAG, "Accelerometer not available.");
            }
            if (magneticField == null) {
                Log.w(TAG, "Magnetic field sensor not available.");
            }
            if (gyroscope == null) {
                Log.i(TAG, "Gyroscope not available. Orientation will rely on accelerometer and magnetic field only.");
            }
        } else {
            Log.e(TAG, "SensorManager not available.");
        }
    }

    public void startListening() {
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (magneticField != null) {
                sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
            }
            // If you want to use gyroscope for more advanced fusion, register it here
            // if (gyroscope != null) {
            //     sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
            // }
            Log.d(TAG, "Started listening to sensors.");
        }
    }

    public void stopListening() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "Stopped listening to sensors.");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.8f; // Factor for low-pass filter

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Apply low-pass filter to gravity data
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // Apply low-pass filter to geomagnetic data
            geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0];
            geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1];
            geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2];
        }

        if (gravity != null && geomagnetic != null) {
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles);
                // orientationAngles[0] = azimuth (yaw) in radians
                // orientationAngles[1] = pitch in radians
                // orientationAngles[2] = roll in radians
                // Log.d(TAG, String.format("Orientation: Azimuth=%.2f, Pitch=%.2f, Roll=%.2f",
                //         Math.toDegrees(orientationAngles[0]), Math.toDegrees(orientationAngles[1]), Math.toDegrees(orientationAngles[2])));
            } else {
                Log.w(TAG, "Failed to get rotation matrix.");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
        Log.d(TAG, "Sensor accuracy changed for " + sensor.getName() + ": " + accuracy);
    }

    public float[] getOrientationAngles() {
        return orientationAngles;
    }
}

