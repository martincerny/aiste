/*
 * Copyright (C) 2012 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cuni.amis.aiste.simulations.spyvsspy;

import cz.cuni.amis.aiste.IRandomizable;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IEnvironmentSpecificRepresentation;
import cz.cuni.amis.aiste.simulations.utils.RandomUtils;
import cz.cuni.amis.pathfinding.alg.astar.AStar;
import cz.cuni.amis.pathfinding.alg.astar.AStarResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class SpyVsSpyReactiveRepresentation implements IEnvironmentSpecificRepresentation<SpyVsSpyAction>, IRandomizable{

    private final Logger logger = Logger.getLogger(SpyVsSpyReactiveRepresentation.class);

    private SpyVsSpy env;
    
    AStar<Integer> astar;
    
    /**
     * Last path returned from AStar, including the start and final location
     */
    Map<AgentBody,List<Integer>> lastPathFound = new HashMap<AgentBody, List<Integer>>();
    Map<AgentBody, Integer> lastPathFoundIndex = new HashMap<AgentBody, Integer>();
    
    Random rand = new Random();

    public SpyVsSpyReactiveRepresentation(SpyVsSpy env) {
        this.env = env;
        astar =  new AStar<Integer>(env.defs.mapForPathFinding);        
    }

    @Override
    public void setRandomSeed(long seed) {
        rand = new Random(seed);
    }
    
    

    @Override
    public SpyVsSpyAction getNextAction(AgentBody body) {
        SpyVsSpyBodyInfo bodyInfo = getBodyInfo(body);

        /*
         * Gather senses
         */
        boolean hasAllItems = true;
        for(int i = 0; i < env.defs.numItemTypes; i++){
            if(!bodyInfo.itemsCarried.contains(i)){
                hasAllItems = false;
                break;
            }
        }
        
        SpyVsSpyMapNode node = env.nodes.get(bodyInfo.locationIndex);
        boolean nodeSecure = node.traps.isEmpty();
        
        int usefulItemIndex = getUsefulItemIndex(node, bodyInfo);
        int trapRemoverIndex = getTrapRemover(node);        
        boolean canRemoveAllTraps = canRemoveAllTraps(node, bodyInfo);
        
        boolean canWin = false;
        
        /**
         * Main evaluation
         */
        SpyVsSpyAction standardReactiveLayerAction = SpyVsSpyControllerHelper.evaluateReactiveLayer(env, body);
        if(standardReactiveLayerAction != null){
            return standardReactiveLayerAction;
        } else if (hasAllItems){
            int destination = env.defs.destination;
            if(!lastPathApplicable(body,destination)){
                findPath(body, destination);
            }
            return followLastPathFound(body);            
        } else if(bodyInfo.numWeapons > 0 && !canWin && findPathToOponent(body)) {
            return followLastPathFound(body);
        } else if(nodeSecure && usefulItemIndex != -1){
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_ITEM, usefulItemIndex);
        } else if(nodeSecure && trapRemoverIndex != -1){
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_TRAP_REMOVER, trapRemoverIndex);     
        } else if(bodyInfo.numWeapons == 0 && findPathToAvailableWeapon(body)){
            return followLastPathFound(body);
        } else if(usefulItemIndex != -1 && canRemoveAllTraps){
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, node.traps.iterator().next() /* The first trap*/);
        } else if(findPathToAvailableUsefulItem(body)){
            return followLastPathFound(body);
        } else if(canRemoveAllTraps && trapRemoverIndex != -1){
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, node.traps.iterator().next() /* The first trap*/);
        } else if(findPathToAvailableUsefulTrapRemover(body)){
            return followLastPathFound(body);
        } else if(findPathToAvailableTrapRemover(body)){
            return followLastPathFound(body);
        } else {
            //random movement
            int randomNode = RandomUtils.randomElementLinearAccess(env.defs.neighbours.get(node.index), rand);
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, randomNode);
        }
        
        
    }
    
    private boolean lastPathApplicable(AgentBody body, int targetNode){
        List<Integer> thePath = lastPathFound.get(body);
        Integer pathIndex = lastPathFoundIndex.get(body);
        if(thePath == null || pathIndex >= thePath.size() - 1){
            return false;
        } else {
            if(thePath.get(pathIndex) == getBodyInfo(body).locationIndex && thePath.get(thePath.size() - 1) == targetNode){
                return true;
            } else {
                return false;
            }
        }
    }
    
    private SpyVsSpyAction followLastPathFound(AgentBody body){
        List<Integer> thePath = lastPathFound.get(body);
        Integer pathIndex = lastPathFoundIndex.get(body);
        if(thePath == null || pathIndex >= thePath.size() - 1){
            logger.debug("Trying to follow finished or invalid path");
            return null;
        } else {
            pathIndex++;
            lastPathFoundIndex.put(body, pathIndex);
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, thePath.get(pathIndex));
        }
    }
    
    private void findPath(AgentBody body,int targetNode){
        AStarResult<Integer> result = astar.findPath(new SpyVsSpyAStarGoal(body.getId(), targetNode, env));
        if(result.isSuccess()){
            lastPathFound.put(body,  result.getPath());
            lastPathFoundIndex.put(body, 0);
        } else {
            lastPathFound.remove(body);
            lastPathFoundIndex.remove(body);
        }

    }


    protected SpyVsSpyBodyInfo getBodyInfo(AgentBody body) {
        return env.bodyInfos.get(body.getId());
    }

    protected boolean canRemoveAllTraps(SpyVsSpyMapNode node, SpyVsSpyBodyInfo bodyInfo) {
        boolean canRemoveAllTraps = true;
        for(int trapType : node.traps){            
            if(bodyInfo.numTrapRemoversCarried[trapType] <= 0){
                canRemoveAllTraps = false;
            } 
        }
        return canRemoveAllTraps;
    }

    protected int getUsefulItemIndex(SpyVsSpyMapNode node, SpyVsSpyBodyInfo bodyInfo) {
        for(int itemType = 0; itemType < env.defs.numItemTypes; itemType++){
            if(node.items.contains(itemType) && !bodyInfo.itemsCarried.contains(itemType)){
                return itemType;
            }
        }
        return  -1;
    }

    protected int getTrapRemover(SpyVsSpyMapNode node) {
        for(int trapType = 0; trapType < env.defs.numTrapTypes; trapType++){
            if(node.numTrapRemovers[trapType] > 0){
                return trapType;
            }
        }
        return -1;
    }

    protected int getOtherId(AgentBody body) {
        return 1 - body.getId();
    }

    private boolean findPathToAvailableTrapRemover(AgentBody body) {
        return false;
    }

    private boolean findPathToAvailableUsefulTrapRemover(AgentBody body) {
        return false;
    }

    private boolean findPathToAvailableUsefulItem(AgentBody body) {
        return false;
    }

    private boolean findPathToAvailableWeapon(AgentBody body) {
        return false;
    }

    private boolean findPathToOponent(AgentBody body) {
        return false;
    }


    

}
