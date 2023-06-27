/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.node;

import static simblock.settings.SimulationConfiguration.DEBUG_MODE;
import static simblock.simulator.Main.OUT_JSON_FILE;
import static simblock.simulator.Simulator.arriveBlock;
import static simblock.simulator.Timer.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import simblock.block.Block;
import simblock.block.Transaction;
import simblock.node.consensus.AbstractConsensusAlgo;
import simblock.node.propagation.AbstractPropagationProtocol;
import simblock.node.routing.AbstractRoutingTable;
import simblock.task.AbstractMessageTask;
import simblock.task.AbstractMintingTask;
import simblock.task.BlockMessageTask;
import simblock.task.CmpctBlockMessageTask;
import simblock.task.GetBlockTxnMessageTask;
import simblock.task.InvMessageTask;
import simblock.task.RecMessageTask;
import simblock.task.TimeoutTask;
import simblock.task.TransactionTask;

/**
 * A class representing a node in the network.
 */
public class Node {

  /**
   * Unique node ID.
   */
  private final int nodeID;

  /**
   * Region assigned to the node.
   */
  private final int region;

  /**
   * Mining power assigned to the node.
   */
  private long miningPower;

  /**
   * A nodes routing table.
   */
  private AbstractRoutingTable routingTable;

  /**
   * a nodes propagtion protocol
   */
  protected AbstractPropagationProtocol propagationProtocol;

  /**
   * The consensus algorithm used by the node.
   */
  private AbstractConsensusAlgo consensusAlgo;

  /**
   * The node causes churn.
   */
  public boolean isChurnNode;

  /**
   * The current block.
   */
  private Block block;

  /**
   * Orphaned blocks known to node.
   */
  private final Set<Block> orphans = new HashSet<>();

  /**
   * The current minting task
   */
  private AbstractMintingTask mintingTask = null;

  /**
   * hashmap of all recieved invs for a block
   */
  protected HashMap<Block, ArrayList<Node>> recievedInvs = new HashMap<>();

  /**
    * blocks the node is currently waiting for
    */
  private final Set<Block> downloadingBlocks = new HashSet<>();

  /**
   * local mempool
   */
  public final HashSet<Transaction> mempool = new HashSet<>();

  /**
   * all transaction known to the node
   */
  public HashSet<Transaction> knownTransactions = new HashSet<>();

  public boolean isMiningPool = false;

