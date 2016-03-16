package br.edu.ifsp.nfc_reader;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

/**
 * <h1>NFC-Reader</h1>
 * Aplicativo para realizar, de forma simplificada, a leitura de tags NFC no padrão Ndef
 * @author  Murilo Rodrigues
 * @version 1.0
 * @since   2016-03-15
 */
public class MainActivity extends AppCompatActivity {
    private	NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMessage = (TextView) findViewById(R.id.txtMessage);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Define que a activity ficará rodando no topo quando realizar a leitura de uma tag
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if (!hasNfc()) {
            Toast.makeText(this, R.string.nfc_disable, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        txtMessage.setText("");
        resolveIntent(intent);
    }

    boolean hasNfc() {
        // Verifica se NFC está habilitado
        boolean hasFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
        boolean isEnabled = NfcAdapter.getDefaultAdapter(this).isEnabled();
        return hasFeature && isEnabled;
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();

        // Verifica se a action da Intent é uma das ações esperadas para o NFC
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            // Extrai as mensagens da intent
            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            // Se foram encontradas mensagem NDef na Intent
            if (messages != null) {

                // Extrai as informações Ndef da primeira mensagem da lista
                NdefMessage ndefMessage = (NdefMessage) messages[0];
                NdefRecord[] ndefRecords = ndefMessage.getRecords();
                NdefRecord ndefRecord = ndefRecords[0];

                try {
                    // Apresenta na TextView o conteúdo da tag
                    txtMessage.setText(readText(ndefRecord));

                } catch (UnsupportedEncodingException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                // Alerta que o tipo da tag é desconhecido
                Toast.makeText(this, R.string.unknown_tag_type, Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    private String readText(NdefRecord record) throws UnsupportedEncodingException {
        byte[] payload = record.getPayload();

        // Pega o Encoding da mensagem
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;

        // Retorna a mensagem sem o encoding
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }
}
