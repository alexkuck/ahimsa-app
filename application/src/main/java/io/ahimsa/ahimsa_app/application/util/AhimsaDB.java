package io.ahimsa.ahimsa_app.application.util;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.ahimsa.ahimsa_app.application.MainApplication;

/**
 * Created by askuck on 6/16/14.
 */
public class AhimsaDB {

    public static final String PENDING = "pending";
    public static final String DISTRIBUTED = "distributed";
    public static final String CONFIRMED = "confirmed";

    private static final String db_name = "ahimsa.db";
    private static final String db_table = "wallet_outpoints";

    private static final String CREATE = String.format("CREATE TABLE IF NOT EXISTS %s " +
            "(txid STRING PRIMARY KEY, vout INTEGER, value INTEGER, status VARCHAR(11), spent BOOLEAN," +
            " CHECK (status IN ('pending', 'distributed', 'confirmed')), CHECK (spent IN (0, 1)) );", db_table);

    MainApplication application;
    SQLiteDatabase db;

    public AhimsaDB(MainApplication _application){
        this.application = _application;
        initialize();
    }

    private void initialize(){
        db = application.openOrCreateDatabase(db_name, Context.MODE_PRIVATE, null);
        db.execSQL(CREATE);
    }

    public void addTx(String txid, int vout, BigInteger value, String status, boolean spent) throws Exception {
        if( !checkTxid(txid) && !checkVout(vout) && !checkValue(value) && !checkStatus(status))
            throw new Exception("Invalid parameters");

        //todo: sanitize
        String INSERT = String.format(
                "INSERT INTO %s (txid, vout, value, status, spent) " +
                "VALUES('%s', %d, %d, '%s', %d);",
                db_table, txid, vout, value.longValue(), status, (spent) ? 1 : 0);

        try{
            db.execSQL(INSERT);
        } catch (SQLException e) {
            throw e;
        }
    }


    public List<Utils.DbTxOutpoint> getUnspent(){
        String UNSPENT = String.format("SELECT * FROM %s WHERE spent == 0", db_table);
        ArrayList<Utils.DbTxOutpoint> result = new ArrayList<Utils.DbTxOutpoint>();
        Cursor cursor = db.rawQuery(UNSPENT, null);

        while(cursor.moveToNext()){
            result.add( new Utils.DbTxOutpoint(cursor.getString(0), cursor.getInt(1), cursor.getInt(2)) );
        }

        return result;
    }

    public BigInteger getBalance(){

        //todo: sanitize
        String SUM = String.format("SELECT SUM(value) FROM %s WHERE (status == 'distributed' OR status == 'confirmed') AND spent == 0;", db_table);

        Cursor cursor = db.rawQuery(SUM, null);
        if (cursor.moveToFirst()) {
            return BigInteger.valueOf((long) cursor.getInt(0));
        }

        return BigInteger.ZERO;

    }

    public void setStatus(String txid, String status) throws Exception {
//        if( !checkTxid(txid) && !checkStatus(status))
//            throw new Exception("Invalid parameters");

        //todo: sanitize
        String UPDATE = String.format("UPDATE %s SET status = '%s' WHERE txid = '%s';", db_table, status, txid);

        try{
            db.execSQL(UPDATE);
        } catch (SQLException e) {
            throw e;
        }

    }

    public void setSpent(String txid, boolean spent) throws Exception {
//        if( !checkTxid(txid) )
//            throw new Exception("Invalid parameters");

        //todo: sanitize
        String UPDATE = String.format("UPDATE %s SET spent = %d WHERE txid = '%s';", db_table, (spent) ? 1 : 0, txid);

        try{
            db.execSQL(UPDATE);
        } catch (SQLException e) {
            throw e;
        }

    }

    //--------------------------------------------------------------------------------
    private boolean checkTxid(String txid){
        if(txid.length() != 64)
            return true;
        else
            return false;
    }

    private boolean checkVout(int vout){
        if(vout < 0)
            return true;
        else
            return false;
    }

    private boolean checkValue(BigInteger val){
        if( val.compareTo(BigInteger.ZERO) == -1 )
            return false;
        else
            return true;
    }

    private boolean checkStatus(String status){
        if(status == "pending" || status == "distributed" || status ==  "confirmed")
            return true;
        else
            return false;
    }
    //-------------------------------------------------------------------------------
    public void reset(){
        application.deleteDatabase(db_name);
        initialize();
    }

    public String toString(){
        String SUM = String.format("SELECT * FROM %s;", db_table);
        String result = "";

        Cursor cursor = db.rawQuery(SUM, null);
        while (cursor.moveToNext()) {
            result += DatabaseUtils.dumpCurrentRowToString(cursor) + "\n";
        }

        return result;

    }


}