  /**
   * Instantiates a new Node.
   *
   * @param nodeID            the node id
   * @param numConnection     the number of connections a node can have
   * @param region            the region
   * @param miningPower       the mining power
   * @param routingTableName  the routing table name
   * @param consensusAlgoName the consensus algorithm name
   * @param useCBR            whether the node uses compact block relay
   * @param isChurnNode       whether the node causes churn
   */
  public Node(
      int nodeID, int numConnection, int region, long miningPower, String routingTableName,
      String consensusAlgoName, String propagationProtocol, boolean isChurnNode) {
    this.nodeID = nodeID;
    this.region = region;
    this.miningPower = miningPower;
    this.isChurnNode = isChurnNode;
    try {
      this.routingTable = (AbstractRoutingTable) Class.forName(routingTableName).getConstructor(
          Node.class).newInstance(this);
      this.propagationProtocol = (AbstractPropagationProtocol) Class.forName(propagationProtocol)
          .getConstructor(Node.class).newInstance(this);
      this.consensusAlgo = (AbstractConsensusAlgo) Class.forName(consensusAlgoName).getConstructor(
          Node.class).newInstance(this);
      this.setNumConnection(numConnection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * always false for regular nodes
   *
   * @return adversarial status
   */
  public boolean IsAdversarial() {
    return false;
  }

  /**
   * Whether the node uses compact block relay.
   */
  protected boolean useCBR() {
    return (this.propagationProtocol.useCBR());
  }

  /**
   * Gets the node id.
   *
   * @return the node id
   */
  public int getNodeID() {
    return this.nodeID;
  }

  /**
   * Gets the region ID assigned to a node.
   *
   * @return the region
   */
  public int getRegion() {
    return this.region;
  }

  /**
   * Gets mining power.
   *
   * @return the mining power
   */
  public long getMiningPower() {
    return this.miningPower;
  }

  /**
   * sets mining power
   * @param power mining power
   */
  public void setMiningPower(long power) {
    this.miningPower = power;
  }

  public AbstractPropagationProtocol getPropagationProtocol() {
    return this.propagationProtocol;
  }

  /**
   * Gets the consensus algorithm.
   *
   * @return the consensus algorithm. See {@link AbstractConsensusAlgo}
   */
  @SuppressWarnings("unused")
  public AbstractConsensusAlgo getConsensusAlgo() {
    return this.consensusAlgo;
  }

  /**
   * Gets routing table.
   *
   * @return the routing table
   */
  public AbstractRoutingTable getRoutingTable() {
    return this.routingTable;
  }

  /**
   * Gets the current block.
   *
   * @return the block
   */
  public Block getBlock() {
    return this.block;
  }

  /**
   * Gets all orphans known to node.
   *
   * @return the orphans
   */
  public Set<Block> getOrphans() {
    return this.orphans;
  }

  /**
   * Gets the number of connections a node can have.
   *
   * @return the number of connection
   */
  @SuppressWarnings("unused")
  public int getNumConnection() {
    return this.routingTable.getNumConnection();
  }

  /**
   * Sets the number of connections a node can have.
   *
   * @param numConnection the n connection
   */
  public void setNumConnection(int numConnection) {
    this.routingTable.setNumConnection(numConnection);
  }

  /**
   * Gets the nodes neighbors.
   *
   * @return the neighbors
   */
  public ArrayList<Node> getNeighbors() {
    return this.routingTable.getNeighbors();
  }

  /**
   * Adds the node as a neighbor.
   *
   * @param node the node to be added as a neighbor
   * @return the success state of the operation
   */
  @SuppressWarnings("UnusedReturnValue")
  public boolean addNeighbor(Node node) {
    return this.routingTable.addNeighbor(node);
  }

  /**
   * Removes the neighbor form the node.
   *
   * @param node the node to be removed as a neighbor
   * @return the success state of the operation
   */
  @SuppressWarnings("unused")
  public boolean removeNeighbor(Node node) {
    return this.routingTable.removeNeighbor(node);
  }

  /**
   * Initializes the routing table.
   */
  public void joinNetwork(boolean conenctMiners) {
    this.routingTable.initTable(conenctMiners);
  }

  /**
   * Mint the genesis block.
   */
  public void genesisBlock() {
    Block genesis = this.consensusAlgo.genesisBlock();
    this.receiveBlock(genesis);
  }

  public Set<Block> getDownloadingBlocks() {
    return this.downloadingBlocks;
  }

  public void addBlockToDownloading(Block block) {
    this.downloadingBlocks.add(block);
  }

  /**
   * checks local downloadingblocks set
   *
   * @param block block thats get checked
   * @return true if block is in downloadingBlocks
   */
  public boolean checkDownloadingBlocks(Block block) {
    return (this.downloadingBlocks.contains(block));
  }

  public void resetNode() {
    this.block = null;
    this.orphans.clear();
    this.downloadingBlocks.clear();
    this.mempool.clear();
    this.knownTransactions.clear();
    this.propagationProtocol.clear();
  }

  /**
   * Adds a new block to the to chain. If node was minting that task instance is
   * abandoned, and
   * the new block arrival is handled.
   *
   * @param newBlock the new block
   */
  public void addToChain(Block newBlock) {
    // If the node has been minting
    if (this.mintingTask != null) {
      removeTask(this.mintingTask);
      this.mintingTask = null;
    }
    // Update the current block
    this.block = newBlock;
    if (DEBUG_MODE) {
      printAddBlock(newBlock);
    }
    // Observe and handle new block arrival
    arriveBlock(newBlock, this);
  }

  /**
   * Logs the provided block to the logfile.
   *
   * @param newBlock the block to be logged
   */
  private void printAddBlock(Block newBlock) {
    OUT_JSON_FILE.print("{");
    OUT_JSON_FILE.print("\"kind\":\"add-block\",");
    OUT_JSON_FILE.print("\"content\":{");
    OUT_JSON_FILE.print("\"timestamp\":" + getCurrentTime() + ",");
    OUT_JSON_FILE.print("\"node-id\":" + this.getNodeID() + ",");
    OUT_JSON_FILE.print("\"block-id\":" + newBlock.getId());
    OUT_JSON_FILE.print("}");
    OUT_JSON_FILE.print("},");
    OUT_JSON_FILE.flush();
  }

  /**
   * Add orphans.
   *
   * @param orphanBlock the orphan block
   * @param validBlock  the valid block
   */
  public void addOrphans(Block orphanBlock, Block validBlock) {
    if (orphanBlock != validBlock) {
      this.orphans.add(orphanBlock);
      this.orphans.remove(validBlock);
      if (validBlock == null || orphanBlock.getHeight() > validBlock.getHeight()) {
        this.addOrphans(orphanBlock.getParent(), validBlock);
      } else if (orphanBlock.getHeight() == validBlock.getHeight()) {
        this.addOrphans(orphanBlock.getParent(), validBlock.getParent());
      } else {
        this.addOrphans(orphanBlock, validBlock.getParent());
      }
    }
  }

  /**
   * Generates a new minting task and registers it
   */
  public void minting() {
    AbstractMintingTask task = this.consensusAlgo.minting();
    this.mintingTask = task;

    if (task != null) {
      if(checkMiningTask(task.getParent(), getCurrentTime() + task.getInterval())){
        addRemovableTask(task);
      }else{
        task=null;
      }
    }
  }

  /**
   * Send inv.
   *
   * @param block the block
   */
  public void sendInv(Block block, List<Node> subList) {
    for (Node to : subList) {
      AbstractMessageTask task = new InvMessageTask(this, to, block);
      putTask(task);
    }
  }

  /**
   * @param block   the block to send
   * @param subList the subgroup of neigbors to send the block to
   */
  public void sendBlock(Block block, List<Node> subList) {
    for (Node to : subList) {
      AbstractMessageTask message = new RecMessageTask(to, this, block);
      this.propagationProtocol.messageQue.add(message);
      if (!this.propagationProtocol.sendingBlock) {
        this.propagationProtocol.sendNextBlockMessage();
      }
    }
  }

  /**
   * Receive block.
   *
   * @param block the block
   */
  public void receiveBlock(Block block) {
    if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
      if (this.block != null && !this.block.isOnSameChainAs(block)) {
        // If orphan mark orphan
        this.addOrphans(this.block, block);
      }
      // Else add to canonical chain
      this.addToChain(block);
      // Generates a new minting task
      this.minting();
      propagationProtocol.clearMempool(block.getTransactions());
      for (Transaction t : block.getTransactions()) {
        knownTransactions.add(t);
      }
      // Advertise received block
      this.propagationProtocol.propagate(getNeighbors(), block);
    } else if (!this.orphans.contains(block) && !block.isOnSameChainAs(this.block)) {
      // If the block was not valid but was an unknown orphan and is not on the same
      // chain as the current block
      this.addOrphans(block, this.block);
      arriveBlock(block, this);
    }
  }

  /**
   * Callback function for timeout tasks
   * gets called when block is not recieved {NetworkConfiguration.T} ms after Inv from an adversarial
   * node
   * sends RecMessag to next saved Inv for the block
   *
   * @param block
   */
  public void callbackTimeout(Block block) {
    if (this.recievedInvs.containsKey(block)) {
      ArrayList<Node> temp = this.recievedInvs.get(block);
      if (!temp.isEmpty()) {
        AbstractMessageTask task = new RecMessageTask(this, temp.get(0), block);
        putTask(task);
        temp.remove(0);
      } else {
        downloadingBlocks.remove(block);
      }
    }
  }

  /**
   * called when node recieves an InvMessage
   * first check id the advertised block is needed send RecMessage if it is the case
   * next set a TimeoutTask if the message sender is adversarial
   * and save follwing InvMessage for the repeating blocks
   * @param message the InvMessage
   */
  protected void handleInvMessage(AbstractMessageTask message) {
    Block block = ((InvMessageTask) message).getBlock();
    if (!this.orphans.contains(block)) {
      if (!this.checkDownloadingBlocks(block)) {
        if (this.consensusAlgo.isReceivedBlockValid(block, this.block)) {
          AbstractMessageTask task = new RecMessageTask(this, message.getFrom(), block);
          putTask(task);
          this.addBlockToDownloading(block);
        } else if (!block.isOnSameChainAs(this.block)) {
          // get new orphan block
          AbstractMessageTask task = new RecMessageTask(this, message.getFrom(), block);
          putTask(task);
          this.addBlockToDownloading(block);
        }

        // add a timeout task if the message sender is adversarial
        // We skip adding a task when the message sender is honest to optimize the simulation
        if (message.getFrom().IsAdversarial()) {
          recievedInvs.put(block, new ArrayList<Node>());
          TimeoutTask t = new TimeoutTask(this, block);
          putTask(t);
        }
      } else {
        if (recievedInvs.containsKey(block)) {
          ArrayList<Node> temp = recievedInvs.get(block);
          temp.add(message.getFrom());
        }
      }
    }
  }

  protected void handleBlockMessage(AbstractMessageTask message) {
    Block block = ((BlockMessageTask) message).getBlock();
    downloadingBlocks.remove(block);
    this.receiveBlock(block);
  }

  /**
   * Receive message.
   *
   * @param message the message
   */
  public void receiveMessage(AbstractMessageTask message) {
    if (message instanceof InvMessageTask) {
      handleInvMessage(message);
    }
    if (message instanceof RecMessageTask) {
      this.getPropagationProtocol().handleRecMessage(message);
    }
    if (message instanceof GetBlockTxnMessageTask) {
      this.getPropagationProtocol().handleGetBlockTxnMessage(message);
    }
    if (message instanceof CmpctBlockMessageTask) {
      propagationProtocol.handleCompactBlockMessage(message);
    }
    if (message instanceof BlockMessageTask) {
      handleBlockMessage(message);
    }
    if (message instanceof TransactionTask) {
      propagationProtocol.handleTransaction(message);
    }
  }
}