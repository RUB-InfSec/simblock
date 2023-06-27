package simblock.node.propagation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import simblock.node.AdversarialNode;
import simblock.node.Node;
import simblock.block.Block;
import simblock.block.Transaction;
import simblock.task.*;

import static simblock.settings.SimulationConfiguration.*;

import static simblock.simulator.Timer.putTask;
import static simblock.simulator.Network.getBandwidth;

/**
 * Abstact propagation protocl class, that implements all basic propagation behavior
 */
abstract public class AbstractPropagationProtocol {
  protected final Node selfNode;
  public final ArrayList<AbstractMessageTask> messageQue = new ArrayList<>();
  public boolean sendingBlock;

  /**
   * Processing time of tasks expressed in milliseconds.
   */
  protected final long processingTime = 2;

  public AbstractPropagationProtocol(Node node) {
    this.selfNode = node;
  }

  /**
   * Flag to signal the use of compact blocks of any form in propagation.
   * default=false
   *
   */
  public boolean useCBR(){
    return false;
  }

  /**
   *Abstarct propagation function starts the creation of Inv,Block,CmpctBlock -message tasks
   * based on the used propagtion protocol
   * @param neigbors list of all neigboring nodes
   * @param block the block to propagte
   */
  abstract public void propagate(ArrayList<Node> neigbors, Block block);

  /**
   *
   * @return Flag to signal the usage of Transactions
   */
  public boolean useTransactions(){
    return USE_TRANSACTIONS;
  }

  /**
   * removes transaction from the mempool of the node
   *
   * @param transactions set of transactions to be removed
   */
  public void clearMempool(HashSet<Transaction> transactions) {
    this.selfNode.mempool.removeAll(transactions);
  }

  /**
   * Gets block size when the node fails compact block relay.
   */
  protected long getFailedBlockSize() {
    Random random = new Random();
    if (this.selfNode.isChurnNode) {
      int index = random.nextInt(CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE.length);
      return (long) (BLOCK_SIZE * CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CHURN_NODE[index]);
    } else {
      int index = random.nextInt(CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE.length);
      return (long) (BLOCK_SIZE * CBR_FAILURE_BLOCK_SIZE_DISTRIBUTION_FOR_CONTROL_NODE[index]);
    }
  }
  /**
   * individual functions to handle the receptions of specific messages
   * @param message the recieved message
   * called if the recieved message is a RecMessage
   * the the reciving node is adversarial call it's  delayMessage function
   * otherwise forward it to blocksending
   */
  public void handleRecMessage(AbstractMessageTask message) {
    if(this.selfNode.IsAdversarial()){
      ((AdversarialNode) this.selfNode).delayMessage(message);
    }else{
      blockSendingMechanism(message);
    }
  }

  /**
   * called if the recieved message is a GetBLockTxnMessage (from a failed compactblock transmission)
   * starts propagation of the full block with reduced size
   * @param message
   */
  public void handleGetBlockTxnMessage(AbstractMessageTask message) {
    blockSendingMechanism(message);
  }

  /**
   *
   * called if the recieved message is a CompactBlockMessage
   * first checks if the recieved block is valid - invalid blocks can apear from forks or  heavily delayed blocks
   *
   * next check for a successfull compact block transmission by either check if all  transactions in the block are known by the node
   * or by using a probabilistic aproach
   *
   *on success handle block arrival
   *on failure send a getBlockTxnMessage
   */
  public void handleCompactBlockMessage(AbstractMessageTask message) {
    Block block = ((CmpctBlockMessageTask) message).getBlock();
    if (!this.selfNode.getConsensusAlgo().isReceivedBlockValid(block, this.selfNode.getBlock())){
      return;
    }
    boolean success;
    if(this.useTransactions()){
      success = true;
      for (Transaction t : block.getTransactions()) {
        if (!this.selfNode.knownTransactions.contains(t)) {
          success = false;
          break;
        }
      }
    }else{
      Random random = new Random();
      float CBRfailureRate = this.selfNode.isChurnNode ? CBR_FAILURE_RATE_FOR_CHURN_NODE : CBR_FAILURE_RATE_FOR_CONTROL_NODE;
      success = random.nextDouble() > CBRfailureRate;
    }
    if (success) {
      this.selfNode.getDownloadingBlocks().remove(block);
      this.selfNode.receiveBlock(block);
    } else {
      AbstractMessageTask task = new GetBlockTxnMessageTask(this.selfNode, message.getFrom(), block);
      putTask(task);
    }
  }

