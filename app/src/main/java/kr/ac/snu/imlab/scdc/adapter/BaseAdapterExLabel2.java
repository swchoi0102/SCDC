package kr.ac.snu.imlab.scdc.adapter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ToggleButton;

import java.util.ArrayList;

import kr.ac.snu.imlab.scdc.R;
import kr.ac.snu.imlab.scdc.activity.LaunchActivity;
import kr.ac.snu.imlab.scdc.entry.LabelEntry;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.Config;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;
import kr.ac.snu.imlab.scdc.util.TimeUtil;

public class BaseAdapterExLabel2 extends BaseAdapter {

  protected static final String TAG = "BaseAdapterExLabel2";

  Context mContext = null;
  ArrayList<LabelEntry> mData = null;
  LayoutInflater mLayoutInflater = null;
  SharedPrefsHandler spHandler = null;
  Handler handler;

  // ****
  ServiceConnection scdcServiceConn = null;
  ServiceConnection scdcManagerConn = null;


  public BaseAdapterExLabel2(Context context, ArrayList<LabelEntry> data) {
    this.mContext = context;
    this.mData = data;
    this.mLayoutInflater = LayoutInflater.from(this.mContext);
    this.spHandler = SharedPrefsHandler.getInstance(this.mContext,
                        Config.SCDC_PREFS, Context.MODE_PRIVATE);
  }

