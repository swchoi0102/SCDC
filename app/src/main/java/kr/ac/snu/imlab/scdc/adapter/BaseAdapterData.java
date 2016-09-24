package kr.ac.snu.imlab.scdc.adapter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import kr.ac.snu.imlab.scdc.R;
import kr.ac.snu.imlab.scdc.activity.LaunchActivity;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.Config;
import kr.ac.snu.imlab.scdc.service.storage.SCDCDatabaseHelper.SensorIdInfo;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

public class BaseAdapterData extends BaseAdapter {

  protected static final String TAG = "BaseAdapterData";

  Context mContext = null;
  ArrayList<SensorIdInfo> mData = null;
  LayoutInflater mLayoutInflater = null;
  SharedPrefsHandler spHandler = null;

  Handler handler;

  private SimpleDateFormat dataFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());

  public BaseAdapterData(Context context, ArrayList<SensorIdInfo> data) {
    this.mContext = context;
    this.mData = data;
    this.mLayoutInflater = LayoutInflater.from(this.mContext);
    this.spHandler = SharedPrefsHandler.getInstance(this.mContext,
                        Config.SCDC_PREFS, Context.MODE_PRIVATE);
  }

  @Override
  public int getCount() {
    return this.mData.size();
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public SensorIdInfo getItem(int position) {
    return this.mData.get(position);
  }

  class ViewHolder {
    ToggleButton sensorIdToggleButton;
//    TextView sensorIdTextView;
//    TextView aloneOrTogetherTextView;
//    TextView labelTextView;
//    TextView startTimeTextView;
//    TextView endTimeTextView;
//    LinearLayout seekBarLayout;
//    CheckBox deleteCheckBox;
//    Button dataDeleteButton, dataSaveButton;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    View itemLayout = convertView;
    final ViewHolder viewHolder;


    if (itemLayout == null) {
      itemLayout = mLayoutInflater.inflate(R.layout.data_list_view_item_layout, null);
      viewHolder = new ViewHolder();
      viewHolder.sensorIdToggleButton = (ToggleButton) itemLayout.findViewById(R.id.sensorIdToggleButton);
//      viewHolder.sensorIdTextView = (TextView)itemLayout.findViewById(R.id.sensor_id_tv);
//      viewHolder.aloneOrTogetherTextView = (TextView)itemLayout.findViewById(R.id.alone_or_together_tv);
//      viewHolder.labelTextView = (TextView)itemLayout.findViewById(R.id.label_tv);
//      viewHolder.startTimeTextView = (TextView)itemLayout.findViewById(R.id.start_time_tv);
//      viewHolder.endTimeTextView = (TextView)itemLayout.findViewById(R.id.end_time_tv);
//      viewHolder.seekBarLayout = (LinearLayout) itemLayout.findViewById(R.id.seekbar_layout);
////      viewHolder.dataSaveButton = (Button) itemLayout.findViewById(R.id.data_save_button);
////      viewHolder.dataDeleteButton = (Button) itemLayout.findViewById(R.id.data_delete_button);
//      viewHolder.deleteCheckBox = (CheckBox) itemLayout.findViewById(R.id.data_delete_checkbox);
      itemLayout.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder)itemLayout.getTag();
    }

    itemLayout.setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        return false;
      }
    });

    SensorIdInfo info = mData.get(position);

    String sensorIdStr = String.valueOf(info.sensorId);
    String startTimeStr = dataFormat.format(info.firstTS*1000);
    String endTimeStr = dataFormat.format(info.lastTS*1000);
    String togetherStr =info.firstTogether;
    String labelStr = info.firstLabel;
    String totalStr = sensorIdStr + "\t\t\t\t" + labelStr + "\t\t" + togetherStr + "\t\t\t\t" + startTimeStr +" ~ " + endTimeStr;
    Log.d(SCDCKeys.LogKeys.DEBB, "is that right??: " + totalStr);
