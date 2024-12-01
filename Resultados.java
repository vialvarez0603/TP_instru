package com.example.finaldiciembre;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Resultados extends AppCompatActivity {
    private Button button;
    private Button exportButton;
    private static final int PERMISSION_REQUEST_CODE = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados);

        button = findViewById(R.id.button);
        exportButton = findViewById(R.id.exportButton);

        // UI Initialization
        final TextView textViewResultado = findViewById(R.id.textViewResultado);
        final TextView textViewFrecuencia = findViewById(R.id.textViewFrecuencia);
        final TextView textViewTiempo = findViewById(R.id.textViewTiempo);
        final TextView textViewEpisodios = findViewById(R.id.textViewEpisodios);

        // Obtain data from the previous activity
        Intent intent = getIntent();
        String diagnosticoBruxismo = intent.getStringExtra("DIAGNOSTICO");
        float frecuenciaEpisodios = intent.getFloatExtra("FRECUENCIA", 0.0f);
        String tiempoStr = intent.getStringExtra("DURACION");
        int episodios = intent.getIntExtra("EPISODIOS", 0);

        // Update UI with received data, checking for potential null values
        textViewResultado.setText(diagnosticoBruxismo != null ? diagnosticoBruxismo : "Diagnóstico no disponible");
        textViewFrecuencia.setText(String.format("Frecuencia de episodios: %.2f episodios/min", frecuenciaEpisodios));
        textViewTiempo.setText(String.format("Duración total: %.2f minutos", tiempoStr));
        textViewEpisodios.setText(String.format("Episodios detectados: %d", episodios));

        // Launch the next activity when button is clicked
        button.setOnClickListener(view -> {
            Intent intentVentana3 = new Intent(Resultados.this, Ventana3.class);
            intentVentana3.putExtra("DIAGNOSTICO_BRUXISMO", diagnosticoBruxismo);
            intentVentana3.putExtra("FRECUENCIA_EPISODIOS", frecuenciaEpisodios);
            intentVentana3.putExtra("DURACION", tiempoStr);
            intentVentana3.putExtra("EPISODIOS", episodios);
            startActivity(intentVentana3);
        });
    }

    // Handle permission checks (unchanged)
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}