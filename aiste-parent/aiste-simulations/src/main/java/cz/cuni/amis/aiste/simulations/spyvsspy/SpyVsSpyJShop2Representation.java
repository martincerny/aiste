/*
 * Copyright (C) 2013 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
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

import JSHOP2.*;
import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.SpyVsSpyJSHOP2;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IJShop2Problem;
import cz.cuni.amis.aiste.environment.ISimulableJShop2Representation;
import cz.cuni.amis.aiste.environment.impl.JShop2Problem;
import static cz.cuni.amis.aiste.environment.impl.JShop2Utils.*;
import cz.cuni.amis.planning4j.ActionDescription;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyJShop2Representation extends AbstractSpyVsSpyRepresentation implements ISimulableJShop2Representation<SpyVsSpyAction, SpyVsSpy> {

    private final cz.cuni.amis.aiste.SpyVsSpyJSHOP2 domain;
    private final Logger logger = Logger.getLogger(SpyVsSpyJShop2Representation.class);

    int locationPredicateIndex;
    int removesTrapPredicateIndex;
    int trapSetPredicateIndex;    
    int objectAtPredicateIndex;

    int[] locationIdToConstants;
    Map<Integer, Integer> constantsToLocationId;
    
    int[] itemIdToConstants;
    Map<Integer, Integer> constantsToItemId;
    
    java.util.List<java.util.List<Integer>> trapRemoversToConstants; //first index is the remove type
    Map<Integer, Integer> constantsToTrapRemoverType;
    
    java.util.List<java.util.List<Integer>> trapToConstants; //first index is the trap type
    Map<Integer, Integer> constantsToTrapType;
    
    String[] additionalConstantNames;
    
    int moveActionId;
    int takeActionId;
    int removeTrapActionId;
    
    int haveMethodId;
    int moveMethodId;
    
    int playerAtPredicateId;
    int adjacentPredicateId;
    int locationPredicateId;
    int carryingPredicateId;
    int removerPredicateId;
    int trapPredicateId;
    int itemPredicateId;
    
    
    boolean termsInitialized = false;
    
    /**
     * Set of action ids that correspond to no real action
     */
    Set<Integer> ignoredActionsId;
    
    
    java.util.List<Predicate> staticDomainInfo;

    public SpyVsSpyJShop2Representation(SpyVsSpy environment) {
        this.environment = environment;
        
        initializeTermConstants();
        
        domain = new SpyVsSpyJSHOP2();
        
        /**
         * Analyze the domain
         */
        ignoredActionsId = new HashSet<Integer>();
        Set<String> ignoredActionsNames = new HashSet<String>(Arrays.asList(new String[]{
            "!visit",
            "!unvisit"
        }));
        
        //analyze action names
        for(int i = 0; i < domain.getPrimitiveTasks().length; i++){
            String currentAction = domain.getPrimitiveTasks()[i];
            if(currentAction.equals("!move")){
                moveActionId = i;
            } else if(currentAction.equals("!take")){
                takeActionId = i;
            } else if(currentAction.equals("!remove_trap")) {
                removeTrapActionId = i;
            }
            else if(ignoredActionsNames.contains(currentAction)){
                ignoredActionsId.add(i);
            }
        }
        
        for(int i = 0; i < domain.getDomainConstantCount(); i++){
            String currentConstant = domain.getDomainConstant(i);
            if(currentConstant.equals("player_at")){
                playerAtPredicateId = i;
            } else if(currentConstant.equals("adjacent")){
                adjacentPredicateId = i;
            } else if(currentConstant.equals("location")){
                locationPredicateId = i;
            } else if(currentConstant.equals("object_at")){
                objectAtPredicateIndex = i;
            } else if(currentConstant.equals("trap_set")){
                trapSetPredicateIndex = i;
            } else if(currentConstant.equals("removes_trap")){
                removesTrapPredicateIndex = i;
            }  else if(currentConstant.equals("carrying")){
                carryingPredicateId = i;
            } else if(currentConstant.equals("trap")){
                trapPredicateId = i;
            } else if(currentConstant.equals("item")){
                itemPredicateId = i;
            } else if(currentConstant.equals("trap_remover")){
                removerPredicateId = i;
            }
        }
        
        for(int i = 0; i < domain.getCompoundTasks().length; i++){
            String currentTask = domain.getCompoundTasks()[i];
            if(currentTask.equals("move")){
                moveMethodId = i;
            }
            else if(currentTask.equals("have")){
                haveMethodId = i;
            }            
        }
        
        /**
         * Create common parts of problem spec
         */
        int numNodes = environment.nodes.size();
        
        locationIdToConstants = new int[numNodes];
        constantsToLocationId = new HashMap<Integer, Integer>();
        additionalConstantNames = new String[getNumAdditionalConstants()];
        
        int problemConstantOffset = domain.getDomainConstantCount();
        int nextConstantIndex = problemConstantOffset;


        trapRemoversToConstants = new ArrayList<java.util.List<Integer>>(environment.defs.numTrapTypes);
        trapToConstants = new ArrayList<java.util.List<Integer>>(environment.defs.numTrapTypes);
        constantsToTrapRemoverType = new HashMap<Integer, Integer>();
        constantsToTrapType = new HashMap<Integer, Integer>();
        
        for(int i = 0; i < environment.defs.numTrapTypes; i++){
            trapRemoversToConstants.add(new ArrayList<Integer>());
            trapToConstants.add(new ArrayList<Integer>());
        }
                
        
        for(SpyVsSpyMapNode mapNode : environment.nodes){
            locationIdToConstants[mapNode.index] = nextConstantIndex;
            constantsToLocationId.put(nextConstantIndex, mapNode.index);
            additionalConstantNames[nextConstantIndex - problemConstantOffset] = "location_" + mapNode.index;            
            nextConstantIndex++;
            
            for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
                for(int remover = 0; remover < mapNode.numTrapRemovers[trapType]; remover++){
                    trapRemoversToConstants.get(trapType).add(nextConstantIndex);
                    constantsToTrapRemoverType.put(nextConstantIndex, trapType);
                    additionalConstantNames[nextConstantIndex - problemConstantOffset] = "trap_remover_" + trapType + "_" + trapRemoversToConstants.get(trapType).size();
                    nextConstantIndex++;
                }
            }
            for(int trapType : mapNode.traps){
                trapToConstants.get(trapType).add(nextConstantIndex);
                constantsToTrapRemoverType.put(nextConstantIndex, trapType);
                additionalConstantNames[nextConstantIndex - problemConstantOffset] = "trap_"  + trapType + "_" + trapToConstants.get(trapType).size();
                nextConstantIndex++;
            }
        }
        
        for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
            for(int trap = 0; trap < environment.defs.trapCounts[trapType]; trap++){
                for(int i  = 0; i < environment.defs.maxPlayers; i++){
                    trapToConstants.get(trapType).add(nextConstantIndex);
                    constantsToTrapRemoverType.put(nextConstantIndex, trapType);
                    additionalConstantNames[nextConstantIndex - problemConstantOffset] = "trap_"  + trapType + "_" + trapToConstants.get(trapType).size();
                    nextConstantIndex++;
                }
            }
        }
        
        itemIdToConstants = new int[environment.defs.numItemTypes];
        constantsToItemId = new HashMap<Integer, Integer>();
        for(int itemType = 0; itemType < environment.defs.numItemTypes; itemType++){
            itemIdToConstants[itemType] = nextConstantIndex;
            constantsToItemId.put(nextConstantIndex, itemType);
            additionalConstantNames[nextConstantIndex - problemConstantOffset] = "item_" + itemType;
            nextConstantIndex++;
        }
        
        /**
         * Create static predicates
         */
        
        staticDomainInfo = new ArrayList<Predicate>();
        for(SpyVsSpyMapNode node : environment.nodes){
            staticDomainInfo.add(new Predicate(locationPredicateId, 0, createTermList(locationIdToConstants[node.index])));
        }
                
        for(Map.Entry<Integer,java.util.List<Integer>> neighbourEntry : environment.defs.neighbours.entrySet()){
            int from = neighbourEntry.getKey();
            for(int to : neighbourEntry.getValue()){
                staticDomainInfo.add(new Predicate(adjacentPredicateId, 0, createTermList(locationIdToConstants[from], locationIdToConstants[to])));
            }
        }
        
        for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
            for(int removerId: trapRemoversToConstants.get(trapType)){
                for(int trapId: trapToConstants.get(trapType)){
                    staticDomainInfo.add(new Predicate(removesTrapPredicateIndex,  0, createTermList(removerId, trapId)));
                }
            }
        }
        
        for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
            for(int removerId: trapRemoversToConstants.get(trapType)){
                staticDomainInfo.add(new Predicate(removerPredicateId,  0, createTermList(removerId)));
            }
            for(int trapId: trapToConstants.get(trapType)){
                staticDomainInfo.add(new Predicate(trapPredicateId,  0, createTermList(trapId)));
            }
        }
        
        for(int itemType = 0; itemType < environment.defs.numItemTypes; itemType++){
            staticDomainInfo.add(new Predicate(itemPredicateId,  0, createTermList(itemIdToConstants[itemType])));
        }
        
    }

    
    
    @Override
    public Domain getDomain(AgentBody body) {
        initializeTermConstants();
        return domain;
    }

    private void initializeTermConstants() {
        if(!termsInitialized){
            int numAdditionalConstants = getNumAdditionalConstants() ;
            
            TermConstant.initialize(20 /*Za domenu, nutno staticky*/ + numAdditionalConstants);            
            termsInitialized = true;
        }
    }

    protected int getNumAdditionalConstants() {
        int trapsPerPlayer = 0;
        for(int i = 0; i < environment.defs.numTrapTypes; i++){
            trapsPerPlayer += environment.defs.trapCounts[i];
        }
        int numAdditionalConstants = environment.nodes.size() /* locations */ + environment.defs.numItemTypes /*items*/ 
                + (environment.nodes.size() * (environment.defs.numTrapTypes * 2)) /* traps and removers instances in the map, worst case*/ 
                + (environment.defs.maxPlayers * trapsPerPlayer) /*traps in player possession, worst case*/;
        return numAdditionalConstants;
    }

    @Override
    public IJShop2Problem getProblem(AgentBody body) {
        
        State initialState = new State(domain.getAxioms().length, domain.getAxioms());
        for(Predicate p : staticDomainInfo){
            initialState.add(p);
        }
        
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        
        initialState.add(new Predicate(playerAtPredicateId, 0, createTermList(locationIdToConstants[info.locationIndex])));

        /**
         * Objects in the environment
         */
        int[] nextTrapIndices = new int[environment.defs.numTrapTypes];
        int[] nextRemoverIndices = new int[environment.defs.numTrapTypes];
        for (SpyVsSpyMapNode mapNode : environment.nodes) {
            for (int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++) {
                for (int remover = 0; remover < mapNode.numTrapRemovers[trapType]; remover++) {
                    initialState.add(new Predicate(objectAtPredicateIndex, 0, createTermList(trapRemoversToConstants.get(trapType).get(nextRemoverIndices[trapType]), locationIdToConstants[mapNode.index])));
                    nextRemoverIndices[trapType]++;
                }
            }
            for (int trapType : mapNode.traps) {
                initialState.add(new Predicate(trapSetPredicateIndex, 0, createTermList(trapToConstants.get(trapType).get(nextTrapIndices[trapType]), locationIdToConstants[mapNode.index])));
                nextTrapIndices[trapType]++;
            }
            for (int itemType : mapNode.items){
                initialState.add(new Predicate(objectAtPredicateIndex, 0, createTermList(itemIdToConstants[itemType], locationIdToConstants[mapNode.index])));
            }
        }
        
        /**
         * Objects carried by the player
         */
        for(int itemType : info.itemsCarried){
            initialState.add(new Predicate(carryingPredicateId, 0, createTermList(itemIdToConstants[itemType])));
        }
        for(int trapType = 0 ; trapType < environment.defs.numTrapTypes; trapType++){
            for(int remover = 0; remover < info.numTrapRemoversCarried[trapType]; remover++){                
                initialState.add(new Predicate(carryingPredicateId, 0, createTermList(trapRemoversToConstants.get(trapType).get(nextRemoverIndices[trapType]))));
                nextRemoverIndices[trapType]++;
            }
            for(int trap = 0; trap < info.numTrapsCarried[trapType]; trap++){                
                initialState.add(new Predicate(carryingPredicateId, 0, createTermList(trapToConstants.get(trapType).get(nextTrapIndices[trapType]))));
                nextTrapIndices[trapType]++;
            }
        }
        
                
        TaskList tasks = new TaskList(2, true);
        TaskList itemTasks = new TaskList(environment.defs.numItemTypes, false); //unordered task to gather all items
        for(int i = 0; i < environment.defs.numItemTypes; i++){
            itemTasks.subtasks[i] = new TaskList(new TaskAtom(new Predicate(haveMethodId, 0, createTermList(itemIdToConstants[i])), false, false));
        }        
        tasks.subtasks[0] = itemTasks;
        tasks.subtasks[1] = new TaskList(new TaskAtom(new Predicate(moveMethodId, 0, createTermList(locationIdToConstants[environment.defs.destination])), false, false));
        return new JShop2Problem(additionalConstantNames, initialState, tasks);
    }


    @Override
    public java.util.List<? extends SpyVsSpyAction> translateAction(Predicate actionFromPlanner) {
        if (!actionFromPlanner.isGround()) {
            throw new AisteException("Cannot translate non-ground action");
        }

        GroundActionInfo info = getGroundInfo(actionFromPlanner);
        String actionName = domain.getPrimitiveTasks()[info.actionId];
        
        
        if(info.actionId == moveActionId){
            int constantIndex = info.params.get(1);
            Integer locationId = constantsToLocationId.get(constantIndex);
            if(locationId == null){
                throw new AisteException("Constant index " + constantIndex + " (" + domain.getConstant(constantIndex) + ") does not correspond to a valid location");
            }
            return Collections.singletonList(new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, locationId));
        } else if(info.actionId == takeActionId){
            int constantIndex = info.params.get(0);
            if(constantsToItemId.containsKey(constantIndex)){
                return Collections.singletonList(new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_ITEM, constantsToItemId.get(constantIndex)));
            } else if (constantsToTrapRemoverType.containsKey(constantIndex)){
                return Collections.singletonList(new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_TRAP_REMOVER, constantsToTrapRemoverType.get(constantIndex)));
            } else {
                throw new AisteException("Unrecognized object to take");
            }
        } else if(info.actionId == removeTrapActionId){
            return Collections.singletonList(new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, constantsToTrapType.get(info.params.get(0))));
        }
        else if (ignoredActionsId.contains(info.actionId)){
            return Collections.EMPTY_LIST;
        }
        else {
            throw new AisteException("Unrecognized action: " + actionName);
        }

    }

    
    
}