//    Log.d(SCDCKeys.LogKeys.DEBB, sensorIdStr+" "+startTimeStr+" "+endTimeStr+" "+togetherStr+" "+labelStr);

    viewHolder.sensorIdToggleButton.setText(totalStr);
    viewHolder.sensorIdToggleButton.setTextOn(totalStr);
    viewHolder.sensorIdToggleButton.setTextOff(totalStr);

//    viewHolder.sensorIdTextView.setText(sensorIdStr);
//    viewHolder.aloneOrTogetherTextView.setText(togetherStr);
//    viewHolder.labelTextView.setText(labelStr);
//    viewHolder.startTimeTextView.setText(startTimeStr+mContext.getString(R.string.data_start));
//    viewHolder.endTimeTextView.setText(endTimeStr+mContext.getString(R.string.data_end));

    handler = new Handler();

//    rangeSeekBar = new RangeSeekBar<>(mContext);


//    double totalTimeInSecond = (int) info.lastTS-info.firstTS;
//    int maxValue = 0;
//    if(totalTimeInSecond>60){
//      maxValue = (int) totalTimeInSecond;
//    }
//    else{
//      int totalTimeInMinute = (int) totalTimeInSecond / 60;
//      maxValue = totalTimeInMinute;
//    }
//    rangeSeekBar.setRangeValues(0, maxValue);
//    rangeSeekBar.setSelectedMinValue(0);
//    rangeSeekBar.setSelectedMaxValue(maxValue);
//    viewHolder.seekBarLayout.addView(rangeSeekBar);

//    viewHolder.deleteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//      @Override
//      public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//
//        if(isChecked){
//          //when checked, delete this data
//        }
//
//        else{
//          //when not checked, do nothing
//        }
//      }
//    });


//    viewHolder.dataSaveButton.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View v) {
//
//        // when clicked, save the change and refresh
//
//      }
//    });

//    viewHolder.dataDeleteButton.setOnClickListener(new View.OnClickListener(){
//      @Override
//      public void onClick(View v) {
//
//        // when clicked, delete this data and refresh
//
//      }
//    });


    viewHolder.sensorIdToggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String[] sIdArr = spHandler.getSensorIdsInData().split(",");
        int sIdAtPosition = Integer.parseInt(sIdArr[position]);
        if (isChecked) {
          Log.d(SCDCKeys.LogKeys.DEB, TAG+".sensorIdToggleButton checked!");
          spHandler.insertSensorIdToRemove(sIdAtPosition);
        } else {
          Log.d(SCDCKeys.LogKeys.DEB, TAG+".sensorIdToggleButton unchecked!");
          spHandler.popSensorIdToRemove(sIdAtPosition);
        }
      }
    });

    itemLayout.setClickable(true);
    return itemLayout;
  }



  // below are copied from original adapter

  protected void notify(int mId, String title, String message,
                           String alert) {

    // Create a new notification builder
    NotificationCompat.Builder builder =
       new NotificationCompat.Builder(mContext)
              .setAutoCancel(false)
              .setContentIntent(getPendingIntent(mId))
              .setContentTitle(title)
              .setContentText(message)
              .setTicker(alert)
              // .setDefaults(Notification.DEFAULT_ALL)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setOngoing(true)
              .setWhen(System.currentTimeMillis());

    @SuppressWarnings("deprecation")
    Notification notification = builder.getNotification();
    NotificationManager notificationMgr = (NotificationManager)mContext.
                          getSystemService(Context.NOTIFICATION_SERVICE);
    notificationMgr.notify(mId, notification);

  }

  protected void cancelNotify(int mId) {
    NotificationManager notificationMgr =
            (NotificationManager)mContext.
                    getSystemService(Context.NOTIFICATION_SERVICE);
    notificationMgr.cancel(mId);
  }

  protected void cancelNotifyAll() {
    NotificationManager notificationMgr =
            (NotificationManager)mContext.
                    getSystemService(Context.NOTIFICATION_SERVICE);
    notificationMgr.cancelAll();
  }

  PendingIntent getPendingIntent(int id) {
    Intent intent = new Intent(mContext, LaunchActivity.class);
    return PendingIntent.getActivity(mContext, id, intent, 0);
  }

}
