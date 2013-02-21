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

    int[] locationIdToConstants;
    Map<Integer, Integer> constantsToLocationId;
    
    String[] additionalConstantNames;
    
    int moveActionId;
    
    int playerAtPredicateId;
    int adjacentPredicateId;
    int locationPredicateId;
    
    boolean termsInitialized = false;
    
    /**
     * Set of action ids that correspond to no real action
     */
    Set<Integer> ignoredActionsId;
    

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
            } else if(ignoredActionsNames.contains(currentAction)){
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
            }
        }
        
        /**
         * Create common parts of problem spec
         */
        int numNodes = environment.nodes.size();
        
        locationIdToConstants = new int[numNodes];
        constantsToLocationId = new HashMap<Integer, Integer>();
        additionalConstantNames = new String[numNodes];
        
        int problemConstantOffset = domain.getDomainConstantCount();
        int nextConstantIndex = problemConstantOffset;
        
        for(SpyVsSpyMapNode mapNode : environment.nodes){
            locationIdToConstants[mapNode.index] = nextConstantIndex;
            constantsToLocationId.put(nextConstantIndex, mapNode.index);
            additionalConstantNames[nextConstantIndex - problemConstantOffset] = "location_" + mapNode.index;
            nextConstantIndex++;
        }
    }

    
    
    @Override
    public Domain getDomain(AgentBody body) {
        initializeTermConstants();
        return domain;
    }

    private void initializeTermConstants() {
        if(!termsInitialized){
            TermConstant.initialize(20 /*Za domenu, nutno staticky*/ + environment.nodes.size() /* locations */);            
            termsInitialized = true;
        }
    }

    @Override
    public IJShop2Problem getProblem(AgentBody body) {
        
        State initialState = new State(domain.getAxioms().length, domain.getAxioms());
        addStaticDomainInfo(initialState);
        
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        
        initialState.add(new Predicate(playerAtPredicateId, 0, createTermList(locationIdToConstants[info.locationIndex])));
        
        TaskList tasks = new TaskList(1, true);
        tasks.subtasks[0] = new TaskList(new TaskAtom(new Predicate(moveActionId, 0, createTermList(locationIdToConstants[environment.defs.destination])), false, false));
        return new JShop2Problem(additionalConstantNames, initialState, tasks);
    }

    protected void addStaticDomainInfo(State initialState) {
        for(SpyVsSpyMapNode node : environment.nodes){
            initialState.add(new Predicate(locationPredicateId, 0, createTermList(locationIdToConstants[node.index])));
        }
                
        for(Map.Entry<Integer,java.util.List<Integer>> neighbourEntry : environment.defs.neighbours.entrySet()){
            int from = neighbourEntry.getKey();
            for(int to : neighbourEntry.getValue()){
                initialState.add(new Predicate(adjacentPredicateId, 0, createTermList(locationIdToConstants[from], locationIdToConstants[to])));
            }
        }
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
        } else if (ignoredActionsId.contains(info.actionId)){
            return Collections.EMPTY_LIST;
        }
        else {
            throw new AisteException("Unrecognized action: " + actionName);
        }

    }

    
    
}
