package simblock.node.propagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import simblock.block.Block;
import simblock.node.Node;

import static simblock.settings.NetworkConfiguration.LATENCY;
import static simblock.settings.SimulationConfiguration.CBR_HIGH_BW_CONNECTIONS;
import static simblock.settings.SimulationConfiguration.COMPACT_BLOCK_SIZE;
import static simblock.simulator.Network.getBandwidth;

/**
 * Class to implement Bitcoin Compact-Block propagation
 */
public class Cbr extends AbstractPropagationProtocol {
  public ArrayList<Node> highBandwidthModeConnections = new ArrayList<>();
  public ArrayList<Node> lowBandwidthModeConnections = new ArrayList<>();
  public boolean setLBWM = false;
  public Cbr(Node node) {
    super(node);
  }

  /*
   * choose 3  neighbors to send blocks with highbandwidht mode to selfnode
   * based on https://github.com/bitcoin/bips/blob/master/bip-0152.mediawiki#user-content-Implementation
   * (with simple checking for the conenctions with the best expected propagation times )
   */
  public void chooseHighBandwidthModeConenctions(){
    ArrayList<Node> sortedNeigbors = (ArrayList<Node>)this.selfNode.getNeighbors().clone();
    Collections.sort(sortedNeigbors,new Comparator<Node>() {
      @Override
      public int compare(Node node1,Node node2){
        Long bw2 = (Long)getBandwidth(node2.getRegion(), selfNode.getRegion());
        Long bw1 = (Long)getBandwidth(node1.getRegion(), selfNode.getRegion());
        Long delay1 = LATENCY[node1.getRegion()][selfNode.getRegion()]+(COMPACT_BLOCK_SIZE/bw1);
        Long delay2 = LATENCY[node2.getRegion()][selfNode.getRegion()]+(COMPACT_BLOCK_SIZE/bw2);
        return delay2.compareTo(delay1);
      }
    });
    for(int i =0; i<CBR_HIGH_BW_CONNECTIONS && i < this.selfNode.getNeighbors().size();i++){
        ((Cbr)sortedNeigbors.get(i).getPropagationProtocol()).highBandwidthModeConnections.add(selfNode);
    }
  }

  @Override
  public boolean useCBR(){
    return true;
  }

  /**
   * on first call calculate the high bandwidth connections that recieve blocks directly
   * than send CmpctBlock/Inv messages accordingly
   */
  @Override
  public void propagate(ArrayList<Node> neighbors, Block block) {
    Collections.shuffle(neighbors);
    if(!setLBWM){
      lowBandwidthModeConnections=(ArrayList<Node>)neighbors.clone();
      lowBandwidthModeConnections.removeIf(n->(highBandwidthModeConnections.contains(n)));
      setLBWM=true;
    }

    if(selfNode.IsAdversarial()){
      this.selfNode.sendInv(block, neighbors);
    }else{
      this.selfNode.sendInv(block, lowBandwidthModeConnections);
      this.selfNode.sendBlock(block, highBandwidthModeConnections);
    }
  }

  @Override
  public void clear(){
    this.messageQue.clear();
    this.highBandwidthModeConnections.clear();
    this.setLBWM=false;
  }
}