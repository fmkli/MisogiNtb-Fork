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
public final class CardInfoDao_Impl implements CardInfoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CardInfo> __insertionAdapterOfCardInfo;

  private final EntityDeletionOrUpdateAdapter<CardInfo> __updateAdapterOfCardInfo;

  public CardInfoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCardInfo = new EntityInsertionAdapter<CardInfo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `card_info` (`cardId`,`balance_in_fen`,`status`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CardInfo entity) {
        statement.bindString(1, entity.getCardId());
        statement.bindLong(2, entity.getBalanceInFen());
        statement.bindString(3, entity.getStatus());
      }
    };
    this.__updateAdapterOfCardInfo = new EntityDeletionOrUpdateAdapter<CardInfo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `card_info` SET `cardId` = ?,`balance_in_fen` = ?,`status` = ? WHERE `cardId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CardInfo entity) {
        statement.bindString(1, entity.getCardId());
        statement.bindLong(2, entity.getBalanceInFen());
        statement.bindString(3, entity.getStatus());
        statement.bindString(4, entity.getCardId());
      }
    };
  }

  @Override
  public Object insertCard(final CardInfo cardInfo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCardInfo.insert(cardInfo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCard(final CardInfo cardInfo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCardInfo.handle(cardInfo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getCard(final String cardId, final Continuation<? super CardInfo> $completion) {
    final String _sql = "SELECT * FROM card_info WHERE cardId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cardId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CardInfo>() {
      @Override
      @Nullable
      public CardInfo call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
          final int _cursorIndexOfBalanceInFen = CursorUtil.getColumnIndexOrThrow(_cursor, "balance_in_fen");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final CardInfo _result;
          if (_cursor.moveToFirst()) {
            final String _tmpCardId;
            _tmpCardId = _cursor.getString(_cursorIndexOfCardId);
            final int _tmpBalanceInFen;
            _tmpBalanceInFen = _cursor.getInt(_cursorIndexOfBalanceInFen);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _result = new CardInfo(_tmpCardId,_tmpBalanceInFen,_tmpStatus);
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
  public Object getAllCards(final Continuation<? super List<CardInfo>> $completion) {
    final String _sql = "SELECT * FROM card_info";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CardInfo>>() {
      @Override
      @NonNull
      public List<CardInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCardId = CursorUtil.getColumnIndexOrThrow(_cursor, "cardId");
          final int _cursorIndexOfBalanceInFen = CursorUtil.getColumnIndexOrThrow(_cursor, "balance_in_fen");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<CardInfo> _result = new ArrayList<CardInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CardInfo _item;
            final String _tmpCardId;
            _tmpCardId = _cursor.getString(_cursorIndexOfCardId);
            final int _tmpBalanceInFen;
            _tmpBalanceInFen = _cursor.getInt(_cursorIndexOfBalanceInFen);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item = new CardInfo(_tmpCardId,_tmpBalanceInFen,_tmpStatus);
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
