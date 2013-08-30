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
package cz.cuni.amis.aiste.simulations.covergame;

import JSHOP2.TaskAtom;
import JSHOP2.TaskList;
import JSHOP2.State;
import JSHOP2.Calculate;
import java.util.ArrayList;
import JSHOP2.JSHOP2;
import JSHOP2.Predicate;
import JSHOP2.TermList;
import JSHOP2.TermNumber;
import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.CoverGameJSHOP2;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IJShop2Problem;
import cz.cuni.amis.aiste.environment.IJShop2Representation;
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.impl.JShop2Problem;
import cz.cuni.amis.pathfinding.alg.floydwarshall.FloydWarshall;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import org.apache.log4j.Logger;
import static cz.cuni.amis.aiste.environment.impl.JShop2Utils.*;
import cz.cuni.amis.aiste.environment.impl.SequencePlan;
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.OpponentData;
import java.util.List;

/**
 *
 * @author Martin
 */
public class CGJSHOPRepresentation extends AbstractCGPlanningRepresentation<JSHOP2, IJShop2Problem, Predicate> 
    implements IJShop2Representation<CGPairAction, CGPlanningGoal>
{

    FloydWarshall<Loc> fixedMapFloydWarshall;
    
    private final Logger logger = Logger.getLogger(CGJSHOPRepresentation.class);

    Map<Loc, Integer> locationsToConstants;
    Map<Integer, Loc> constantsToLocations;
        
    int bodyConstants[] = new int[] {CoverGameJSHOP2.CONST_BODY_0, CoverGameJSHOP2.CONST_BODY_1};
    int oponentConstants[] = new int[] {CoverGameJSHOP2.CONST_OPONENT_0, CoverGameJSHOP2.CONST_OPONENT_1};
    
    String[] additionalConstantNames;
    
    Map<AgentBody, JSHOP2> jshops = new HashMap<AgentBody, JSHOP2>();
            
    Map<AgentBody, java.util.List<Predicate> > staticDomainInfos = new HashMap<AgentBody, java.util.List<Predicate>>();
         
    
    public CGJSHOPRepresentation(CoverGame env) {
        super(env);
        fixedMapFloydWarshall = new FloydWarshall<Loc>(env.defs.navGraphMap);
        
        additionalConstantNames = new String[getNumAdditionalConstants()];
        final int problemConstantOffset = CoverGameJSHOP2.NUM_CONSTANTS;
        
        int nextConstantIndex = problemConstantOffset;
        
        locationsToConstants = new HashMap<Loc, Integer>();
        constantsToLocations = new HashMap<Integer, Loc>();
    
        
/*        bodyConstants = new int[] {nextConstantIndex, nextConstantIndex + 1};
        oponentConstants = new int[] {nextConstantIndex + 2, nextConstantIndex + 3};
        additionalConstantNames[nextConstantIndex] = "body_0";
        additionalConstantNames[nextConstantIndex + 1] = "body_1";
        additionalConstantNames[nextConstantIndex + 2] = "oponent_0";
        additionalConstantNames[nextConstantIndex + 3] = "oponent_1";        
        nextConstantIndex += 4;
      */
        for(Loc location : env.defs.navGraph.keySet()){
            locationsToConstants.put(location,nextConstantIndex);
            constantsToLocations.put(nextConstantIndex, location);
            additionalConstantNames[nextConstantIndex - problemConstantOffset] = "location_" + location.x + "_" + location.y;
            nextConstantIndex++;
        }
        
        
    }

    protected int constantToBodyId(int bodyConstant){
        if(bodyConstant == bodyConstants[0]){
            return 0;
        } else if(bodyConstant == bodyConstants[1]){
            return 1;
        } else {
            throw new AisteException("Parameter is not a body constant: " + bodyConstant);
        }
    }
    
    protected int getMaxNumConstants(){
        return CoverGameJSHOP2.NUM_CONSTANTS + getNumAdditionalConstants();
    }

    protected int getNumAdditionalConstants() {
        //TODO: count!
        return 13000;
    }
    
    @Override
    public JSHOP2 getDomain(AgentBody body) {
        JSHOP2 jshop = new JSHOP2();
        
        Map<String, Calculate> userFunctions = new HashMap<String, Calculate>();
        userFunctions.put("find_path", new FindPathCalculate(this, body));
        
        CoverGameJSHOP2 domain = new CoverGameJSHOP2(jshop, userFunctions,  java.util.Collections.EMPTY_MAP);
        jshop.initialize(domain, getMaxNumConstants());
        jshops.put(body, jshop);
        
        //Create static domain information
        java.util.List<Predicate> staticDomainInfo = new ArrayList<Predicate>();
        
        //Adjacency
        for(Loc node : env.defs.navGraph.keySet()){            
            final Integer nodeConstantId = locationsToConstants.get(node);
            staticDomainInfo.add(new Predicate(CoverGameJSHOP2.CONST_LOCATION, createTermList(jshop, nodeConstantId)));
            for(Loc otherNode : env.defs.navGraph.get(node)){
                final Integer otherNodeConstantId = locationsToConstants.get(otherNode);
                staticDomainInfo.add(new Predicate(CoverGameJSHOP2.CONST_ADJACENT, createTermList(jshop, nodeConstantId, otherNodeConstantId)));
            }
        }

        //All pairwise distances
        for(Loc node : env.defs.navGraph.keySet()){            
            final Integer nodeConstantId = locationsToConstants.get(node);
            for(Loc otherNode : env.defs.navGraph.keySet()){
                if(otherNode.equals(node)){
                    continue;
                }
                final Integer otherNodeConstantId = locationsToConstants.get(otherNode);
                final int pathCost = fixedMapFloydWarshall.getPathCost(node, otherNode);
                if(pathCost < Integer.MAX_VALUE){
                    staticDomainInfo.add(new Predicate(CoverGameJSHOP2.CONST_DISTANCE, createTermList(jshop, new TermNumber(pathCost) ,nodeConstantId, otherNodeConstantId)));
                }
            }
        }
                
        for(int i = 0; i < 2; i++){
            staticDomainInfo.add(new Predicate(CoverGameJSHOP2.CONST_BODY, createTermList(jshop, bodyConstants[i])));
            staticDomainInfo.add(new Predicate(CoverGameJSHOP2.CONST_OPONENT, createTermList(jshop, oponentConstants[i])));
        }
        
        staticDomainInfos.put(body, staticDomainInfo);
        return jshop;
    }

    @Override
    public IJShop2Problem getProblem(AgentBody body, CGPlanningGoal goal) {

        /**
         * Create static predicates
         */
        JSHOP2 jshop = jshops.get(body);
        if(jshop == null){
            throw new IllegalStateException("Getting problem before getting domain");
        }
        
        java.util.List<Predicate> staticDomainInfo = staticDomainInfos.get(body);
            
        
        State initialState = new State(jshop.getDomain().getAxioms().length, jshop.getDomain().getAxioms());
        for(Predicate p : staticDomainInfo){
            initialState.add(p);
        }
        
        
        CoverGame.CGBodyPair bodyPair = env.bodyPairs.get(body.getId());

        int ids[] = env.getOpponentIds(body.getId());
        OpponentData oponentData[] = env.getOpponentTeamData(body.getId()).opponentData;
        
        // body - related state
        for (int i = 0; i < 2; i++) {
            initialState.add(new Predicate(CoverGameJSHOP2.CONST_AT, createTermList(jshop, bodyConstants[i], locationsToConstants.get(bodyPair.getBodyInfo(i).loc))));
/*            CoverGame.CGBodyInfo bodyInfo = bodyPair.getBodyInfo(i);
            for(int op = 0; op < 2; op++){                
                CoverGame.CGBodyInfo oponentInfo = env.bodyInfos.get(ids[op]);
                if(!env.isVisible(oponentInfo.loc, bodyInfo.loc) || env.isCovered(oponentInfo.loc, bodyInfo.loc)){
                    initialState.add(new Predicate(CoverGameJSHOP2.CONST_PARTIAL_COVER, goal));
                }
            }*/
        }

        /* Oponent - related state*/
        for(int i = 0; i < 2; i++){
            for(Loc uncoveredLoc : oponentData[i].uncoveredNavpoints){
                initialState.add(new Predicate(CoverGameJSHOP2.CONST_UNCOVERED_BY_OPONENT, createTermList(jshop, oponentConstants[i], locationsToConstants.get(uncoveredLoc))));
            }
        }
        
        TaskList tasks;
        
        //tasks = new TaskList(new TaskAtom(new Predicate(CoverGameJSHOP2.METHOD_TAKE_COVER, 0, createTermList(jshop, bodyConstants[0])), false, false));
        tasks = new TaskList(new TaskAtom(new Predicate(CoverGameJSHOP2.METHOD_TAKE_COVER_ALL, 0, TermList.NIL), false, false));
        
        return new JShop2Problem(additionalConstantNames, initialState, tasks);

    }

    @Override
    public IReactivePlan<? extends CGPairAction> translateAction(Queue<Predicate> actionsFromPlanner, AgentBody body) {
        //eat sync actions at the beginning
        while(!actionsFromPlanner.isEmpty() && (actionsFromPlanner.peek().getHead() == CoverGameJSHOP2.PRIMITIVE_SYNC_START || actionsFromPlanner.peek().getHead() == CoverGameJSHOP2.PRIMITIVE_SYNC_END)){
            actionsFromPlanner.poll();
        }

        if(actionsFromPlanner.isEmpty()){
            return new SequencePlan<CGPairAction>();
        }
        
        List<List<CGAction>> actionsForBodies = new ArrayList<List<CGAction>>();
        ArrayList<CGAction> actionsForBody1 = new ArrayList<CGAction>();
        actionsForBodies.add(actionsForBody1);
        ArrayList<CGAction> actionsForBody2 = new ArrayList<CGAction>();
        actionsForBodies.add(actionsForBody2);
        
        JSHOP2 jshop = jshops.get(body);
        
        while(!actionsFromPlanner.isEmpty() && actionsFromPlanner.peek().getHead() != CoverGameJSHOP2.PRIMITIVE_SYNC_START && actionsFromPlanner.peek().getHead() != CoverGameJSHOP2.PRIMITIVE_SYNC_END){
            Predicate action = actionsFromPlanner.poll();
            GroundActionInfo info = getGroundInfo(jshop, action);
            switch(info.actionId){
                case CoverGameJSHOP2.PRIMITIVE_MOVE : {
                    Loc to = constantsToLocations.get(info.params.get(2));
                    actionsForBodies.get(constantToBodyId(info.params.get(0))).add(new CGAction(CGAction.Action.MOVE, to));
                    break;
                }default :{
                    throw new AisteException("Unsupported action: " + action.toString(jshops.get(body)));
                }
            }
        }
        
        boolean syncStart;
        
        if(actionsFromPlanner.isEmpty()){
            syncStart = true;
        } else {
            Predicate action = actionsFromPlanner.poll();
            switch(action.getHead()){
                case CoverGameJSHOP2.PRIMITIVE_SYNC_START : {
                    syncStart = true;
                    break;
                }
                case CoverGameJSHOP2.PRIMITIVE_SYNC_END : {
                    syncStart = false;
                    break;
                }
                default : {
                    throw new AisteException("Unrecognized sync task: " + action.toString(jshops.get(body)));                    
                }
            }
        }
        
        int pairPlanSize = Math.max(actionsForBodies.get(0).size(), actionsForBodies.get(1).size());
            
        List<CGPairAction> pairActions = new ArrayList<CGPairAction>(pairPlanSize);
        
        if(syncStart){
            for(int i = 0; i < pairPlanSize; i++){
                CGPairAction action;
                if(i >= actionsForBody1.size()){
                    action = new CGPairAction(CGAction.NO_OP_ACTION, actionsForBody2.get(i));
                } else if (i >= actionsForBody2.size()){
                    action = new CGPairAction(actionsForBody1.get(i), CGAction.NO_OP_ACTION);
                } else {
                    action = new CGPairAction(actionsForBody1.get(i), actionsForBody2.get(i));
                }
                pairActions.add(action);
            }
        } else {
            throw new AisteException("Sync end not supported yet");
        }
        
        return new SequencePlan<CGPairAction>(pairActions);
    }
    
    
}
