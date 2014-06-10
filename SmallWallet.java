package com.google.bitcoin.examples;

import com.google.bitcoin.core.*;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.SPVBlockStore;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.concurrent.Future;

public class SmallWallet {
    // Not really a wallet, pretty much everything else but the wallet actually
    public PeerGroup peerGroup;
    // there is an Simplified Payment Verification block store behind this
    // that just stores the block headers
    public BlockChain chain; 
    public Wallet wallet;

    public static void main(String[] args) throws Exception {
        wallet = new Wallet(params);
        wallet.addKey(new ECKey());

        SmallWallet ourWallet = new SmallWallet();
        ourWallet.createBlockChain(wallet);

        List<Transactions> fundingTxs = ourWallet.findTxInBlock(271734, wallet);
  }

   public void SmallWallet() {
        peerGroup = new PeerGroup();
        peerGroup.waitForPeers(1).get();
    }

    public void sendTransaction(tx Transaction) {
        // Sends a transaction and waits for connected nodes to respond

        // This should be run with some sort of timeout mechanism.
        ListenableFuture<Transaction> future = peerGroup.broadcastTransaction(tx, 6);
        future.get();
    }

    public BlockChain createBlockChain(Wallet wallet) {
        // Makes a blockchain, a peergroup and a SPV blockstore.
        // Requires a file named checkpoint.
        // Some stub code to download the blockchain.
        
        // Initialize everything
        NetworkParameters params = TestNet3Params.get();

        File chainFile = new File("iholdblockheaders");
        boolean chainExistedAlready = chainFile.exists();
        blockStore = new SPVBlockStore(params, chainFile);
        if (!chainExistedAlready) {
            File checkpointsFile = new File("checkpoints");    // Replace path to the file here.
            FileInputStream stream = new FileInputStream(checkpointsFile);
            CheckpointManager.checkpoint(params, stream, blockStore, wallet.getEarliestKeyCreationTime());
        }

        BlockChain chain = new BlockChain(params, wallet, blockStore);
        peerGroup = new PeerGroup(params, chain);

        peerGroup.addWallet(wallet);

        // Typically Bloom filters are used to lower the number of tx's sent to a device.
        // In the case where we just broadcast a transaction and want to see it,
        // it is essential that we do not leak any information about the relationship
        // between the node and that broadcast.
        peerGroup.setBloomFilterFalsePositiveRate(1.0);
        peerGroup.startAndWait();

        peerGroup.addAddress(InetAddress.getByName("54.83.21.194"));

        peerGroup.waitForPeers(1).get();


        peerGroup.downloadBlockChain();

        //System.out.println(peerGroup.getConnectedPeers().toString() + " : " + peerGroup.getDownloadPeer().toString());
        //System.out.println("Downloaded the spv chain:");
        //System.out.println(blockStore.getChainHead());
        return chain;

    }

    public static StoredBlock getBlock(int height, BlockStore store) {
        // Finds a block at a given height in a blockstore
        // If we are using a checkpointed SPV this may fail if you go too far back
        try {
            StoredBlock curr = store.getChainHead(); 
            if (height > curr.getHeight()) {
                return null;
            }
            for (int i = 0; i >= height; i -= 1) {
                StoredBlock next = store.get(curr.getHeader().getPrevBlockHash());
                curr = next;
            } 
            return curr;
        } catch (BlockStoreException e) {
            System.out.println("Height: " + height.toString());
            System.out.println("That block is not in the block store");
            return null;
        }
    }

    public List<Transaction> findTxInBlock(int blockHeight, Wallet wallet) {
        // Returns all of the funding transactions in the block at this height
        StoredBlock targetBlock = getBlock(blockHeight, store);
        Sha256Hash hash = targetBlock.getHash();
        // we must download the full block from the network
        Peer peer = peerGroup.getConnectedPeers().get(0);
        Block actualBlock = peer.getBlock(hash).get();
        
        List<Transaction> foundtxs = new ArrayList<Transaction>;
        for (Transaction tx : actualBlock.getTransactions()) {
           if (isMyTx(tx, wallet) {
               foundtxs.append(tx);
           } 
        }
        return foundtxs;
    }

    public bool isMyTx(Transaction tx, Wallet wallet) { 
    // Uses a wallet to determine if any of the txouts are sent to a key in the wallet
       for (TransactionOutput txOut : tx.getOutputs()) {
            if (txOut.isMine(wallet)) {
                return true;
            }
       } 
       return false;
    }
}
