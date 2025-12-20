package com.meteuapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StationSelectionActivity extends AppBaseActivity {

    private Spinner spSaved;
    private Button btnOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_selection);
        spSaved = findViewById(R.id.sp_saved_topics_station);
        btnOpen = findViewById(R.id.btn_open_station);

        loadSavedTopics();

        btnOpen.setOnClickListener(v -> {
            Object sel = spSaved.getSelectedItem();
            if (sel == null) {
                Toast.makeText(this, "Selecciona una suscripción", Toast.LENGTH_SHORT).show();
                return;
            }
            String topic = sel.toString();
            Intent it = new Intent(this, StationLiveActivity.class);
            it.putExtra("topic", topic);
            startActivity(it);
        });
    }

    private void loadSavedTopics() {
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<String>> call = api.getSubscriptions();
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                runOnUiThread(() -> spSaved.setAdapter(new ArrayAdapter<>(StationSelectionActivity.this, android.R.layout.simple_spinner_dropdown_item, response.body())));
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Log.w("meteu", "Could not load saved topics: " + t.getMessage());
            }
        });
    }
}
