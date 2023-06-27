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

package simblock.task;

import static simblock.simulator.Timer.getCurrentTime;
import static simblock.simulator.Simulator.getSimulatedNodes;
import static simblock.simulator.Timer.putTask;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.stream.Collectors;
import static simblock.settings.SimulationConfiguration.INTERVAL;

import simblock.block.ProofOfWorkBlock;
import simblock.block.Transaction;
import simblock.node.Node;

/**
 * The type Mining task.
 */
public class MiningTask extends AbstractMintingTask {
  private final BigInteger difficulty;

  /**
   * Instantiates a new Mining task.
   *
   * @param minter   the minter
   * @param interval   the interval
   * @param difficulty the difficulty
   */
  public MiningTask(Node minter, long interval, BigInteger difficulty) {
    super(minter, interval);
    this.difficulty = difficulty;
  }

  HashSet<Transaction> transactions = new HashSet<>();

  @Override
  public void run() {
    if (this.getMinter().getPropagationProtocol().useTransactions()) {
      // moves 10 Transactions from mempool of minter into the block
      System.out.println("mempool "+this.minter.mempool.size()+" "+this.minter.knownTransactions.size());
      transactions = new HashSet<>(this.getMinter().mempool.stream().limit(10).collect(Collectors.toSet()));
      // creates 10 new Transactions at random nodes anytime in the next 10 mins
      for (int i = 0; i <= 10 && i<=transactions.size(); i++) {
        Transaction t = new Transaction();
        TransactionTask task = new TransactionTask(null, getSimulatedNodes().get((int) (Math.random() * getSimulatedNodes().size())), t, (long) (Math.max(Math.random() * INTERVAL,1)));
        putTask(task);
      }
    }

    ProofOfWorkBlock createdBlock = new ProofOfWorkBlock(
        (ProofOfWorkBlock) this.getParent(), this.getMinter(), getCurrentTime(),
        this.difficulty, transactions
    );
    this.getMinter().receiveBlock(createdBlock);
  }
}
