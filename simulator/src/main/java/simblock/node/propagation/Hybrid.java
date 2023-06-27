package simblock.node.propagation;

import java.util.ArrayList;
import java.util.Collections;
import simblock.block.Block;
import simblock.node.Node;

/**
 * class to implement ethereum style square-root propagtion
 */
public class Hybrid extends AbstractPropagationProtocol {
  public Hybrid(Node node) {
    super(node);
  }

  /**
   * first calculate border = squareroot of current number of neighbors
   * than directly send the block to border neigbors and send Inv message to all others
   */
  @Override
  public void propagate(ArrayList<Node> neigbors, Block block) {
    int border = this.calculatePropagation(neigbors);
    Collections.shuffle(neigbors);
    this.selfNode.sendBlock(block, neigbors.subList(0, border));
    this.selfNode.sendInv(block, neigbors.subList(border, neigbors.size()));
  }

  public int calculatePropagation(ArrayList<Node> neigbors) {
    return Math.round(Math.round(Math.sqrt(neigbors.size())));
  }
}