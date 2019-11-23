package ru.shelom.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;
import ru.shelom.activities.mainactivity.R;
import ru.shelom.adapters.ScanAdapter;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {

    private ScanAdapter resultsAdapter;
    private Disposable scanDisposable;
    RecyclerView recyclerView;
    RxBleClient rxBleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rxBleClient = RxBleClient.create(this);

        recyclerView = (RecyclerView) findViewById(R.id.scan_results);
        resultsAdapter = new ScanAdapter();
        recyclerView.setAdapter(resultsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsAdapter.setOnAdapterItemClickListener(new ScanAdapter.OnAdapterItemClickListener() {
            @Override
            public void onAdapterViewClick(View view) {
                final int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
                final ScanResult itemAtPosition = resultsAdapter.getItemAtPosition(childAdapterPosition);
                onAdapterItemClick(itemAtPosition);
            }
        });
//        resultsAdapter.setOnAdapterItemClickListener(view -> {
//            final int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
//            final ScanResult itemAtPosition = resultsAdapter.getItemAtPosition(childAdapterPosition);
//            onAdapterItemClick(itemAtPosition);
//        });

// When done, just dispose.
//        scanSubscription.dispose();

        ((Button)findViewById(R.id.searchButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
                }

                scanDisposable = rxBleClient.scanBleDevices(
                        new ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .build(),
                        new ScanFilter.Builder()
//                            .setDeviceAddress("B4:99:4C:34:DC:8B")
                                // add custom filters if needed
                                .build()
                )
                        .observeOn(AndroidSchedulers.mainThread())
//                        .doFinally(this::dispose)
                        .subscribe(resultsAdapter::addScanResult);

            }
        });
    }

    private void onAdapterItemClick(ScanResult scanResults) {
        final String macAddress = scanResults.getBleDevice().getMacAddress();
        RxBleDevice device = rxBleClient.getBleDevice(macAddress);
//        Disposable disposable = device.establishConnection(false) // <-- autoConnect flag
//                .subscribe(
//                        rxBleConnection -> {
//                            // All GATT operations are done through the rxBleConnection.
//                            System.out.println("Connection = "+ rxBleConnection.discoverServices().blockingGet().getBluetoothGattServices().size());
//                        },
//                        throwable -> {
//                            System.out.println("throwable " + throwable.getMessage());
//                            // Handle an error here.
//                        }
//                );

        Disposable disposable = device.establishConnection(true) // <-- autoConnect flag
                .subscribe(
                        rxBleConnection -> {
                            System.out.println("rxBleConnection = ");
                            device.establishConnection(true)
                                    .flatMapSingle(rxBleConnection2 -> rxBleConnection.readCharacteristic(UUID.fromString(macAddress)))
                                    .subscribe(
                                            characteristicValue -> {
                                                System.out.println("chart = " + characteristicValue.length);
                                                // Read characteristic value.
                                            },
                                            throwable -> {
                                                System.out.println("chart throwable = " + throwable.getMessage());
                                                // Handle an error here.
                                            }
                                    );
                        },
                        throwable -> {
                            System.out.println("onAdapterItemClick = throwable");
                            // Handle an error here.
                        }
                );



//        final Intent intent = new Intent(this, DeviceActivity.class);
//        intent.putExtra(DeviceActivity.EXTRA_MAC_ADDRESS, macAddress);
//        startActivity(intent);
    }

    private void dispose() {
        scanDisposable = null;
//        resultsAdapter.clearScanResults();
//        updateButtonUIState();
    }
}
