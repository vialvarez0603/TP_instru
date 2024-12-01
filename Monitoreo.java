package com.example.finaldiciembre;


import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Monitoreo extends AppCompatActivity {

    private static final int MESSAGE_READ = 2;
    public static Handler handler;
    private ConnectedThread connectedThread;
    private TextView textViewStatus, textViewValue, textViewUmbral, textViewEpisodios, textViewDiagnostico;
    private Button buttonStop;
    private float umbral;
    private boolean isExtendedWindow = false;
    private boolean episodeInProgress = false;  // Indica si hay un episodio activo
    private ArrayList<Float> emgData = new ArrayList<>();
    private boolean isMonitoring = false;
    private boolean umbralSuperado = false;

    private int episodeCount = 0;
    private static final long EPISODE_THRESHOLD_TIME = 600000;  // 10 minutos en milisegundos

    private long lastReadTime = 0;  // Controla el tiempo de lectura
    private static final long WINDOW_DURATION = 5000;  // 5 segundos en milisegundos
    private long monitoringStartTime;  // Store the start time for total duration calculation
    private Button buttonSiguiente;
    private ArrayList<ArrayList<Float>> episodeWindows = new ArrayList<>();
    private ArrayList<ArrayList<Float>> emgEpisodes = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoreo);

        textViewStatus = findViewById(R.id.textViewStatus);
        textViewValue = findViewById(R.id.textViewTiempoRestante);
        textViewUmbral = findViewById(R.id.textViewUmbral);
        textViewEpisodios = findViewById(R.id.textViewEpisodios);
        textViewDiagnostico = findViewById(R.id.textViewDiagnostico);
        buttonStop = findViewById(R.id.buttonFinalizar);
        buttonSiguiente = findViewById(R.id.buttonSiguiente);
        monitoringStartTime = System.currentTimeMillis();
        buttonSiguiente.setOnClickListener(view -> openResultadosActivity());

        // Obtener el umbral de la actividad anterior
        umbral = getIntent().getFloatExtra("UMBRAL", 0);
        textViewUmbral.setText("Umbral: " + umbral);

        // Configurar la conexión Bluetooth
        BluetoothSocket bluetoothSocket = Ventana7.mmSocket;
        if (bluetoothSocket != null) {
            connectedThread = new ConnectedThread(bluetoothSocket);
            connectedThread.start();
            Log.d("Monitoreo", "Conexión Bluetooth iniciada.");
        }

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String arduinoMsg = (String) msg.obj;
                    processArduinoMessage(arduinoMsg);
                }
            }
        };

        buttonStop.setOnClickListener(view -> stopMonitoring());
        startMonitoring();
    }

    private void startMonitoring() {
        isMonitoring = true;
        textViewStatus.setText("Monitoreando...");
        Log.d("Monitoreo", "Inicio del monitoreo.");
        emgData.clear();
        lastReadTime = System.currentTimeMillis();  // Establece el tiempo de inicio
    }

    private void processArduinoMessage(String message) {
        if (isMonitoring) {
            Log.d("Monitoreo", "Mensaje recibido: " + message);

            String[] emgValues = message.split(",");
            for (String value : emgValues) {
                try {
                    float emgValue = Float.parseFloat(value);
                    textViewValue.setText("Valor EMG: " + emgValue);
                    emgData.add(emgValue);  // Acumula los valores EMG

                    if (emgValue > umbral) {
                        umbralSuperado = true;
                        Log.e("Monitoreo", "Se supero el umbral: " + value);
                    }
                } catch (NumberFormatException e) {
                    Log.e("Monitoreo", "Error al convertir valor EMG: " + value);
                }
            }

            long currentTime = System.currentTimeMillis();
            // Si han pasado 5 segundos desde la última lectura
            if (currentTime - lastReadTime >= WINDOW_DURATION) {
                lastReadTime = currentTime;
                Log.d("Monitoreo", "5 segundos de datos recopilados.");

                if (umbralSuperado) {
                    emgEpisodes.add(new ArrayList<>(emgData));  // Guarda la ventana actual en el vector de vectores
                    Log.d("Monitoreo", "Ventana guardada con " + emgData.size() + " valores.");
                    emgData.clear();
                    // Si el umbral se supera, no pausamos y seguimos procesando datos
                    if (!isExtendedWindow) {
                        isExtendedWindow = true;
                        episodeInProgress = true;
                        Log.d("Monitoreo", "¡Umbral superado! Ventana extendida.");
                    }
                    //analyzeWindow();
                    // Aquí no hacemos pausa, continuamos monitoreando
                    Log.d("Monitoreo", "¡Umbral superado (no primera vez)! Ventana extendida.");
                    umbralSuperado = false;
                } else {
                    // Si no se supera el umbral, se resetean los datos y se hace la pausa
                    if (isExtendedWindow) {
                        isExtendedWindow = false;
                        if (episodeInProgress) {
                            episodeCount++;  // Cuenta el episodio finalizado
                            textViewEpisodios.setText("Episodios: " + episodeCount);
                            episodeInProgress = false;
                            Log.d("Monitoreo", "Episodio finalizado. Iniciando descanso.");
                        }
                    }

                    // Realizamos un descanso de 5 segundos antes de continuar
                    Log.d("Monitoreo", "Recuento de episodios" + episodeCount);
                    Log.d("Monitoreo", "No se superó el umbral, tomando descanso...");
                    textViewStatus.setText("Descanso de 5 segundos...");
                    emgData.clear();  // Limpiar los datos de EMG para evitar acumulación

                    currentTime = System.currentTimeMillis();
                    // Si han pasado 5 segundos desde la última lectura
                    while (currentTime - lastReadTime <= WINDOW_DURATION) {
                        currentTime = System.currentTimeMillis();
                        Log.d("Monitoreo", "Descanso de 5 segs");
                    }
                    // Reiniciar monitoreo después de descanso
                    textViewStatus.setText("Monitoreando...");
                    Log.d("Monitoreo", "Descanso finalizado. Reanudando monitoreo.");
                    lastReadTime = System.currentTimeMillis();
                    isMonitoring= true;
                }
            }
        }
    }

    private void analyzeWindow() {
        Log.d("Monitoreo", "Procesando ventana de datos EMG...");
        // Aquí puedes agregar procesamiento adicional si es necesario
        updateDiagnosis();
    }

    private void stopMonitoring() {
        isMonitoring = false;
        updateDiagnosis();
        textViewStatus.setText("Monitoreo detenido.");
        Log.d("Monitoreo", "Monitoreo detenido.");
    }

    private void updateDiagnosis() {
        Log.d("Monitoreo", "Calculando paramentros.");

        // Calcular la duración en milisegundos y convertir a segundos
        long durationMillis = (System.currentTimeMillis() - monitoringStartTime)/1000;
        // Calcular tiempo total en horas para la frecuencia (1/h)
        float tiempoTotalHoras = durationMillis / 3600f;

        // Calcular frecuencia (episodios por hora)
        float frequency = episodeCount / tiempoTotalHoras;

        // Calcular horas, minutos y segundos
        int horas = (int) (durationMillis / 3600);  // Total de horas
        int minutos = (int) ((durationMillis % 3600) / 60);  // Minutos restantes
        int segundos = (int) (durationMillis % 60);  // Segundos restantes

        // Formatear la cadena de tiempo en formato hh:mm:ss
        String tiempoStr = String.format("%d:%02d:%02d", horas, minutos, segundos);


        String diagnosis;
        if (frequency > 10.17) {
            diagnosis = "Se encontraron indicios de bruxismo";
        } else {
            diagnosis = "No se encontraron indicios de bruxismo";
        }
        textViewDiagnostico.setText("Diagnóstico: " + diagnosis);
        Log.d("Monitoreo", "Frecuencia: " + frequency + ", Diagnóstico: " + diagnosis);
    }
    private void openResultadosActivity() {
        // Calcular la duración en milisegundos y convertir a segundos
        long durationMillis = (System.currentTimeMillis() - monitoringStartTime)/1000;
        // Calcular tiempo total en horas para la frecuencia (1/h)
        float tiempoTotalHoras = durationMillis / 3600f;

        // Calcular frecuencia (episodios por hora)
        float frequency = episodeCount / tiempoTotalHoras;

        // Calcular horas, minutos y segundos
        int horas = (int) (durationMillis / 3600);  // Total de horas
        int minutos = (int) ((durationMillis % 3600) / 60);  // Minutos restantes
        int segundos = (int) (durationMillis % 60);  // Segundos restantes

        // Formatear la cadena de tiempo en formato hh:mm:ss
        String tiempoStr = String.format("%d:%02d:%02d", horas, minutos, segundos);

        // Create an intent to navigate to the Resultados activity
        Intent intent = new Intent(Monitoreo.this, Resultados.class);
        intent.putExtra("EPISODIOS", episodeCount);
        intent.putExtra("FRECUENCIA", frequency);
        intent.putExtra("DIAGNOSTICO", textViewDiagnostico.getText().toString());
        intent.putExtra("DURACION", tiempoStr);
        intent.putExtra("EPISODES_DATA", emgEpisodes);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedThread != null) connectedThread.cancel();
    }

    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("Monitoreo", "Error al crear los streams de entrada", e);
            }
            mmInStream = tmpIn;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
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
                Log.e("Monitoreo", "Error al cerrar el socket", e);
            }
        }
    }
}