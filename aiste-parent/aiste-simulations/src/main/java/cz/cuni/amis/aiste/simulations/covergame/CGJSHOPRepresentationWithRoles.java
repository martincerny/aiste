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
import java.util.ArrayList;
import JSHOP2.JSHOP2;
import JSHOP2.Predicate;
import JSHOP2.TermList;
import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.CoverGameWithRolesJSHOP2;
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
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.CGBodyPair;
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.OpponentData;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Martin
 */
public class CGJSHOPRepresentationWithRoles extends AbstractCGPlanningRepresentation<JSHOP2, IJShop2Problem, Predicate> 
    implements IJShop2Representation<CGPairAction, CGPlanningGoal>
{

    FloydWarshall<Loc> fixedMapFloydWarshall;
    
    private final Logger logger = Logger.getLogger(CGJSHOPRepresentationWithRoles.class);

    Map<Loc, Integer> locationsToConstants;
    Map<Integer, Loc> constantsToLocations;
        
    int bodyConstants[] = new int[] {CoverGameWithRolesJSHOP2.CONST_BODY_0, CoverGameWithRolesJSHOP2.CONST_BODY_1};
    int opponentConstants[] = new int[] {CoverGameWithRolesJSHOP2.CONST_OPPONENT_0, CoverGameWithRolesJSHOP2.CONST_OPPONENT_1};
    
    String[] additionalConstantNames;
    
    Map<AgentBody, JSHOP2> jshops = new HashMap<AgentBody, JSHOP2>();
            
    Map<AgentBody, java.util.List<Predicate> > staticDomainInfos = new HashMap<AgentBody, java.util.List<Predicate>>();
         
    
    public CGJSHOPRepresentationWithRoles(CoverGame env) {
        super(env);
        fixedMapFloydWarshall = new FloydWarshall<Loc>(env.defs.navGraphMap);
        
        for(Loc l1 : env.defs.navGraph.keySet()){
            for(Loc l2 : env.defs.navGraph.keySet()){
                if(fixedMapFloydWarshall.getPathCost(l1, l2) >= Integer.MAX_VALUE){
                    throw new AisteException("Unreachable pair of navpoints: " + l1 + ", " + l2);
                }
            }
        }
        
        additionalConstantNames = new String[getNumAdditionalConstants()];
        final int problemConstantOffset = CoverGameWithRolesJSHOP2.NUM_CONSTANTS;
        
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

    protected int constantToBodyIndex(int bodyConstant){
        if(bodyConstant == bodyConstants[0]){
            return 0;
        } else if(bodyConstant == bodyConstants[1]){
            return 1;
        } else {
            throw new AisteException("Parameter is not a body constant: " + bodyConstant);
        }
    }
    
    protected int opponentConstantToBodyIndex(GroundActionInfo info, AgentBody body) throws AisteException {
        int opponentConstantId = info.params.get(1);
        if(opponentConstantId == opponentConstants[0]){
            return 0;                        
        } else if(opponentConstantId == opponentConstants[1]) {
            return 1;
        } else {
            throw new AisteException("Unrecognized oponent constant: " + opponentConstantId + ": " + jshops.get(body).getDomain().getConstant(opponentConstantId));
        }
    }
    
    
    protected int getMaxNumConstants(){
        return CoverGameWithRolesJSHOP2.NUM_CONSTANTS + getNumAdditionalConstants();
    }

    protected int getNumAdditionalConstants() {
        //TODO: count!
        return 13000;
    }
    
    @Override
    public JSHOP2 getDomain(AgentBody body) {
        JSHOP2 jshop = new JSHOP2();
        
        
        CoverGameWithRolesJSHOP2 domain = new CoverGameWithRolesJSHOP2(jshop);
        jshop.initialize(domain, getMaxNumConstants());
        jshops.put(body, jshop);
        
        //Create static domain information
        java.util.List<Predicate> staticDomainInfo = new ArrayList<Predicate>();
        
                
/*        for(int i = 0; i < 2; i++){
            staticDomainInfo.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_BODY, createTermList(jshop, bodyConstants[i])));
            staticDomainInfo.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_OPONENT, createTermList(jshop, opponentConstants[i])));
        }
  */      
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

        int opponentIds[] = env.getOpponentIds(body.getId());
        OpponentData opponentData[] = env.getOpponentTeamData(body.getId()).opponentData;

        
        
        // body - related state
        for (int i = 0; i < 2; i++) {
            initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_AT, createTermList(jshop, bodyConstants[i], locationsToConstants.get(bodyPair.getBodyInfo(i).loc))));
            //High health -> I should withstand two shots
            if(bodyPair.getBodyInfo(i).health >= env.defs.shootDamage * 2) {
                initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_HIGH_HEALTH, createTermList(jshop, bodyConstants[i])));                
            }
