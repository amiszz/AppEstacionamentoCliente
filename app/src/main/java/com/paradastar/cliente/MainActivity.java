package com.paradastar.cliente;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    EditText etIdVaga, etPlaca, etModelo;
    Button btnOcupar, btnEncerrar, btnInfo, btnSair;
    TextView tvInfoVaga;
    View layoutOcupacao, layoutVagaAtiva;

    FirebaseAuth auth;
    DatabaseReference db;
    String uid;
    String ocupacaoAtualId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        uid = auth.getCurrentUser().getUid();

        etIdVaga = findViewById(R.id.etIdVaga);
        etPlaca = findViewById(R.id.etPlaca);
        etModelo = findViewById(R.id.etModelo);
        btnOcupar = findViewById(R.id.btnOcupar);
        btnEncerrar = findViewById(R.id.btnEncerrar);
        btnInfo = findViewById(R.id.btnInfo);
        btnSair = findViewById(R.id.btnSair);
        tvInfoVaga = findViewById(R.id.tvInfoVaga);
        layoutOcupacao = findViewById(R.id.layoutOcupacao);
        layoutVagaAtiva = findViewById(R.id.layoutVagaAtiva);

        btnOcupar.setOnClickListener(v -> ocuparVaga());
        btnEncerrar.setOnClickListener(v -> encerrarOcupacao());
        btnInfo.setOnClickListener(v -> mostrarInfo());
        btnSair.setOnClickListener(v -> logout());

        verificarVagaAtiva();
    }

    void verificarVagaAtiva() {
        db.child("vagas").orderByChild("uidCliente").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot vaga : snapshot.getChildren()) {
                            String status = vaga.child("status").getValue(String.class);
                            if ("ocupada".equals(status)) {
                                ocupacaoAtualId = vaga.getKey();
                                String idVaga = vaga.getKey();
                                String placa = vaga.child("placa").getValue(String.class);
                                String modelo = vaga.child("modelo").getValue(String.class);
                                mostrarVagaAtiva(idVaga, placa, modelo);
                                return;
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    void ocuparVaga() {
        String idVaga = etIdVaga.getText().toString().trim().toUpperCase();
        String placa = etPlaca.getText().toString().trim().toUpperCase();
        String modelo = etModelo.getText().toString().trim();

        if (TextUtils.isEmpty(idVaga) || TextUtils.isEmpty(placa) || TextUtils.isEmpty(modelo)) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!placa.matches("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$")) {
            Toast.makeText(this, "Placa inválida! Use o formato ABC1234 ou ABC1D23.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.child("vagas").child(idVaga).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String status = snapshot.child("status").getValue(String.class);
                if ("ocupada".equals(status)) {
                    Toast.makeText(this, "Vaga já está ocupada!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if ("bloqueada".equals(status)) {
                    Toast.makeText(this, "Vaga bloqueada/interditada!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            db.child("usuarios").child(uid).child("nome").get().addOnSuccessListener(nomeSnap -> {
                String nomeCliente = nomeSnap.getValue(String.class);
                long horaEntrada = System.currentTimeMillis();

                Map<String, Object> dados = new HashMap<>();
                dados.put("status", "ocupada");
                dados.put("placa", placa);
                dados.put("modelo", modelo);
                dados.put("uidCliente", uid);
                dados.put("nomeCliente", nomeCliente);
                dados.put("horaEntrada", horaEntrada);

                db.child("vagas").child(idVaga).setValue(dados)
                        .addOnSuccessListener(aVoid -> {
                            ocupacaoAtualId = idVaga;
                            Toast.makeText(this, "Vaga ocupada com sucesso!", Toast.LENGTH_SHORT).show();
                            mostrarVagaAtiva(idVaga, placa, modelo);
                        });
            });
        });
    }

    void mostrarVagaAtiva(String idVaga, String placa, String modelo) {
        layoutOcupacao.setVisibility(View.GONE);
        layoutVagaAtiva.setVisibility(View.VISIBLE);
        tvInfoVaga.setText("Vaga: " + idVaga + "\nPlaca: " + placa + "\nModelo: " + modelo);
    }

    void encerrarOcupacao() {
        if (ocupacaoAtualId == null) return;

        db.child("vagas").child(ocupacaoAtualId).child("horaEntrada")
                .get().addOnSuccessListener(snapshot -> {
                    long horaEntrada = snapshot.getValue(Long.class);
                    long horaSaida = System.currentTimeMillis();
                    long duracaoMs = horaSaida - horaEntrada;

                    long minutos = (duracaoMs / 1000) / 60;
                    long horas = minutos / 60;
                    long minutosRestantes = minutos % 60;

                    double valorPorHora = 5.0;
                    double totalHoras = duracaoMs / 3600000.0;
                    double valorBruto = Math.max(valorPorHora, totalHoras * valorPorHora);
                    final double valor = Math.ceil(valorBruto * 100) / 100.0;

                    Map<String, Object> atualizacao = new HashMap<>();
                    atualizacao.put("status", "disponivel");
                    atualizacao.put("placa", "");
                    atualizacao.put("modelo", "");
                    atualizacao.put("uidCliente", "");
                    atualizacao.put("nomeCliente", "");
                    atualizacao.put("horaEntrada", 0);

                    db.child("vagas").child(ocupacaoAtualId).updateChildren(atualizacao)
                            .addOnSuccessListener(aVoid -> {
                                String tempo = horas + "h " + minutosRestantes + "min";
                                String valorStr = String.format(Locale.getDefault(), "R$ %.2f", valor);

                                new AlertDialog.Builder(this)
                                        .setTitle("Ocupação encerrada")
                                        .setMessage(
                                                "Tempo: " + tempo +
                                                        "\nValor: " + valorStr +
                                                        "\n\nPor favor, dirija-se à portaria para realizar o pagamento." +
                                                        "\n\nObrigado por utilizar o ParadaStar!"
                                        )
                                        .setPositiveButton("OK", (d, w) -> {
                                            ocupacaoAtualId = null;
                                            layoutVagaAtiva.setVisibility(View.GONE);
                                            layoutOcupacao.setVisibility(View.VISIBLE);
                                            etIdVaga.setText("");
                                            etPlaca.setText("");
                                            etModelo.setText("");
                                        })
                                        .setCancelable(false)
                                        .show();
                            });
                });
    }

    void mostrarInfo() {
        new AlertDialog.Builder(this)
                .setTitle("ParadaStar")
                .setMessage(
                        "Endereço: Rua das Flores, 123 - Centro\n\n" +
                                "Funcionamento: 24h\n\n" +
                                "Valor: R$ 5,00 por hora\n\n" +
                                "Contato: (44) 1234-5678"
                )
                .setPositiveButton("Fechar", null)
                .show();
    }

    void logout() {
        auth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}