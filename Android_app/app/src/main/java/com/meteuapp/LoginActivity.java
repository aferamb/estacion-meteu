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

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import okhttp3.ResponseBody;
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
        Call<ResponseBody> jwtCall = api.loginJwt(user, pass);
        jwtCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progress.setVisibility(View.GONE);
                Log.d("LoginActivity", "JWT login response code=" + response.code());
                String raw = null;
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        raw = response.body().string();
                    } else if (response.errorBody() != null) {
                        raw = response.errorBody().string();
                    }
                } catch (Exception e) {
                    Log.d("LoginActivity", "Error reading raw body: " + e.getMessage());
                }

                if (raw != null) {
                    Log.d("LoginActivity", "JWT raw response: " + raw);
                    // try parse as JSON TokenResponse
                    try {
                        TokenResponse tr = new Gson().fromJson(raw, TokenResponse.class);
                        if (tr != null && tr.getToken() != null) {
                            session.setLoggedIn(user, true);
                            session.setJwtToken(tr.getToken());
                            if (tr.getRole() != null) session.setRole(tr.getRole());
                            Log.d("LoginActivity", "JWT parsed token, role=" + tr.getRole());
                            Toast.makeText(LoginActivity.this, "Login correcto (JWT)", Toast.LENGTH_SHORT).show();
                            // route by role: admin -> DashboardActivity, others -> UserDashboardActivity
                            if (tr.getRole() != null && tr.getRole().equalsIgnoreCase("admin")) {
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            } else {
                                startActivity(new Intent(LoginActivity.this, UserDashboardActivity.class));
                            }
                            finish();
                            return;
                        }
                    } catch (JsonSyntaxException jse) {
                        Log.d("LoginActivity", "JWT parse error: " + jse.getMessage());
                    }
                }

                Log.d("LoginActivity", "JWT login did not return token or was not JSON; aborting login (legacy disabled)");
                Toast.makeText(LoginActivity.this, "Login JWT falló: respuesta inesperada", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Log.e("LoginActivity", "JWT login failure: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Error conexión JWT: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Legacy login disabled. Kept method commented for possible future use.
    /*
    private void attemptLegacyLogin(String user, String pass) {
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<Void> legacyCall = api.login(user, pass);
        legacyCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    session.setLoggedIn(user, true);
                    // fetch role via /api/me and store it
                    try {
                        ServerApi a2 = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
                        a2.whoami().enqueue(new Callback<java.util.Map<String,Object>>() {
                            @Override
                            public void onResponse(Call<java.util.Map<String,Object>> call, Response<java.util.Map<String,Object>> response) {
                                if (response.isSuccessful() && response.body() != null && response.body().get("role") != null) {
                                    session.setRole(response.body().get("role").toString());
                                }
                            }

                            @Override
                            public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t) { }
                        });
                    } catch (Exception ignored) {}
                    Toast.makeText(LoginActivity.this, "Login correcto Legacy", Toast.LENGTH_SHORT).show();
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
    */
}
