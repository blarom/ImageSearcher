package com.imagesearcher.utilities;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.imagesearcher.R;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkUtilities {

    public static boolean isInternetAvailable(Context context) {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = null;
        if (connMgr != null) activeNetworkInfo = connMgr.getActiveNetworkInfo();

        // return true if connected to the internet
        return activeNetworkInfo != null;
    }
    public static void tellUserInternetIsUnavailable(Context context) {
        Toast.makeText(context, context.getResources().getString(R.string.failedToAccessOnlineResources), Toast.LENGTH_SHORT).show();
    }
    public static String getWebsiteContent(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        if (response != null && response.body() != null) {
            return response.body().string();
        }
        else return "";
    }
}
