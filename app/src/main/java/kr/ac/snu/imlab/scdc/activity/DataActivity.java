package kr.ac.snu.imlab.scdc.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import kr.ac.snu.imlab.scdc.R;
import kr.ac.snu.imlab.scdc.adapter.BaseAdapterData;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.storage.SCDCDatabaseHelper.SensorIdInfo;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

public class DataActivity extends ActionBarActivity {

    protected static final String TAG = "DataActivity";

    private Button backButton, applyButton;
    private ListView dataListView;
    public BaseAdapterData dataAdapter;
    private SharedPrefsHandler spHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        spHandler = SharedPrefsHandler.getInstance(this,
                SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE);

        Log.d(SCDCKeys.LogKeys.DEBB, TAG + ".onCreate()");
        Gson gson = new Gson();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        Intent intent = this.getIntent();
        String dataStr = intent.getStringExtra("data");
        Log.d(SCDCKeys.LogKeys.DEBB, " " + dataStr);

        Type arrListType = new TypeToken<ArrayList<SensorIdInfo>>() {
        }.getType();
        ArrayList<SensorIdInfo> data = gson.fromJson(dataStr, arrListType);

        spHandler.initializeSensorIdToRemove();
        spHandler.initializeSensorIdsInData();
        for (SensorIdInfo sii : data) {
            spHandler.setSensorIdsInData(sii.sensorId);
        }

        backButton = (Button) findViewById(R.id.data_back_button);
        applyButton = (Button) findViewById(R.id.data_apply_button);
        dataListView = (ListView) findViewById(R.id.data_list_view);
        final View header = getLayoutInflater().inflate(R.layout.data_list_view_header_layout, null, false);
        dataListView.addHeaderView(header);

        dataAdapter = new BaseAdapterData(this.getBaseContext(), data);
        dataListView.setAdapter(dataAdapter);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(SCDCKeys.LogKeys.DEBB, "back button clicked");
                spHandler.setSensorIdsRemoveOrNot(false);
                finish();
            }
        });

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(SCDCKeys.LogKeys.DEBB, "apply button clicked");
                spHandler.setSensorIdsRemoveOrNot(true);
                finish();
            }
        });
    }
}
