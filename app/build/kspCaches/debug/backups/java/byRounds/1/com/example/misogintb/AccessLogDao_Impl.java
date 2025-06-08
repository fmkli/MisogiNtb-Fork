package com.example.misogintb;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AccessLogDao_Impl implements AccessLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AccessLog> __insertionAdapterOfAccessLog;

  private final EntityDeletionOrUpdateAdapter<AccessLog> __updateAdapterOfAccessLog;

  public AccessLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAccessLog = new EntityInsertionAdapter<AccessLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `access_log` (`id`,`card_id_fk`,`entry_time_millis`,`exit_time_millis`,`amount_deducted_in_fen`,`is_inside`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AccessLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCardIdFk());
        statement.bindLong(3, entity.getEntryTimeMillis());
        if (entity.getExitTimeMillis() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getExitTimeMillis());
        }
        if (entity.getAmountDeductedInFen() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getAmountDeductedInFen());
        }
        final int _tmp = entity.isInside() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__updateAdapterOfAccessLog = new EntityDeletionOrUpdateAdapter<AccessLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `access_log` SET `id` = ?,`card_id_fk` = ?,`entry_time_millis` = ?,`exit_time_millis` = ?,`amount_deducted_in_fen` = ?,`is_inside` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AccessLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCardIdFk());
        statement.bindLong(3, entity.getEntryTimeMillis());
        if (entity.getExitTimeMillis() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getExitTimeMillis());
        }
        if (entity.getAmountDeductedInFen() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getAmountDeductedInFen());
        }
        final int _tmp = entity.isInside() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getId());
      }
    };
  }

  @Override
  public Object insertLog(final AccessLog accessLog, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAccessLog.insertAndReturnId(accessLog);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLog(final AccessLog accessLog, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAccessLog.handle(accessLog);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatestUnexitedLog(final String cardId,
      final Continuation<? super AccessLog> $completion) {
    final String _sql = "SELECT * FROM access_log WHERE card_id_fk = ? AND is_inside = 1 ORDER BY entry_time_millis DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cardId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AccessLog>() {
      @Override
      @Nullable
      public AccessLog call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCardIdFk = CursorUtil.getColumnIndexOrThrow(_cursor, "card_id_fk");
          final int _cursorIndexOfEntryTimeMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "entry_time_millis");
          final int _cursorIndexOfExitTimeMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "exit_time_millis");
          final int _cursorIndexOfAmountDeductedInFen = CursorUtil.getColumnIndexOrThrow(_cursor, "amount_deducted_in_fen");
          final int _cursorIndexOfIsInside = CursorUtil.getColumnIndexOrThrow(_cursor, "is_inside");
          final AccessLog _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpCardIdFk;
            _tmpCardIdFk = _cursor.getString(_cursorIndexOfCardIdFk);
            final long _tmpEntryTimeMillis;
            _tmpEntryTimeMillis = _cursor.getLong(_cursorIndexOfEntryTimeMillis);
            final Long _tmpExitTimeMillis;
            if (_cursor.isNull(_cursorIndexOfExitTimeMillis)) {
              _tmpExitTimeMillis = null;
            } else {
              _tmpExitTimeMillis = _cursor.getLong(_cursorIndexOfExitTimeMillis);
            }
            final Integer _tmpAmountDeductedInFen;
            if (_cursor.isNull(_cursorIndexOfAmountDeductedInFen)) {
              _tmpAmountDeductedInFen = null;
            } else {
              _tmpAmountDeductedInFen = _cursor.getInt(_cursorIndexOfAmountDeductedInFen);
            }
            final boolean _tmpIsInside;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsInside);
            _tmpIsInside = _tmp != 0;
            _result = new AccessLog(_tmpId,_tmpCardIdFk,_tmpEntryTimeMillis,_tmpExitTimeMillis,_tmpAmountDeductedInFen,_tmpIsInside);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllLogsForCard(final String cardId,
      final Continuation<? super List<AccessLog>> $completion) {
    final String _sql = "SELECT * FROM access_log WHERE card_id_fk = ? ORDER BY entry_time_millis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cardId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AccessLog>>() {
      @Override
      @NonNull
      public List<AccessLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCardIdFk = CursorUtil.getColumnIndexOrThrow(_cursor, "card_id_fk");
          final int _cursorIndexOfEntryTimeMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "entry_time_millis");
          final int _cursorIndexOfExitTimeMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "exit_time_millis");
          final int _cursorIndexOfAmountDeductedInFen = CursorUtil.getColumnIndexOrThrow(_cursor, "amount_deducted_in_fen");
          final int _cursorIndexOfIsInside = CursorUtil.getColumnIndexOrThrow(_cursor, "is_inside");
          final List<AccessLog> _result = new ArrayList<AccessLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AccessLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpCardIdFk;
            _tmpCardIdFk = _cursor.getString(_cursorIndexOfCardIdFk);
            final long _tmpEntryTimeMillis;
            _tmpEntryTimeMillis = _cursor.getLong(_cursorIndexOfEntryTimeMillis);
            final Long _tmpExitTimeMillis;
            if (_cursor.isNull(_cursorIndexOfExitTimeMillis)) {
              _tmpExitTimeMillis = null;
            } else {
              _tmpExitTimeMillis = _cursor.getLong(_cursorIndexOfExitTimeMillis);
            }
            final Integer _tmpAmountDeductedInFen;
            if (_cursor.isNull(_cursorIndexOfAmountDeductedInFen)) {
              _tmpAmountDeductedInFen = null;
            } else {
              _tmpAmountDeductedInFen = _cursor.getInt(_cursorIndexOfAmountDeductedInFen);
            }
            final boolean _tmpIsInside;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsInside);
            _tmpIsInside = _tmp != 0;
            _item = new AccessLog(_tmpId,_tmpCardIdFk,_tmpEntryTimeMillis,_tmpExitTimeMillis,_tmpAmountDeductedInFen,_tmpIsInside);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
