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

package simblock.simulator;

import static simblock.settings.SimulationConfiguration.*;
import static simblock.settings.NetworkConfiguration.*;
import static simblock.simulator.Network.getDegreeDistribution;
import static simblock.simulator.Network.getRegionDistribution;
import static simblock.simulator.Network.printRegion;
import static simblock.simulator.Simulator.addNode;
import static simblock.simulator.Simulator.getSimulatedNodes;
import static simblock.simulator.Simulator.printAllPropagation;
import static simblock.simulator.Simulator.initPrint;
import static simblock.simulator.Simulator.setTargetInterval;
import static simblock.simulator.Simulator.resetNodeList;
import static simblock.simulator.Simulator.resetPropagationLists;
import static simblock.simulator.Timer.getCurrentTime;
import static simblock.simulator.Timer.getTask;
import static simblock.simulator.Timer.runTask;
import static simblock.simulator.Timer.TASK_COUNTS;
import static simblock.simulator.Timer.resetTimer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import simblock.block.Block;
import simblock.node.AdversarialNode;
import simblock.node.Node;
import simblock.node.propagation.Cbr;
import simblock.task.AbstractMintingTask;

/**
 * The type Main represents the entry point.
 */
public class Main {

  /**
   * The constant to be used as the simulation seed.
   */
  public static Random random = new Random(10);

  /**
   * The initial simulation time.
   */
  public static long simulationTime = 0;

  /**
   * internal variables for experiment 2(zeronodes) and experiment 3(miningpools)
   */
  public static int internalSmallMinerNumber = 0;
  public static boolean internalMinerNodesCreatedFlag = false;
  public static int internalZeroNodesNumber = 0;
  public static boolean interalZeroNodesCreatedFlag = false;

  /**
   * Path to config file.
   */
  public static URI CONF_FILE_URI;
  /**
   * Output path.
   */
  public static URI OUT_FILE_URI;
  public static PrintWriter OUT_STAT_FILE;

