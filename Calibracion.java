package com.example.finaldiciembre;


import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Calibracion extends AppCompatActivity {
    public static Handler handler;
    public static ConnectedThread connectedThread;
    private final static int MESSAGE_READ = 2;
    private List<Float> emgData = new ArrayList<>();
    private float maxEmgValue = 0;
    private float umbral;
    private TextView textViewUmbral;
    private Button buttonComenzar, buttonNext;
    private boolean isCalibrating = false;
    private long calibrationStartTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibracion);

        buttonComenzar = findViewById(R.id.buttonComenzar);
        buttonNext = findViewById(R.id.buttonNext);
        buttonNext.setVisibility(View.GONE);
        textViewUmbral = findViewById(R.id.textViewUmbral);
        textViewUmbral.setVisibility(View.GONE);

        BluetoothSocket bluetoothSocket = Ventana7.mmSocket;

        if (bluetoothSocket != null) {
            connectedThread = new ConnectedThread(bluetoothSocket);
            connectedThread.start();
        }

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ && isCalibrating) {
                    String arduinoMsg = (String) msg.obj;
                    processArduinoMessage(arduinoMsg);
                }
            }
        };

        buttonComenzar.setOnClickListener(view -> startCalibration());
        buttonNext.setOnClickListener(view -> goToMonitoring());
    }

    private void startCalibration() {
        isCalibrating = true;
        calibrationStartTime = System.currentTimeMillis();
        emgData.clear();
        maxEmgValue = 0;
    }

    // Este método ahora solo procesa los datos numéricos directamente
    private void processArduinoMessage(String message) {
        if (isCalibrating) {
            long elapsed = System.currentTimeMillis() - calibrationStartTime;

            if (elapsed <= 10000) { // 10 segundos
                // Se asume que los valores llegan como una secuencia de números separados por comas
                String[] emgValues = message.split(","); // Se asume que el Arduino envía valores separados por comas
                for (String value : emgValues) {
                    try {
                        float emgValue = Float.parseFloat(value); // Convierte a float
                        emgData.add(emgValue);
                        Log.d("Calibracion", "Valor EMG recibido: " + emgValue);

                        if (emgValue > maxEmgValue) {
                            maxEmgValue = emgValue;
                        }
                    } catch (NumberFormatException e) {
                        Log.e("Calibracion", "Error al convertir el valor EMG: " + value);
                    }
                }
            } else {
                isCalibrating = false;
                umbral = maxEmgValue * 0.4f;
                textViewUmbral.setVisibility(View.VISIBLE);
                textViewUmbral.setText("Umbral calculado: " + umbral);
                buttonNext.setVisibility(View.VISIBLE);

                Log.d("Calibracion", "Calibración finalizada. Valor máximo EMG: " + maxEmgValue);
            }
        }
    }

    private void goToMonitoring() {
        Intent intent = new Intent(Calibracion.this, Monitoreo.class);
        intent.putExtra("UMBRAL", umbral);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }


    public static class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Ventana8", "Error occurred when creating streams", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        @Override
        public void run() {
            byte[] buffer = new byte[1024];  // Buffer para almacenar los datos
            int bytes;

            while (true) {
                try {
                    // Leer datos del inputStream
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Pasar los datos al Handler para su procesamiento
                    Message readMsg = handler.obtainMessage(MESSAGE_READ, readMessage);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.e("ConnectedThread", "Error al leer los datos", e);
                    break;
                }
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Ventana8", "Could not close the client socket", e);
            }
        }
    }
}