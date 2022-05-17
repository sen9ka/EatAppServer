package com.senya.eatappserver;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.senya.eatappserver.callback.ISingleShippingOrderCallbackListener;
import com.senya.eatappserver.common.Common;
import com.senya.eatappserver.databinding.ActivityTrackingOrderBinding;
import com.senya.eatappserver.model.ShippingOrderModel;
import com.senya.eatappserver.remote.IGoogleAPI;
import com.senya.eatappserver.remote.RetrofitGoogleAPIClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class TrackingOrderActivity extends FragmentActivity implements OnMapReadyCallback, ISingleShippingOrderCallbackListener {

    private GoogleMap mMap;
    private ISingleShippingOrderCallbackListener iSingleShippingOrderCallbackListener;
    private ActivityTrackingOrderBinding binding;

    //Route
    private Marker shipperMarker;

    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> polylineList;
    private Polyline yellowPolyline, grayPolyline, blackPolyline;

    private IGoogleAPI iGoogleAPI;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTrackingOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initViews();
    }

    private void initViews() {
        iSingleShippingOrderCallbackListener = this;

        iGoogleAPI = RetrofitGoogleAPIClient.getInstance().create(IGoogleAPI.class);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;



        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean success= googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,
                    R.raw.uber_light_with_label));
            if(!success)
                Log.e("Senya","Style parsing failed");
        }catch (Resources.NotFoundException ex)
        {
            Log.e("Senya","Resource not found");
        }

        checkOrderFromFirebase();
    }

    private void checkOrderFromFirebase() {
        FirebaseDatabase.getInstance()
                .getReference(Common.SHIPPING_ORDER_REF)
                .child(Common.currentOrderSelected.getOrderNumber())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists())
                        {
                            ShippingOrderModel shippingOrderModel = snapshot.getValue(ShippingOrderModel.class);
                            shippingOrderModel.setKey(snapshot.getKey());

                            iSingleShippingOrderCallbackListener.onSingleShippingOrderLoadSuccess(shippingOrderModel);
                        }
                        else
                        {
                            Toast.makeText(TrackingOrderActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(TrackingOrderActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onSingleShippingOrderLoadSuccess(ShippingOrderModel shippingOrderModel) {
        LatLng locationOrder = new LatLng(shippingOrderModel.getOrderModel().getLat(),
                shippingOrderModel.getOrderModel().getLng());
        LatLng locationShipper = new LatLng(shippingOrderModel.getCurrentLat(),
                shippingOrderModel.getCurrentLng());

        //add box
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(shippingOrderModel.getOrderModel().getUserName())
                .snippet(shippingOrderModel.getOrderModel().getShippingAddress())
                .position(locationOrder));

        if(shipperMarker == null) {
            int height, width;
            height = width = 80;
            BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat
                    .getDrawable(TrackingOrderActivity.this, R.drawable.shippernew);
            Bitmap resized = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), width, height, false);

            shipperMarker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(resized))
                    .title(shippingOrderModel.getShipperName())
                    .snippet(shippingOrderModel.getShipperPhone())
                    .position(locationShipper));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18));
        }
        else
        {
            shipperMarker.setPosition(locationShipper);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18));
        }

        //draw routes
        String to = new StringBuilder()
                .append(shippingOrderModel.getOrderModel().getLat())
                .append(",")
                .append(shippingOrderModel.getOrderModel().getLng())
                .toString();
        String from = new StringBuilder()
                .append(shippingOrderModel.getCurrentLat())
                .append(",")
                .append(shippingOrderModel.getCurrentLng())
                .toString();

        compositeDisposable.add(iGoogleAPI.getDirections("driving",
                "less_driving",
                from,to,
                getString(R.string.google_maps_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {

                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for(int i = 0; i < jsonArray.length(); i++)
                        {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polylineList = Common.decodePoly(polyline);
                        }

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.RED);
                        polylineOptions.width(12);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polylineList);
                        yellowPolyline = mMap.addPolyline(polylineOptions);
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }, throwable -> Toast.makeText(TrackingOrderActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show()));

    }

    @Override
    protected void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }
}