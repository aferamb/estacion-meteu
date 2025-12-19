package com.meteuapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsersActivity extends AppBaseActivity {

    private androidx.recyclerview.widget.RecyclerView rv;
    private ProgressBar progress;
    private com.meteuapp.adapters.SimpleStringAdapter adapter;
    private List<Map<String,Object>> users = new ArrayList<>();
    private int selectedIndex = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        rv = findViewById(R.id.rv_users);
        progress = findViewById(R.id.progress);
        Button btnAdd = findViewById(R.id.btn_add_user);
        Button btnDelete = findViewById(R.id.btn_delete_user);

        adapter = new com.meteuapp.adapters.SimpleStringAdapter();
        adapter.setOnItemLongClick((pos, value) -> confirmDelete(pos));
        adapter.setOnItemClick((pos, value) -> { selectedIndex = pos; btnDelete.setEnabled(true); });
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rv.setAdapter(adapter);
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = findViewById(R.id.swipe);
        swipe.setOnRefreshListener(this::fetchUsers);

        btnAdd.setOnClickListener(v -> showCreateDialog());
        btnDelete.setOnClickListener(v -> {
            if (selectedIndex >= 0 && selectedIndex < users.size()) confirmDelete(selectedIndex);
            else Toast.makeText(UsersActivity.this, "Selecciona un usuario", Toast.LENGTH_SHORT).show();
        });

        btnDelete.setEnabled(false);

        fetchUsers();
    }

    private void fetchUsers() {
        progress.setVisibility(View.VISIBLE);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<Map<String,Object>>> call = api.getUsers();
        call.enqueue(new Callback<List<Map<String,Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String,Object>>> call, Response<List<Map<String,Object>>> response) {
                progress.setVisibility(View.GONE);
                androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe = findViewById(R.id.swipe);
                if (swipe != null) swipe.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    users.clear(); users.addAll(response.body());
                    refreshList();
                } else {
                    Toast.makeText(UsersActivity.this, "No autorizado o error cargando usuarios", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String,Object>>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(UsersActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void refreshList() {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (Map<String,Object> u : users) {
            String name = u.get("username") == null ? "" : u.get("username").toString();
            String role = u.get("role") == null ? "" : u.get("role").toString();
            list.add(name + " (" + role + ")");
        }
        adapter.setItems(list);
    }

    private void showCreateDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_create_user, null);
        EditText etUser = v.findViewById(R.id.et_username);
        EditText etPass = v.findViewById(R.id.et_password);
        Spinner spRole = v.findViewById(R.id.sp_role);
        // populate role spinner
        android.widget.ArrayAdapter<String> roleAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"user","admin"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(roleAdapter);

        b.setTitle("Crear usuario");
        b.setView(v);
        b.setPositiveButton("Crear", (d, w) -> {
            String u = etUser.getText().toString().trim();
            String p = etPass.getText().toString();
            String r = spRole.getSelectedItem() == null ? "user" : spRole.getSelectedItem().toString();
            if (u.isEmpty() || p.isEmpty()) { Toast.makeText(UsersActivity.this, "Usuario y contraseña requeridos", Toast.LENGTH_SHORT).show(); return; }
            createUser(u, p, r);
        });
        b.setNegativeButton("Cancelar", null);
        b.show();
    }

    private void createUser(String username, String password, String role) {
        progress.setVisibility(View.VISIBLE);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<Void> call = api.createUser(username, password, role);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(UsersActivity.this, "Usuario creado", Toast.LENGTH_SHORT).show();
                    fetchUsers();
                } else {
                    Toast.makeText(UsersActivity.this, "Error creando usuario", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(UsersActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete(int position) {
        String uname = users.get(position).get("username").toString();
        new AlertDialog.Builder(this)
                .setTitle("Eliminar usuario")
                .setMessage("¿Desea eliminar al usuario '" + uname + "'?")
                .setPositiveButton("Sí", (d, w) -> deleteUser(uname))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteUser(String username) {
        progress.setVisibility(View.VISIBLE);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<Void> call = api.deleteUser(username);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful() || response.code() == 204) {
                    Toast.makeText(UsersActivity.this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                    fetchUsers();
                } else {
                    Toast.makeText(UsersActivity.this, "Error eliminando usuario", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(UsersActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
