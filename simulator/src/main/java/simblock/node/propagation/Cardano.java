package simblock.node.propagation;
import java.util.ArrayList;

import simblock.node.Node;
import simblock.task.*;
import simblock.block.Block;
import static simblock.simulator.Timer.putTask;
import static simblock.simulator.Network.getBandwidth;

import static simblock.settings.SimulationConfiguration.BLOCK_SIZE;

/**
 * class to implement a propagation protocol based on the ouroboros network of the  cardano blockchain
 * https://ouroboros-network.cardano.intersectmbo.org/pdfs/network-spec/network-spec.pdf
 */
public class Cardano extends AbstractPropagationProtocol{
    //number of blocks currently send at the same time;
    int currentlySending;
    public Cardano(Node node){
        super(node);
        this.currentlySending=0;
    }

    /**
     * Send InvMessage to all neighbors
     * InvMessage here represents MsgRollForward of the protocol
     */
    @Override
    public void propagate(ArrayList<Node> neigbors, Block block){
        this.selfNode.sendInv(block,neigbors);
    }

    @Override
    public void blockSendingMechanism(AbstractMessageTask message){
        this.sendParallelBlocks(message);
    }

    /**
     * simplified parrallel sending of blocks to all neighbors
     * as accurate dynamic adaption of propagation times would
     * create to much overhead by recreating and rescheduling of the same tasks
     * @param message the blockmessage
     */
    public void sendParallelBlocks(AbstractMessageTask message){
        this.currentlySending++;
        Node to = message.getFrom();
        long bandwidth = getBandwidth(this.selfNode.getRegion(), to.getRegion());
        Block block = ((RecMessageTask) message).getBlock();
        long delay = BLOCK_SIZE  / ((bandwidth / 1000)/Math.min(1,currentlySending)) + processingTime;
        BlockMessageTask task = new BlockMessageTask(this.selfNode, to, block, delay);
        putTask(task);
    }

    //gets called when the Blockmessage arrives
    @Override
    public void endBlockTransmission(){
        this.currentlySending --;
    }
}