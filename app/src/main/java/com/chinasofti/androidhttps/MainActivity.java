package com.chinasofti.androidhttps;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_request;
    private TextView tx_content;

    private static final int UPDATE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_request = (Button) findViewById(R.id.btn_request);
        tx_content = (TextView) findViewById(R.id.tx_content);

        btn_request.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_request:
                try {
                    request();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    private void request() throws Exception {
        final String KEYSTORE_PASSWORD = "123456";
        final String KEYSTORE_TYPE = "PKCS12";
        final String Certificate_TYPE = "X.509";

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                Log.e("HTTPS-TEST", hostname);
                return true;
            }
        });
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(getAssets().open("client_side.p12"), KEYSTORE_PASSWORD.toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        KeyStore trustKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustKeyStore.load(null);
        CertificateFactory certificateFactory = CertificateFactory.getInstance(Certificate_TYPE);
        X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(getAssets().open("server_side.cer"));
        trustKeyStore.setCertificateEntry(x509Certificate.getSubjectX500Principal().getName(), x509Certificate);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustKeyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        URL url = new URL("https://192.168.40.240:8443/");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(10 * 1000);
        connection.setRequestMethod("GET");
        connection.setSSLSocketFactory(sslContext.getSocketFactory());

        new AsyncTask<HttpsURLConnection, Void, String>() {

            @Override
            protected String doInBackground(HttpsURLConnection... params) {
                HttpsURLConnection connection = params[0];
                try {
                    InputStream in = connection.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    StringBuilder stringBuffer = new StringBuilder();
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        stringBuffer.append(line);
                    }
                    connection.disconnect();
                    br.close();
                    in.close();
                    return stringBuffer.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "error";
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                tx_content.setText(s);
            }
        }.execute(connection);
    }
}
