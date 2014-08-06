package io.ahimsa.ahimsa_app.core;

import android.util.Log;

import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Peer;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * Created by askuck on 8/4/14.
 */
public class MyDownloadListener extends AbstractPeerEventListener
{
    public static final String TAG = "MyDownloadListener";
    private int originalBlocksLeft = -1;
    private int lastPercent = 0;
    private Semaphore done = new Semaphore(0);
    private boolean caughtUp = false;

    public void onChainDownloadStarted(Peer peer, int blocksLeft) {
        Log.d(TAG, String.format("onChainDownloadStarted | peer: %s, blocksLeft: %s", peer.toString(), blocksLeft));
        startDownload(blocksLeft);
        originalBlocksLeft = blocksLeft;
        if (blocksLeft == 0) {
            doneDownload();
            done.release();
        }
    }

    public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
        Log.d(TAG, "blocksLeft: " + blocksLeft);

        if (caughtUp)
            return;

        if (blocksLeft == 0) {
            caughtUp = true;
            doneDownload();
            done.release();
        }

        if (blocksLeft < 0 || originalBlocksLeft <= 0)
            return;

        double pct = 100.0 - (100.0 * (blocksLeft / (double) originalBlocksLeft));
        if ((int) pct != lastPercent) {
            progress(pct, blocksLeft, new Date(block.getTimeSeconds() * 1000));
            lastPercent = (int) pct;
        }
    }

    /**
     * Called when download progress is made.
     *
     * @param pct  the percentage of chain downloaded, estimated
     * @param date the date of the last block downloaded
     */
    protected void progress(double pct, int blocksSoFar, Date date) {
        Log.d(TAG, String.format("Chain download %d%% done with %d blocks to go, block date %s", (int) pct,
                blocksSoFar, DateFormat.getDateTimeInstance().format(date)));
    }

    /**
     * Called when download is initiated.
     *
     * @param blocks the number of blocks to download, estimated
     */
    protected void startDownload(int blocks) {
        Log.d(TAG, "startDownload, blocks remaining: " + blocks);
        if (blocks > 0)
            Log.d(TAG, "Downloading block chain of size " + blocks + ". " +
                    (blocks > 1000 ? "This may take a while." : ""));
    }

    /**
     * Called when we are done downloading the block chain.
     */
    protected void doneDownload() {
        Log.d(TAG, "doneDownload()");
    }

    /**
     * Wait for the chain to be downloaded.
     */
    public void await() throws InterruptedException {
        Log.d(TAG, "await()");
        done.acquire();
    }


}
