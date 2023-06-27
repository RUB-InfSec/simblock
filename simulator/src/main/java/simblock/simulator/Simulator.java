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

import static simblock.simulator.Timer.getCurrentTime;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import simblock.block.Block;
import simblock.node.Node;

import static simblock.settings.SimulationConfiguration.delta_one;
import static simblock.settings.SimulationConfiguration.delta_two;
import static simblock.settings.SimulationConfiguration.delta_cutoff;;

/**
 * The type Simulator is tasked with maintaining the list of simulated nodes and managing the
 * block interval. It observes and manages the arrival of new blocks at the simulation level.
 */
public class Simulator {

  private static boolean first = true;

  /**
   * A list of nodes that will be used in a simulation.
   */
  private static final ArrayList<Node> simulatedNodes = new ArrayList<>();

  /**
   * The target block interval in milliseconds.
   */
  private static long targetInterval;

  /**
   * Get simulated nodes list.
   *
   * @return the array list
   */
  public static ArrayList<Node> getSimulatedNodes() {
    return simulatedNodes;
  }

  public static void resetNodeList() {
    simulatedNodes.clear();
  }

  public static void resetPropagationLists() {
    propagationCount.clear();
    propagationCountMP.clear();
    propagationTimes.clear();
    observedBlocks.clear();
  }

  /**
   * Get target block interval.
   *
   * @return the target block interval in milliseconds
   */
  public static long getTargetInterval() {
    return targetInterval;
  }

  /**
   * Sets the target block interval.
   *
   * @param interval - block interval in milliseconds
   */
  public static void setTargetInterval(long interval) {
    targetInterval = interval;
  }

  /**
   * Add node to the list of simulated nodes.
   *
   * @param node the node
   */
  public static void addNode(Node node) {
    simulatedNodes.add(node);
  }

  /**
   * Remove node from the list of simulated nodes.
   *
   * @param node the node
   */
  @SuppressWarnings("unused")
  public static void removeNode(Node node) {
    simulatedNodes.remove(node);
  }

  /**
   * Add node to the list of simulated nodes and immediately try to add the new node as a
   * neighbor to all simulated
   * nodes.
   *
   * @param node the node

  */
  /**
   * A list of observed {@link Block} instances.
   */
  private static final ArrayList<Block> observedBlocks = new ArrayList<>();

  /**
   * A list of observed block propagation times. The map key represents the id of the node that
   * has seen the
   * block, the value represents the difference between the current time and the block minting
   * time, effectively
   * recording the absolute time it took for a node to witness the block.
   */
  private static final ArrayList<LinkedHashMap<Integer, Long>> observedPropagations =
      new ArrayList<>();

  private static ArrayList<ArrayList<Integer>> propagationCount = new ArrayList<>();

  private static ArrayList<ArrayList<Long>> propagationCountMP = new ArrayList<>();

  private static ArrayList<ArrayList<Long>> propagationTimes = new ArrayList<>();

  /*
   * function updates the propagation listes
   *
   * if a propagtion time is within the delta the count gets updated
   * otherwise a the new longest propagtion time is added in propagtionTimes and a zero is added in propagtionCount
   */
  private static void addToList(Block block, Node node) {
    int index = observedBlocks.indexOf(block);
    long propagation_time = getCurrentTime() - block.getTime();
    int delta = propagation_time < delta_cutoff ? delta_one : delta_two;
    if (propagation_time - propagationTimes.get(index).get(propagationTimes.get(index).size() - 1) < delta) {
      propagationCount.get(index).set(propagationCount.get(index).size() - 1, propagationCount.get(index).get(propagationCount.get(index).size() - 1) + 1);
      propagationCountMP.get(index).set(propagationCountMP.get(index).size() - 1, propagationCountMP.get(index).get(propagationCountMP.get(index).size() - 1) + node.getMiningPower());
    } else {
      propagationTimes.get(index).add(propagation_time);
      propagationCount.get(index).add(1);
      propagationCountMP.get(index).add(node.getMiningPower());
    }
  }

  /**
   * Handle the arrival of a new block. For every observed block, propagation information is
   * updated, and for a new
   * block propagation information is created.
   *
   * @param block the block
   * @param node  the node
   */
  public static void arriveBlock(Block block, Node node) {
    // If block is already seen by any node
    if (observedBlocks.contains(block)) {
      addToList(block,node);
    } else {
      // If the block has not been seen by any node
      observedBlocks.add(block);
      propagationTimes.add(new ArrayList<Long>(Arrays.asList((long) 0)));
      propagationCount.add(new ArrayList<Integer>(Arrays.asList(0)));
      propagationCountMP.add(new ArrayList<Long>(Arrays.asList((long)0)));
    }
  }

  /**
   * Print propagation information about the propagation of the provided block  in the format:
   *
   * <p><em> propagation_time,count, mining power </em>
   * <p><em>propagation_time</em>: The time from when the block of the block ID is generated to
   * when the
   * <p> <em>count</em>: number of arraivals within delta of propagation time
   * <p><em>mining power</em>:sum of miningpower of all nodes that recieved the block at this time</em>
   *
   *
   * @param block     the block
   */
  private static void printPropagation(PrintWriter pw, Block block, ArrayList<Integer> count, ArrayList<Long> mpCount,  ArrayList<Long> times) {
    // Print block and its height
    pw.print("\"" + block + "\": [");
    boolean first = true;
    for (int i = 0; i < count.size(); i++) {
      if (!first)
        pw.print(",");
      pw.print("[");
      pw.print(times.get(i));
      pw.print(",");
      pw.print(count.get(i));
      pw.print(",");
      pw.print(mpCount.get(i));
      pw.print("]");
      first = false;
    }
    pw.print("]");
  }

  /**
   * Print propagation information about all blocks, internally relying on
   * {@link Simulator#printPropagation(Block, LinkedHashMap)}.
   */
  public static void printAllPropagation(PrintWriter pw) {
    for (int i = 0; i < observedBlocks.size(); i++) {
      if (!first)
        pw.println(",");
      first = false;
      printPropagation(pw, observedBlocks.get(i), propagationCount.get(i), propagationCountMP.get(i), propagationTimes.get(i));
    }
    pw.print("}");
    pw.flush();
  }

  public static void initPrint(PrintWriter pw) {
    first = true;
    pw.print("{");
  }
}
