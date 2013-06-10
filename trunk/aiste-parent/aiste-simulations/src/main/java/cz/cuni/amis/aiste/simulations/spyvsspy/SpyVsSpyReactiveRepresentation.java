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
import cz.cuni.amis.pathfinding.alg.floydwarshall.FloydWarshall;
import cz.cuni.amis.pathfinding.map.IPFGoal;
import cz.cuni.amis.utils.heap.IHeap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 *
 * @author 
 */
public class SpyVsSpyReactiveRepresentation extends AbstractSpyVsSpyRepresentation implements IEnvironmentSpecificRepresentation<SpyVsSpyAction>, IRandomizable{

    private final Logger logger = Logger.getLogger(SpyVsSpyReactiveRepresentation.class);
    
    FloydWarshall<Integer> floydWarshall;
    
    /**
     * Last path returned from AStar, including the start and final location
     */
    Map<AgentBody,List<Integer>> lastPathFound = new HashMap<AgentBody, List<Integer>>();
    Map<AgentBody, Integer> lastPathFoundIndex = new HashMap<AgentBody, Integer>();
    
    Random rand = new Random();

    public SpyVsSpyReactiveRepresentation(SpyVsSpy env) {
        this.environment = env;
        floydWarshall = new FloydWarshall<Integer>(env.defs.mapForPathFinding);
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
        for(int i = 0; i < environment.defs.numItemTypes; i++){
            if(!bodyInfo.itemsCarried.contains(i)){
                hasAllItems = false;
                break;
            }
        }
        
        SpyVsSpyMapNode currentNode = environment.nodes.get(bodyInfo.locationIndex);
        boolean nodeSecure = currentNode.traps.isEmpty();
        
        int usefulItemIndex = getUsefulItemIndex(currentNode, bodyInfo);
        int trapRemoverIndex = getTrapRemover(currentNode);        
        int usefulTrapRemoverIndex = getUsefulTrapRemover(bodyInfo, currentNode);        
        boolean canRemoveAllTraps = canRemoveAllTraps(currentNode, bodyInfo);
        
        
        /**
         * Main evaluation
         */
        SpyVsSpyAction standardReactiveLayerAction = SpyVsSpyControllerHelper.evaluateReactiveLayer(environment, body);
        if(standardReactiveLayerAction != null){
            logger.debug(body.getId() + ": Action from reactive layer");
            return standardReactiveLayerAction;
        } else if (hasAllItems){
            logger.debug(body.getId() + ": Has all items.");
            int destination = environment.defs.destination;
            if(!lastPathApplicable(body,destination)){
                findPath(body, destination);
            }
            return followLastPathFound(body);            
        } else if(bodyInfo.numWeapons > 0 && agentUnableToReachGoal(body) && findPathToOponent(body)) {
            logger.debug(body.getId() + ": Cannot win but can hunt enemy");
            return followLastPathFound(body);
        } else if(nodeSecure && usefulItemIndex != -1){
            logger.debug(body.getId() + ": Useful unguarded item");
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_ITEM, usefulItemIndex);
        } else if(nodeSecure && trapRemoverIndex != -1){
            logger.debug(body.getId() + ": Unguarded trap remover");
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_TRAP_REMOVER, trapRemoverIndex);     
        } else if (bodyInfo.numWeapons == 0 && currentNode.numWeapons > 0 && canRemoveAllTraps) {
            logger.debug(body.getId() + ": Removing traps for weapon");
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, currentNode.traps.iterator().next() /* The first trap*/);
        } else  {
            
            /**
             * For the rest of deliberation I need to find nearest locations of various kinds.
             * To save computing time, they are all searched for simultaneously and the decisions
             * are made afterwards.
             */
           
            int nearestAvailableWeaponLocation = -1;
            int nearestAvailableWeaponLocationDistance = Integer.MAX_VALUE;

            int nearestAvailableUsefulItemLocation = -1;
            int nearestAvailableUsefulItemLocationDistance = Integer.MAX_VALUE;
            
            int nearestAvailableUsefulTrapRemoverLocation = -1;
            int nearestAvailableUsefulTrapRemoverDistance = Integer.MAX_VALUE;

            int nearestAvailableTrapRemoverLocation = -1;
            int nearestAvailableTrapRemoverLocationDistance = Integer.MAX_VALUE;

            for(SpyVsSpyMapNode inspectedNode : environment.nodes){

                if(!canRemoveAllTraps(inspectedNode, bodyInfo)){
                    continue;
                }
                
                int nodeDistance = floydWarshall.getPathCost(bodyInfo.locationIndex, inspectedNode.index);
                
                //Available trap removers and useful trap removers (exploiting the fact, that they are always a subset)
                if(nodeDistance < nearestAvailableTrapRemoverLocationDistance){                
                    for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
                        if(inspectedNode.numTrapRemovers[trapType] > 0){
                            nearestAvailableTrapRemoverLocationDistance = nodeDistance;
                            nearestAvailableTrapRemoverLocation = inspectedNode.index;
                            if(bodyInfo.numTrapRemoversCarried[trapType] == 0 && nodeDistance < nearestAvailableUsefulTrapRemoverDistance){
                                nearestAvailableUsefulTrapRemoverDistance = nodeDistance;
                                nearestAvailableUsefulTrapRemoverLocation = inspectedNode.index;
                                break;
                            }
                        }
                    }
                }
                
                if(nodeDistance < nearestAvailableWeaponLocationDistance && inspectedNode.numWeapons > 0){
                    nearestAvailableWeaponLocationDistance = nodeDistance;
                    nearestAvailableWeaponLocation = inspectedNode.index;
                }
                
                if(nodeDistance < nearestAvailableUsefulItemLocationDistance && getUsefulItemIndex(inspectedNode, bodyInfo) != -1){
                    nearestAvailableUsefulItemLocationDistance = nodeDistance;
                    nearestAvailableUsefulItemLocation = inspectedNode.index;
                }
                
            }
            
            if (bodyInfo.numWeapons == 0 && nearestAvailableWeaponLocation != -1) {
                logger.debug(body.getId() + ": Moving to available weapon at loc " + nearestAvailableWeaponLocation);
                usePathFromFloydWarshall(body, nearestAvailableWeaponLocation);
                return followLastPathFound(body);
            } else if (usefulItemIndex != -1 && canRemoveAllTraps) {
                logger.debug(body.getId() + ": Removing traps for useful item");
                return new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, currentNode.traps.iterator().next() /* The first trap*/);
            } else if (nearestAvailableUsefulItemLocation != -1) {
                logger.debug(body.getId() + ": Moving to available useful item at loc " + nearestAvailableUsefulItemLocation);
                usePathFromFloydWarshall(body, nearestAvailableUsefulItemLocation);                
                return followLastPathFound(body);
            } else if (canRemoveAllTraps && usefulTrapRemoverIndex != -1) {
                logger.debug(body.getId() + ": Removing traps for a useful trap remover");
                return new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, currentNode.traps.iterator().next() /* The first trap*/);
            } else if (nearestAvailableUsefulTrapRemoverLocation != -1) {
                logger.debug(body.getId() + ": Moving to available useful trap remover at loc " + nearestAvailableUsefulTrapRemoverLocation);                
                usePathFromFloydWarshall(body, nearestAvailableUsefulTrapRemoverLocation);
                return followLastPathFound(body);
            } else if(bodyInfo.numWeapons > 0 && findPathToOponent(body)) {
                logger.debug(body.getId() + ": Nothing better to do, hunting enemy");
                return followLastPathFound(body);
            } else if (canRemoveAllTraps && trapRemoverIndex != -1) {
                logger.debug(body.getId() + ": Removing traps for a trap remover");
                return new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, currentNode.traps.iterator().next() /* The first trap*/);
            } else if (nearestAvailableTrapRemoverLocation != -1) {
                logger.debug(body.getId() + ": Moving to available trap remover at loc " + nearestAvailableTrapRemoverLocation);
                usePathFromFloydWarshall(body, nearestAvailableTrapRemoverLocation);                
                return followLastPathFound(body);
            } else {
                //random movement
                logger.debug(body.getId() + ": Random movement");
                int randomNode = RandomUtils.randomElementLinearAccess(environment.defs.neighbours.get(currentNode.index), rand);
                return new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, randomNode);
            }
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
            logger.debug(body.getId() + ": Trying to follow finished or invalid path");
            return null;
        } else {
            pathIndex++;
            lastPathFoundIndex.put(body, pathIndex);
            Integer nextLocation = thePath.get(pathIndex);
            int currentLocation = getBodyInfo(body).locationIndex;
            if(!environment.defs.neighbours.get(currentLocation).contains(nextLocation)){
                logger.debug(body.getId() + ": Invalid location. From: " + currentLocation + " to: " + nextLocation);
            }
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, nextLocation);
        }
    }
    
    private boolean findPath(AgentBody body,int targetNode){
        return usePathFromFloydWarshall(body, targetNode);
    }


    protected SpyVsSpyBodyInfo getBodyInfo(AgentBody body) {
        return environment.bodyInfos.get(body.getId());
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
        for(int itemType = 0; itemType < environment.defs.numItemTypes; itemType++){
            if(node.items.contains(itemType) && !bodyInfo.itemsCarried.contains(itemType)){
                return itemType;
            }
        }
        return  -1;
    }

    protected int getTrapRemover(SpyVsSpyMapNode node) {
        for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
            if(node.numTrapRemovers[trapType] > 0){
                return trapType;
            }
        }
        return -1;
    }

    protected int getUsefulTrapRemover(SpyVsSpyBodyInfo bodyInfo, SpyVsSpyMapNode node) {
        for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
            if(node.numTrapRemovers[trapType] > 0 && bodyInfo.numTrapRemoversCarried[trapType] == 0){
                return trapType;
            }
        }
        return -1;
    }

    protected int getOtherId(AgentBody body) {
        return 1 - body.getId();
    }
    
   

    private boolean findPathToOponent(final AgentBody body) {
        return findPath(body, environment.bodyInfos.get(getOtherId(body)).locationIndex);
    }

    protected boolean usePathFromFloydWarshall(AgentBody body, int targetNode) {
//        if(lastPathApplicable(body, targetNode)){
//            return true;
//        }
        
        int startNode = getBodyInfo(body).locationIndex;
        int pathCost = floydWarshall.getPathCost(startNode, targetNode);
        if(pathCost < Integer.MAX_VALUE){
            List<Integer> path = new ArrayList<Integer>(pathCost + 2);
            path.add(startNode);
            path.addAll(floydWarshall.getPath(startNode, targetNode));
            if(targetNode != startNode){
                path.add(targetNode);
            }
            lastPathFound.put(body,  path);
            lastPathFoundIndex.put(body, 0);
            //logger.debug("Path from " + startNode + " to " + targetNode + ": " + path);
            return true;
        } else {
            lastPathFound.remove(body);
            lastPathFoundIndex.remove(body);
            return false;
        }
    }

    private abstract class ABreadthFirstSearchAStarGoal implements IPFGoal<Integer>{

        int start;

        public ABreadthFirstSearchAStarGoal(int start) {
            this.start = start;
        }
        
        @Override
        public Integer getStart() {
            return start;
        }

        @Override
        public int getEstimatedCostToGoal(Integer node) {
            if(isGoalReached(node)){
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public void setOpenList(IHeap<Integer> openList) {
        }

        @Override
        public void setCloseList(Set<Integer> closedList) {
        }
        
    }
    

}
