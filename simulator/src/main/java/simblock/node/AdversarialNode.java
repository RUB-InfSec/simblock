package simblock.node;

import static simblock.simulator.Timer.putTask;
import static simblock.settings.NetworkConfiguration.M;
import static simblock.settings.NetworkConfiguration.Q;
import static simblock.simulator.Main.random;

import simblock.block.Block;

import java.util.HashSet;
import java.util.List;

import simblock.task.*;

public class AdversarialNode extends Node {
  private HashSet<Node> delayedLinks = new HashSet<>();

  /**
   * creates a adversarial node
   */
  public AdversarialNode(int nodeID, int numConnection, int region, long miningPower, String routingTableName,
                         String consensusAlgoName, String propagationProtocol, boolean isChurnNode) {
    super(nodeID, numConnection, region, miningPower, routingTableName,
        consensusAlgoName, propagationProtocol, isChurnNode);
  }

  @Override
  public boolean IsAdversarial() {
    return true;
  }

  /**
   * calcuates whether a connection to a regular node is affected by delay M.
   */
  public void calculateDelayedLinks() {
    this.delayedLinks.clear();
    if (this.IsAdversarial()) {
      this.delayedLinks = new HashSet<>();
      for (Node n : this.getNeighbors()) {
        if (random.nextDouble() < Q && !(n.IsAdversarial())) {
          delayedLinks.add(n);
        }
      }
    } else {
      this.delayedLinks = new HashSet<>();
    }
  }

  /**
   *
   * @return set of delayed neighbors
   */
  public HashSet<Node> getDelayedLinks() {
    return this.delayedLinks;
  }

  /**
   * set Delaytask with delay M
   * if the reciver of the message is in DelayedLinks
   * otherwise set the TransactionTask or queue the block for sending
   * @param message message to send
   */
  public void delayMessage(AbstractMessageTask message) {
    if (this.getDelayedLinks().contains(message.getFrom())) {
      if (M == -1) {
        return;
      }
      DelayTask delay = new DelayTask(this, M, message);
      putTask(delay);
    } else {
      if (message instanceof TransactionTask) {
        putTask(message);
      } else {
        this.propagationProtocol.blockSendingMechanism(message);
      }
    }
  }

  /**
   * @param block   the block to send
   * @param subList the subgroup of neigbors to send the block to
   */
  @Override
  public void sendBlock(Block block, List<Node> subList) {
    for (Node to : subList) {
      AbstractMessageTask message = new RecMessageTask(to, this, block);
      delayMessage(message);
    }
  }

  /**
   * gets called by DelayTask after delay M ms are over to exectue the delayed message
   *
   */
  public void callbackDelay(AbstractMessageTask message) {
    if (message instanceof TransactionTask) {
      putTask(message);
    } else {
      this.propagationProtocol.blockSendingMechanism(message);
    }
  }
}