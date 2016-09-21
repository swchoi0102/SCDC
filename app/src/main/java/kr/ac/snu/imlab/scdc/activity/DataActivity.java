package kr.ac.snu.imlab.scdc.activity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.util.ArrayList;

import kr.ac.snu.imlab.scdc.R;
import kr.ac.snu.imlab.scdc.adapter.BaseAdapterData;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCPipeline;
import kr.ac.snu.imlab.scdc.service.storage.SCDCDatabaseHelper;
import kr.ac.snu.imlab.scdc.service.storage.SCDCDatabaseHelper.SensorIdInfo;

public class DataActivity extends ActionBarActivity {

    protected static final String TAG = "DataActivity";

    private SCDCPipeline pipeline;
    private SCDCDatabaseHelper databaseHelper;
    private Button finishButton;
    private ListView dataListView;
    public BaseAdapterData dataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(SCDCKeys.LogKeys.DEBB, TAG+".onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        Intent intent = this.getIntent();
        String str = intent.getStringExtra("text");
        Log.d(SCDCKeys.LogKeys.DEBB, " "+str);



//        databaseHelper = (SCDCDatabaseHelper) pipeline.getDatabaseHelper();
//        ArrayList<SensorIdInfo> data = databaseHelper.getSensorIdInfo(pipeline.getDb());
//        ArrayList<SensorIdInfo> data = null;
//
          finishButton = (Button) findViewById(R.id.finish_button);
//        dataListView = (ListView)findViewById(R.id.data_list_view);
//        final View header = getLayoutInflater().inflate(R.layout.data_list_view_header_layout, null, false);
//        dataListView.addHeaderView(header);
//
//
//
//        dataAdapter = new BaseAdapterData(this.getBaseContext(), data);
//        dataListView.setAdapter(dataAdapter);

        finishButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d(SCDCKeys.LogKeys.DEBB, "finish button clicked");
                finish();
            }
        });

    }
}
