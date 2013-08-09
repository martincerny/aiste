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
    
    PDDLType oponentType;
    PDDLObjectInstance oponentInstances[];
    PDDLPredicate uncoveredByOponentPredicate;
    PDDLPredicate visibleByOponentPredicate;
    PDDLPredicate vantagePointPredicate;
    
    PDDLPredicate oponentAtPredicate;

    public CGPDDLRepresentation(CoverGame env) {
        super(env);

        navPointType = new PDDLType("nav_point");
        oponentType = new PDDLType("oponent");

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

        oponentAtPredicate = new PDDLPredicate("oponent_at",new PDDLParameter("o", oponentType),  new PDDLParameter("loc", navPointType));
        uncoveredByOponentPredicate = new PDDLPredicate("uncovered_by_oponent", new PDDLParameter("o", oponentType), new PDDLParameter("loc", navPointType));            
        visibleByOponentPredicate = new PDDLPredicate("visible_by_oponent", new PDDLParameter("o", oponentType), new PDDLParameter("loc", navPointType));            
        vantagePointPredicate = new PDDLPredicate("vantage_point", new PDDLParameter("o", oponentType), new PDDLParameter("loc", navPointType));            
        
        
        bodyPDDLs = new OneBodyPDDL[]{new OneBodyPDDL(0, env), new OneBodyPDDL(1, env)};
        oponentInstances = new PDDLObjectInstance[] { new PDDLObjectInstance("oponent_1", oponentType), new PDDLObjectInstance("oponent_2", oponentType)};

    }

    @Override
    public PDDLDomain getDomain(AgentBody body) {
        PDDLDomain domain = new PDDLDomain("cover_game", EnumSet.of(PDDLRequirement.TYPING));
        domain.addType(navPointType);
        domain.addType(oponentType);
        domain.addPredicate(adjacentPredicate);
        domain.addPredicate(body_0_turn);
        domain.addPredicate(uncoveredByOponentPredicate);
        domain.addPredicate(oponentAtPredicate);
        domain.addFunction(new PDDLFunction("total-cost"));

        for (OneBodyPDDL pddl : bodyPDDLs) {
            for (PDDLSimpleAction action : pddl.bodyActions) {
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

        int ids[] = env.getOponentIds(body.getId());
        OpponentData oponentData[] = env.getOpponentTeamData(body.getId()).opponentData;

        // body - related state
        for (int i = 0; i < 2; i++) {
            initialState.add(bodyPDDLs[i].bodyAtPredicate.stringAfterSubstitution(navPointInstances.get(bodyPair.getBodyInfo(i).loc)));
            CoverGame.CGBodyInfo bodyInfo = bodyPair.getBodyInfo(i);
            for(int op = 0; op < 2; op++){                
                CoverGame.CGBodyInfo oponentInfo = env.bodyInfos.get(ids[op]);
                if(!env.isVisible(oponentInfo.loc, bodyInfo.loc) || env.isCovered(oponentInfo.loc, bodyInfo.loc)){
                    initialState.add(bodyPDDLs[i].partialCoverPredicate.stringAfterSubstitution());
                }
            }
        }

        initialState.add(body_0_turn.stringAfterSubstitution());

        /* Oponent - related state*/
        for(int i = 0; i < 2; i++){
            problem.addObject(oponentInstances[i]);
            for(Loc uncoveredLoc : oponentData[i].uncoveredNavpoints){
                initialState.add(uncoveredByOponentPredicate.stringAfterSubstitution(oponentInstances[i], navPointInstances.get(uncoveredLoc)));
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
            default: {
                throw new UnsupportedOperationException("Unrecognized goal: " + goal.getType());

            }

        }
        problem.setGoalConditions(goalConditions);
        return problem;
    }

    @Override
    public IReactivePlan<? extends CGPairAction> translateAction(Queue<ActionDescription> actionsFromPlanner, AgentBody body) {

        //I believe that actions are alternating (they definitely should be :-)
        CGAction action0 = translateSingleAction(0, actionsFromPlanner.poll());
        CGAction action1 = CGAction.NO_OP_ACTION;
        if (!actionsFromPlanner.isEmpty()) {
            action1 = translateSingleAction(1, actionsFromPlanner.poll());
        }
        CGPairAction pairAction = new CGPairAction(action0, action1);
        return new SequencePlan<CGPairAction>(pairAction);

    }

    protected CGAction translateSingleAction(int bodyId, ActionDescription desc) {
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

    class OneBodyPDDL {

        PDDLPredicate bodyAtPredicate;
        PDDLPredicate partialCoverPredicate;
        PDDLPredicate fullCoverPredicate;
        
        PDDLSimpleAction moveAction;
        PDDLSimpleAction noOpAction;
        List<PDDLSimpleAction> bodyActions = new ArrayList<PDDLSimpleAction>();
        List<PDDLPredicate> bodyPredicates = new ArrayList<PDDLPredicate>();
        String bodyPrefix;

        public OneBodyPDDL(int bodyId, CoverGame cg) {
            bodyPrefix = "body_" + bodyId + "_";
            
            String uncoveredCostString = Double.toString(1d / cg.defs.partialCoverAimPenalty);


            bodyAtPredicate = new PDDLPredicate(bodyPrefix + "at", new PDDLParameter("loc", navPointType));            
            bodyPredicates.add(bodyAtPredicate);
            
            partialCoverPredicate = new PDDLPredicate(bodyPrefix + "partial_cover");
            bodyPredicates.add(partialCoverPredicate);

            fullCoverPredicate = new PDDLPredicate(bodyPrefix + "full_cover");
            bodyPredicates.add(fullCoverPredicate);

            moveAction = new PDDLSimpleAction(bodyPrefix + "move", new PDDLParameter("from", navPointType), new PDDLParameter("to", navPointType));
            moveAction.setPreconditionList(
                    bodyAtPredicate.stringAfterSubstitution("?from"),
                    adjacentPredicate.stringAfterSubstitution("?from", "?to"));
            String coveredCondition = "forall (?o - " + oponentType.getTypeName() + ") (not (" + uncoveredByOponentPredicate.stringAfterSubstitution("?o", "?to") + "))";
            moveAction.setPositiveEffects(bodyAtPredicate.stringAfterSubstitution("?to"),
                    "increase (total-cost) 1",
                    "when (" + coveredCondition + ") (" + partialCoverPredicate.stringAfterSubstitution() + ")",
                    "when (not (" + coveredCondition + ")) (and (not (" + partialCoverPredicate.stringAfterSubstitution() + ")) (increase (total-cost) "+ uncoveredCostString + "))"
                    );            
            moveAction.setNegativeEffects(bodyAtPredicate.stringAfterSubstitution("?from"), fullCoverPredicate.stringAfterSubstitution());
            bodyActions.add(moveAction);

            noOpAction = new PDDLSimpleAction(bodyPrefix + "noop", new PDDLParameter("to", navPointType));
            noOpAction.setPreconditionList(bodyAtPredicate.stringAfterSubstitution("?to"));
            noOpAction.addPositiveEffect("when (not (" + coveredCondition + ") ) (increase (total-cost) "+ uncoveredCostString + ")");            
            bodyActions.add(noOpAction);

            for (PDDLSimpleAction action : bodyActions) {
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
    
}
