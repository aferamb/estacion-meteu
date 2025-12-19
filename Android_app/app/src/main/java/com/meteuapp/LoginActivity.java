package com.meteuapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity handles user login using the server's /login endpoint.
 * It performs simple validation of inputs and stores a logged-in flag in SessionManager.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPass;
    private ProgressBar progress;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);
        // If already logged in, skip login screen
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        etUser = findViewById(R.id.et_username);
        etPass = findViewById(R.id.et_password);
        Button btn = findViewById(R.id.btn_login);
        progress = findViewById(R.id.progress);

        btn.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String user = etUser.getText().toString().trim();
        String pass = etPass.getText().toString();

        // Basic validation
        if (user.isEmpty()) { etUser.setError("Usuario requerido"); etUser.requestFocus(); return; }
        if (pass.isEmpty()) { etPass.setError("Contraseña requerida"); etPass.requestFocus(); return; }

        progress.setVisibility(View.VISIBLE);

        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<Void> call = api.login(user, pass);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    // Login success: server uses session cookie - CookieJar stores it
                    session.setLoggedIn(user, true);
                    Toast.makeText(LoginActivity.this, "Login correcto", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    // Some servers redirect on success; if not successful treat as auth failure
                    Toast.makeText(LoginActivity.this, "Error de autenticación", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
