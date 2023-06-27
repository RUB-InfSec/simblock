package simblock.node.propagation;

import java.util.ArrayList;
import java.util.Collections;

import simblock.node.Node;
import simblock.block.Block;

/**
 * Propagation protocol for advertisment based propagtion
 */
public class Advertisment extends AbstractPropagationProtocol {
  public Advertisment(Node node) {
    super(node);
  }

  /**
   * sinds InvMessages to all neighbors to advertise the block
   */
  public void propagate(ArrayList<Node> neigbors, Block block) {
    Collections.shuffle(neigbors);
    this.selfNode.sendInv(block, neigbors);
  }
}