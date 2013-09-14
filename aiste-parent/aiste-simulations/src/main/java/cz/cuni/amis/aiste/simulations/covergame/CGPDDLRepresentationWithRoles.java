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
import cz.cuni.amis.planning4j.pddl.PDDLFunction;
import cz.cuni.amis.planning4j.pddl.PDDLObjectInstance;
import cz.cuni.amis.planning4j.pddl.PDDLOperators;
import cz.cuni.amis.planning4j.pddl.PDDLParameter;
import cz.cuni.amis.planning4j.pddl.PDDLPredicate;
import cz.cuni.amis.planning4j.pddl.PDDLProblem;
import cz.cuni.amis.planning4j.pddl.PDDLRequirement;
import cz.cuni.amis.planning4j.pddl.PDDLSimpleAction;
import cz.cuni.amis.planning4j.pddl.PDDLType;
import cz.cuni.amis.planning4j.utils.Planning4JUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Martin
 */
public class CGPDDLRepresentationWithRoles extends AbstractCGPlanningRepresentation<PDDLDomain, PDDLProblem, ActionDescription>
        implements ISimulablePDDLRepresentation<CGPairAction, CoverGame, CGPlanningGoal> {

    FloydWarshall<Loc> fixedMapFloydWarshall;

    private final Logger logger = Logger.getLogger(CGPDDLRepresentationWithRoles.class);

    PDDLType locationType;

    PDDLType agentType;

    PDDLType bodyType;

    PDDLType opponentType;

    Map<Loc, PDDLObjectInstance> locationsToConstants;

    Map<String, Loc> constantsToLocations;

    PDDLObjectInstance bodyConstants[];

    PDDLObjectInstance opponentConstants[];

    PDDLPredicate winPredicate;

    PDDLPredicate atPredicate;

    PDDLPredicate highHealthPredicate;

    PDDLPredicate vantagePointPredicate;

    PDDLPredicate vantagePointSafePredicate;

    PDDLPredicate attackPointPredicate;

    PDDLPredicate uncoveredByOpponentPredicate;

    PDDLPredicate opponentLowHealthPredicate;

    /*
    PDDLSimpleAction aggressiveAction;

    PDDLSimpleAction aggressiveRecklessAction;*/

    PDDLSimpleAction moveSafeAction;

    PDDLSimpleAction moveRecklessAction;

    PDDLSimpleAction winBySafeDoubleFireAction;

    PDDLSimpleAction winBySafeFireAction;

    PDDLSimpleAction winByRecklessFireAction;

    PDDLSimpleAction winByDefense;

    public CGPDDLRepresentationWithRoles(CoverGame env) {
        super(env);
        fixedMapFloydWarshall = new FloydWarshall<Loc>(env.defs.navGraphMap);

        for (Loc l1 : env.defs.navGraph.keySet()) {
            for (Loc l2 : env.defs.navGraph.keySet()) {
                if (fixedMapFloydWarshall.getPathCost(l1, l2) >= Integer.MAX_VALUE) {
                    throw new AisteException("Unreachable pair of navpoints: " + l1 + ", " + l2);
                }
            }
        }

        locationType = new PDDLType("location");
        agentType = new PDDLType("agent");
        bodyType = new PDDLType("body", agentType);
        opponentType = new PDDLType("opponent", agentType);

        locationsToConstants = new HashMap<Loc, PDDLObjectInstance>();
        constantsToLocations = new HashMap<String, Loc>();


        bodyConstants = new PDDLObjectInstance[] { new PDDLObjectInstance("body_1", bodyType), new PDDLObjectInstance("body_2", bodyType) };
        opponentConstants = new PDDLObjectInstance[] { new PDDLObjectInstance("opponent_1", opponentType), new PDDLObjectInstance("opponent_2", opponentType) };
        
        for (Loc location : env.defs.navGraph.keySet()) {
            PDDLObjectInstance locInstance = new PDDLObjectInstance("loc_" + location.x + "_" + location.y, locationType);
            locationsToConstants.put(location, locInstance);
            constantsToLocations.put(Planning4JUtils.normalizeIdentifier(locInstance.getName()), location);
        }

        winPredicate = new PDDLPredicate("win");
        atPredicate = new PDDLPredicate("at", new PDDLParameter("b", bodyType), new PDDLParameter("loc", locationType));
        highHealthPredicate = new PDDLPredicate("high_health", new PDDLParameter("b", bodyType));
        vantagePointPredicate = new PDDLPredicate("vantage_point", new PDDLParameter("loc", locationType), new PDDLParameter("b", bodyType), new PDDLParameter("op", opponentType));
        vantagePointSafePredicate = new PDDLPredicate("vantage_point_safe", new PDDLParameter("loc", locationType), new PDDLParameter("b", bodyType), new PDDLParameter("op", opponentType));
        attackPointPredicate = new PDDLPredicate("attack_point", new PDDLParameter("loc", locationType), new PDDLParameter("b", bodyType), new PDDLParameter("op", opponentType));

        uncoveredByOpponentPredicate = new PDDLPredicate("uncovered_by_opponent", new PDDLParameter("loc", locationType), new PDDLParameter("op", opponentType));
        opponentLowHealthPredicate = new PDDLPredicate("opponent_low_health", new PDDLParameter("op", opponentType));

        moveSafeAction = new PDDLSimpleAction("move_safe", 
                new PDDLParameter("b1", bodyType), 
                new PDDLParameter("target_loc", locationType),
                new PDDLParameter("b1_loc", locationType),
                new PDDLParameter("b2", bodyType),
                new PDDLParameter("b2_loc", locationType),
                new PDDLParameter("op", opponentType)
        );
        
        moveSafeAction.setPreconditionList(
                "not (= ?b1 ?b2)",
                atPredicate.stringAfterSubstitution("?b1", "?b1_loc" ),
                atPredicate.stringAfterSubstitution("?b2", "?b2_loc" )
        );
        moveSafeAction.setPositiveEffects(
                atPredicate.stringAfterSubstitution("?b1", "?target_loc" )                
        );
        moveSafeAction.setNegativeEffects(
                atPredicate.stringAfterSubstitution("?b1", "?b1_loc" )
        );
        
        
        
        //moveRecklessAction = new PDDLSimpleAction("move_reckless", parameters);
        
        winByDefense = new PDDLSimpleAction("win_by_defense");
        winByDefense.setPositiveEffects("increase (total-cost) 30", winPredicate.stringAfterSubstitution());
        
        winBySafeDoubleFireAction = new PDDLSimpleAction("win_by_safe_double_fire", 
                new PDDLParameter("b1", bodyType), 
                new PDDLParameter("target", opponentType),
                new PDDLParameter("b2", bodyType), 
                new PDDLParameter("b1_loc", locationType),
                new PDDLParameter("b2_loc", locationType)
        );
        winBySafeDoubleFireAction.setPreconditionList(
                "not (= ?b1 ?b2)",
                atPredicate.stringAfterSubstitution("?b1", "?b1_loc"),
                atPredicate.stringAfterSubstitution("?b2", "?b2_loc"),
                getCoveredCondition("?b1_loc"),
                getCoveredCondition("?b2_loc"),
                attackPointPredicate.stringAfterSubstitution("?b1_loc", "?b1", "?target"),
                attackPointPredicate.stringAfterSubstitution("?b2_loc", "?b2", "?target")
        );
        winBySafeDoubleFireAction.setPositiveEffects(
                winPredicate.stringAfterSubstitution()
        );
        addAttackPointCostEffect(winBySafeDoubleFireAction, "?b1_loc", "?b1", "?target");
        addAttackPointCostEffect(winBySafeDoubleFireAction, "?b2_loc", "?b2", "?target");
                
        winBySafeFireAction = new PDDLSimpleAction("win_by_safe_fire", 
                new PDDLParameter("b1", bodyType), 
                new PDDLParameter("target", opponentType),
                new PDDLParameter("b2", bodyType), 
                new PDDLParameter("b1_loc", locationType),
                new PDDLParameter("b2_loc", locationType)
        );        
        winBySafeFireAction.setPreconditionList(
                "not (= ?b1 ?b2)",
                atPredicate.stringAfterSubstitution("?b1", "?b1_loc"),
                atPredicate.stringAfterSubstitution("?b2", "?b2_loc"),
                getCoveredCondition("?b1_loc"),
                getCoveredCondition("?b2_loc"),
                attackPointPredicate.stringAfterSubstitution("?b1_loc", "?b1", "?target")
        );
        winBySafeFireAction.setPositiveEffects(
                winPredicate.stringAfterSubstitution(),
                "increase (total-cost) 15"
        );
        addAttackPointCostEffect(winBySafeFireAction, "?b1_loc", "?b1", "?target");
        
        
        winByRecklessFireAction = new PDDLSimpleAction("win_by_reckless_fire", 
                new PDDLParameter("b1", bodyType), 
                new PDDLParameter("target", opponentType),
                new PDDLParameter("b2", bodyType), 
                new PDDLParameter("b1_loc", locationType),
                new PDDLParameter("b2_loc", locationType)
        );        
        winByRecklessFireAction.setPreconditionList(
                "not (= ?b1 ?b2)",
                atPredicate.stringAfterSubstitution("?b1", "?b1_loc"),
                atPredicate.stringAfterSubstitution("?b2", "?b2_loc"),
                PDDLOperators.makeOr(getCoveredCondition("?b1_loc"), highHealthPredicate.stringAfterSubstitution("?b1")),
                PDDLOperators.makeOr(getCoveredCondition("?b2_loc"), highHealthPredicate.stringAfterSubstitution("?b2")),
                opponentLowHealthPredicate.stringAfterSubstitution("?target"),
                attackPointPredicate.stringAfterSubstitution("?b1_loc", "?b1", "?target")
        );
        winByRecklessFireAction.setPositiveEffects(
                winPredicate.stringAfterSubstitution(),
                "increase (total-cost) 20"
        );
        addAttackPointCostEffect(winByRecklessFireAction, "?b1_loc", "?b1", "?target");
        addAttackPointCostEffect(winByRecklessFireAction, "?b2_loc", "?b2", "?target");
        
    }
    
    protected String getCoveredCondition(String locationExpression) {
        return "forall (?o - " + opponentType.getTypeName() + ") (not (" + uncoveredByOpponentPredicate.stringAfterSubstitution(locationExpression,"?o") + "))";
    }

    protected void addAttackPointCostEffect(PDDLSimpleAction action, String locationExpression, String bodyExpression, String opponentExpression) {
        action.addPositiveEffect("when (not (" +  vantagePointSafePredicate.stringAfterSubstitution(locationExpression, bodyExpression, opponentExpression) + "))"
                + " (increase (total-cost) 2)") ;
        action.addPositiveEffect("when (not (" +  vantagePointPredicate.stringAfterSubstitution(locationExpression, bodyExpression, opponentExpression) + "))"
                + " (increase (total-cost) 1)") ; //the two effects can apply simultaneously
    }
    

    protected int constantToBodyIndex(String bodyConstant) {
        if (bodyConstant.equals(Planning4JUtils.normalizeIdentifier(bodyConstants[0].getName()))) {
            return 0;
        } else if (bodyConstant.equals(Planning4JUtils.normalizeIdentifier(bodyConstants[1].getName()))) {
            return 1;
        } else {
            throw new AisteException("Parameter is not a body constant: " + bodyConstant);
        }
    }

    protected int opponentConstantToBodyIndex(String opponentConstant) throws AisteException {
        if (opponentConstant.equals(Planning4JUtils.normalizeIdentifier(opponentConstants[0].getName()))) {
            return 0;
        } else if (opponentConstant.equals(Planning4JUtils.normalizeIdentifier(opponentConstants[1].getName()))) {
            return 1;
        } else {
            throw new AisteException("Unrecognized opponent constant: " + opponentConstant);
        }
    }

    @Override
    public PDDLDomain getDomain(AgentBody body) {
        PDDLDomain domain = new PDDLDomain("cover_game", EnumSet.of(PDDLRequirement.ADL));
        
        domain.addFunction(new PDDLFunction("total-cost"));        
        
        
        domain.addAction(moveSafeAction);
        //domain.addAction(moveRecklessAction);
        domain.addAction(winByDefense);
        domain.addAction(winByRecklessFireAction);
        domain.addAction(winBySafeDoubleFireAction);
        domain.addAction(winBySafeFireAction);
        
        domain.addPredicate(atPredicate);
        domain.addPredicate(uncoveredByOpponentPredicate);
        domain.addPredicate(attackPointPredicate);
        domain.addPredicate(vantagePointPredicate);
        domain.addPredicate(vantagePointSafePredicate);
        domain.addPredicate(winPredicate);
        domain.addPredicate(highHealthPredicate);
        domain.addPredicate(opponentLowHealthPredicate);

       
        return domain;
    }

    @Override
    public PDDLProblem getProblem(AgentBody body, CGPlanningGoal goal) {
        PDDLProblem problem = new PDDLProblem("cover_game_problem", "cover_game");




        CoverGame.CGBodyPair bodyPair = env.bodyPairs.get(body.getId());

        int opponentIds[] = env.getOpponentIds(body.getId());
        OpponentData opponentData[] = env.getOpponentTeamData(body.getId()).opponentData;


        List<String> initialLiterals = new ArrayList<String>();

        Set<Loc> usefulLocations = new HashSet<Loc>();

        // body - related state
        for (int i = 0; i < 2; i++) {

            problem.addObject(bodyConstants[i]);
                    
            initialLiterals.add(atPredicate.stringAfterSubstitution(bodyConstants[i], locationsToConstants.get(bodyPair.getBodyInfo(i).loc)));
            usefulLocations.add(bodyPair.getBodyInfo(i).loc);
            //High health -> I should withstand two shots
            if (bodyPair.getBodyInfo(i).health >= env.defs.shootDamage * 2) {
                initialLiterals.add(highHealthPredicate.stringAfterSubstitution(bodyConstants[i]));
            }

        }


        // Oponent - related state
        for (int opp = 0; opp < 2; opp++) {
            problem.addObject(opponentConstants[opp]);            
            
            //Low health -> I expect them to fall for a single shot, even if they heal a little
            if (env.bodyInfos.get(opponentIds[opp]).health < env.defs.shootDamage - env.defs.healPerRound) {
                initialLiterals.add(opponentLowHealthPredicate.stringAfterSubstitution(opponentConstants[opp]));
            }

            for (int bodyId = 0; bodyId < 2; bodyId++) {
                Loc nearestSafeVantage = null;
                int nearestSafeVantageDistance = Integer.MAX_VALUE;
                Loc nearestVantage = null;
                int nearestVantageDistance = Integer.MAX_VALUE;

                Loc bodyLoc = bodyPair.getBodyInfo(bodyId).getLoc();
                for (Loc vantage_point : opponentData[opp].navpointsInvalidatingCover) {
                    int distance = fixedMapFloydWarshall.getPathCost(bodyLoc, vantage_point);
                    if (distance < nearestSafeVantageDistance) {
                        int possibleUncoveredShots = getPossibleUncoveredShots(bodyLoc, vantage_point, opponentData);
                        if (possibleUncoveredShots <= 3 && distance < nearestVantageDistance) {
                            nearestVantageDistance = distance;
                            nearestVantage = vantage_point;
                        }

                        if (possibleUncoveredShots == 0) {
                            nearestSafeVantageDistance = distance;
                            nearestSafeVantage = vantage_point;
                        }
                    }
                }


                Loc nearestAttackPoint = null;
                int nearestAttackDistance = Integer.MAX_VALUE;
                for (Loc attackPoint : opponentData[opp].visibleNavpoints) {
                    int distance = fixedMapFloydWarshall.getPathCost(bodyLoc, attackPoint);
                    if (distance < nearestAttackDistance && getPossibleUncoveredShots(bodyLoc, attackPoint, opponentData) == 0) {
                        nearestAttackPoint = attackPoint;
                        nearestAttackDistance = distance;
                    }
                }

                if (nearestAttackPoint != null) {
                    initialLiterals.add(attackPointPredicate.stringAfterSubstitution(locationsToConstants.get(nearestAttackPoint), bodyConstants[bodyId], opponentConstants[opp]));
                    usefulLocations.add(nearestAttackPoint);
                }
                if (nearestVantage != null) {
                    if(!nearestVantage.equals(nearestAttackPoint)){
                        initialLiterals.add(attackPointPredicate.stringAfterSubstitution(locationsToConstants.get(nearestVantage), bodyConstants[bodyId], opponentConstants[opp]));                        
                    }
                    initialLiterals.add(vantagePointPredicate.stringAfterSubstitution(locationsToConstants.get(nearestVantage), bodyConstants[bodyId], opponentConstants[opp]));
                    usefulLocations.add(nearestVantage);
                }
                if (nearestSafeVantage != null) {
                    if(!nearestAttackPoint.equals(nearestAttackPoint)){
                        initialLiterals.add(attackPointPredicate.stringAfterSubstitution(locationsToConstants.get(nearestSafeVantage), bodyConstants[bodyId], opponentConstants[opp]));                        
                    }
                    if(!nearestSafeVantage.equals(nearestVantage)){                        
                        initialLiterals.add(vantagePointPredicate.stringAfterSubstitution(locationsToConstants.get(nearestSafeVantage), bodyConstants[bodyId], opponentConstants[opp]));
                    }
                    initialLiterals.add(vantagePointSafePredicate.stringAfterSubstitution(locationsToConstants.get(nearestSafeVantage), bodyConstants[bodyId], opponentConstants[opp]));
                    usefulLocations.add(nearestSafeVantage);
                }
                


                if (logger.isTraceEnabled()) {
                    logger.trace(body.getId() + ": Body " + bodyId + " nearest vantage for opp " + opp + ": " + nearestVantage + " distance: " + nearestVantageDistance);
                    logger.trace(body.getId() + ": Body " + bodyId + " nearest safe vantage for opp " + opp + ": " + nearestSafeVantage + " distance: " + nearestSafeVantageDistance);
                    logger.trace(body.getId() + ": Body " + bodyId + " nearest attack point for opp " + opp + ": " + nearestAttackPoint + " distance: " + nearestAttackDistance);
                }
            }
        }

        for (Loc usefulLocation : usefulLocations) {
            problem.addObject(locationsToConstants.get(usefulLocation));
        }
        
        for (int opp = 0; opp < 2; opp++) {
            problem.addObject(opponentConstants[opp]);            
            
            for (Loc uncoveredLoc : opponentData[opp].uncoveredNavpoints) {
                if(usefulLocations.contains(uncoveredLoc)){
                    initialLiterals.add(uncoveredByOpponentPredicate.stringAfterSubstitution(locationsToConstants.get(uncoveredLoc), opponentConstants[opp]));
                }
            }
        }        
        


        problem.setInitialLiterals(initialLiterals);

        problem.setGoalCondition(winPredicate.stringAfterSubstitution());

        problem.setMinimizeActionCosts(true);
        
        return problem;

    }

    @Override
    public IReactivePlan<? extends CGPairAction> translateAction(Queue<ActionDescription> actionsFromPlanner, AgentBody body) {
        return translateActionForSimulation(env, actionsFromPlanner, body);
    }

    @Override
    public IReactivePlan<? extends CGPairAction> translateActionForSimulation(CoverGame simulationEnv, Queue<ActionDescription> actionsFromPlanner, AgentBody body) {

        if (actionsFromPlanner.isEmpty()) {
            return new SequencePlan<CGPairAction>();
        }

        List<List<CGRolePlan>> actionsForBodies = new ArrayList<List<CGRolePlan>>();
        ArrayList<CGRolePlan> actionsForBody1 = new ArrayList<CGRolePlan>();
        actionsForBodies.add(actionsForBody1);
        ArrayList<CGRolePlan> actionsForBody2 = new ArrayList<CGRolePlan>();
        actionsForBodies.add(actionsForBody2);

        int[] bodyIds = simulationEnv.getTeamIds(body.getId());


        while (!actionsFromPlanner.isEmpty()) {
            ActionDescription action = actionsFromPlanner.poll();
            boolean requiresSync = false;

            if (actionsMatch(action, winByDefense)) {
                actionsForBodies.get(0).add(new CGRoleDefensive(simulationEnv, 0));
                actionsForBodies.get(1).add(new CGRoleDefensive(simulationEnv, 1));
                break;
            }             
            
            int bodyIndex = constantToBodyIndex(action.getParameters().get(0));
            int otherBodyIndex = 1 - bodyIndex;
            int bodyId = bodyIds[bodyIndex];
            CoverGame.CGBodyInfo bodyInfo = env.bodyInfos.get(bodyId);

            CoverGame.CGBodyInfo otherBodyInfo = bodyInfo.team.getOtherInfo(bodyInfo);

            if (actionsMatch(action, moveSafeAction)) {
                Loc target = constantsToLocations.get(action.getParameters().get(1));
                actionsForBodies.get(bodyIndex).add(new CGRoleMove(simulationEnv, bodyId, target, 0));
                actionsForBodies.get(otherBodyIndex).add(new CGRoleOverWatch(simulationEnv, otherBodyInfo.id, true));
                requiresSync = true;
            } else if (actionsMatch(action, moveRecklessAction)) {
                Loc target = constantsToLocations.get(action.getParameters().get(1));
                actionsForBodies.get(bodyIndex).add(new CGRoleMove(simulationEnv, bodyId, target, 1));
            } 
            /*else if (actionsMatch(action, aggressiveAction)) {
                int opponentId = opponentConstantToBodyIndex(action.getParameters().get(1));
                actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(simulationEnv, bodyId, opponentId, 0));
            } else if (actionsMatch(action, aggressiveRecklessAction)) {
                int opponentId = opponentConstantToBodyIndex(action.getParameters().get(1));
                actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(simulationEnv, bodyId, opponentId, 1));
            } */
            else if (actionsMatch(action, winBySafeDoubleFireAction)) {
                int opponentId = opponentConstantToBodyIndex(action.getParameters().get(1));
                actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(simulationEnv, bodyId, opponentId, 1));
                actionsForBodies.get(otherBodyIndex).add(new CGRoleAggressive(simulationEnv, otherBodyInfo.id, opponentId, 0));
                requiresSync = true;
            } else if (actionsMatch(action, winBySafeFireAction)) {
                int opponentId = opponentConstantToBodyIndex(action.getParameters().get(1));
                actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(simulationEnv, bodyId, opponentId, 1));
                actionsForBodies.get(otherBodyIndex).add(new CGRoleOverWatch(simulationEnv, otherBodyInfo.id, true));                
                requiresSync = true;
            } else if (actionsMatch(action, winByRecklessFireAction)){
                int opponentId = opponentConstantToBodyIndex(action.getParameters().get(1));
                actionsForBodies.get(bodyIndex).add(new CGRoleAggressive(simulationEnv, bodyId, opponentId, 1));
                actionsForBodies.get(otherBodyIndex).add(new CGRoleAggressive(simulationEnv, otherBodyInfo.id, opponentId, 1));                
                requiresSync = true;
            }
            else {
                throw new AisteException("Unsupported action: " + action.toString());
            }

            if (requiresSync) {
                break;
            }
        }


        CGBodyPair bodyPair = simulationEnv.bodyPairs.get(body.getId());
        CGPairRolePlan plan = new CGPairRolePlan(actionsForBody1, actionsForBody2);
        if (logger.isDebugEnabled() && !simulationEnv.isSimulation) {
            logger.debug(body.getId() + ": RolePlan: " + plan);
        }
        return plan;

    }

    protected boolean actionsMatch(ActionDescription actionFromPlanner, PDDLSimpleAction action) {
        return actionFromPlanner.getName().equals(Planning4JUtils.normalizeIdentifier(action.getName()));
    }

    protected boolean isCovered(Loc loc, OpponentData[] opponentData) {
        for (int opp2 = 0; opp2 < 2; opp2++) {
            if (opponentData[opp2].uncoveredNavpoints.contains(loc)) {
                return false;
            }
        }

        return true;
    }

    protected int getPossibleUncoveredShots(Loc bodyLoc, Loc vantage_point, OpponentData[] opponentData) {
        int possibleUncoveredShots = 0;
        List<Loc> path = fixedMapFloydWarshall.getPath(bodyLoc, vantage_point);
        path.add(vantage_point);
        for (Loc pathLoc : path) {
            for (int opp2 = 0; opp2 < 2; opp2++) {
                if (opponentData[opp2].uncoveredNavpoints.contains(pathLoc)) {
                    possibleUncoveredShots++;
                }
            }
        }
        return possibleUncoveredShots;
    }
}
