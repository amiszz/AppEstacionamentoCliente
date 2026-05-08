package com.paradastar.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CadastroActivity extends AppCompatActivity {

    EditText etNome, etEmail, etCpf, etSenha;
    Button btnCadastrar, btnVoltar;
    FirebaseAuth auth;
    DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference("usuarios");

        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmail);
        etCpf = findViewById(R.id.etCpf);
        etSenha = findViewById(R.id.etSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        btnVoltar = findViewById(R.id.btnVoltar);

        btnCadastrar.setOnClickListener(v -> cadastrar());
        btnVoltar.setOnClickListener(v -> finish());
    }

    void cadastrar() {
        String nome = etNome.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String cpf = etCpf.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();

        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(cpf) || TextUtils.isEmpty(senha)) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (senha.length() < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    db.child(uid).child("nome").setValue(nome);
                    db.child(uid).child("email").setValue(email);
                    db.child(uid).child("cpf").setValue(cpf);
                    db.child(uid).child("perfil").setValue("cliente");

                    Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}