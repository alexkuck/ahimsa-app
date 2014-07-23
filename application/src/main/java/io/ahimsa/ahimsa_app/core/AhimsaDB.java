package io.ahimsa.ahimsa_app.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.AhimsaApplication;

/**
 * Created by askuck on 6/16/14.
 */
public class AhimsaDB {

    private static final String db_name = "ahimsa.db";
    public static final String db_table_txouts = "txouts";
    public static final String db_table_transactions = "transactions";
    public static final String db_table_bulletins = "bulletins";

    public static final String txid = "txid";
    public static final String raw = "raw";
    public static final String confirmed = "confirmed";
    public static final String sent_time = "sent_time";
    public static final String highest_block = "highest_block";
    public static final String topic = "topic";
    public static final String message = "message";
    public static final String fee = "fee";
    public static final String vout = "vout";
    public static final String value = "value";
    public static final String status = "status";
    public static final String unspent = "unspent";
    public static final String spent = "spent";
    public static final String pending = "pending";

    AhimsaApplication application;
    SQLiteDatabase db;

    public AhimsaDB(AhimsaApplication application){
        this.application = application;
        initialize();
    }

    private void initialize(){
        final String CREATE_TXOUTS = String.format("CREATE TABLE IF NOT EXISTS %s " +
                "(txid STRING, vout INTEGER, value INTEGER, status VARCHAR(7), raw BLOB, PRIMARY KEY(txid, vout)," +
                " CHECK (status IN ('unspent', 'spent', 'pending')), FOREIGN KEY(txid) REFERENCES transactions(txid) );", db_table_txouts);

        final String CREATE_TRANSACTIONS = String.format("CREATE TABLE IF NOT EXISTS %s " +
                "(txid STRING, raw BLOB, sent_time INTEGER, confirmed BOOLEAN, highest_block INTEGER, PRIMARY KEY(txid), CHECK (confirmed IN (0, 1)) );", db_table_transactions);

        final String CREATE_BULLETINS = String.format("CREATE TABLE IF NOT EXISTS %s " +
                "(txid STRING, topic STRING, message STRING, fee INTEGER, PRIMARY KEY(TXID));", db_table_bulletins);

        db = application.openOrCreateDatabase(db_name, Context.MODE_PRIVATE, null);
        db.execSQL(CREATE_TXOUTS);
        db.execSQL(CREATE_TRANSACTIONS);
        db.execSQL(CREATE_BULLETINS);

    }

    public void addTx(Transaction tx, boolean tx_confirmed, @Nullable Long tx_highest_block){
        ContentValues params = new ContentValues();
        params.put(this.txid, tx.getHashAsString());
        params.put(this.raw, tx.bitcoinSerialize());
        params.put(this.sent_time, System.currentTimeMillis()/1000);
        params.put(this.confirmed, tx_confirmed);

        if(tx_highest_block == null) {
            params.putNull(highest_block);
        } else {
            params.put(highest_block, tx_highest_block);
        }

        db.insertOrThrow(db_table_transactions, null, params);
    }

    public void addBulletin(String txid, String topic, String message, Long fee){
        ContentValues params = new ContentValues();
        params.put(this.txid, txid);
        params.put(this.topic, topic);
        params.put(this.message, message);
        params.put(this.fee, fee);

        db.insertOrThrow(db_table_bulletins, null, params);
    }

    public void addTxOut(TransactionOutput out){
        Transaction parent = out.getParentTransaction();

        ContentValues params = new ContentValues();
        params.put(this.txid, parent.getHashAsString());
        params.put(this.vout, parent.getOutputs().indexOf(out));
        params.put(this.value, out.getValue().longValue());
        params.put(this.raw, out.bitcoinSerialize());
        params.put(this.status, this.unspent);

        db.insertOrThrow(db_table_txouts, null, params);
    }

    public void confirmTx(String txid) {
        ContentValues param = new ContentValues();
        param.put(this.confirmed, true);

        String where = this.txid + " == ?";
        String[] whereArgs = {txid};

        db.update(db_table_transactions, param, where, whereArgs);
    }

    public void setStatusSpent(String txid, Long vout) {
        setStatus(txid, vout, this.spent);
    }

