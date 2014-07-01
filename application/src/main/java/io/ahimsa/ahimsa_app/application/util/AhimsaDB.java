package io.ahimsa.ahimsa_app.application.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.ahimsa.ahimsa_app.application.Constants;
import io.ahimsa.ahimsa_app.application.MainApplication;

/**
 * Created by askuck on 6/16/14.
 */
public class AhimsaDB {

    private static final String db_name = "ahimsa.db";
    private static final String db_table_txouts = "txouts";
    private static final String db_table_transactions = "transactions";
    private static final String db_table_bulletins = "bulletins";

    private static final String CREATE_TXOUTS = String.format("CREATE TABLE IF NOT EXISTS %s " +
            "(txid STRING, vout INTEGER, value INTEGER, spent BOOLEAN, raw BLOB, PRIMARY KEY(txid, vout)," +
            " CHECK (spent IN (0, 1)), FOREIGN KEY(txid) REFERENCES transactions(txid) );", db_table_txouts);

    private static final String CREATE_TRANSACTIONS = String.format("CREATE TABLE IF NOT EXISTS %s " +
            "(txid STRING, raw BLOB, confirmed BOOLEAN, sent_time INTEGER, PRIMARY KEY(txid), CHECK (confirmed IN (0, 1)) );", db_table_transactions);

    private static final String CREATE_BULLETINS = String.format("CREATE TABLE IF NOT EXISTS %s " +
            "(txid STRING, topic STRING, message STRING, PRIMARY KEY(TXID));", db_table_bulletins);

    MainApplication application;
    SQLiteDatabase db;

    public AhimsaDB(MainApplication _application){
        this.application = _application;
        initialize();
    }

    private void initialize(){
        db = application.openOrCreateDatabase(db_name, Context.MODE_PRIVATE, null);
        db.execSQL(CREATE_TXOUTS);
        db.execSQL(CREATE_TRANSACTIONS);
        db.execSQL(CREATE_BULLETINS);

    }

    public void addTx(Transaction tx, boolean confirmed){
        ContentValues params = new ContentValues();
        params.put("txid", tx.getHashAsString());
        params.put("raw", tx.bitcoinSerialize());
        params.put("confirmed", confirmed);
        params.put("sent_time", System.currentTimeMillis()/1000);

        db.insertOrThrow(db_table_transactions, null, params);
    }

    public void addBulletin(String txid, String topic, String message){
        ContentValues params = new ContentValues();
        params.put("txid", txid);
        params.put("topic", topic);
        params.put("message", message);

        db.insertOrThrow(db_table_bulletins, null, params);
    }

    public void addTxOut(TransactionOutput out){
        Transaction parent = out.getParentTransaction();

        ContentValues params = new ContentValues();
        params.put("txid", parent.getHashAsString());
        params.put("vout", parent.getOutputs().indexOf(out));
        params.put("value", out.getValue().longValue());
        params.put("raw", out.bitcoinSerialize());
        params.put("spent", false);

        db.insertOrThrow(db_table_txouts, null, params);
    }

    public void confirmTx(String txid) {
        ContentValues param = new ContentValues();
        param.put("confirmed", true);

        String where = "txid == ?";
        String[] whereArgs = {txid};

        db.update(db_table_transactions, param, where, whereArgs);
    }

    public void setSpent(String txid, Long vout, boolean spent) {
        ContentValues param = new ContentValues();
        param.put("spent", spent);

        String where = "(txid == ? AND vout == CAST(? AS INTEGER))";
        String[] whereArgs = {txid, vout.toString()};

        db.update(db_table_txouts, param, where, whereArgs);
    }

    //----------------------------------------------------------------------------------------------
    public List<TransactionOutput> getUnspent(){
        //returns all unspent transactions

        String sql = "SELECT txouts.raw, transactions.raw FROM txouts JOIN transactions ON transactions.txid == txouts.txid " +
                     "WHERE transactions.confirmed == 1 AND txouts.spent == 0;";

        Cursor cursor = db.rawQuery(sql, null);
        ArrayList<TransactionOutput> result = new ArrayList<TransactionOutput>();

        while(cursor.moveToNext()){
            Transaction txFromDb = new Transaction(Constants.NETWORK_PARAMETERS, cursor.getBlob(1));
            // todo make efficient
            result.add( new TransactionOutput(Constants.NETWORK_PARAMETERS, txFromDb, cursor.getBlob(0), 0));
        }

        return result;
    }

    public BigInteger getBalance(){
        String SUM = String.format("SELECT SUM(txouts.value) FROM txouts JOIN transactions ON transactions.txid == txouts.txid " +
                                   "WHERE txouts.spent == 0 AND transactions.confirmed == 1;");

        Cursor cursor = db.rawQuery(SUM, null);
        if (cursor.moveToFirst()) {
            return BigInteger.valueOf((long) cursor.getInt(0));
        }

        return BigInteger.ZERO;
    }
    //----------------------------------------------------------------------------------------------
    public void reset(){
        application.deleteDatabase(db_name);
        initialize();
    }

    public String toString(){
        String db_outs = String.format("SELECT * FROM %s;", db_table_txouts);
        String db_txs = String.format("SELECT * FROM %s;", db_table_transactions);
        String db_bullet = String.format("SELECT * FROM %s;", db_table_bulletins);

        String result = "~~db_table_txouts~~\n";
        Cursor cursor_out = db.rawQuery(db_outs, null);
        while (cursor_out.moveToNext()) {
            result += DatabaseUtils.dumpCurrentRowToString(cursor_out) + "\n";
        }

        result += "~~db_table_transactions~~\n";
        Cursor cursor_txs = db.rawQuery(db_txs, null);
        while (cursor_txs.moveToNext()) {
            result += DatabaseUtils.dumpCurrentRowToString(cursor_txs) + "\n";
        }

        result += "~~db_table_bulletins~~\n";
        Cursor cursor_bullet = db.rawQuery(db_bullet, null);
        while (cursor_bullet.moveToNext()) {
            result += DatabaseUtils.dumpCurrentRowToString(cursor_bullet) + "\n";
        }

        return result;

    }
}
