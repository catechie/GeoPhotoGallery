package com.boobastudio.geophotogallery;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by donna on 5/22/16.
 */
public class RestfulCallFetchResult {
    /*
    *making restful api call to get the jason string as jsonResult
     */
    static byte[] queryJsonResult(String urlString) {
        byte[] result = null;
        final String TAG = "RestfulCallFetchResult";
        HttpURLConnection connection = null;

        if (urlString != null && urlString.length() != 0) {

            try {

                URL url = new URL(urlString);

                try {
                    connection = (HttpURLConnection) url.openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                InputStream in = connection.getInputStream();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                int bytesRead = 0;
                byte[] buffer = new byte[1024];
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                out.close();
                result = out.toByteArray();
                return result;

            } catch (MalformedURLException e) {
                Log.e(TAG, "Error in queryJsonResult: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error in queryJsonResult: " + e.getMessage());
            } finally {
                if (connection != null)
                    connection.disconnect();
            }

        }

        return result;
    }
}