  static {
    try {
      CONF_FILE_URI = ClassLoader.getSystemResource("simulator.conf").toURI();
      OUT_FILE_URI = CONF_FILE_URI.resolve(new URI("../output/"));
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  /**
   * The output writer.
   */
  public static PrintWriter OUT_JSON_FILE;

  /**
   * The constant STATIC_JSON_FILE.
   */
  public static PrintWriter STATIC_JSON_FILE;

  static {
    try {
      OUT_JSON_FILE = new PrintWriter(
          new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve("./output.json")))));
      STATIC_JSON_FILE = new PrintWriter(
          new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve("./static.json")))));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static boolean validOption(String value, String[] options) {
    for (String s : options) {
      if (value.toLowerCase().equals(s))
        return true;
    }
    System.out.println("Error, " + value + " not in options (" + String.join(", ", options) + ")");
    return false;
  }

  private static int optionId(String value, String[] options) {
    int i = 0;
    for (String s : options) {
      if (value.toLowerCase().equals(s))
        return i;
      i++;
    }
    System.out.println("Error, " + value + " not in options (" + String.join(", ", options) + ")");
    return -1;
  }

  private static final String[] miningDistrs = {"default", "default_z", "zeronodes", "miningpools"};
  private static final String[] cryptos = {"btc", "eth", "doge", "monero", "cardano"};
  private static final String[] matchingProp = {"cbr", "hybrid", "cbr", "monero", "cardano"};

  public static final long[] transactionSizes = {60000, 40, 30, 390, 101};
  public static final long[] blockSizes = {1700000, 1200, 15000, 79900, 31000};

  /*
   * bitcoin cbr size based on average nr of transaction per block between 23.10-30.10.23(blockchain.info) * 6 ( reduced transaction header size)
   * + 80 ( blockheader) + 100 (coinbase tx)
   *  monero cbr size avg nr of tx (https://localmonero.co/blocks/stats) * 32 txid size + header 46 + coinbase 100
   */
  public static final long[] CBRblockSizes = {17807, 1200, 15000, 1266, 31000};
  public static final long[] blockInterval = {600000, 13000, 60000, 120000, 20000};

  /**
   * max time to wait for a response after requesting a block
   * btc: https://github.com/bitcoin/bitcoin/blob/master/src/net_processing.cpp l.126
   * eth: https://github.com/ethereum/consensus-specs/blob/dev/specs/phase0/p2p-interface.md#the-gossip-domain-gossipsub
   * doge: https://github.com/dogecoin/dogecoin/blob/master/src/net_processing.cpp l.3443
   */
  public static final long[] ConnectionTimeouts = {600000, 5000, 60000, 0, 20000};
  public static final int[] networksizes ={10,100,1000,10000, 100000};// ,500000};

  /**
   * The entry point.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    if (args.length != 7 && args.length != 10) {
      System.out.println("Error, expecting 7 or 10 arguments (version mining_distr [default,zeronodes,miningpools], propagation_mech [adv,hybrid,push,cbr], run_number, useTransactions,filterMiningTasks,(opt) P, (opt) Q, (opt) M)");
      return;
    }
    String version = args[0];
    if (version.equals("debug")) {
      DEBUG_MODE = true;
    }
    if (!DEBUG_MODE){
      System.out.println("Logging is disabled");
      OUT_JSON_FILE = null;
    }

    String miningDistr = args[1];
    String crypto = args[2];
    int id = optionId(crypto, cryptos);
    String propagation = matchingProp[id];
    BLOCK_SIZE = blockSizes[id];
    COMPACT_BLOCK_SIZE = CBRblockSizes[id];
    INTERVAL = blockInterval[id];
    TRANSACTION_SIZE = transactionSizes[id];
    T = ConnectionTimeouts[id];
    Propagation = "simblock.node.propagation." + propagation.substring(0, 1).toUpperCase() + propagation.substring(1).toLowerCase();
    int RUN_NUMBER = Integer.parseInt(args[3]);
    USE_TRANSACTIONS = Boolean.parseBoolean(args[4]);
    FILTER_MINING_TASKS = Boolean.parseBoolean(args[5]);
    FIXED_SIZE = Boolean.parseBoolean(args[6]);
    if (FIXED_SIZE) {
      BLOCK_SIZE=blockSizes[0];
      COMPACT_BLOCK_SIZE = CBRblockSizes[0];
    }
    if (!(validOption(miningDistr, miningDistrs) && validOption(crypto, cryptos))) {
      System.out.println("invalid parameter, expecting 7 or 10 arguments (version mining distr [default,zeronodes,miningpools], propagation mech [adv,hybrid,push,cbr], run number, useTransactions,filterMiningtasks,(opt) P, (opt) Q, (opt) M)");
      return;
    }
    if (args.length == 10) {
      // probability of a node to be adverserial
      P = Double.parseDouble(args[7]);
      // propability of a connection between a adversarial node and a regular node to be affected by M
      Q = Double.parseDouble(args[8]);
      // delay by which a block gets delayed in ms
      M = Long.parseLong(args[9]);
    }
    random = new Random(RUN_NUMBER);
    System.out.println("Starting new run with run number: " + RUN_NUMBER);
    final long start = System.currentTimeMillis();
    for (int i = 0; i < 2; i++) {
      for (int sizeIndex = 0; sizeIndex < networksizes.length; sizeIndex++) {
        int n = networksizes[sizeIndex];
        setTargetInterval(INTERVAL);
        resetTimer();
        // Log regions
        printRegion();
        // Setup network
        if (miningDistr.equals("zeronodes")) {
          setUSEMININGPOOLS(false);
          setPoolCount(0);
          if (i == 0) {
            internalZeroNodesNumber = 5000;
          } else {
            internalZeroNodesNumber = 500000;
          }
          args[1] = "zeronodes-" + internalZeroNodesNumber;
          constructNetwork(n, internalZeroNodesNumber, false);
        } else if (miningDistr.equals("default_z")) {
          setUSEMININGPOOLS(false);
          internalZeroNodesNumber = 10000;
          if (i > 0) {
            continue;
          }
          args[1] = "defaultz-" + internalZeroNodesNumber;
          constructNetwork(n, internalZeroNodesNumber, false);
        } else if (miningDistr.equals("miningpools")) {
          setUSEMININGPOOLS(true);
          if (i == 0) {
            setPoolCount(PoolCount_B);
            setPoolProportions(PoolProportion_B);
            internalSmallMinerNumber = 0;
          } else {
            setPoolCount(PoolCount_A);
            setPoolProportions(PoolProportion_A);
            internalSmallMinerNumber = 10000;
          }
          args[1] = "miningpools-" + (getPoolCount() + internalSmallMinerNumber);
          constructNetwork(getPoolCount() + internalSmallMinerNumber, n, true);
        } else {
          setUSEMININGPOOLS(false);
          if (i > 0) {
            continue;
          }
          constructNetwork(n, 0, false);
        }
        String configname = String.join("_", args);
        try {
          OUT_STAT_FILE = new PrintWriter(
              new BufferedWriter(new FileWriter(new File(OUT_FILE_URI.resolve(configname + "_Nodes_" + n + "_propagation.json")))));
        } catch (IOException e) {
          e.printStackTrace();
        }
        initPrint(OUT_STAT_FILE);
        // Initial block height, we stop at END_BLOCK_HEIGHT
        int currentBlockHeight = 1;
        // Iterate over tasks and handle
        while (getTask() != null) {
          if (getTask() instanceof AbstractMintingTask) {
            AbstractMintingTask task = (AbstractMintingTask) getTask();
            if (task.getParent().getHeight() == currentBlockHeight) {
              currentBlockHeight++;
            }
            if (currentBlockHeight > END_BLOCK_HEIGHT) {
              break;
            }
          }
          // Execute task
          runTask();
        }
        // Print propagation information about all blocks
        printAllPropagation(OUT_STAT_FILE);

        System.out.println();
        Set<Block> blocks = new HashSet<>();
        // Get the latest block from the first simulated node
        Block block = getSimulatedNodes().get(0).getBlock();
        //Update the list of known blocks by adding the parents of the aforementioned block
        while (block.getParent() != null) {
          blocks.add(block);
          block = block.getParent();
        }
        Set<Block> orphans = new HashSet<>();
        int averageOrphansSize = 0;
        // Gather all known orphans
        for (Node node : getSimulatedNodes()) {
          orphans.addAll(node.getOrphans());
          averageOrphansSize += node.getOrphans().size();
        }
        averageOrphansSize = averageOrphansSize / getSimulatedNodes().size();
        // Record orphans to the list of all known blocks
        blocks.addAll(orphans);
        ArrayList<Block> blockList = new ArrayList<>(blocks);
        long end = System.currentTimeMillis();
        simulationTime += end - start;
        // Log simulation time in milliseconds
        System.out.println("Time spent : " + (int)(simulationTime/1000) + "s N=" + networksizes[sizeIndex] + ", conf: " + configname);
        System.out.println("Simulation time: " + getCurrentTime());
        System.out.println("Number of tasks: " + TASK_COUNTS);
        resetPropagationLists();
        resetNodeList();
      }
      try {
        String configname = String.join("_", args);
        File finishFile = new File(OUT_FILE_URI.resolve(configname + "-finished.txt"));
        finishFile.createNewFile();
      } catch (IOException e) {
        System.out.println("An error occurred.");
      }
    }
  }

  /**
   * Populate the list using the distribution.
   *
   * @param distribution the distribution
   * @param facum        whether the distribution is cumulative distribution
   * @return array list
   */
  public static ArrayList<Integer> makeRandomListFollowDistribution(double[] distribution, boolean facum, int size) {
    ArrayList<Integer> list = new ArrayList<>();
    int index = 0;
    if (facum) {
      for (; index < distribution.length; index++) {
        while (list.size() <= size * distribution[index]) {
          list.add(index);
        }
      }
      while (list.size() < size) {
        list.add(index);
      }
    } else {
      double acumulative = 0.0;
      for (; index < distribution.length; index++) {
        acumulative += distribution[index];
        while (list.size() <= size * acumulative) {
          list.add(index);
        }
      }
      while (list.size() < size) {
        list.add(index);
      }
    }
    Collections.shuffle(list, random);
    return list;
  }

  /**
   * Populate the list using the rate.
   *
   * @param rate the rate of true
   * @return array list
   */
  public static ArrayList<Boolean> makeRandomList(float rate, int size) {
    ArrayList<Boolean> list = new ArrayList<Boolean>();
    for (int i = 0; i < size; i++) {
      list.add(i < size * rate);
    }
    Collections.shuffle(list, random);
    return list;
  }

  /**
   * Generates a random mining power expressed as Hash Rate, and is the number of mining (hash
   * calculation) executed per millisecond.
   *
   * @return the number of hash  calculations executed per millisecond.
   */
  public static int genMiningPower() {
    double r = random.nextGaussian();
    return Math.max((int) (r * STDEV_OF_MINING_POWER + AVERAGE_MINING_POWER), 1);
  }

  /**
   * @param numNodes
   * @return return the mining power every regular node receives
   */
  public static long calcMiningPower(int numNodes) {
    if (getUSEMINGPOOLS()) {
      Double allPools = 0.0;
      for (int i = 0; i < getPoolProportion().length; i++) {
        allPools += getPoolProportion()[i];
      }
      long poolPower = Math.round((getNetworkMiningPower() / 100) * allPools);
      if (numNodes != PoolCount) {
        return (getNetworkMiningPower() - poolPower) / (numNodes - PoolCount);
      } else {
        return 0;
      }
    } else {
      return getNetworkMiningPower() / (numNodes);
    }
  }

  public static void constructNetwork(int miners, int zeronodes, boolean connectedMiners) {
    double[] regionDistribution = getRegionDistribution();
    List<Integer> regionList = makeRandomListFollowDistribution(regionDistribution, false, miners + zeronodes);
    // Random distribution of node degrees
    double[] degreeDistribution = getDegreeDistribution();
    List<Integer> degreeList = makeRandomListFollowDistribution(degreeDistribution, true, miners + zeronodes);
    // List of churn nodes.
    List<Boolean> churnNodes = makeRandomList(CHURN_NODE_RATE, miners + zeronodes);
    long miningPower;
    long avg_miningPower = calcMiningPower(miners);
    Collections.shuffle(degreeList);
    Collections.shuffle(regionList);
    Collections.shuffle(churnNodes);
    for (int id = 1; id <= miners + zeronodes; id++) {
      if (id <= zeronodes) {
        miningPower = 0;
      } else {
        miningPower = avg_miningPower;
      }
      if (random.nextDouble() < P) {
        AdversarialNode node = new AdversarialNode(
            getSimulatedNodes().size() + 1, degreeList.get(id - 1) + 1, regionList.get(id - 1), miningPower, TABLE,
            ALGO, Propagation, churnNodes.get(id - 1)
        );
        addNode(node);
      } else {
        Node node = new Node(
            getSimulatedNodes().size() + 1, degreeList.get(id - 1) + 1, regionList.get(id - 1), miningPower, TABLE,
            ALGO, Propagation, churnNodes.get(id - 1)
        );
        addNode(node);
      }
    }
    //set miningpower for pools
    if (getUSEMINGPOOLS()) {
      int count = 0;
      HashSet<Integer> used = new HashSet<>();
      while (count < getPoolCount()) {
        int rand = random.nextInt(getSimulatedNodes().size());
        if (!used.contains(rand)) {
          if ((avg_miningPower > 0 && getSimulatedNodes().get(rand).getMiningPower() > 0) || avg_miningPower == 0) {
            getSimulatedNodes().get(rand).setMiningPower(Math.round((getNetworkMiningPower() / 100) * getPoolProportion()[count]));
            count++;
            getSimulatedNodes().get(rand).isMiningPool = true;
            used.add(rand);
          }
        }
      }
    }
    //connect nodes
    for (Node node : getSimulatedNodes()) {
      node.resetNode();
      node.joinNetwork(connectedMiners);
      //set new delayed connections
      if (node.IsAdversarial()) {
        ((AdversarialNode) node).calculateDelayedLinks();
      }
      //choose new highbandwidth connections
      if (node.getPropagationProtocol() instanceof Cbr) {
        ((Cbr) node.getPropagationProtocol()).chooseHighBandwidthModeConenctions();
      }
    }
    getSimulatedNodes().get(0).genesisBlock();
  }
}
