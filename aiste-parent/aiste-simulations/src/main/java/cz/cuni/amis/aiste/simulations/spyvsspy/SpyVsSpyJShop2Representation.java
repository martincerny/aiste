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
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.ISimulableJShop2Representation;
import cz.cuni.amis.aiste.environment.impl.EmptyReactivePlan;
import cz.cuni.amis.aiste.environment.impl.JShop2Problem;
import static cz.cuni.amis.aiste.environment.impl.JShop2Utils.*;
import cz.cuni.amis.aiste.environment.impl.SequencePlan;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyJShop2Representation extends AbstractSpyVsSpyPlanningRepresentation<JSHOP2, IJShop2Problem, Predicate> implements ISimulableJShop2Representation<SpyVsSpyAction, SpyVsSpy, SpyVsSpyPlanningGoal> {

    private final Logger logger = Logger.getLogger(SpyVsSpyJShop2Representation.class);

    int[] locationIdToConstants;
    Map<Integer, Integer> constantsToLocationId;
    
    int[] itemIdToConstants;
    Map<Integer, Integer> constantsToItemId;
    
    /**
     * For simplicity this contains the player as well
     */
    int[] oponentIdToConstants;
    Map<Integer, Integer> constantsToOponentId;
    
    java.util.List<java.util.List<Integer>> trapRemoversToConstants; //first index is the remove type
    Map<Integer, Integer> constantsToTrapRemoverType;
    
    java.util.List<java.util.List<Integer>> trapToConstants; //first index is the trap type
    Map<Integer, Integer> constantsToTrapType;

    java.util.List<Integer> weaponConstants;
    Set<Integer> constantsThatAreWeapons;
    
    
    String[] additionalConstantNames;
    
    Map<AgentBody, JSHOP2> jshops = new HashMap<AgentBody, JSHOP2>();
        
    /**
     * Set of action ids that correspond to no real action
     */
    Set<Integer> ignoredActionsId;
    
    
    java.util.List<Predicate> staticDomainInfo;

    public SpyVsSpyJShop2Representation(SpyVsSpy environment) {
        this.environment = environment;
                
        /**
         * Analyze the domain
         */
        ignoredActionsId = new HashSet<Integer>(Arrays.asList(new Integer[] {
            SpyVsSpyJSHOP2.PRIMITIVE_VISIT,
            SpyVsSpyJSHOP2.PRIMITIVE_UNVISIT,
            SpyVsSpyJSHOP2.PRIMITIVE_FINISH_SECURING,
            SpyVsSpyJSHOP2.PRIMITIVE_START_SECURING,
        }));
        
                
        /**
         * Create common parts of problem spec
         */
        int numNodes = environment.nodes.size();
        
        locationIdToConstants = new int[numNodes];
        constantsToLocationId = new HashMap<Integer, Integer>();        
        additionalConstantNames = new String[getNumAdditionalConstants()];
               
        int problemConstantOffset = SpyVsSpyJSHOP2.NUM_CONSTANTS;
        int nextConstantIndex = problemConstantOffset;


        trapRemoversToConstants = new ArrayList<java.util.List<Integer>>(environment.defs.numTrapTypes);
        trapToConstants = new ArrayList<java.util.List<Integer>>(environment.defs.numTrapTypes);
        constantsToTrapRemoverType = new HashMap<Integer, Integer>();
        constantsToTrapType = new HashMap<Integer, Integer>();        
        weaponConstants = new ArrayList<Integer>();
        constantsThatAreWeapons = new HashSet<Integer>();
        
        
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
                constantsToTrapType.put(nextConstantIndex, trapType);
                additionalConstantNames[nextConstantIndex - problemConstantOffset] = "trap_"  + trapType + "_" + trapToConstants.get(trapType).size();
                nextConstantIndex++;
            }
            
            for(int i  = 0; i < mapNode.numWeapons; i++){
                additionalConstantNames[nextConstantIndex - problemConstantOffset] = "weapon_" + weaponConstants.size();
                weaponConstants.add(nextConstantIndex);
                constantsThatAreWeapons.add(nextConstantIndex);
                nextConstantIndex++;
            }
        }
        
        for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
            for(int trap = 0; trap < environment.defs.trapCounts[trapType]; trap++){
                for(int i  = 0; i < environment.defs.maxPlayers; i++){
                    trapToConstants.get(trapType).add(nextConstantIndex);
                    constantsToTrapType.put(nextConstantIndex, trapType);
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

        
        oponentIdToConstants = new int[environment.defs.maxPlayers];
        constantsToOponentId = new HashMap<Integer, Integer>();
        for(int i = 0; i < environment.defs.maxPlayers; i++){
            oponentIdToConstants[i] = nextConstantIndex;
            constantsToOponentId.put(nextConstantIndex, i);
            additionalConstantNames[nextConstantIndex - problemConstantOffset] = "oponent_" + i;
            nextConstantIndex++;
        }
        
        
        
    }

    
    
    @Override
    public JSHOP2 getDomain(AgentBody body) {
        JSHOP2 jshop = new JSHOP2();
        
        Map<String, Calculate> userFunctions = new HashMap<String, Calculate>();
        userFunctions.put("find_path", new FindPathCalculate(this, body));
        Map<String, Comparator<Term>> userComparators = new HashMap<String, Comparator<Term>>();
        userComparators.put("location_security", new LocationSecurityComparator(this, body));
        
        SpyVsSpyJSHOP2 domain = new SpyVsSpyJSHOP2(jshop, userFunctions, userComparators);
        jshop.initialize(domain, getMaxNumConstants());
        jshops.put(body, jshop);
        return jshop;
    }

    protected int getMaxNumConstants() {
        int numAdditionalConstants = getNumAdditionalConstants() ;
            
        return (20 /*Za domenu, nutno staticky*/ + numAdditionalConstants);            

    }   

    protected int getNumAdditionalConstants() {
        int trapsPerPlayer = 0;
        for(int i = 0; i < environment.defs.numTrapTypes; i++){
            trapsPerPlayer += environment.defs.trapCounts[i];
        }
        int numAdditionalConstants = environment.nodes.size() /* locations */ + environment.defs.numItemTypes /*items*/ + environment.defs.maxPlayers
                + (environment.nodes.size() * (environment.defs.numTrapTypes * 2 + 1)) /* traps and removers and weapn instances in the map, worst case*/ 
                + (environment.defs.maxPlayers * trapsPerPlayer) /*traps in player possession, worst case*/;
        return numAdditionalConstants;
    }

    @Override
    public IJShop2Problem getProblem(AgentBody body, SpyVsSpyPlanningGoal goal) {
        
        /**
         * Create static predicates
         */
        JSHOP2 jshop = jshops.get(body);
        if(jshop == null){
            throw new IllegalStateException("Getting problem before getting domain");
        }
        
        staticDomainInfo = new ArrayList<Predicate>();
        for(SpyVsSpyMapNode node : environment.nodes){            
            staticDomainInfo.add(new Predicate(SpyVsSpyJSHOP2.CONST_LOCATION, 0, createTermList(jshop, locationIdToConstants[node.index])));
        }
                
        for(Map.Entry<Integer,java.util.List<Integer>> neighbourEntry : environment.defs.neighbours.entrySet()){
            int from = neighbourEntry.getKey();
            for(int to : neighbourEntry.getValue()){                
                staticDomainInfo.add(new Predicate(SpyVsSpyJSHOP2.CONST_ADJACENT, 0, createTermList(jshop, locationIdToConstants[from], locationIdToConstants[to])));
            }
        }
        
        for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
            for(int removerId: trapRemoversToConstants.get(trapType)){
                for(int trapId: trapToConstants.get(trapType)){                    
                    staticDomainInfo.add(new Predicate(SpyVsSpyJSHOP2.CONST_REMOVES_TRAP,  0, createTermList(jshop, removerId, trapId)));
                }
            }
        }
        
        for(int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++){
            for(int removerId: trapRemoversToConstants.get(trapType)){                
                staticDomainInfo.add(new Predicate(SpyVsSpyJSHOP2.CONST_TRAP_REMOVER,  0, createTermList(jshop, removerId)));
            }
            for(int trapId: trapToConstants.get(trapType)){ 
                staticDomainInfo.add(new Predicate(SpyVsSpyJSHOP2.CONST_TRAP,  0, createTermList(jshop, trapId)));
            }
        }
        
        for(int itemType = 0; itemType < environment.defs.numItemTypes; itemType++){ 
            staticDomainInfo.add(new Predicate(SpyVsSpyJSHOP2.CONST_ITEM,  0, createTermList(jshop, itemIdToConstants[itemType])));
        }
        
        for(int weaponConstantId : weaponConstants){
            staticDomainInfo.add(new Predicate(SpyVsSpyJSHOP2.CONST_WEAPON, 0 , createTermList(jshop, weaponConstantId)));
        }
        
        
        State initialState = new State(jshop.getDomain().getAxioms().length, jshop.getDomain().getAxioms());
        for(Predicate p : staticDomainInfo){
            initialState.add(p);
        }
        
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        
        initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_PLAYER_AT, 0, createTermList(jshop, locationIdToConstants[info.locationIndex])));

        /**
         * Objects in the environment
         */
        int[] nextTrapIndices = new int[environment.defs.numTrapTypes];
        int[] nextRemoverIndices = new int[environment.defs.numTrapTypes];
        int nextWeaponIndex = 0;
        for (SpyVsSpyMapNode mapNode : environment.nodes) {
            for (int trapType = 0; trapType < environment.defs.numTrapTypes; trapType++) {
                for (int remover = 0; remover < mapNode.numTrapRemovers[trapType]; remover++) { 
                    initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_OBJECT_AT, 0, createTermList(jshop, trapRemoversToConstants.get(trapType).get(nextRemoverIndices[trapType]), locationIdToConstants[mapNode.index])));
                    nextRemoverIndices[trapType]++;
                }
            }
            for (int trapType : mapNode.traps) { 
                initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_TRAP_SET, 0, createTermList(jshop, trapToConstants.get(trapType).get(nextTrapIndices[trapType]), locationIdToConstants[mapNode.index])));
                nextTrapIndices[trapType]++;
            }
            for (int itemType : mapNode.items){
                initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_OBJECT_AT, 0, createTermList(jshop, itemIdToConstants[itemType], locationIdToConstants[mapNode.index])));
            }
            for(int weaponId = 0; weaponId < mapNode.numWeapons; weaponId++){
                initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_OBJECT_AT, 0, createTermList(jshop, weaponConstants.get(nextWeaponIndex), locationIdToConstants[mapNode.index])));                
                nextWeaponIndex++;
            }
        }
        
        /**
         * Objects carried by the player
         */
        for(int itemType : info.itemsCarried){ 
            initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_CARRYING, 0, createTermList(jshop, itemIdToConstants[itemType])));
        }
        for(int trapType = 0 ; trapType < environment.defs.numTrapTypes; trapType++){
            for(int remover = 0; remover < info.numTrapRemoversCarried[trapType]; remover++){                
                initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_CARRYING, 0, createTermList(jshop, trapRemoversToConstants.get(trapType).get(nextRemoverIndices[trapType]))));
                nextRemoverIndices[trapType]++;
            }
            for(int trap = 0; trap < info.numTrapsCarried[trapType]; trap++){                
                initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_CARRYING, 0, createTermList(jshop, trapToConstants.get(trapType).get(nextTrapIndices[trapType]))));
                nextTrapIndices[trapType]++;
            }
        }
        for(int i = 0 ; i < info.numWeapons; i++){ 
            initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_CARRYING, 0, createTermList(jshop, weaponConstants.get(nextWeaponIndex))));
            nextWeaponIndex++;
        }
        
        initialState.add(new Predicate(SpyVsSpyJSHOP2.CONST_USE_DIRECT_MOVES, 0, TermList.NIL));        
                
        TaskList tasks;
        switch(goal.getType()){
            case DIRECT_WIN: {
                tasks = new TaskList(2, true);
                TaskList itemTasks = new TaskList(environment.defs.numItemTypes, false); //unordered task to gather all items
                for(int i = 0; i < environment.defs.numItemTypes; i++){
                    itemTasks.subtasks[i] = new TaskList(new TaskAtom(new Predicate(SpyVsSpyJSHOP2.METHOD_HAVE, 0, createTermList(jshop, itemIdToConstants[i])), false, false));
                }        
                tasks.subtasks[0] = itemTasks;
                tasks.subtasks[1] = new TaskList(new TaskAtom(new Predicate(SpyVsSpyJSHOP2.METHOD_MOVE, 0, createTermList(jshop, locationIdToConstants[environment.defs.destination])), false, false));
                break;
            } case GET_ARMED : {
                tasks = new TaskList(new TaskAtom(new Predicate(SpyVsSpyJSHOP2.METHOD_GET_ARMED, 0, TermList.NIL), false, false));
                break;
            } case KILL_OPONENT: {
                tasks = new TaskList(new TaskAtom(new Predicate(SpyVsSpyJSHOP2.METHOD_KILL, 0, createTermList(jshop, oponentIdToConstants[goal.getParameter()])), false, false));
                break;
            }
            default: {
                throw new IllegalStateException("Unrecognized goal type: " + goal.getType());
            }
                
        }
                
        return new JShop2Problem(additionalConstantNames, initialState, tasks);
    }


    @Override
    public IReactivePlan<? extends SpyVsSpyAction> translateAction(Predicate actionFromPlanner, AgentBody body) {
        if (!actionFromPlanner.isGround()) {
            throw new AisteException("Cannot translate non-ground action");
        }

        Domain domain = jshops.get(body).getDomain();
        GroundActionInfo info = getGroundInfo(actionFromPlanner);
        String actionName = domain.getPrimitiveTasks()[info.actionId];
        
        
        if(info.actionId == SpyVsSpyJSHOP2.PRIMITIVE_MOVE){
            int constantIndex = info.params.get(1);
            Integer locationId = constantsToLocationId.get(constantIndex);
            if(locationId == null){
                throw new AisteException("Constant index " + constantIndex + " (" + domain.getConstant(constantIndex) + ") does not correspond to a valid location");
            }
            return new SequencePlan(new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, locationId));
        } else if(info.actionId ==  SpyVsSpyJSHOP2.PRIMITIVE_TAKE){
            int constantIndex = info.params.get(0);
            if(constantsToItemId.containsKey(constantIndex)){
                return new SequencePlan(new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_ITEM, constantsToItemId.get(constantIndex)));
            } else if (constantsToTrapRemoverType.containsKey(constantIndex)){
                return new SequencePlan(new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_TRAP_REMOVER, constantsToTrapRemoverType.get(constantIndex)));
            } else if (constantsThatAreWeapons.contains(constantIndex)) {
                return new SequencePlan(new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_WEAPON, -1 /*Parameter is ignored*/));
            } else {
                throw new AisteException("Unrecognized object to take");
            }
        } else if(info.actionId ==  SpyVsSpyJSHOP2.PRIMITIVE_REMOVE_TRAP){
            return new SequencePlan(new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, constantsToTrapType.get(info.params.get(0))));
        } else if(info.actionId == SpyVsSpyJSHOP2.PRIMITIVE_HUNT_WITH_WEAPON){
            return getFollowAndAttackReactivePlan(body, constantsToOponentId.get(info.params.get(1)));
        }
        else if (ignoredActionsId.contains(info.actionId)){
            return EmptyReactivePlan.<SpyVsSpyAction>emptyPlan();
        }
        else {
            throw new AisteException("Unrecognized action: " + actionName);
        }

    }

        
}