  /**
   * called if the recieved message is a transaction message
   * while the local mempool still has space and the transaction is new  add the transaction to the mempool and the knownTransactions list
   * then send the transactions to neighbors and delay if the node is adversarial
   *
   * (the check if the transaction is known by neighbors is used to reduce the number of transaction tasks in the taskqueue
   *  as network congestion is not modeled a transaction message the is send and refused is the same as a message that is not send)
   */
  public void handleTransaction(AbstractMessageTask message) {
    Transaction t = ((TransactionTask) message).getTransaction();
    if ((this.selfNode.mempool.size() < 600) && !this.selfNode.knownTransactions.contains(t)) {
      this.selfNode.mempool.add(t);
      this.selfNode.knownTransactions.add(t);
    }
    for (Node n : this.selfNode.getNeighbors()) {
      if (!n.knownTransactions.contains(t)) {
        TransactionTask task = new TransactionTask(this.selfNode, n, t);
        if (this.selfNode.IsAdversarial()) {
          ((AdversarialNode) this.selfNode).delayMessage(task);
        }else{
          putTask(task);
        }
      }
    }
  }

  /**
   * Queue a blockmessage of any type for sending
   */
  public void blockSendingMechanism(AbstractMessageTask message){
    this.messageQue.add(message);
    if (!sendingBlock) {
      this.sendNextBlockMessage();
    }
  }

  /**
   * Send next queued block message.
   */
  public void sendNextBlockMessage() {
    if (this.messageQue.size() > 0) {
      Node to = this.messageQue.get(0).getFrom();
      long bandwidth = getBandwidth(this.selfNode.getRegion(), to.getRegion());
      AbstractMessageTask messageTask;
      if (this.messageQue.get(0) instanceof RecMessageTask) {
        Block block = ((RecMessageTask) this.messageQue.get(0)).getBlock();
        // send compact block if accepted by both nodes (sending and recieving)
        if (this.messageQue.get(0).getFrom().getPropagationProtocol().useCBR() && this.useCBR()) {
          long delay = COMPACT_BLOCK_SIZE / (bandwidth / 1000) + processingTime;
          messageTask = new CmpctBlockMessageTask(this.selfNode, to, block, delay);
        } else {
          long delay = BLOCK_SIZE  / (bandwidth / 1000) + processingTime;
          messageTask = new BlockMessageTask(this.selfNode, to, block, delay);
        }
      } else if (this.messageQue.get(0) instanceof GetBlockTxnMessageTask) {
        // Else from requests missing transactions.
        Block block = ((GetBlockTxnMessageTask) this.messageQue.get(0)).getBlock();
        long delay = getFailedBlockSize()  / (bandwidth / 1000) + processingTime;
        messageTask = new BlockMessageTask(this.selfNode, to, block, delay);
      } else {
        throw new UnsupportedOperationException();
      }
      sendingBlock = true;
      this.messageQue.remove(0);
      putTask(messageTask);
    } else {
      sendingBlock = false;
    }
  }
  /**
   * starts the next block transmission when the current transmission ends
   */
  public void endBlockTransmission(){
    this.sendNextBlockMessage();
  }
  /**
   * clears message queue for reusing nodes in multiple simulations
   */
  public void clear(){
    this.messageQue.clear();
  }
}