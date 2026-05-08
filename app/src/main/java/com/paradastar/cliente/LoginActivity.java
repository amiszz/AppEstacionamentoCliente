package com.paradastar.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etSenha;
    Button btnEntrar, btnIrCadastro;
    TextView tvSairApp;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etSenha = findViewById(R.id.etSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        btnIrCadastro = findViewById(R.id.btnIrCadastro);
        tvSairApp = findViewById(R.id.tvSairApp);

        btnEntrar.setOnClickListener(v -> fazerLogin());
        btnIrCadastro.setOnClickListener(v -> {
            startActivity(new Intent(this, CadastroActivity.class));
        });
        tvSairApp.setOnClickListener(v -> finishAffinity());

    }

    void fazerLogin() {
        String email = etEmail.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(senha)) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener(result -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Email ou senha incorretos!", Toast.LENGTH_SHORT).show();
                });
    }
}