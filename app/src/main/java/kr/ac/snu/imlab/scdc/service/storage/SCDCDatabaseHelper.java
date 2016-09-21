package kr.ac.snu.imlab.scdc.service.storage;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Parcelable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.StringUtil;
import edu.mit.media.funf.util.UuidUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

/**
 * Created by kilho on 15. 7. 28.
 */
public class SCDCDatabaseHelper extends SQLiteOpenHelper {

    public static final int CURRENT_VERSION = 1;

    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_SENSOR_ID = SCDCKeys.SharedPrefs.KEY_SENSOR_ID;
    public static final String COLUMN_EXP_ID = SCDCKeys.SharedPrefs.KEY_EXP_ID;
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_VALUE = "value";
    public static final Table DATA_TABLE = new Table("data",
            Arrays.asList(new Column(COLUMN_NAME, "TEXT"), // ACTION from data broadcast
                    new Column(COLUMN_SENSOR_ID, "INT"), // ACTION from data broadcast
                    new Column(COLUMN_EXP_ID, "INT"), // ACTION from data broadcast
                    new Column(COLUMN_TIMESTAMP, "FLOAT"), // TIMESTAMP in data broadcast
                    new Column(COLUMN_VALUE, "TEXT"))); // JSON representing
    public static final String COLUMN_DATABASE_NAME = "dbname";
    public static final String COLUMN_INSTALLATION = "device";
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_CREATED = "created";
    public static final Table FILE_INFO_TABLE = new Table("file_info",
            Arrays.asList(new Column(COLUMN_DATABASE_NAME, "TEXT"), // Name of this database
                    new Column(COLUMN_INSTALLATION, "TEXT"), // Universally Unique Id for device installation
                    new Column(COLUMN_UUID, "TEXT"), // Universally Unique Id for file
                    new Column(COLUMN_CREATED, "FLOAT"))); // TIMESTAMP in data broadcast

    private final Context context;
    private final String databaseName;

