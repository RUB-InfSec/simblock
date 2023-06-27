package simblock.node.propagation;
import java.util.ArrayList;
import java.util.Random;
import simblock.block.Block;
import simblock.node.Node;
import simblock.task.AbstractMessageTask;
import simblock.task.CmpctBlockMessageTask;
import simblock.task.GetBlockTxnMessageTask;
import static simblock.simulator.Timer.putTask;
import simblock.task.*;

import static simblock.settings.SimulationConfiguration.BLOCK_SIZE;
import static simblock.settings.SimulationConfiguration.MONERO_FLUFFY_BLOCK_USAGE;
import static simblock.settings.SimulationConfiguration.COMPACT_BLOCK_SIZE;
import static simblock.simulator.Network.getBandwidth;

/**
 * class that implements a propagation protocol based on the Monero blockchain
 */
public class Monero extends AbstractPropagationProtocol {
  private boolean useCBR;

  /**
   * on creation choose whether a node uses fluffy blocks (Moneros compact blocks)
   * @param node
   */
  public Monero(Node node){
    super(node);
    Random random = new Random();
    if(random.nextDouble() < MONERO_FLUFFY_BLOCK_USAGE){
      this.useCBR=true;
    }else{
      this.useCBR=false;
    }
  }

  @Override
  public boolean useCBR(){
    return this.useCBR;
  }

  /**
   * first send fluffyblocks than normal full blocks
   */
  @Override
  public void propagate(ArrayList<Node> neigbors, Block block){
    ArrayList<Node> neighborsWithFluffyBlocks = new ArrayList<>();
    ArrayList<Node> neighborsWithOutFluffyBlocks = new ArrayList<>();;
    for(Node n : neigbors){
      if(n.getPropagationProtocol().useCBR()){
        neighborsWithFluffyBlocks.add(n);
      }else{
        neighborsWithOutFluffyBlocks.add(n);
      }
    }
    this.selfNode.sendBlock(block, neighborsWithFluffyBlocks);
    this.selfNode.sendBlock(block, neighborsWithOutFluffyBlocks);
  }

  /**
   * only checks whether the recieving node expects compact block or not
   */
  @Override
  public void sendNextBlockMessage() {
    if (this.messageQue.size() > 0) {
      Node to = this.messageQue.get(0).getFrom();
      long bandwidth = getBandwidth(this.selfNode.getRegion(), to.getRegion());
      AbstractMessageTask messageTask;
      if (this.messageQue.get(0) instanceof RecMessageTask) {
        Block block = ((RecMessageTask) this.messageQue.get(0)).getBlock();
        // If use compact block relay.
        if (this.messageQue.get(0).getFrom().getPropagationProtocol().useCBR()){
          long delay = COMPACT_BLOCK_SIZE  / (bandwidth / 1000) + processingTime;
          // Send compact block message.
          messageTask = new CmpctBlockMessageTask(this.selfNode, to, block, delay);
        } else {
          // Else use lagacy protocol.
          long delay = BLOCK_SIZE / (bandwidth / 1000) + processingTime;
          messageTask = new BlockMessageTask(this.selfNode, to, block, delay);
        }
      }else if (this.messageQue.get(0) instanceof GetBlockTxnMessageTask) {
        // Else from requests missing transactions.
        Block block = ((GetBlockTxnMessageTask) this.messageQue.get(0)).getBlock();
        long delay = getFailedBlockSize() / (bandwidth / 1000) + processingTime;
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
}