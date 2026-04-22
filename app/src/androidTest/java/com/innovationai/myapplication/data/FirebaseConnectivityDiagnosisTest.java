package com.innovationai.myapplication.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;
import com.innovationai.myapplication.R;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class FirebaseConnectivityDiagnosisTest {
    private static final long SDK_TIMEOUT_SECONDS = 60L;

    @Test
    public void rawHttpsRequestToFirestoreRestEndpoint_shouldReturnDocuments() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String projectId = context.getString(R.string.project_id);
        URL url = new URL("https://firestore.googleapis.com/v1/projects/"
                + projectId + "/databases/(default)/documents/users?pageSize=1");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(20_000);
        connection.setReadTimeout(20_000);

        int statusCode = connection.getResponseCode();
        String body = readStream(statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream());

        assertEquals(200, statusCode);
        assertNotNull(body);
        assertTrue(body.contains("\"documents\""));
    }

    @Test
    public void firestoreSdkQuery_shouldReturnAtLeastOneUserDocument() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> count = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        FirebaseFirestore.getInstance()
                .collection("users")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    count.set(queryDocumentSnapshots.size());
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    error.set(e);
                    latch.countDown();
                });

        assertTrue("Firestore SDK query timed out", latch.await(SDK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        assertNotNull(count.get());
        assertTrue(count.get() > 0);
    }

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return new JSONObject(builder.toString()).toString();
        }
    }
}
