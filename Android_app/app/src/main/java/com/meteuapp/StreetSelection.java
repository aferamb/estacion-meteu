package com.meteuapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreetSelection extends AppBaseActivity {
    Spinner spinner;
    Button boton;
    List<Street> listaCalles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_street_selection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        spinner = findViewById(R.id.spinner);
        boton = findViewById(R.id.button);
        obtenerDatosDelServidor();
    }

    private void obtenerDatosDelServidor() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        Call<List<Street>> call = apiService.getItems();
        call.enqueue(new Callback<List<Street>>() {
            @Override
            public void onResponse(Call<List<Street>> call, Response<List<Street>> response) {
                if (response.isSuccessful()) {
                    List<Street> lista = response.body();
                    mostrarLista(lista);
                } else {
                    Log.e("meteu","Error del servidor");
                }
            }

            @Override
            public void onFailure(Call<List<Street>> call, Throwable t) {
                Log.e("meteu","Error: " + t.getMessage());
            }
        });
    }

    private void mostrarLista(List<Street> lista) {
        StringBuilder builder = new StringBuilder();
        listaCalles = lista;

        for (Street item : lista) {
            builder.append(item.getId())
                    .append(" - ")
                    .append(item.getNombre())
                    .append("\n");
        }

        Log.i("meteu",builder.toString());

        ArrayList<String> nombres = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            nombres.add(lista.get(i).getNombre());
        }
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("meteu", "Boton pulsado: "+ spinner.getSelectedItem());
                Intent intent = new Intent(StreetSelection.this, StreetMonitoring.class);
                intent.putExtra("street_id", listaCalles.get((int) spinner.getSelectedItemId()).getId());
                intent.putExtra("street_name", listaCalles.get((int) spinner.getSelectedItemId()).getNombre());
                startActivity(intent);
                finish();
            }
        });
    }
}
