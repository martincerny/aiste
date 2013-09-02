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

import java.util.ArrayList;
import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.ISimulablePDDLRepresentation;
import cz.cuni.amis.pathfinding.alg.floydwarshall.FloydWarshall;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import org.apache.log4j.Logger;
import cz.cuni.amis.aiste.environment.impl.SequencePlan;
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.CGBodyPair;
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.OpponentData;
import cz.cuni.amis.planning4j.ActionDescription;
import cz.cuni.amis.planning4j.IPDDLObjectDomainProvider;
import cz.cuni.amis.planning4j.impl.PDDLFileDomainProvider;
import cz.cuni.amis.planning4j.pddl.PDDLDomain;
import cz.cuni.amis.planning4j.pddl.PDDLObjectInstance;
import cz.cuni.amis.planning4j.pddl.PDDLPredicate;
import cz.cuni.amis.planning4j.pddl.PDDLProblem;
import cz.cuni.amis.planning4j.pddl.PDDLRequirement;
import cz.cuni.amis.planning4j.pddl.PDDLSimpleAction;
import cz.cuni.amis.planning4j.pddl.PDDLType;
import cz.cuni.amis.planning4j.utils.Planning4JUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 *
 * @author Martin
 */
public class CGPDDLRepresentationWithRoles extends AbstractCGPlanningRepresentation<PDDLDomain, PDDLProblem, ActionDescription>
        implements ISimulablePDDLRepresentation<CGPairAction, CoverGame, CGPlanningGoal>
{

    FloydWarshall<Loc> fixedMapFloydWarshall;
    
    private final Logger logger = Logger.getLogger(CGPDDLRepresentationWithRoles.class);

    PDDLType locationType;
    PDDLType agentType;
    PDDLType bodyType;
    PDDLType opponentType;
    
    Map<Loc, PDDLObjectInstance> locationsToConstants;
    Map<PDDLObjectInstance, Loc> constantsToLocations;
        
    PDDLObjectInstance bodyConstants[];
    PDDLObjectInstance opponentConstants[];
    
    PDDLPredicate atPredicate;
    PDDLPredicate winPredicate;
    
    PDDLSimpleAction aggressiveAction;
    PDDLSimpleAction aggressiveRecklessAction;
    PDDLSimpleAction overwatchDefensiveAction;
    PDDLSimpleAction moveSafeAction;
    PDDLSimpleAction moveRecklessAction;
    PDDLSimpleAction defensiveAction;
                          
    
    public CGPDDLRepresentationWithRoles(CoverGame env) {
        super(env);
        fixedMapFloydWarshall = new FloydWarshall<Loc>(env.defs.navGraphMap);
        
        for(Loc l1 : env.defs.navGraph.keySet()){
            for(Loc l2 : env.defs.navGraph.keySet()){
                if(fixedMapFloydWarshall.getPathCost(l1, l2) >= Integer.MAX_VALUE){
                    throw new AisteException("Unreachable pair of navpoints: " + l1 + ", " + l2);
                }
            }
        }
        
        locationType = new PDDLType("location");
        agentType = new PDDLType("agent");
        bodyType = new PDDLType("body", agentType);
        opponentType = new PDDLType("opponent", agentType);
        
        locationsToConstants = new HashMap<Loc, PDDLObjectInstance>();
        constantsToLocations = new HashMap<PDDLObjectInstance, Loc>();
    
        
/*        bodyConstants = new int[] {nextConstantIndex, nextConstantIndex + 1};
        oponentConstants = new int[] {nextConstantIndex + 2, nextConstantIndex + 3};
        additionalConstantNames[nextConstantIndex] = "body_0";
        additionalConstantNames[nextConstantIndex + 1] = "body_1";
        additionalConstantNames[nextConstantIndex + 2] = "oponent_0";
        additionalConstantNames[nextConstantIndex + 3] = "oponent_1";        
        nextConstantIndex += 4;
      */
        for(Loc location : env.defs.navGraph.keySet()){
            PDDLObjectInstance locInstance = new PDDLObjectInstance("loc_" + location.x + "_" + location.y, locationType);
            locationsToConstants.put(location,locInstance);
            constantsToLocations.put(locInstance, location);
        }
      
        //vytvorit akce
        
    }

    protected int constantToBodyIndex(String bodyConstant){
        if(bodyConstant.equals(Planning4JUtils.normalizeIdentifier(bodyConstants[0].getName()))){
            return 0;
        } else if(bodyConstant.equals(Planning4JUtils.normalizeIdentifier(bodyConstants[1].getName()))){
            return 1;
        } else {
            throw new AisteException("Parameter is not a body constant: " + bodyConstant);
        }
    }
    
    protected int opponentConstantToBodyIndex(String opponentConstant) throws AisteException {
        if(opponentConstant.equals(Planning4JUtils.normalizeIdentifier(opponentConstants[0].getName()))){
            return 0;                        
        } else if(opponentConstant.equals(Planning4JUtils.normalizeIdentifier(opponentConstants[1].getName()))){
            return 1;
        } else {
            throw new AisteException("Unrecognized opponent constant: " + opponentConstant);
        }
    }
    
    

    
    @Override
    public PDDLDomain getDomain(AgentBody body) {
        PDDLDomain domain = new PDDLDomain("cover_game", EnumSet.of(PDDLRequirement.ACTION_COSTS, PDDLRequirement.ADL));
        
        domain.addAction(aggressiveAction);
        domain.addAction(aggressiveRecklessAction);
        domain.addAction(moveSafeAction);
        domain.addAction(moveRecklessAction);
        domain.addAction(overwatchDefensiveAction);
        domain.addAction(defensiveAction);
        
        domain.addPredicate(null);
        //TODO
        
        return domain;
    }

    @Override
    public PDDLProblem getProblem(AgentBody body, CGPlanningGoal goal) {
        //TODO
        return new PDDLProblem("cover_game_problem", "cover_game");
        /**
         * Create static predicates
         */
/*        JSHOP2 jshop = jshops.get(body);
        if(jshop == null){
            throw new IllegalStateException("Getting problem before getting domain");
        }
        
        java.util.List<Predicate> staticDomainInfo = staticDomainInfos.get(body);
            
        
        State initialState = new State(jshop.getDomain().getAxioms().length, jshop.getDomain().getAxioms());
        for(Predicate p : staticDomainInfo){
            initialState.add(p);
        }
        
        
        CoverGame.CGBodyPair bodyPair = env.bodyPairs.get(body.getId());

        int opponentIds[] = env.getOpponentIds(body.getId());
        OpponentData opponentData[] = env.getOpponentTeamData(body.getId()).opponentData;

        
        
        // body - related state
        for (int i = 0; i < 2; i++) {
            initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_AT, createTermList(jshop, bodyConstants[i], locationsToConstants.get(bodyPair.getBodyInfo(i).loc))));
            //High health -> I should withstand two shots
            if(bodyPair.getBodyInfo(i).health >= env.defs.shootDamage * 2) {
                initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_HIGH_HEALTH, createTermList(jshop, bodyConstants[i])));                
            }*/
        
/*            CoverGame.CGBodyInfo bodyInfo = bodyPair.getBodyInfo(i);
            for(int op = 0; op < 2; op++){                
                CoverGame.CGBodyInfo oponentInfo = env.bodyInfos.get(ids[op]);
                if(!env.isVisible(oponentInfo.loc, bodyInfo.loc) || env.isCovered(oponentInfo.loc, bodyInfo.loc)){
                    initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_PARTIAL_COVER, goal));
                }
            }*/
/*        }

        
        // Oponent - related state
        for(int opp = 0; opp < 2; opp++){
            for(Loc uncoveredLoc : opponentData[opp].uncoveredNavpoints){
                initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_UNCOVERED_BY_OPPONENT, createTermList(jshop, locationsToConstants.get(uncoveredLoc), opponentConstants[opp])));
            }
            //Low health -> I expect them to fall for a single shot, even if they heal a little
            if(env.bodyInfos.get(opponentIds[opp]).health < env.defs.shootDamage - env.defs.healPerRound){
                initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_LOW_HEALTH, createTermList(jshop, opponentConstants[opp])));                                
            }
            
            for (int bodyId = 0; bodyId < 2; bodyId++) {
                Loc nearestSafeVantage = null; 
                int nearestSafeVantageDistance = Integer.MAX_VALUE;
                Loc nearestVantage = null;
                int nearestVantageDistance = Integer.MAX_VALUE;

                Loc bodyLoc = bodyPair.getBodyInfo(bodyId).getLoc();
                for(Loc vantage_point : opponentData[opp].navpointsInvalidatingCover){
                    int distance = fixedMapFloydWarshall.getPathCost(bodyLoc, vantage_point);
                    if(distance < nearestSafeVantageDistance){
                        int possibleUncoveredShots = getPossibleUncoveredShots(bodyLoc, vantage_point, opponentData);
                        if(possibleUncoveredShots <= 3 && distance < nearestVantageDistance){
                            nearestVantageDistance = distance;
                            nearestVantage = vantage_point;
                        }                    
                        
                        if(possibleUncoveredShots == 0){
                            nearestSafeVantageDistance = distance;
                            nearestSafeVantage = vantage_point;
                        }
                    }
                }
                
                if(nearestVantage != null){
                    initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_VANTAGE_POINT, createTermList(jshop, locationsToConstants.get(nearestVantage), bodyConstants[bodyId], opponentConstants[opp])));                    
                }
                if(nearestSafeVantage != null){
                    initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_VANTAGE_POINT_SAFE, createTermList(jshop, locationsToConstants.get(nearestSafeVantage), bodyConstants[bodyId], opponentConstants[opp])));
                }
                
                Loc nearestAttackPoint = null;
                int nearestAttackDistance = Integer.MAX_VALUE;                
                for(Loc attackPoint : opponentData[opp].visibleNavpoints){
                    int distance = fixedMapFloydWarshall.getPathCost(bodyLoc, attackPoint);
                    if(distance < nearestAttackDistance && getPossibleUncoveredShots(bodyLoc, attackPoint, opponentData) == 0){
                        nearestAttackPoint = attackPoint;
                        nearestAttackDistance = distance;
                    }
                }
                
                if(nearestAttackPoint != null){
                    initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_ATTACK_POINT, createTermList(jshop, locationsToConstants.get(nearestAttackPoint), bodyConstants[bodyId], opponentConstants[opp])));                    
                }
                
                
                if(logger.isTraceEnabled()){
                    logger.trace(body.getId() + ": Body " + bodyId + " nearest vantage for opp " + opp + ": " + nearestVantage + " distance: " + nearestVantageDistance);
                    logger.trace(body.getId() + ": Body " + bodyId + " nearest safe vantage for opp " + opp + ": " + nearestSafeVantage + " distance: " + nearestSafeVantageDistance);
                    logger.trace(body.getId() + ": Body " + bodyId + " nearest attack point for opp " + opp + ": " + nearestAttackPoint + " distance: " + nearestAttackDistance);
                }
            }
        }

        
        
        TaskList tasks;
        
        //tasks = new TaskList(new TaskAtom(new Predicate(CoverGameWithRolesJSHOP2.METHOD_TAKE_COVER, 0, createTermList(jshop, bodyConstants[0])), false, false));
        tasks = new TaskList(new TaskAtom(new Predicate(CoverGameWithRolesJSHOP2.METHOD_WIN, 0, TermList.NIL), false, false));
        
        return new JShop2Problem(additionalConstantNames, initialState, tasks);
*/
    }

    @Override
    public IReactivePlan<? extends CGPairAction> translateAction(Queue<ActionDescription> actionsFromPlanner, AgentBody body) {
        return translateActionForSimulation(env, actionsFromPlanner, body);
    }    
    
    @Override
    public IReactivePlan<? extends CGPairAction> translateActionForSimulation(CoverGame simulationEnv, Queue<ActionDescription> actionsFromPlanner, AgentBody body) {
        //TODO
        return null;
    
/*    

        if(actionsFromPlanner.isEmpty()){
            return new SequencePlan<CGPairAction>();
        }
        
        List<List<CGRolePlan>> actionsForBodies = new ArrayList<List<CGRolePlan>>();
        ArrayList<CGRolePlan> actionsForBody1 = new ArrayList<CGRolePlan>();
        actionsForBodies.add(actionsForBody1);
        ArrayList<CGRolePlan> actionsForBody2 = new ArrayList<CGRolePlan>();
        actionsForBodies.add(actionsForBody2);
        
        int[] bodyIds = simulationEnv.getTeamIds(body.getId());
        
        JSHOP2 jshop = jshops.get(body);
        
        while(!actionsFromPlanner.isEmpty() && actionsFromPlanner.peek().getHead() != CoverGameWithRolesJSHOP2.PRIMITIVE_SYNC){
            Predicate action = actionsFromPlanner.poll();
            GroundActionInfo info = getGroundInfo(jshop, action);
            if(info.actionId == CoverGameWithRolesJSHOP2.PRIMITIVE_ADDED_COST){
                //added cost action can safely be ignored
                continue;
            }            
            int bodyIndex = constantToBodyIndex(info.params.get(0));
            int bodyId = bodyIds[bodyIndex];
            switch(info.actionId){
                case CoverGameWithRolesJSHOP2.PRIMITIVE_MOVE_SAFE : {
                    Loc target = constantsToLocations.get(info.params.get(1));
                    actionsForBodies.get(bodyIndex).add(new CGRoleMove(simulationEnv, bodyId, target, 0));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_MOVE_RECKLESS : {
                    Loc target = constantsToLocations.get(info.params.get(1));
                    actionsForBodies.get(bodyIndex).add(new CGRoleMove(simulationEnv, bodyId, target, 1));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_AGGRESSIVE : {
                    int opponentId = opponentConstantToBodyIndex(info, body);
                    actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(simulationEnv, bodyId, 0, opponentId));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_AGGRESSIVE_RECKLESS : {
                    int opponentId = opponentConstantToBodyIndex(info, body);
                    actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(simulationEnv, bodyId, 1, opponentId));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_DEFENSIVE : {
                    actionsForBodies.get(bodyIndex).add(new CGRoleDefensive(simulationEnv, bodyId));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_OVERWATCH_DEFENSIVE : {
                    actionsForBodies.get(bodyIndex).add(new CGRoleOverWatch(simulationEnv, bodyId, true));
                    break;
                }
                case CoverGameWithRolesJSHOP2.PRIMITIVE_ADDED_COST : {
                    //ignored action
                    break;
                }
                default :{
                    throw new AisteException("Unsupported action: " + action.toString(jshops.get(body)));
                }
            }            
        }
        
        
        CGBodyPair bodyPair = simulationEnv.bodyPairs.get(body.getId());
        CGPairRolePlan plan = new CGPairRolePlan(actionsForBody1, actionsForBody2);
        if(logger.isDebugEnabled() && !simulationEnv.isSimulation){
            logger.debug(body.getId() + ": RolePlan: " + plan);
        }
        return plan;*/
    }

    protected boolean isCovered(Loc loc, OpponentData[] opponentData){
        for(int opp2 = 0; opp2 < 2; opp2++){
            if(opponentData[opp2].uncoveredNavpoints.contains(loc)){
                return false;
            }
        }
        
        return true;
    }
    
    protected int getPossibleUncoveredShots(Loc bodyLoc, Loc vantage_point, OpponentData[] opponentData) {
        int possibleUncoveredShots = 0;
        List<Loc> path = fixedMapFloydWarshall.getPath(bodyLoc, vantage_point);
        path.add(vantage_point);
        for(Loc pathLoc : path){
            for(int opp2 = 0; opp2 < 2; opp2++){
                if(opponentData[opp2].uncoveredNavpoints.contains(pathLoc)){
                    possibleUncoveredShots++;
                }
            }
        }
        return possibleUncoveredShots;
    }

    
    
}