/*            CoverGame.CGBodyInfo bodyInfo = bodyPair.getBodyInfo(i);
            for(int op = 0; op < 2; op++){                
                CoverGame.CGBodyInfo oponentInfo = env.bodyInfos.get(ids[op]);
                if(!env.isVisible(oponentInfo.loc, bodyInfo.loc) || env.isCovered(oponentInfo.loc, bodyInfo.loc)){
                    initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_PARTIAL_COVER, goal));
                }
            }*/
        }

        
        /* Oponent - related state*/
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
                        
                        if(possibleUncoveredShots <= 1){
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
                    if(distance < nearestAttackDistance && getPossibleUncoveredShots(bodyLoc, attackPoint, opponentData) < 1){
                        nearestAttackPoint = attackPoint;
                        nearestAttackDistance = distance;
                    }
                }
                
                if(nearestAttackPoint != null){
                    initialState.add(new Predicate(CoverGameWithRolesJSHOP2.CONST_ATTACK_POINT, createTermList(jshop, locationsToConstants.get(nearestAttackPoint), bodyConstants[bodyId], opponentConstants[opp])));                    
                }
                
                
                if(logger.isDebugEnabled()){
                    logger.debug(body.getId() + ": Body " + bodyId + " nearest vantage for opp " + opp + ": " + nearestVantage + " distance: " + nearestVantageDistance);
                    logger.debug(body.getId() + ": Body " + bodyId + " nearest safe vantage for opp " + opp + ": " + nearestSafeVantage + " distance: " + nearestSafeVantageDistance);
                    logger.debug(body.getId() + ": Body " + bodyId + " nearest attack point for opp " + opp + ": " + nearestAttackPoint + " distance: " + nearestAttackDistance);
                }
            }
        }

        
        
        TaskList tasks;
        
        //tasks = new TaskList(new TaskAtom(new Predicate(CoverGameWithRolesJSHOP2.METHOD_TAKE_COVER, 0, createTermList(jshop, bodyConstants[0])), false, false));
        tasks = new TaskList(new TaskAtom(new Predicate(CoverGameWithRolesJSHOP2.METHOD_WIN, 0, TermList.NIL), false, false));
        
        return new JShop2Problem(additionalConstantNames, initialState, tasks);

    }

    @Override
    public IReactivePlan<? extends CGPairAction> translateAction(Queue<Predicate> actionsFromPlanner, AgentBody body) {
        //eat sync actions at the beginning
        while(!actionsFromPlanner.isEmpty() && (actionsFromPlanner.peek().getHead() == CoverGameWithRolesJSHOP2.PRIMITIVE_SYNC)){
            actionsFromPlanner.poll();
        }

        if(actionsFromPlanner.isEmpty()){
            return new SequencePlan<CGPairAction>();
        }
        
        List<List<CGRolePlan>> actionsForBodies = new ArrayList<List<CGRolePlan>>();
        ArrayList<CGRolePlan> actionsForBody1 = new ArrayList<CGRolePlan>();
        actionsForBodies.add(actionsForBody1);
        ArrayList<CGRolePlan> actionsForBody2 = new ArrayList<CGRolePlan>();
        actionsForBodies.add(actionsForBody2);
        
        int[] bodyIds = env.getTeamIds(body.getId());
        
        while(!actionsFromPlanner.isEmpty() && actionsFromPlanner.peek().getHead() != CoverGameWithRolesJSHOP2.PRIMITIVE_SYNC){
            Predicate action = actionsFromPlanner.poll();
            GroundActionInfo info = getGroundInfo(action);
            int bodyIndex = constantToBodyIndex(info.params.get(0));
            int bodyId = bodyIds[bodyIndex];
            switch(info.actionId){
                case CoverGameWithRolesJSHOP2.PRIMITIVE_MOVE_SAFE : {
                    Loc target = constantsToLocations.get(info.params.get(1));
                    actionsForBodies.get(bodyIndex).add(new CGRoleMove(env, bodyId, target, 0));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_MOVE_RECKLESS : {
                    Loc target = constantsToLocations.get(info.params.get(1));
                    actionsForBodies.get(bodyIndex).add(new CGRoleMove(env, bodyId, target, 1));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_AGGRESSIVE : {
                    int opponentId = opponentConstantToBodyIndex(info, body);
                    actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(env, bodyId, 0, opponentId));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_AGGRESSIVE_RECKLESS : {
                    int opponentId = opponentConstantToBodyIndex(info, body);
                    actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(env, bodyId, 1, opponentId));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_DEFENSIVE : {
                    actionsForBodies.get(bodyIndex).add(new CGRoleDefensive(env, bodyId));
                    break;
                } 
                case CoverGameWithRolesJSHOP2.PRIMITIVE_OVERWATCH_DEFENSIVE : {
                    actionsForBodies.get(bodyIndex).add(new CGRoleOverWatch(env, bodyId, true));
                    break;
                }
                default :{
                    throw new AisteException("Unsupported action: " + action.toString(jshops.get(body)));
                }
            }            
        }
        
        
        CGBodyPair bodyPair = env.bodyPairs.get(body.getId());
        CGPairRolePlan plan = new CGPairRolePlan(actionsForBody1, actionsForBody2);
        if(logger.isDebugEnabled()){
            logger.debug(body.getId() + ": RolePlan: " + plan);
        }
        return plan;
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
