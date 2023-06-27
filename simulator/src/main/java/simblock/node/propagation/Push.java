package simblock.node.propagation;

import java.util.ArrayList;
import java.util.Collections;
import simblock.block.Block;
import simblock.node.Node;

/**
 * propagation protocol that directly forwards every block to all neighbors
 */
public class Push extends AbstractPropagationProtocol {
  public Push(Node node) {
    super(node);
  }

  @Override
  public void propagate(ArrayList<Node> neigbors, Block block) {
    Collections.shuffle(neigbors);
    this.selfNode.sendBlock(block, neigbors);
  }
}