  public LabelEntry getLoggedItem() {
    for(int i=0; i<this.mData.size(); i++){
      if(this.mData.get(i).isLogged()) return this.mData.get(i);
    }
    return null;
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
  public LabelEntry getItem(int position) {
    return this.mData.get(position);
  }

  class ViewHolder {
    ToggleButton labelLogToggleButton;  // ToggleButton seems to work better....
//    Button labelLogToggleButton;
  }

  @Override
  public boolean isEnabled(int position) {
    return true;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    View itemLayout = convertView;
    final ViewHolder viewHolder;

    if (itemLayout == null) {
      itemLayout = mLayoutInflater.inflate(R.layout.label_grid_view_item_layout, null);

      viewHolder = new ViewHolder();
      viewHolder.labelLogToggleButton = (ToggleButton) itemLayout.findViewById(R.id.labelLogToggleButton);

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

//    viewHolder.labelLogToggleButton.setText(mData.get(position).getName());
    viewHolder.labelLogToggleButton.setTextOn(mData.get(position).getName());
    viewHolder.labelLogToggleButton.setTextOff(mData.get(position).getName());

    // Load alone and together toggle button views from LaunchActivity context
    final ToggleButton aloneToggleButton = (ToggleButton)((LaunchActivity)mContext).findViewById(R.id.aloneToggleButton);
    final ToggleButton togetherToggleButton = (ToggleButton)((LaunchActivity)mContext).findViewById(R.id.togetherToggleButton);

    handler = new Handler();

//    // Two conditions for enabling each label button
//    // 1) when alone or together, but not sensorOn
//    Boolean first = (aloneToggleButton.isChecked() || togetherToggleButton.isChecked())
//            && !spHandler.isSensorOn();
    Boolean first = (spHandler.isAloneOn() || spHandler.isTogetherOn())
            && !spHandler.isSensorOn();
//    // 2) when sensorOn, and this label is being logged
//    Boolean second = spHandler.isSensorOn() && mData.get(position).isLogged();
    Boolean second = spHandler.isSensorOn() && mData.get(position).isLogged();
    viewHolder.labelLogToggleButton.setEnabled(first || second);

    // keep the button checked when this label is being logged
//    Log.d(SCDCKeys.LogKeys.DEB, TAG+".getView(): set check "+mData.get(position).getName() + second.toString());
    viewHolder.labelLogToggleButton.setChecked(second);

    // When alone or together goes off, turn off the labelLogButton too
    if(!spHandler.isAloneOn() && !spHandler.isTogetherOn()){
      viewHolder.labelLogToggleButton.setEnabled(false);
      viewHolder.labelLogToggleButton.setChecked(false);
      mData.get(position).endLog();
    }

    viewHolder.labelLogToggleButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        boolean wasChecked = spHandler.isSensorOn();
        if (!wasChecked) {
          Log.d(SCDCKeys.LogKeys.DEBB, TAG+": start logging "+mData.get(position).getName());
//          Intent intent = new Intent(mContext, SCDCService.class);
//
//          // Increment sensorId by 1
//          spHandler.setSensorId(spHandler.getSensorId() + 1);
//          Toast.makeText(mContext,
//                  SCDCKeys.SharedPrefs.KEY_SENSOR_ID + ": " + spHandler.getSensorId(),
//                  Toast.LENGTH_SHORT).show();
//
//          // Start/Bind SCDCService and unbind SCDCManager instead
//          mContext.startService(intent);
//          Log.d(SCDCKeys.LogKeys.DEB, TAG+": start scdcService");
//          mContext.bindService(intent, scdcServiceConn, Context.BIND_AUTO_CREATE);
//          Log.d(SCDCKeys.LogKeys.DEB, TAG+": bind scdcService");
//          mContext.unbindService(scdcManagerConn);
//          Log.d(SCDCKeys.LogKeys.DEB, TAG+": unbind scdcManager");

          boolean pastIsActiveLabelOn = spHandler.isActiveLabelOn();
          mData.get(position).startLog();

          boolean currIsActiveLabelOn = spHandler.isActiveLabelOn();
          // Update config again only when isActiveLabelOn status gets changed
          if (pastIsActiveLabelOn != currIsActiveLabelOn &&
                  mContext instanceof LaunchActivity)
            ((LaunchActivity)mContext).changeConfig(currIsActiveLabelOn);
        }
        else{
          long elapsedTime = TimeUtil.getElapsedTimeUntilNow(mData.get(position).getStartLoggingTime(), "second");
          Log.d(SCDCKeys.LogKeys.DEBB, TAG+": stop logging "+mData.get(position).getName()+" ("+String.valueOf(elapsedTime)+" seconds)");
          boolean pastIsActiveLabelOn = spHandler.isActiveLabelOn();
          mData.get(position).endLog(elapsedTime);
//          labelingOffWaiting();

//          // Unbind/Stop SCDCService and bind SCDCManager instead
//          mContext.unbindService(scdcServiceConn);
//          Log.d(SCDCKeys.LogKeys.DEB, TAG+": unbind scdcService");
//          mContext.stopService(new Intent(mContext, SCDCService.class));
//          Log.d(SCDCKeys.LogKeys.DEB, TAG+": stop scdcService");
//          mContext.bindService(new Intent(mContext,SCDCManager.class), scdcManagerConn, Context.BIND_AUTO_CREATE);
//          Log.d(SCDCKeys.LogKeys.DEB, TAG+": bind scdcManager");

          boolean currIsActiveLabelOn = spHandler.isActiveLabelOn();
          // Update config again only when isActiveLabelOn status gets changed
          if (pastIsActiveLabelOn != currIsActiveLabelOn &&
                  mContext instanceof LaunchActivity)
            ((LaunchActivity)mContext).changeConfig(currIsActiveLabelOn);

          // When labelLogButton goes off, turn off the alone or together too
          spHandler.setAloneOn(false);
          spHandler.setTogetherOn(false);
          aloneToggleButton.setChecked(false);
          togetherToggleButton.setChecked(false);
        }

        // sensorOn should be changed after binding/unbinding SCDCService and SCDCManager
        spHandler.setSensorOn(!wasChecked);

        // when too much data
        if(spHandler.isTooMuchData()){
          aloneToggleButton.setEnabled(false);
          togetherToggleButton.setEnabled(false);
        }
        else{
          aloneToggleButton.setEnabled(wasChecked);
          togetherToggleButton.setEnabled(wasChecked);
        }
        viewHolder.labelLogToggleButton.setChecked(!wasChecked);

//        Log.d(SCDCKeys.LogKeys.DEBB, "alone :\t" +String.valueOf(spHandler.isAloneOn()) + "\t"
//                + "togeth :\t" +String.valueOf(spHandler.isTogetherOn()) + "\t"
//                + "sensor :\t" +String.valueOf(spHandler.isSensorOn()));
        Log.d(SCDCKeys.LogKeys.DEBB, String.valueOf(mData.get(position).getName())+"\t"+String.valueOf(mData.get(position).isLogged()));
      }
    });

    itemLayout.setClickable(true);
    return itemLayout;
  }


  private void labelingOffWaiting() {
    new AsyncTask<Void, Void, Boolean>() {
      private ProgressDialog progressDialog;

      @Override
      protected void onPreExecute() {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage(mContext.getString(R.string.labeling_off_message));
        progressDialog.setCancelable(false);
        progressDialog.show();
      }

      @Override
      protected Boolean doInBackground(Void... voids) {
        try {
          Thread.sleep(10000);         // one second sleep
          publishProgress();          // trigger onProgressUpdate()
        } catch(InterruptedException e) {
          Log.e(SCDCKeys.LogKeys.DEB, TAG + ": " + Log.getStackTraceString(e));
          return false;
        }
        return true;
      }

      @Override
      protected void onPostExecute(Boolean isSuccess) {
        progressDialog.dismiss();
      }
    }.execute();
  }




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
