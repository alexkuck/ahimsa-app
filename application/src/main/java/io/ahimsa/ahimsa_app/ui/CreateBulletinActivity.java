package io.ahimsa.ahimsa_app.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.bitcoin.core.Sha256Hash;
import com.google.common.io.ByteStreams;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.ahimsa.ahimsa_app.AhimsaApplication;
import io.ahimsa.ahimsa_app.Configuration;
import io.ahimsa.ahimsa_app.Constants;
import io.ahimsa.ahimsa_app.R;
import io.ahimsa.ahimsa_app.core.AhimsaService;
import io.ahimsa.ahimsa_app.core.AhimsaWallet;
import io.ahimsa.ahimsa_app.core.Utils;

public class CreateBulletinActivity extends Activity
{
    AhimsaApplication application;
    AhimsaWallet ahimwall;
    Configuration config;
    CreateBulletinFragment frag;

    String TAG = "CreateBulletinActivity";

    @Override
    public void onPause()
    {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        application = (AhimsaApplication) getApplication();
        ahimwall = application.getAhimsaWallet();
        config = application.getConfig();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_bulletin);

        if(savedInstanceState == null)
        {
            frag = CreateBulletinFragment.newInstance(application.getUpdateBundle());

            getFragmentManager().beginTransaction()
                    .add(R.id.container, frag)
                    .commit();
        }

        IntentFilter filter = new IntentFilter(Constants.ACTION_UPDATED_OVERVIEW);
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter);

    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if(Constants.ACTION_UPDATED_OVERVIEW.equals(action))
            {
                updateOverview();
            }
        }
    };

    private void updateOverview()
    {
        if(frag != null)
        {
            frag.updateView(application.getUpdateBundle());
        }
    }
//
//    @Override
//    public void finish() {
//        super.finish();
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_bulletin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_broadcast_bulletin)
        {
            Bundle bulletin_bundle = frag.getBulletinBundle();
            String topic   = bulletin_bundle.getString(frag.EXTRA_STRING_TOPIC);
            String message = bulletin_bundle.getString(frag.EXTRA_STRING_MESSAGE);

            Long estimate = Utils.getEstimatedCost(Constants.MIN_FEE, Constants.MIN_DUST, topic.length(), message.length());
            Log.d("createdBulletinActivity", "estimated cost: " + estimate);

            if(Constants.getStandardCoin() <= ahimwall.getConfirmedBalance(false))
            {
                AhimsaService.startBroadcastBulletin(this, topic, message, Constants.MIN_FEE);
                Toast.makeText(this, "Broadcast bulletin request.\ntopic: " + topic + "\nestimated cost: " + estimate + " Satoshis", Toast.LENGTH_LONG).show();
                finish();
            }
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Womp! Not enough coin.");
                builder.setMessage(String.format("%s confirmed Satoshis are required to create a bulletin. \n\nOur apologies, a future version will not have this limitation...", Constants.getStandardCoin()));
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
            return true;
        }

        if (id == R.id.action_camera)
        {
            dispatchTakePictureIntent2();
        }

        return super.onOptionsItemSelected(item);
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    String mCurrentPhotoPath;
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private Uri outputFileUri;
    private void dispatchTakePictureIntent2() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                outputFileUri = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        outputFileUri);

                // This intent presents applications that allow the user to choose a picture
                Intent pickIntent = new Intent();
                pickIntent.setType("image/*");
                pickIntent.setAction(Intent.ACTION_GET_CONTENT);

                // This intent prompts the user to choose from a list of possible intents.
                String pickTitle = "Select or Take a new Picture";
                Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
                chooserIntent.putExtra
                        (
                                Intent.EXTRA_INITIAL_INTENTS,
                                new Intent[] { takePictureIntent }
                        );

                startActivityForResult(chooserIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            if(requestCode == REQUEST_TAKE_PHOTO)
            {
                final boolean isCamera;
                if(data == null)
                {
                    isCamera = true;
                }
                else
                {
                    final String action = data.getAction();
                    if(action == null)
                    {
                        isCamera = false;
                    }
                    else
                    {
                        isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                }

                Uri selectedImageUri;
                if(isCamera)
                {
                    selectedImageUri = outputFileUri;
                }
                else
                {
                    selectedImageUri = data == null ? null : data.getData();
                }

                try
                {
                    printHash(selectedImageUri);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void printHash(Uri fileUri) throws IOException
    {
        String path = getPath(this, fileUri);
        Log.d(TAG, "Uri path | " + path);
        FileInputStream in = new FileInputStream(path);
        final byte[] array = ByteStreams.toByteArray(in);
        in.close();

        new uploadImageTask().execute(array);
    }

    private class uploadImageTask extends AsyncTask<byte[], Void, String>
    {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected String doInBackground(byte[]... data)
        {
            Sha256Hash hash = Sha256Hash.create(data[0]);
            Log.d(TAG, "hash | " + hash.toString());

            String hash64 = Base64.encodeToString(hash.getBytes(), Base64.URL_SAFE);
            Log.d(TAG, "hash64 | " + hash64);

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://img.ahimsa.io/upload");

            int code = -1;

            try {
                // Add your data
                httppost.addHeader(HTTP.CONTENT_TYPE, "image/jpeg");
                // "ContentLength" automatically generated for post request
                ByteArrayEntity entity = new ByteArrayEntity(data[0]);
                httppost.setEntity(entity);

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                code = response.getStatusLine().getStatusCode();
                Log.d(TAG, "HttpResponse status code | " + code);

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            if(code == 201 || code == 409)
            {
                return hash64;
            }

            return null;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(String hash64)
        {
            if(hash64 != null)
            {
                frag.addEntityHash(hash64);
            }
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
