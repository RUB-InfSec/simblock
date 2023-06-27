package simblock.task;

import simblock.block.Transaction;
import simblock.node.Node;

import static simblock.simulator.Network.getLatency;
import static simblock.settings.SimulationConfiguration.TRANSACTION_SIZE;
import static simblock.simulator.Network.getBandwidth;
/*
 * Task that models  the sending of a Transaction
 */
public class TransactionTask extends AbstractMessageTask{
  private Transaction transaction;
  private final long interval;
  private final long size =TRANSACTION_SIZE;

  /**
   * creator used for sending transactions
   */
  public TransactionTask(Node from ,Node to,Transaction transaction){
    super(from, to);
    this.transaction = transaction;
    this.interval = getLatency(this.getFrom().getRegion(), this.getTo().getRegion())+(
      this.size / getBandwidth(this.getFrom().getRegion(), this.getTo().getRegion()) );
  }
  /**
   *
   * creator used for crating new transactions after a delay
   */
  public TransactionTask(Node from ,Node to,Transaction transaction, long delay){
    super(from, to);
    this.transaction = transaction;
    if(from == null){
      this.interval = 0 + delay;
    }else{
      this.interval = getLatency(this.getFrom().getRegion(), this.getTo().getRegion())+(
      this.size / getBandwidth(this.getFrom().getRegion(), this.getTo().getRegion()) )+ delay;
    }
  }

  @Override
  public long getInterval() {
    return this.interval ;
  }

  public Transaction getTransaction(){
    return this.transaction;
  }
}

