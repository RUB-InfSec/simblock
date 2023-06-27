package simblock.node.propagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import simblock.block.Block;
import simblock.block.Transaction;
import simblock.node.AdversarialNode;
import simblock.node.Node;
import simblock.task.AbstractMessageTask;
import simblock.task.CmpctBlockMessageTask;
import simblock.task.DelayTask;
import simblock.task.GetBlockTxnMessageTask;
import simblock.task.TransactionTask;
import static simblock.simulator.Timer.putTask;
import static simblock.settings.NetworkConfiguration.M;

public class CompactBlockRelay extends AbstractPropagationProtocol {
  public CompactBlockRelay(Node node) {
    super(node);
  }

  @Override
  public void propagate(ArrayList<Node> neigbors, Block block) {
    Collections.shuffle(neigbors);
    this.selfNode.sendBlock(block, neigbors);
  }

  public void clearMempool(HashSet<Transaction> transactions) {
    for (Transaction t : transactions) {
      this.selfNode.mempool.remove(t);
    }
  }

  public void handleCompactBlockMessage(AbstractMessageTask message) {
    Block block = ((CmpctBlockMessageTask) message).getBlock();
    boolean success = true;
    for (Transaction t : block.getTransactions()) {
      if (!this.selfNode.mempool.contains(t)) {
        success = false;
      }
    }
    if (success) {
      this.selfNode.getDownloadingBlocks().remove(block);
      this.selfNode.receiveBlock(block);
    } else {
      AbstractMessageTask task = new GetBlockTxnMessageTask(this.selfNode, message.getFrom(), block);
      putTask(task);
    }
  }

  public void handleTransaction(AbstractMessageTask message) {
    Transaction t = ((TransactionTask) message).getTransaction();
    if (this.selfNode.mempool.size() < 600) {
      this.selfNode.mempool.add(t);
    }
    for (Node n : this.selfNode.getNeighbors()) {
      if (!n.mempool.contains(t)) {
        TransactionTask task = new TransactionTask(this.selfNode, n, t);
        if (this.selfNode.IsAdversarial() && ((AdversarialNode) this.selfNode).getDelayedLinks().contains(n)) {
          DelayTask delay = new DelayTask((AdversarialNode) this.selfNode, M, (AbstractMessageTask) task);
          putTask(delay);
        } else {
          putTask(task);
        }
      }
    }
  }
}