    public SCDCDatabaseHelper(Context context, String name, int version) {
        super(context, name, null, version);
        this.context = context;
        this.databaseName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATA_TABLE.getCreateTableSQL());
        db.execSQL(FILE_INFO_TABLE.getCreateTableSQL());
        // Insert file identifier information
        String installationUuid = UuidUtil.getInstallationId(context);
        String fileUuid = UUID.randomUUID().toString();
        double createdTime = TimeUtil.getTimestamp().doubleValue();
        db.execSQL(String.format(Locale.US, "insert into %s (%s, %s, %s, %s) values ('%s', '%s', '%s', %f)",
                FILE_INFO_TABLE.name,
                COLUMN_DATABASE_NAME, COLUMN_INSTALLATION, COLUMN_UUID, COLUMN_CREATED,
                databaseName, installationUuid, fileUuid, createdTime));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nothing yet
    }

    public ArrayList<SensorIdInfo> getSensorIdInfo(SQLiteDatabase db) {
        ArrayList<SensorIdInfo> sensorIdInfoList = new ArrayList<>();
        try {
            Cursor sensorIdCursor = db.rawQuery("SELECT DISTINCT " + COLUMN_SENSOR_ID + " FROM " + DATA_TABLE.name, null);
            JsonParser parser = new JsonParser();
            String[] labelArr = new String[]{
                    SCDCKeys.LabelKeys.SLEEP_LABEL,
                    SCDCKeys.LabelKeys.EATING_LABEL,
                    SCDCKeys.LabelKeys.IN_CLASS_LABEL,
                    SCDCKeys.LabelKeys.STUDYING_LABEL,
                    SCDCKeys.LabelKeys.DRINKING_LABEL,
                    SCDCKeys.LabelKeys.MOVING_LABEL,
                    SCDCKeys.LabelKeys.NONE_OF_ABOVE_LABEL,
                    SCDCKeys.LabelKeys.TOGETHER_STATUS,
            };
            while (sensorIdCursor.moveToNext()){
                int sensorId = sensorIdCursor.getInt(0);
                SensorIdInfo sensorIdInfo = new SensorIdInfo(sensorId, db, parser, labelArr);
                sensorIdInfoList.add(sensorIdInfo);
            }
        } catch (Exception e) {
            return sensorIdInfoList;
        }

        return sensorIdInfoList;
    }

    public boolean updateTable(SQLiteDatabase db, int sensorId, boolean deleteAction, double startTS, double endTS){
        try {
            String sql = "DELETE FROM " + DATA_TABLE.name
                    + " WHERE " + COLUMN_SENSOR_ID + " = " + sensorId;
            if(deleteAction) {
                sql += ";";
            } else{
                sql += " AND " + COLUMN_TIMESTAMP + " < " + startTS + " AND " + COLUMN_TIMESTAMP + " > " + endTS + ";";
            }
            db.execSQL(sql);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * @param db
     * @author Kilho Kim
     * Drop and re-Create 'data' table.
     */
    public boolean dropAndCreateDataTable(SQLiteDatabase db) {
      try {
        db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE.name + ";");
        db.execSQL(DATA_TABLE.getCreateTableSQL());
      } catch (Exception e) {
        return false;
      }

      return true;
    }

    // TODO: Consider moving these to an external utils class

    /**
     * Immutable Table Definition
     */
    public static class Table {
        private static final String CREATE_TABLE_FORMAT = "CREATE TABLE %s (_id INTEGER primary key autoincrement, %s);";

        public final String name;
        private final List<Column> columns;

        public Table(final String name, final List<Column> columns) {
            this.name = name;
            this.columns = new ArrayList<>(columns);
        }

        public List<Column> getColumns() {
            return new ArrayList<>(columns);
        }

        public String getCreateTableSQL() {
            return String.format(CREATE_TABLE_FORMAT, name, StringUtil.join(columns, ", "));
        }
    }

    /**
     * Immutable Column Definition
     */
    public static class Column {
        public final String name, type;

        public Column(final String name, final String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return name + " " + type;
        }
    }

    public class SensorIdInfo {
        public final int sensorId;
        public final double firstTS;
        public final String firstLabel, firstTogether;
        public final double lastTS;
        public final String lastLabel, lastTogether;

        public SensorIdInfo(int sensorId, SQLiteDatabase db, JsonParser parser, String[] labelArr){
            Cursor firstCursor = db.rawQuery("SELECT " + COLUMN_TIMESTAMP + ", " + COLUMN_VALUE
                    + " FROM " + DATA_TABLE.name + " LIMIT 1", null);
            firstCursor.moveToNext();
            double firstTS = firstCursor.getDouble(0);
            String firstLabel = SCDCKeys.LabelKeys.SLEEP_LABEL;
            String firstTogether = "혼자";
            JsonObject firstValue = parser.parse(firstCursor.getString(1)).getAsJsonObject();
            for (String label : labelArr){
                boolean thisLabel = firstValue.get(label).getAsBoolean();
                if (thisLabel){
                    firstLabel = label;
                }
            }
            boolean together = firstValue.get(SCDCKeys.LabelKeys.TOGETHER_STATUS).getAsBoolean();
            if (together){
                firstTogether = "함께";
            }
            firstCursor.close();

            Cursor lastCursor = db.rawQuery("SELECT " + COLUMN_TIMESTAMP + ", " + COLUMN_VALUE +
                    " FROM " + DATA_TABLE.name + " ORDER BY _id DESC LIMIT 1", null);
            lastCursor.moveToNext();
            double lastTS = lastCursor.getDouble(0);
            String lastLabel = SCDCKeys.LabelKeys.SLEEP_LABEL;
            String lastTogether = "혼자";
            JsonObject lastValue = parser.parse(lastCursor.getString(1)).getAsJsonObject();
            for (String label : labelArr){
                boolean thisLabel = lastValue.get(label).getAsBoolean();
                if (thisLabel){
                    lastLabel = label;
                }
            }
            together = lastValue.get(SCDCKeys.LabelKeys.TOGETHER_STATUS).getAsBoolean();
            if (together){
                lastTogether = "함께";
            }
            lastCursor.close();

            this.sensorId = sensorId;
            this.firstTS = firstTS;
            this.lastTS = lastTS;
            this.firstLabel = firstLabel;
            this.firstTogether = firstTogether;
            this.lastLabel = lastLabel;
            this.lastTogether = lastTogether;
        }   
    }
}