    public void setStatusUnspent(String txid, Long vout) {
        setStatus(txid, vout, this.unspent);
    }

    public void setStatusPending(String txid, Long vout) {
        setStatus(txid, vout, this.pending);
    }

    private void setStatus(String txid, Long vout, String status) {
        ContentValues param = new ContentValues();
        param.put(this.status, status);

        String where = "(txid == ? AND vout == CAST(? AS INTEGER))";
        String[] whereArgs = {txid, vout.toString()};

        db.update(db_table_txouts, param, where, whereArgs);
    }

    //----------------------------------------------------------------------------------------------
    public boolean hasTransaction(Transaction tx){
        return hasTransaction(tx.getHashAsString());
    }

    private boolean hasTransaction(String txid_arg){
        String sql = String.format("SELECT txid FROM %s WHERE txid = ?;", db_table_transactions);
        String[] args = {txid_arg};
        Cursor cursor = db.rawQuery(sql, args);

        if(cursor.getCount() == 0){
            return false;
        }
        return true;
    }

    public Transaction getTransaction(String txid){
        //todo necessary?
        //regardless, should implement..

        return null;
    }
    //----------------------------------------------------------------------------------------------
    // todo add pending functionality. you can do it!

    public void reserveTxOuts(Long min_coin_necessary){
        String sql = "SELECT txouts.txid, txouts.vout, txouts.value FROM txouts JOIN transactions ON transactions.txid == txouts.txid" +
                "WHERE transactions.confirmed == 1 AND txouts.status == 'unspent' ORDER BY txouts.value DESC;";

        Long min = min_coin_necessary;
        Long bal = new Long(0);

        Cursor cursor = db.rawQuery(sql, null);
        while(cursor.moveToNext()) {
            // 0~txid (string) | 1~vout (integer) | 2~value (integer)
            if(bal >= min) {
                break;
            }
            bal += cursor.getLong(2);
            setStatusPending(cursor.getString(0), cursor.getLong(1));
        }
    }

    public void unreserveTxOuts(Long min_coin_necessary){
        String sql = "SELECT txouts.txid, txouts.vout, txouts.value FROM txouts JOIN transactions ON transactions.txid == txouts.txid" +
                "WHERE transactions.confirmed == 1 AND txouts.status == 'pending' ORDER BY txouts.value ASC";

        Long min = min_coin_necessary;
        Long bal = new Long(0);

        Cursor cursor = db.rawQuery(sql, null);
        while(cursor.moveToNext()) {
            // 0~txid (string) | 1~vout (integer) | 2~value (integer)
            if(bal >= min) {
                break;
            }
            bal += cursor.getLong(2);
            setStatusUnspent(cursor.getString(0), cursor.getLong(1));
        }
    }

    public List<TransactionOutput> getUnspentOutputs(Long min_coin_necessary){
        //get unspent transactions sorted in descending order
        List<TransactionOutput> db_unspent = getAllUnspentOutputs();
        BigInteger min = BigInteger.valueOf(min_coin_necessary);

        ArrayList<TransactionOutput> unspents = new ArrayList<TransactionOutput>();
        BigInteger bal = BigInteger.ZERO;

        for(TransactionOutput out : db_unspent){
            if(bal.compareTo(min) >= 0)
                break;

            unspents.add(out);
            bal = bal.add(out.getValue());
        }

        return unspents;
    }

    //----------------------------------------------------
    public List<TransactionOutput> getAllUnspentOutputs(){
        //return all confirmed and unspent/pending transaction outputs in descending order by value

        String sql = "SELECT txouts.raw, transactions.raw FROM txouts JOIN transactions ON transactions.txid == txouts.txid " +
                "WHERE transactions.confirmed == 1 AND (txouts.status == 'unspent' OR txouts.status == 'pending') ORDER BY txouts.value DESC;";

        Cursor cursor = db.rawQuery(sql, null);
        ArrayList<TransactionOutput> result = new ArrayList<TransactionOutput>();

        while(cursor.moveToNext()){
            Transaction txFromDb = new Transaction(Constants.NETWORK_PARAMETERS, cursor.getBlob(1));
            result.add( new TransactionOutput(Constants.NETWORK_PARAMETERS, txFromDb, cursor.getBlob(0), 0));
        }

        return result;
    }

