package com.meteuapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meteuapp.models.TokenResponse;

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

        // attempt JWT login first, then fall back to cookie-based login on failure
        attemptJwtLogin(user, pass);
    }

    private void attemptJwtLogin(String user, String pass) {
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<TokenResponse> jwtCall = api.loginJwt(user, pass);
        jwtCall.enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getToken() != null) {
                    String token = response.body().getToken();
                    session.setLoggedIn(user, true);
                    session.setJwtToken(token);
                    Toast.makeText(LoginActivity.this, "Login correcto (JWT)", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    attemptLegacyLogin(user, pass);
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                // network error trying JWT; fall back to legacy login
                attemptLegacyLogin(user, pass);
            }
        });
    }

    private void attemptLegacyLogin(String user, String pass) {
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<Void> legacyCall = api.login(user, pass);
        legacyCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    session.setLoggedIn(user, true);
                    Toast.makeText(LoginActivity.this, "Login correcto", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                } else {
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
