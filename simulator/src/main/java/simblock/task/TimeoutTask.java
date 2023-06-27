package simblock.task;
import simblock.node.Node;
import simblock.block.Block;
import static simblock.settings.NetworkConfiguration.T;
/**
 * Task for NetworkTimeouts when waiting to long for blocks
 */
public class TimeoutTask implements Task {

  Node node;
  Block block;

  public TimeoutTask(Node node, Block block){
    this.block = block;
    this.node = node;
  }

  /**
   * calls back to waiting node
   */
  @Override
  public void run() {
    this.node.callbackTimeout(block);
  }

  @Override
  public long getInterval() {
    return T;
  }
}