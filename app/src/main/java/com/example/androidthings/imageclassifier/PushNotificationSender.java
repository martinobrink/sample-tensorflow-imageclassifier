package com.example.androidthings.imageclassifier;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PushNotificationSender {

    private final RequestQueue requestQueue;

    public PushNotificationSender(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public void sendNotification(String title, String message) {

        try {
            String notificationUrl = "https://api.pushbullet.com/v2/pushes";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("type", "note");
            jsonBody.put("title", title);
            jsonBody.put("body", message);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, notificationUrl, jsonBody,
                    response -> {
                        Log.i("HEST", "DET GIK GODT!");

                //Toast.makeText(getApplicationContext(), "Response:  " + response.toString(), Toast.LENGTH_SHORT).show();
            }, error -> {

                Log.e("HEST", "DET GIK SKIDT!");
                //onBackPressed();

            }) {
                @Override
                public Map<String, String> getHeaders() {
                    final Map<String, String> headers = new HashMap<>();
                    headers.put("Access-Token", "o.MdUfjfpmMK0E0lXf8Y9raxah6GQlNoOy");
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Toast.makeText(getApplicationContext(), "done", Toast.LENGTH_LONG).show();

    }

}
