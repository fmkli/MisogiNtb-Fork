package com.example.misogintb;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile CardInfoDao _cardInfoDao;

  private volatile AccessLogDao _accessLogDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `card_info` (`cardId` TEXT NOT NULL, `balance_in_fen` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`cardId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `access_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `card_id_fk` TEXT NOT NULL, `entry_time_millis` INTEGER NOT NULL, `exit_time_millis` INTEGER, `amount_deducted_in_fen` INTEGER, `is_inside` INTEGER NOT NULL, FOREIGN KEY(`card_id_fk`) REFERENCES `card_info`(`cardId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_access_log_card_id_fk` ON `access_log` (`card_id_fk`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '97ba74e6218635b21213ba5de76915b7')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `card_info`");
        db.execSQL("DROP TABLE IF EXISTS `access_log`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCardInfo = new HashMap<String, TableInfo.Column>(3);
        _columnsCardInfo.put("cardId", new TableInfo.Column("cardId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCardInfo.put("balance_in_fen", new TableInfo.Column("balance_in_fen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCardInfo.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCardInfo = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCardInfo = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCardInfo = new TableInfo("card_info", _columnsCardInfo, _foreignKeysCardInfo, _indicesCardInfo);
        final TableInfo _existingCardInfo = TableInfo.read(db, "card_info");
        if (!_infoCardInfo.equals(_existingCardInfo)) {
          return new RoomOpenHelper.ValidationResult(false, "card_info(com.example.misogintb.CardInfo).\n"
                  + " Expected:\n" + _infoCardInfo + "\n"
                  + " Found:\n" + _existingCardInfo);
        }
        final HashMap<String, TableInfo.Column> _columnsAccessLog = new HashMap<String, TableInfo.Column>(6);
        _columnsAccessLog.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccessLog.put("card_id_fk", new TableInfo.Column("card_id_fk", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccessLog.put("entry_time_millis", new TableInfo.Column("entry_time_millis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccessLog.put("exit_time_millis", new TableInfo.Column("exit_time_millis", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccessLog.put("amount_deducted_in_fen", new TableInfo.Column("amount_deducted_in_fen", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccessLog.put("is_inside", new TableInfo.Column("is_inside", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAccessLog = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysAccessLog.add(new TableInfo.ForeignKey("card_info", "CASCADE", "NO ACTION", Arrays.asList("card_id_fk"), Arrays.asList("cardId")));
        final HashSet<TableInfo.Index> _indicesAccessLog = new HashSet<TableInfo.Index>(1);
        _indicesAccessLog.add(new TableInfo.Index("index_access_log_card_id_fk", false, Arrays.asList("card_id_fk"), Arrays.asList("ASC")));
        final TableInfo _infoAccessLog = new TableInfo("access_log", _columnsAccessLog, _foreignKeysAccessLog, _indicesAccessLog);
        final TableInfo _existingAccessLog = TableInfo.read(db, "access_log");
        if (!_infoAccessLog.equals(_existingAccessLog)) {
          return new RoomOpenHelper.ValidationResult(false, "access_log(com.example.misogintb.AccessLog).\n"
                  + " Expected:\n" + _infoAccessLog + "\n"
                  + " Found:\n" + _existingAccessLog);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "97ba74e6218635b21213ba5de76915b7", "129385321eb8450d6dde56b9b930beb0");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "card_info","access_log");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `card_info`");
      _db.execSQL("DELETE FROM `access_log`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CardInfoDao.class, CardInfoDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AccessLogDao.class, AccessLogDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CardInfoDao cardInfoDao() {
    if (_cardInfoDao != null) {
      return _cardInfoDao;
    } else {
      synchronized(this) {
        if(_cardInfoDao == null) {
          _cardInfoDao = new CardInfoDao_Impl(this);
        }
        return _cardInfoDao;
      }
    }
  }

  @Override
  public AccessLogDao accessLogDao() {
    if (_accessLogDao != null) {
      return _accessLogDao;
    } else {
      synchronized(this) {
        if(_accessLogDao == null) {
          _accessLogDao = new AccessLogDao_Impl(this);
        }
        return _accessLogDao;
      }
    }
  }
}