    public BigInteger getConfirmedBalance(boolean include_pending){
        String SUM;
        if(include_pending){
            SUM = String.format("SELECT SUM(txouts.value) FROM txouts JOIN transactions ON transactions.txid == txouts.txid " +
                    "WHERE (txouts.status == 'unspent' OR txouts.status == 'pending') AND transactions.confirmed == 1;");
        } else {
            SUM = String.format("SELECT SUM(txouts.value) FROM txouts JOIN transactions ON transactions.txid == txouts.txid " +
                    "WHERE (txouts.status == 'unspent') AND transactions.confirmed == 1;");
        }

        Cursor cursor = db.rawQuery(SUM, null);
        if (cursor.moveToFirst()) {
            return BigInteger.valueOf((long) cursor.getInt(0));
        }

        return BigInteger.ZERO;
    }

    public BigInteger getUnconfirmedBalance(){
        String SUM = String.format("SELECT SUM(txouts.value) FROM txouts JOIN transactions ON transactions.txid == txouts.txid " +
                "WHERE (txouts.status == 'unspent' OR txouts.status == 'pending') AND transactions.confirmed == 0;");

        Cursor cursor = db.rawQuery(SUM, null);
        if (cursor.moveToFirst()) {
            return BigInteger.valueOf((long) cursor.getInt(0));
        }

        return BigInteger.ZERO;
    }

    //----------------------------------------------------------------------------------------------
    public Cursor getTransaction(){
        String ALL_TXS = String.format("SELECT rowid _id,* FROM %s;", db_table_transactions);
        Cursor cursor = db.rawQuery(ALL_TXS, null);
        return cursor;
    }

    public Cursor getBulletins(){
        String ALL_BULLETINS = String.format("SELECT rowid _id,* FROM %s", db_table_bulletins);
        Cursor cursor = db.rawQuery(ALL_BULLETINS, null);
        return cursor;
    }

    public Cursor getTransactionOutputs(){
        String ALL_TX_OUTPUTS = String.format("SELECT rowid _id,* FROM %s", db_table_txouts);
        Cursor cursor = db.rawQuery(ALL_TX_OUTPUTS, null);
        return cursor;
    }

    public Cursor getConfirmedTxs() {
        String ALL_CONFIRMED_TXS = String.format("SELECT * FROM %s WHERE confirmed == 1;", db_table_transactions);

        Cursor cursor = db.rawQuery(ALL_CONFIRMED_TXS, null);
        return cursor;
    }

    public Cursor getUnconfirmedTxs() {
        String ALL_UNCONFIRMED_TXS = String.format("SELECT * FROM %s WHERE confirmed == 0;", db_table_transactions);

        Cursor cursor = db.rawQuery(ALL_UNCONFIRMED_TXS, null);
        return cursor;
    }

    public Cursor getDraftTx() {
        String ALL_DRAFTS = String.format("SELECT * FROM %s WHERE txid NOT IN (SELECT txid FROM %s);", db_table_bulletins, db_table_transactions);
        Cursor cursor = db.rawQuery(ALL_DRAFTS, null);
        return cursor;
    }

    public Cursor getConfirmedAndUnspentTxOuts() {
        String ALL_CONF_AND_UNSP_TXOUTS = String.format("SELECT * FROM txouts JOIN transactions " +
                "ON transactions.txid == txouts.txid WHERE txouts.status == 'unspent' AND transactions.confirmed == 1");
        Cursor cursor = db.rawQuery(ALL_CONF_AND_UNSP_TXOUTS, null);
        return cursor;
    }

    public Cursor getUnconfirmedAndUnspentTxOuts() {
        String ALL_UNCONF_AND_UNSP_TXOUTS = String.format("SELECT * FROM txouts JOIN transactions " +
                "ON transactions.txid == txouts.txid WHERE txouts.status == 'unspent' AND transactions.confirmed == 0");
        Cursor cursor = db.rawQuery(ALL_UNCONF_AND_UNSP_TXOUTS, null);
        return cursor;
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
