package com.example.vishnu.hilltop;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BookingStatus extends AppCompatActivity {

    RequestQueue mRequestQueue;
    String buid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_status);
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();
        buid = getIntent().getStringExtra("buid");
        retrieveBookings(buid);

        if (!isNetworkAvailable()) {
            Toast toast = Toast.makeText(this,"Please check your internet connectivity",Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void retrieveBookings(String buid) {
        final String URL = "http://hilltop-bradleyuniv.rhcloud.com/rest/bookingList/"+buid;
        JsonArrayRequest req = new JsonArrayRequest(URL, new Response.Listener<JSONArray> () {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    VolleyLog.v("Response:%n %s", response.toString(4));
                    buildStatusTable(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
            }
        });
        mRequestQueue.add(req);

    }


    public void buildStatusTable(JSONArray status) {

        TableLayout layout = (TableLayout)findViewById(R.id.statusTableLayout);
        layout.removeAllViews();
        TableRow header = new TableRow(this);
        header.setBackgroundDrawable(getResources().getDrawable(R.drawable.headerrowborder));
        header.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        TextView idHeader = new TextView(this);
        idHeader.setText("Booking ID");
        TextView statusHeader = new TextView(this);
        statusHeader.setText("Status");
        statusHeader.setTextColor(getResources().getColor(R.color.black));
        idHeader.setTextColor(getResources().getColor(R.color.black));
        header.addView(idHeader);
        header.addView(statusHeader);
        layout.addView(header);
        for(int i =0;i<status.length();i++) {
            TableRow row = null;
            JSONObject entry = null;
            try {
                entry = status.getJSONObject(i);

                row = new TableRow(this);
                row.setBackgroundDrawable(getResources().getDrawable(R.drawable.rowborder));
                row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.WRAP_CONTENT));
                TextView idCell = new TextView(this);
                TextView statusCell = new TextView(this);
                idCell.setTextColor(getResources().getColor(R.color.smokeGrey));
                if (entry.getString("booking_status").equals("approved"))
                    statusCell.setTextColor(getResources().getColor(R.color.gossipGreen));
                else if (entry.getString("booking_status").equals("rejected"))
                    statusCell.setTextColor(getResources().getColor(R.color.lynch));
                else if (entry.getString("booking_status").equals("complete"))
                    statusCell.setTextColor(getResources().getColor(R.color.lynch));
                else
                    statusCell.setTextColor(getResources().getColor(R.color.smokeGrey));
                String bookingId = entry.getString("booking_id");
                idCell.setText(bookingId);
                row.setId(Integer.parseInt(bookingId));
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick( View v ) {
                        TableRow row = (TableRow)v;
                        String status = ((TextView)row.getChildAt(1)).getText().toString();
                        String id = ((TextView)row.getChildAt(0)).getText().toString();
                        if (status.equals("waiting for approval") || status.equals("approved")) {
                            openCancelDialog(id,status);
                        }

                    }});
                statusCell.setText(entry.getString("booking_status"));
                row.addView(idCell);
                row.addView(statusCell);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            layout.addView(row);
        }
    }

    public void refresh(View view) {
        retrieveBookings(buid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        retrieveBookings(buid);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public void openCancelDialog(final String id,String status) {
        final Dialog dialog = new Dialog(this); // Context, this, etc.
        dialog.setContentView(R.layout.cancel_booking);
        dialog.setTitle("Cancel booking");
        TextView dialogText = (TextView) dialog.findViewById(R.id.dialog_info);
        dialogText.setText("Would you like to cancel booking? \n ID: "+id+" \n Status : "+status);
        dialog.findViewById(R.id.dialog_yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String URL = "http://hilltop-bradleyuniv.rhcloud.com/rest/cancelBooking/"+id;
                JsonObjectRequest req = new JsonObjectRequest(URL, new Response.Listener<JSONObject> () {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                            retrieveBookings(buid);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.e("Error: ", error.getMessage());
                    }
                });
                mRequestQueue.add(req);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.dialog_no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
