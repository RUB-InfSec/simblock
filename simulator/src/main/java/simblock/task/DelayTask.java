package simblock.task;
import simblock.node.AdversarialNode;
/**
 * Task to model an adversarialy delayed sending of a message
 */
public class DelayTask implements Task{
  long delay;
  AbstractMessageTask message;
  AdversarialNode node;

  public DelayTask(AdversarialNode node ,long delay,AbstractMessageTask message){
    this.node = node;
    this.delay = delay;
    this.message = message;
  }

  /**
   * calls back to adversarial node to send the actual message
   */
  public void run() {
    node.callbackDelay(message);
  }

  @Override
  public long getInterval() {
    return delay;
  }
}