package kr.ac.snu.imlab.scdc.adapter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

import kr.ac.snu.imlab.scdc.R;
import kr.ac.snu.imlab.scdc.activity.LaunchActivity;
import kr.ac.snu.imlab.scdc.entry.LabelEntry;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LabelKeys;
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
    ToggleButton labelLogToggleButton;
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
      viewHolder.labelLogToggleButton = (ToggleButton)itemLayout.findViewById(R.id.labelLogToggleButton);

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

    // two conditions for enabling the button
    Boolean first = (spHandler.isAloneOn()||spHandler.isTogetherOn()) && !spHandler.isLabelOn(); //when alone or together, but not labelOn
    Boolean second = spHandler.isLabelOn() && mData.get(position).isLogged(); //when labelOn, and this label is being logged
    viewHolder.labelLogToggleButton.setEnabled(first || second);

    // keep the button checked when this label is being logged
    viewHolder.labelLogToggleButton.setChecked(second);

    // If alone or together goes off, turn off the labelLogButton too
    if(!aloneToggleButton.isChecked() && !togetherToggleButton.isChecked()){
      viewHolder.labelLogToggleButton.setEnabled(false);
      viewHolder.labelLogToggleButton.setChecked(false);
      mData.get(position).endLog();
    }


    viewHolder.labelLogToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        spHandler.setLabelOn(isChecked);

        if(isChecked){
          boolean pastIsActiveLabelOn = spHandler.isActiveLabelOn();
          mData.get(position).startLog();

          boolean currIsActiveLabelOn = spHandler.isActiveLabelOn();
          // Update config again only when isActiveLabelOn status gets changed
          if (pastIsActiveLabelOn != currIsActiveLabelOn &&
                  mContext instanceof LaunchActivity)
            ((LaunchActivity)mContext).changeConfig(currIsActiveLabelOn);
        }
        else{
          boolean pastIsActiveLabelOn = spHandler.isActiveLabelOn();
          mData.get(position).endLog();

          boolean currIsActiveLabelOn = spHandler.isActiveLabelOn();
          // Update config again only when isActiveLabelOn status gets changed
          if (pastIsActiveLabelOn != currIsActiveLabelOn &&
                  mContext instanceof LaunchActivity)
            ((LaunchActivity)mContext).changeConfig(currIsActiveLabelOn);
        }
      }
    });

    itemLayout.setClickable(true);
    return itemLayout;
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
