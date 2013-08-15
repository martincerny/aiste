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

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.ISimulablePDDLRepresentation;
import cz.cuni.amis.aiste.environment.impl.SequencePlan;
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.CGBodyPair;
import cz.cuni.amis.aiste.simulations.covergame.CoverGame.OpponentData;
import cz.cuni.amis.planning4j.ActionDescription;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Martin Cerny
 */
public class CGPDDLRepresentation extends AbstractCGPlanningRepresentation<PDDLDomain, PDDLProblem, ActionDescription>
        implements ISimulablePDDLRepresentation<CGPairAction, CoverGame, CGPlanningGoal> {

    PDDLType navPointType;
    Map<Loc, PDDLObjectInstance> navPointInstances;
    Map<String, Loc> navPointNamesToLocations;
    
    PDDLPredicate adjacentPredicate;
    PDDLPredicate body_0_turn;
    
    List<String> adjacencyPredicates;
    
    OneBodyPDDL bodyPDDLs[];
    
    PDDLType opponentType;
    PDDLObjectInstance opponentInstances[];
    
    PDDLPredicate uncoveredByOpponentPredicate;
    PDDLPredicate visibleByOpponentPredicate;
    PDDLPredicate vantagePointPredicate;
    PDDLPredicate winPredicate;
        
    PDDLPredicate opponentAtPredicate;
    
    //joint actions
    PDDLSimpleAction attackCrossfireAction;
    PDDLSimpleAction attackSingleAction;
    PDDLSimpleAction defendAction;
    
    //tuning parameters for joint actions
    double defendCost = 100;
    double attackSingleCost = 3;

    public CGPDDLRepresentation(CoverGame env) {
        super(env);

        navPointType = new PDDLType("nav_point");
        opponentType = new PDDLType("opponent");

        navPointInstances = new HashMap<Loc, PDDLObjectInstance>();
        navPointNamesToLocations = new HashMap<String, Loc>();
        for (Loc navPoint : env.defs.navGraph.keySet()) {
            String navPointName = "nav_point_" + navPoint.x + "_" + navPoint.y;
            navPointInstances.put(navPoint, new PDDLObjectInstance(navPointName, navPointType));
            navPointNamesToLocations.put(Planning4JUtils.normalizeIdentifier(navPointName), navPoint);
        }

        adjacentPredicate = new PDDLPredicate("adjacent", new PDDLParameter("p1", navPointType), new PDDLParameter("p2", navPointType));
        adjacencyPredicates = new ArrayList<String>();

        for (Loc navPoint : env.defs.navGraph.keySet()) {
            for (Loc neighbour : env.defs.navGraph.get(navPoint)) {
                adjacencyPredicates.add(adjacentPredicate.stringAfterSubstitution(navPointInstances.get(navPoint), navPointInstances.get(neighbour)));
            }
        }

        body_0_turn = new PDDLPredicate("body_0_turn");        

        opponentAtPredicate = new PDDLPredicate("opponent_at",new PDDLParameter("o", opponentType),  new PDDLParameter("loc", navPointType));
        uncoveredByOpponentPredicate = new PDDLPredicate("uncovered_by_opponent", new PDDLParameter("o", opponentType), new PDDLParameter("loc", navPointType));            
        visibleByOpponentPredicate = new PDDLPredicate("visible_by_opponent", new PDDLParameter("o", opponentType), new PDDLParameter("loc", navPointType));            
        vantagePointPredicate = new PDDLPredicate("vantage_point", new PDDLParameter("o", opponentType), new PDDLParameter("loc", navPointType));            
        
        winPredicate = new PDDLPredicate("win");
        
        opponentInstances = new PDDLObjectInstance[] { new PDDLObjectInstance("opponent_1", opponentType), new PDDLObjectInstance("opponent_2", opponentType)};
        
        bodyPDDLs = new OneBodyPDDL[]{new OneBodyPDDL(0, env), new OneBodyPDDL(1, env)};
        bodyPDDLs[0].createJointActions(bodyPDDLs[1]);
        bodyPDDLs[1].createJointActions(bodyPDDLs[0]);

        /* Joint actions */
        defendAction = new PDDLSimpleAction("defend");
        defendAction.setPreconditionList(
                body_0_turn.stringAfterSubstitution(),
                bodyPDDLs[0].partialCoverPredicate.stringAfterSubstitution(), 
                bodyPDDLs[1].partialCoverPredicate.stringAfterSubstitution());
        defendAction.setPositiveEffects(winPredicate.stringAfterSubstitution(),
                "increase (total-cost) " + Double.toString(defendCost));
        
        

    }

    @Override
    public PDDLDomain getDomain(AgentBody body) {
        PDDLDomain domain = new PDDLDomain("cover_game", EnumSet.of(PDDLRequirement.TYPING));
        domain.addType(navPointType);
        domain.addType(opponentType);
        
        domain.addPredicate(adjacentPredicate);
        domain.addPredicate(body_0_turn);
        domain.addPredicate(uncoveredByOpponentPredicate);        
        domain.addPredicate(opponentAtPredicate);
        domain.addPredicate(visibleByOpponentPredicate);        
        domain.addPredicate(vantagePointPredicate);
        domain.addPredicate(winPredicate);
        
        domain.addFunction(new PDDLFunction("total-cost"));        
        
        domain.addAction(defendAction);

        for (OneBodyPDDL pddl : bodyPDDLs) {
            for (PDDLSimpleAction action : pddl.bodyActions) {
                domain.addAction(action);
            }
            for (PDDLSimpleAction action : pddl.jointActions) {
                domain.addAction(action);
            }
            for (PDDLPredicate bodyPredicate : pddl.bodyPredicates) {
                domain.addPredicate(bodyPredicate);
            }
        }
        
        return domain;
    }

    @Override
    public PDDLProblem getProblem(AgentBody body, CGPlanningGoal goal) {
        PDDLProblem problem = new PDDLProblem("cover_problem", "cover_game");
        List<String> initialState = new ArrayList<String>();
        initialState.addAll(adjacencyPredicates);

        for (PDDLObjectInstance navPointInstance : navPointInstances.values()) {
            problem.addObject(navPointInstance);
        }

        CoverGame.CGBodyPair bodyPair = env.bodyPairs.get(body.getId());

        int ids[] = env.getOpponentIds(body.getId());
        OpponentData opponentData[] = env.getOpponentTeamData(body.getId()).opponentData;

        // body - related state
        for (int i = 0; i < 2; i++) {
            initialState.add(bodyPDDLs[i].bodyAtPredicate.stringAfterSubstitution(navPointInstances.get(bodyPair.getBodyInfo(i).loc)));
            CoverGame.CGBodyInfo bodyInfo = bodyPair.getBodyInfo(i);
            boolean coveredFromAll = true;
            for(int op = 0; op < 2; op++){                
                CoverGame.CGBodyInfo opponentInfo = env.bodyInfos.get(ids[op]);
                if(env.isVisible(opponentInfo.loc, bodyInfo.loc) && !env.isCovered(opponentInfo.loc, bodyInfo.loc)){
                    coveredFromAll = false;
                }
            }
            if(coveredFromAll){
                initialState.add(bodyPDDLs[i].partialCoverPredicate.stringAfterSubstitution());                
            }
        }

        initialState.add(body_0_turn.stringAfterSubstitution());

        /* Opponent - related state*/
        for(int i = 0; i < 2; i++){
            problem.addObject(opponentInstances[i]);
            for(Loc uncoveredLoc : opponentData[i].uncoveredNavpoints){
                initialState.add(uncoveredByOpponentPredicate.stringAfterSubstitution(opponentInstances[i], navPointInstances.get(uncoveredLoc)));
            }
            for(Loc visibleLoc : opponentData[i].visibleNavpoints){
                initialState.add(visibleByOpponentPredicate.stringAfterSubstitution(opponentInstances[i], navPointInstances.get(visibleLoc)));
            }
            for(Loc vantageLoc : opponentData[i].navpointsInvalidatingCover){
                initialState.add(vantagePointPredicate.stringAfterSubstitution(opponentInstances[i], navPointInstances.get(vantageLoc)));
            }
        }
        
        problem.setInitialLiterals(initialState);

        problem.setMinimizeActionCosts(true);

        List<String> goalConditions = new ArrayList<String>();
        switch (goal.getType()) {
            case FIND_COVER: {
                goalConditions.add(bodyPDDLs[0].partialCoverPredicate.stringAfterSubstitution());
                goalConditions.add(bodyPDDLs[1].partialCoverPredicate.stringAfterSubstitution());
                break;
            }
            case ATTACK: {
                throw new UnsupportedOperationException();
            }
            case WIN : {
                goalConditions.add(winPredicate.stringAfterSubstitution());
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unrecognized goal: " + goal.getType());

            }

        }
        problem.setGoalConditions(goalConditions);
        return problem;
    }

    @Override
    public IReactivePlan<? extends CGPairAction> translateAction(Queue<ActionDescription> actionsFromPlanner, AgentBody body) {
        ActionDescription nextAction = actionsFromPlanner.peek();

        CGBodyPair bodypair = env.bodyPairs.get(body.getId());
        //Check for joint actions
        if(nextAction.getName().equals(Planning4JUtils.normalizeIdentifier(defendAction.getName()))){
            actionsFromPlanner.poll();
            return new CGPairRolePlan(Collections.<CGRolePlan>singletonList(new CGRoleDefensive(env, bodypair.bodyInfo0.id)), Collections.<CGRolePlan>singletonList(new CGRoleDefensive(env, bodypair.bodyInfo1.id)));
        }
        //body-related joint actions
        for(int bodyIndex = 0; bodyIndex < 2; bodyIndex++){
            CGAction mainAction = null;
            CGAction supportAction = null;
            if(nextAction.getName().equals(Planning4JUtils.normalizeIdentifier(bodyPDDLs[bodyIndex].attackWithSupportAgainst_0_Action.getName()))){
                int targetOpponent = getBodyIdFromOpponentInstanceName(body, nextAction.getParameters().get(0));
                mainAction = new CGAction(CGAction.Action.SHOOT, targetOpponent);
                supportAction = new CGAction(CGAction.Action.SUPPRESS, getBodyIdFromOpponentIndex(body, 0));                
            }
            else if(nextAction.getName().equals(Planning4JUtils.normalizeIdentifier(bodyPDDLs[bodyIndex].attackWithSupportAgainst_1_Action.getName()))){
                int targetOpponent = getBodyIdFromOpponentInstanceName(body, nextAction.getParameters().get(0));
                mainAction = new CGAction(CGAction.Action.SHOOT, targetOpponent);
                supportAction = new CGAction(CGAction.Action.SUPPRESS, getBodyIdFromOpponentIndex(body, 1));                
            }
            else if(nextAction.getName().equals(Planning4JUtils.normalizeIdentifier(bodyPDDLs[bodyIndex].moveWithSupportAgainst_0_Action.getName()))){
                String navPointName = nextAction.getParameters().get(1);
                Loc target = navPointNamesToLocations.get(navPointName);                
                mainAction = new CGAction(CGAction.Action.MOVE, target);
                supportAction = new CGAction(CGAction.Action.SUPPRESS, getBodyIdFromOpponentIndex(body, 0));                
            }
            else if(nextAction.getName().equals(Planning4JUtils.normalizeIdentifier(bodyPDDLs[bodyIndex].moveWithSupportAgainst_1_Action.getName()))){
                String navPointName = nextAction.getParameters().get(1);
                Loc target = navPointNamesToLocations.get(navPointName);                
                mainAction = new CGAction(CGAction.Action.MOVE, target);
                supportAction = new CGAction(CGAction.Action.SUPPRESS, getBodyIdFromOpponentIndex(body, 1));                
            }

            
            if(mainAction != null){
                actionsFromPlanner.poll();
                if(bodyIndex == 0){
                    return new SequencePlan<CGPairAction>(new CGPairAction(mainAction, supportAction));
                } else {
                    return new SequencePlan<CGPairAction>(new CGPairAction(supportAction, mainAction));
                }
            }
        }
        
        //I believe that actions are alternating (they definitely should be :-)
        CGAction action0 = translateSingleBodyAction(0, actionsFromPlanner.poll());
        CGAction action1 = CGAction.NO_OP_ACTION;
        if (!actionsFromPlanner.isEmpty()) {
            action1 = translateSingleBodyAction(1, actionsFromPlanner.poll());
        }
        CGPairAction pairAction = new CGPairAction(action0, action1);
        return new SequencePlan<CGPairAction>(pairAction);

    }

    protected CGAction translateSingleBodyAction(int bodyId, ActionDescription desc) {
        if (!desc.getName().startsWith(Planning4JUtils.normalizeIdentifier(bodyPDDLs[bodyId].bodyPrefix))) {
            throw new IllegalArgumentException("Incorrect body");
        }
        String actionWithoutPrefix = desc.getName().substring(bodyPDDLs[bodyId].bodyPrefix.length());
        if (actionWithoutPrefix.equals("MOVE")) {
            String navPointName = desc.getParameters().get(1);
            Loc target = navPointNamesToLocations.get(navPointName);
            return new CGAction(CGAction.Action.MOVE, target);
        } else if (actionWithoutPrefix.equals("NOOP")) {
            return CGAction.NO_OP_ACTION;
        }
        throw new IllegalArgumentException("Unrecognized action");
    }
    
    protected int getBodyIdFromOpponentInstanceName(AgentBody body, String opponentInstanceName){
        if(opponentInstanceName.equals(Planning4JUtils.normalizeIdentifier(opponentInstances[0].getName()))){
            return getBodyIdFromOpponentIndex(body, 0);
        }
        else if(opponentInstanceName.equals(Planning4JUtils.normalizeIdentifier(opponentInstances[1].getName()))){
            return getBodyIdFromOpponentIndex(body, 1);
        } else {
            throw new IllegalArgumentException("Unrecognized opp name: " + opponentInstanceName);
        }
    }
    
    protected int getBodyIdFromOpponentIndex(AgentBody body, int opponentIndex){
        int opponentBodyPairId;
        if(body.getId() == 0){
            opponentBodyPairId = 1;
        } else if(body.getId() == 1){
            opponentBodyPairId = 0;
        } else {
            throw new IllegalArgumentException("Unrecognized body Id: " + body.getId());
        }
        
        return env.bodyPairs.get(opponentBodyPairId).getBodyInfo(opponentIndex).id;
        
    }
    
    protected String getCoveredCondition(String locationExpression) {
        return "forall (?o - " + opponentType.getTypeName() + ") (not (" + uncoveredByOpponentPredicate.stringAfterSubstitution("?o", locationExpression) + "))";
    }
    

    class OneBodyPDDL {

        PDDLPredicate bodyAtPredicate;
        PDDLPredicate partialCoverPredicate;
        PDDLPredicate fullCoverPredicate;
        
        PDDLSimpleAction moveAction;
        PDDLSimpleAction noOpAction;
        PDDLSimpleAction takeFullCoverAction;
        
        /**
         * Joint actions with the other body
         */
        PDDLSimpleAction attackWithSupportAgainst_0_Action;
        PDDLSimpleAction attackWithSupportAgainst_1_Action;
        PDDLSimpleAction moveWithSupportAgainst_0_Action;
        PDDLSimpleAction moveWithSupportAgainst_1_Action;
        
        List<PDDLSimpleAction> bodyActions = new ArrayList<PDDLSimpleAction>();
        List<PDDLSimpleAction> jointActions = new ArrayList<PDDLSimpleAction>();
        List<PDDLPredicate> bodyPredicates = new ArrayList<PDDLPredicate>();
        String bodyPrefix;
        
        String uncoveredCostString;

        public OneBodyPDDL(int bodyId, CoverGame cg) {
            bodyPrefix = "body_" + bodyId + "_";
            
            uncoveredCostString = Double.toString(1d / cg.defs.partialCoverAimPenalty);


            bodyAtPredicate = new PDDLPredicate(bodyPrefix + "at", new PDDLParameter("loc", navPointType));            
            bodyPredicates.add(bodyAtPredicate);
            
            partialCoverPredicate = new PDDLPredicate(bodyPrefix + "partial_cover");
            bodyPredicates.add(partialCoverPredicate);

            fullCoverPredicate = new PDDLPredicate(bodyPrefix + "full_cover");
            bodyPredicates.add(fullCoverPredicate);

            /* Body acitons */
            moveAction = new PDDLSimpleAction(bodyPrefix + "move", new PDDLParameter("from", navPointType), new PDDLParameter("to", navPointType));
            moveAction.setPreconditionList(
                    bodyAtPredicate.stringAfterSubstitution("?from"),
                    adjacentPredicate.stringAfterSubstitution("?from", "?to"));
            String coveredCondition = getCoveredCondition("?to");
            moveAction.setPositiveEffects(bodyAtPredicate.stringAfterSubstitution("?to"),
                    "increase (total-cost) 1",
                    "when (" + coveredCondition + ") (" + partialCoverPredicate.stringAfterSubstitution() + ")",
                    "when (not (" + coveredCondition + ")) (not (" + partialCoverPredicate.stringAfterSubstitution() + ")) "
                    );            
            moveAction.setNegativeEffects(bodyAtPredicate.stringAfterSubstitution("?from"), fullCoverPredicate.stringAfterSubstitution());
            bodyActions.add(moveAction);
            
            takeFullCoverAction = new PDDLSimpleAction(bodyPrefix + "take_full_cover");
            takeFullCoverAction.setPreconditionList(partialCoverPredicate.stringAfterSubstitution());
            takeFullCoverAction.setPositiveEffects(fullCoverPredicate.stringAfterSubstitution());
            
            noOpAction = new PDDLSimpleAction(bodyPrefix + "noop", new PDDLParameter("to", navPointType));
            noOpAction.setPreconditionList(bodyAtPredicate.stringAfterSubstitution("?to"));
            bodyActions.add(noOpAction);

            for (PDDLSimpleAction action : bodyActions) {
                action.addPositiveEffect("when (not (" + partialCoverPredicate.stringAfterSubstitution() + ")) (increase (total-cost) "+ uncoveredCostString + ")");
                if (bodyId == 0) {
                    action.addPrecondition(body_0_turn.stringAfterSubstitution());
                    action.addNegativeEffect(body_0_turn.stringAfterSubstitution());
                } else {
                    action.addPrecondition(PDDLOperators.makeNot(body_0_turn.stringAfterSubstitution()));
                    action.addPositiveEffect(body_0_turn.stringAfterSubstitution());
                }
            }
            
        }
        
        protected void createJointActions(OneBodyPDDL otherBody){
            /* Joint actions with this body in lead role*/
            attackWithSupportAgainst_0_Action = createAttackWithSupportAction(otherBody, opponentInstances[0], opponentInstances[1]);
            attackWithSupportAgainst_1_Action = createAttackWithSupportAction(otherBody, opponentInstances[1], opponentInstances[0]);
            jointActions.add(attackWithSupportAgainst_0_Action);
            jointActions.add(attackWithSupportAgainst_1_Action);

            moveWithSupportAgainst_0_Action = createMoveWithSupportAction(otherBody, opponentInstances[0], opponentInstances[1]);
            moveWithSupportAgainst_1_Action = createMoveWithSupportAction(otherBody, opponentInstances[1], opponentInstances[0]);
            jointActions.add(moveWithSupportAgainst_0_Action);
            jointActions.add(moveWithSupportAgainst_1_Action);

            for(PDDLSimpleAction jointAction : jointActions){
                jointAction.addPrecondition(body_0_turn.stringAfterSubstitution());                
            }            
        }
        
        private PDDLSimpleAction createAttackWithSupportAction(OneBodyPDDL otherBody, PDDLObjectInstance supressedOpponent, PDDLObjectInstance otherOpponent){
            PDDLSimpleAction action = new PDDLSimpleAction(bodyPrefix + "attack_with_support_against_" + supressedOpponent.getName(), new PDDLParameter("target", opponentType), new PDDLParameter("my_loc", navPointType), new PDDLParameter("other_loc", navPointType));
            action.setPreconditionList(
                    bodyAtPredicate.stringAfterSubstitution("?my_loc"),
                    otherBody.bodyAtPredicate.stringAfterSubstitution("?other_loc"),
                    visibleByOpponentPredicate.stringAfterSubstitution(supressedOpponent.getNameForPDDL(), "?other_loc"),
                    vantagePointPredicate.stringAfterSubstitution("?target", "?my_loc"),
                    PDDLOperators.makeNot(uncoveredByOpponentPredicate.stringAfterSubstitution(otherOpponent, "?my_loc")),
                    PDDLOperators.makeNot(uncoveredByOpponentPredicate.stringAfterSubstitution(otherOpponent, "?other_loc"))                   
                    );
            action.setPositiveEffects(
                    winPredicate.stringAfterSubstitution()
                    );
            return action;
        } 

        private PDDLSimpleAction createMoveWithSupportAction(OneBodyPDDL otherBody, PDDLObjectInstance supressedOpponent, PDDLObjectInstance otherOpponent){
            PDDLSimpleAction action = new PDDLSimpleAction(bodyPrefix + "move_with_support_against_" + supressedOpponent.getName(), new PDDLParameter("from", navPointType), new PDDLParameter("to", navPointType), new PDDLParameter("other_loc", navPointType));
            action.setPreconditionList(
                    bodyAtPredicate.stringAfterSubstitution("?from"),
                    otherBody.bodyAtPredicate.stringAfterSubstitution("?other_loc"),
                    visibleByOpponentPredicate.stringAfterSubstitution(supressedOpponent.getNameForPDDL(), "?other_loc"),
                    adjacentPredicate.stringAfterSubstitution("?from", "?to"),
                    PDDLOperators.makeNot(uncoveredByOpponentPredicate.stringAfterSubstitution(otherOpponent, "?from")),
                    PDDLOperators.makeNot(uncoveredByOpponentPredicate.stringAfterSubstitution(otherOpponent, "?other_loc"))                   
                    );
            
            String coveredCondition = getCoveredCondition("?to");
            
            action.setPositiveEffects(bodyAtPredicate.stringAfterSubstitution("?to"),
                    "increase (total-cost) 2",
                    "when (" + coveredCondition + ") (" + partialCoverPredicate.stringAfterSubstitution() + ")",
                    "when (not (" + coveredCondition + ")) (not (" + partialCoverPredicate.stringAfterSubstitution() + "))" 
                    );            
            action.setNegativeEffects(bodyAtPredicate.stringAfterSubstitution("?from"), fullCoverPredicate.stringAfterSubstitution());
            return action;
        } 
        
        
    }
    
}
