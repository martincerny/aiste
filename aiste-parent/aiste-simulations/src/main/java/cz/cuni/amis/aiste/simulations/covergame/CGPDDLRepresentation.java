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
import java.util.Iterator;
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
    PDDLPredicate opponentVisiblePredicate;
    PDDLPredicate vantagePointPredicate;
    PDDLPredicate winPredicate;
        
    PDDLPredicate opponentAtPredicate;
    
    //joint actions
    PDDLSimpleAction attackCrossfireAction;
    PDDLSimpleAction holdPositionAction;
    
    List<PDDLSimpleAction> globalJointActions;
    
    //tuning parameters for joint actions
    double holdPositionCost = 10;
    double attackSingleCost = 5;
    
    /**
     * Whether the domain forces bodies to interleave actions using the body_0_turn predicate.
     */
    private boolean forceBodyAlternation;

    public CGPDDLRepresentation(CoverGame env, boolean forceBodyAlternation) {
        super(env);
        
        this.forceBodyAlternation = forceBodyAlternation;

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
        opponentVisiblePredicate = new PDDLPredicate("opponent_visible", new PDDLParameter("o", opponentType), new PDDLParameter("loc", navPointType));            
        vantagePointPredicate = new PDDLPredicate("vantage_point", new PDDLParameter("o", opponentType), new PDDLParameter("loc", navPointType));            
        
        winPredicate = new PDDLPredicate("win");
        
        opponentInstances = new PDDLObjectInstance[] { new PDDLObjectInstance("opponent_0", opponentType), new PDDLObjectInstance("opponent_1", opponentType)};
        
        bodyPDDLs = new OneBodyPDDL[]{new OneBodyPDDL(0, env), new OneBodyPDDL(1, env)};
        bodyPDDLs[0].createJointActions(bodyPDDLs[1]);
        bodyPDDLs[1].createJointActions(bodyPDDLs[0]);

        /* Joint actions */
        globalJointActions = new ArrayList<PDDLSimpleAction>();
        
        holdPositionAction = new PDDLSimpleAction("hold_position", new PDDLParameter("loc0", navPointType), new PDDLParameter("loc1", navPointType), new PDDLParameter("op0", opponentType), new PDDLParameter("op1", opponentType));
        holdPositionAction.setPreconditionList(                
                bodyPDDLs[0].partialCoverPredicate.stringAfterSubstitution(), 
                bodyPDDLs[1].partialCoverPredicate.stringAfterSubstitution(),
                bodyPDDLs[0].bodyAtPredicate.stringAfterSubstitution("?loc0"),
                bodyPDDLs[1].bodyAtPredicate.stringAfterSubstitution("?loc1"),
                opponentVisiblePredicate.stringAfterSubstitution("?op0", "?loc0"),
                opponentVisiblePredicate.stringAfterSubstitution("?op1", "?loc1")
                );
        if(forceBodyAlternation){
            holdPositionAction.addPrecondition(body_0_turn.stringAfterSubstitution());
        }
        holdPositionAction.setPositiveEffects(winPredicate.stringAfterSubstitution(),
                "increase (total-cost) " + Double.toString(holdPositionCost));
        globalJointActions.add(holdPositionAction);

        
        attackCrossfireAction = new PDDLSimpleAction("attackCrossfire", new PDDLParameter("target", opponentType), new PDDLParameter("loc0", navPointType), new PDDLParameter("loc1", navPointType));
        attackCrossfireAction.setPreconditionList(
                bodyPDDLs[0].partialCoverPredicate.stringAfterSubstitution(), 
                bodyPDDLs[1].partialCoverPredicate.stringAfterSubstitution(),
                bodyPDDLs[0].bodyAtPredicate.stringAfterSubstitution("?loc0"),
                bodyPDDLs[1].bodyAtPredicate.stringAfterSubstitution("?loc1"),
                vantagePointPredicate.stringAfterSubstitution("?target", "?loc0"),
                vantagePointPredicate.stringAfterSubstitution("?target", "?loc1")
        );
        if(forceBodyAlternation){
            attackCrossfireAction.addPrecondition(body_0_turn.stringAfterSubstitution());
        }
        attackCrossfireAction.setPositiveEffects(winPredicate.stringAfterSubstitution());        
        globalJointActions.add(attackCrossfireAction);
        //crossfire has zero cost, as it is the most desired situation    
    }

    @Override
    public PDDLDomain getDomain(AgentBody body) {
        PDDLDomain domain = new PDDLDomain("cover_game", EnumSet.of(PDDLRequirement.TYPING));
        domain.addType(navPointType);
        domain.addType(opponentType);
        
        domain.addPredicate(adjacentPredicate);
        
        if(forceBodyAlternation){
            domain.addPredicate(body_0_turn);
        }
        
        domain.addPredicate(uncoveredByOpponentPredicate);        
        domain.addPredicate(opponentAtPredicate);
        domain.addPredicate(opponentVisiblePredicate);        
        domain.addPredicate(vantagePointPredicate);
        domain.addPredicate(winPredicate);
        
        domain.addFunction(new PDDLFunction("total-cost"));        
        
        domain.addAction(holdPositionAction);
        domain.addAction(attackCrossfireAction);        

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

        if(forceBodyAlternation){
            initialState.add(body_0_turn.stringAfterSubstitution());
        }

        /* Opponent - related state*/
        for(int i = 0; i < 2; i++){
            problem.addObject(opponentInstances[i]);
            for(Loc uncoveredLoc : opponentData[i].uncoveredNavpoints){
                initialState.add(uncoveredByOpponentPredicate.stringAfterSubstitution(opponentInstances[i], navPointInstances.get(uncoveredLoc)));
            }
            for(Loc visibleLoc : opponentData[i].possibleAttackNavpoints){
                initialState.add(opponentVisiblePredicate.stringAfterSubstitution(opponentInstances[i], navPointInstances.get(visibleLoc)));
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
        return translateActionForSimulation(env, actionsFromPlanner, body);
    }

    
    
    @Override
    public IReactivePlan<? extends CGPairAction> translateActionForSimulation(CoverGame simulationEnv, Queue<ActionDescription> actionsFromPlanner, AgentBody body) {
        ActionDescription nextAction = actionsFromPlanner.peek();

        CGBodyPair bodypair = simulationEnv.bodyPairs.get(body.getId());
        //Check for joint actions
        if(nextAction.getName().equals(Planning4JUtils.normalizeIdentifier(holdPositionAction.getName()))){
            actionsFromPlanner.poll();
            return new CGPairRolePlan(Collections.<CGRolePlan>singletonList(new CGRoleOverWatch(simulationEnv, bodypair.bodyInfo0.id)), Collections.<CGRolePlan>singletonList(new CGRoleOverWatch(simulationEnv, bodypair.bodyInfo1.id)));
        } else if(nextAction.getName().equals(Planning4JUtils.normalizeIdentifier(attackCrossfireAction.getName()))){
            actionsFromPlanner.poll();
            int targetIndex = getBodyIndexFromOpponentInstanceName(body, nextAction.getParameters().get(0));
            return new CGPairRolePlan(Collections.<CGRolePlan>singletonList(new CGRoleAggressive(simulationEnv, bodypair.bodyInfo0.id, targetIndex)), Collections.<CGRolePlan>singletonList(new CGRoleAggressive(simulationEnv, bodypair.bodyInfo1.id, targetIndex)));
        }
        
        //body-related joint actions
        for(int bodyIndex = 0; bodyIndex < 2; bodyIndex++){
            if(nextAction.getName().equals(Planning4JUtils.normalizeIdentifier(bodyPDDLs[bodyIndex].attackSingleAction.getName()))){
                actionsFromPlanner.poll();
                int targetIndex = getBodyIndexFromOpponentInstanceName(body, nextAction.getParameters().get(0));
                return new CGPairRolePlan(Collections.<CGRolePlan>singletonList(new CGRoleAggressive(simulationEnv, bodypair.bodyInfo0.id, targetIndex)), Collections.<CGRolePlan>singletonList(new CGRoleAggressive(simulationEnv, bodypair.bodyInfo1.id, targetIndex)));
            }
            
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
        CGAction[] actions = new CGAction[] {CGAction.NO_OP_ACTION, CGAction.NO_OP_ACTION};

        if(forceBodyAlternation){
            actions[0] = translateSingleBodyAction(0, actionsFromPlanner.poll());
            if (!actionsFromPlanner.isEmpty()) {
                actions[1] = translateSingleBodyAction(1, actionsFromPlanner.poll());
            }
        } else {
            
            /*
             * If alternation is not forced, I execute first available action for both bodies. But I never
             * search behind any joint action
             * */
            
            ActionDescription firstActionDescription = actionsFromPlanner.poll();
            int firstActionBodyIndex;
            if(firstActionDescription.getName().startsWith(Planning4JUtils.normalizeIdentifier(bodyPDDLs[0].bodyPrefix))){
                firstActionBodyIndex = 0;
            } else if (firstActionDescription.getName().startsWith(Planning4JUtils.normalizeIdentifier(bodyPDDLs[1].bodyPrefix))){
                firstActionBodyIndex = 1;
            } else {
                throw new IllegalArgumentException("Could not relate action to a body for identifier: " + firstActionDescription.getName());
            }
            
            int otherBodyIndex = 1 - firstActionBodyIndex;
            actions[firstActionBodyIndex] = translateSingleBodyAction(firstActionBodyIndex, firstActionDescription);
                        
            Iterator<ActionDescription> it = actionsFromPlanner.iterator();
            while(it.hasNext()){
                ActionDescription desc = it.next();
                
                if(isJointAction(desc)){
                    break;
                }
                
                if(desc.getName().startsWith(Planning4JUtils.normalizeIdentifier(bodyPDDLs[otherBodyIndex].bodyPrefix))){
                    actions[otherBodyIndex] = translateSingleBodyAction(otherBodyIndex, desc);
                    it.remove();
                    break;
                }
            }
        }
        CGPairAction pairAction = new CGPairAction(actions[0], actions[1]);
        return new SequencePlan<CGPairAction>(pairAction);

    }
    
    protected boolean isJointAction(ActionDescription desc){
        for(PDDLSimpleAction gja : globalJointActions){
            if(desc.getName().equals(Planning4JUtils.normalizeIdentifier(gja.getName()))){
                return true;
            }
        }
        for(int bodyId = 0; bodyId < 2; bodyId ++){
            for(PDDLSimpleAction ja : bodyPDDLs[bodyId].jointActions){
                if(desc.getName().equals(Planning4JUtils.normalizeIdentifier(ja.getName()))){
                    return true;
                }
            }
        }
        
        return false;
    }

    protected CGAction translateSingleBodyAction(int bodyId, ActionDescription desc) {
        if (!desc.getName().startsWith(Planning4JUtils.normalizeIdentifier(bodyPDDLs[bodyId].bodyPrefix))) {
            throw new IllegalArgumentException("Incorrect body. Expected " + bodyId + " for identifier " + desc.getName());
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
    
    protected int getBodyIndexFromOpponentInstanceName(AgentBody body, String opponentInstanceName){
        if(opponentInstanceName.equals(Planning4JUtils.normalizeIdentifier(opponentInstances[0].getName()))){
            return 0;
        }
        else if(opponentInstanceName.equals(Planning4JUtils.normalizeIdentifier(opponentInstances[1].getName()))){
            return 1;
        } else {
            throw new IllegalArgumentException("Unrecognized opp name: " + opponentInstanceName);
        }
    }
    
    
    protected int getBodyIdFromOpponentInstanceName(AgentBody body, String opponentInstanceName){
        return getBodyIdFromOpponentIndex(body, getBodyIndexFromOpponentInstanceName(body, opponentInstanceName));        
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
    

    @Override
    public String getLoggableRepresentation() {
        return "PDDL_Straightforward" + (forceBodyAlternation ? "_Alterning" : "");
    }
    
    
    class OneBodyPDDL {

        PDDLPredicate bodyAtPredicate;
        PDDLPredicate partialCoverPredicate;
        PDDLPredicate fullCoverPredicate;
        PDDLPredicate canSupressPredicate;
        
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
        
        PDDLSimpleAction attackSingleAction;
        
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
            
            canSupressPredicate = new PDDLPredicate(bodyPrefix + "can_supress");
            bodyPredicates.add(canSupressPredicate);

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
                
                //all actions allow me to supress next turn
                action.addPositiveEffect(canSupressPredicate.stringAfterSubstitution());
                if(forceBodyAlternation){
                    if (bodyId == 0) {
                        action.addPrecondition(body_0_turn.stringAfterSubstitution());
                        action.addNegativeEffect(body_0_turn.stringAfterSubstitution());
                    } else {
                        action.addPrecondition(PDDLOperators.makeNot(body_0_turn.stringAfterSubstitution()));
                        action.addPositiveEffect(body_0_turn.stringAfterSubstitution());
                    }
                }
            }
            
        }
        
        protected void createJointActions(OneBodyPDDL otherBody){
            /* Joint actions with this body in lead role*/
            
            attackWithSupportAgainst_0_Action = createAttackWithSupportAction(otherBody, opponentInstances[0], opponentInstances[1]);
            attackWithSupportAgainst_1_Action = createAttackWithSupportAction(otherBody, opponentInstances[1], opponentInstances[0]);
            /* Attack with support does not seem effective - removing
            jointActions.add(attackWithSupportAgainst_0_Action);
            jointActions.add(attackWithSupportAgainst_1_Action);
            */
 
            moveWithSupportAgainst_0_Action = createMoveWithSupportAction(otherBody, opponentInstances[0], opponentInstances[1]);
            moveWithSupportAgainst_1_Action = createMoveWithSupportAction(otherBody, opponentInstances[1], opponentInstances[0]);
            jointActions.add(moveWithSupportAgainst_0_Action);
            jointActions.add(moveWithSupportAgainst_1_Action);

            
            attackSingleAction = new PDDLSimpleAction(bodyPrefix + "attackSingle", new PDDLParameter("target", opponentType), new PDDLParameter("loc0", navPointType), new PDDLParameter("loc1", navPointType));
            attackSingleAction.setPreconditionList(
                    partialCoverPredicate.stringAfterSubstitution(), 
                    otherBody.partialCoverPredicate.stringAfterSubstitution(),
                    bodyAtPredicate.stringAfterSubstitution("?loc0"),
                    otherBody.bodyAtPredicate.stringAfterSubstitution("?loc1"),
                    vantagePointPredicate.stringAfterSubstitution("?target", "?loc0")                    
            );
            attackSingleAction.setPositiveEffects(winPredicate.stringAfterSubstitution(),
                    "increase (total-cost) " + Double.toString(attackSingleCost));        
            jointActions.add(attackSingleAction);
            

            if(forceBodyAlternation){
                for(PDDLSimpleAction jointAction : jointActions){
                    jointAction.addPrecondition(body_0_turn.stringAfterSubstitution());                
                }            
            }
        }
        
        private PDDLSimpleAction createAttackWithSupportAction(OneBodyPDDL otherBody, PDDLObjectInstance supressedOpponent, PDDLObjectInstance otherOpponent){
            PDDLSimpleAction action = new PDDLSimpleAction(bodyPrefix + "attack_with_support_against_" + supressedOpponent.getName(), new PDDLParameter("target", opponentType), new PDDLParameter("my_loc", navPointType), new PDDLParameter("other_loc", navPointType));
            action.setPreconditionList(
                    otherBody.canSupressPredicate.stringAfterSubstitution(),
                    bodyAtPredicate.stringAfterSubstitution("?my_loc"),
                    otherBody.bodyAtPredicate.stringAfterSubstitution("?other_loc"),
                    opponentVisiblePredicate.stringAfterSubstitution(supressedOpponent.getNameForPDDL(), "?other_loc"),
                    vantagePointPredicate.stringAfterSubstitution("?target", "?my_loc"),
                    PDDLOperators.makeNot(uncoveredByOpponentPredicate.stringAfterSubstitution(otherOpponent, "?my_loc")),
                    PDDLOperators.makeNot(uncoveredByOpponentPredicate.stringAfterSubstitution(otherOpponent, "?other_loc"))                   
                    );
            action.setPositiveEffects(
                    winPredicate.stringAfterSubstitution()
                    );
            action.setNegativeEffects(otherBody.canSupressPredicate.stringAfterSubstitution());
            return action;
        } 

        private PDDLSimpleAction createMoveWithSupportAction(OneBodyPDDL otherBody, PDDLObjectInstance supressedOpponent, PDDLObjectInstance otherOpponent){
            PDDLSimpleAction action = new PDDLSimpleAction(bodyPrefix + "move_with_support_against_" + supressedOpponent.getName(), new PDDLParameter("from", navPointType), new PDDLParameter("to", navPointType), new PDDLParameter("other_loc", navPointType));
            action.setPreconditionList(
                    otherBody.canSupressPredicate.stringAfterSubstitution(),
                    bodyAtPredicate.stringAfterSubstitution("?from"),
                    otherBody.bodyAtPredicate.stringAfterSubstitution("?other_loc"),
                    opponentVisiblePredicate.stringAfterSubstitution(supressedOpponent.getNameForPDDL(), "?other_loc"),
                    adjacentPredicate.stringAfterSubstitution("?from", "?to"),
                    PDDLOperators.makeNot(uncoveredByOpponentPredicate.stringAfterSubstitution(otherOpponent, "?from")),
                    PDDLOperators.makeNot(uncoveredByOpponentPredicate.stringAfterSubstitution(otherOpponent, "?other_loc"))                   
                    );
            action.setNegativeEffects(otherBody.canSupressPredicate.stringAfterSubstitution());
            
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
