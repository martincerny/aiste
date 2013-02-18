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

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IActionFailureRepresentation;
import cz.cuni.amis.aiste.environment.ISimulablePDDLRepresentation;
import cz.cuni.amis.planning4j.ActionDescription;
import cz.cuni.amis.planning4j.pddl.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyPDDLRepresentation 
    implements ISimulablePDDLRepresentation<SpyVsSpyAction, SpyVsSpy>, IActionFailureRepresentation {
    
    
    private SpyVsSpy environment;
    
    /*
     * PDDL generating stuf
     */
    PDDLType interactiveObjectType;
    PDDLType itemType;
    PDDLType locationType;
    PDDLType trapType;
    PDDLType trapRemoverType;
    PDDLType oponentType;
    
    PDDLObjectInstance[] itemConstants;
    PDDLObjectInstance[] locationConstants;
    
    PDDLPredicate playerAtPredicate;
    PDDLPredicate adjacentPredicate;
    PDDLPredicate objectAtPredicate;
    PDDLPredicate oponentAtPredicate;    
    PDDLPredicate carryingObjectPredicate;
    PDDLPredicate oponentCarryingObjectPredicate;
    PDDLPredicate trapSetPredicate;
    PDDLPredicate removesTrapPredicate;
    PDDLPredicate attackedOponent;
    PDDLPredicate metOponent;

    PDDLSimpleAction moveAction;
    PDDLSimpleAction takeObjectAction;
    PDDLSimpleAction removeTrapAction;
    PDDLSimpleAction setTrapAction;
    PDDLSimpleAction attackAction;

    public static final String LOCATION_PREFIX = "location";
    public static final String ITEM_PREFIX = "item";
    public static final String TRAP_PREFIX = "trap";
    public static final String REMOVER_PREFIX = "remover";
    public static final String SEPARATOR = "_";

    public SpyVsSpyPDDLRepresentation(SpyVsSpy environment) {
        this.environment = environment;
        
        /*
         * Declare global PDDL objects
         */

        interactiveObjectType = new PDDLType("interactiveObject");
        itemType = new PDDLType("item", interactiveObjectType);
        trapType = new PDDLType("trap", interactiveObjectType);
        trapRemoverType = new PDDLType("trapRemover", interactiveObjectType);


        locationType = new PDDLType("location");

        locationConstants = new PDDLObjectInstance[environment.nodes.size()];
        for (int i = 0; i < environment.nodes.size(); i++) {
            locationConstants[i] = new PDDLObjectInstance(LOCATION_PREFIX + SEPARATOR + i, locationType);
        }

        playerAtPredicate = new PDDLPredicate("playerAt", new PDDLParameter("loc", locationType));
        adjacentPredicate = new PDDLPredicate("adjacent", new PDDLParameter("loc1", locationType), new PDDLParameter("loc2", locationType));
        objectAtPredicate = new PDDLPredicate("objectAt", new PDDLParameter("obj", interactiveObjectType), new PDDLParameter("loc", locationType));
        carryingObjectPredicate = new PDDLPredicate("carrying", new PDDLParameter("obj", interactiveObjectType));
        trapSetPredicate = new PDDLPredicate("trapSet", new PDDLParameter("trap", trapType), new PDDLParameter("loc", locationType));
        removesTrapPredicate = new PDDLPredicate("removesTrap", new PDDLParameter("remover", trapRemoverType), new PDDLParameter("trap", trapType));

        moveAction = new PDDLSimpleAction("move", new PDDLParameter("from", locationType), new PDDLParameter("to", locationType));
        moveAction.setPreconditionList(
                playerAtPredicate.stringAfterSubstitution("?from"),
                adjacentPredicate.stringAfterSubstitution("?from", "?to"));
        moveAction.setPositiveEffects(playerAtPredicate.stringAfterSubstitution("?to"));
        moveAction.setNegativeEffects(playerAtPredicate.stringAfterSubstitution("?from"));

        takeObjectAction = new PDDLSimpleAction("take", new PDDLParameter("obj", interactiveObjectType), new PDDLParameter("loc", locationType));
        takeObjectAction.setPreconditionList(
                playerAtPredicate.stringAfterSubstitution("?loc"),
                objectAtPredicate.stringAfterSubstitution("?obj", "?loc"),
                "not (exists (?t - " + trapType.getTypeName() + ") (" + trapSetPredicate.stringAfterSubstitution("?t", "?loc") + ") )");
        takeObjectAction.setPositiveEffects(carryingObjectPredicate.stringAfterSubstitution("?obj"));
        takeObjectAction.setNegativeEffects(objectAtPredicate.stringAfterSubstitution("?obj", "?loc"));

        removeTrapAction = new PDDLSimpleAction("removeTrap", new PDDLParameter("remover", trapRemoverType), new PDDLParameter("trap", trapType), new PDDLParameter("loc", locationType));
        removeTrapAction.setPreconditionList(
                playerAtPredicate.stringAfterSubstitution("?loc"),
                trapSetPredicate.stringAfterSubstitution("?trap", "?loc"),
                carryingObjectPredicate.stringAfterSubstitution("?remover"),
                removesTrapPredicate.stringAfterSubstitution("?remover", "?trap"));
        removeTrapAction.setNegativeEffects(
                trapSetPredicate.stringAfterSubstitution("?trap", "?loc"),
                carryingObjectPredicate.stringAfterSubstitution("?remover"));

        setTrapAction = new PDDLSimpleAction("setTrap", new PDDLParameter("trap", trapType), new PDDLParameter("loc", locationType));
        setTrapAction.setPreconditionList(
                playerAtPredicate.stringAfterSubstitution("?loc"),
                carryingObjectPredicate.stringAfterSubstitution("?trap"));
        setTrapAction.setPositiveEffects(objectAtPredicate.stringAfterSubstitution("?trap", "?loc"));
        setTrapAction.setNegativeEffects(carryingObjectPredicate.stringAfterSubstitution("?trap"));

        itemConstants = new PDDLObjectInstance[environment.defs.numItemTypes];
        for (int i = 0; i < environment.defs.numItemTypes; i++) {
            itemConstants[i] = new PDDLObjectInstance(ITEM_PREFIX + SEPARATOR + i, itemType);
        }
        
    }
    
    
    
    @Override
    public PDDLDomain getDomain(AgentBody body) {
        PDDLDomain domain = new PDDLDomain("SpyVsSpy", EnumSet.of(PDDLRequirement.TYPING, PDDLRequirement.STRIPS));
        domain.addType(locationType);
        domain.addType(interactiveObjectType);
        domain.addType(itemType);
        domain.addType(trapType);
        domain.addType(trapRemoverType);

        domain.addPredicate(playerAtPredicate);
        domain.addPredicate(adjacentPredicate);
        domain.addPredicate(trapSetPredicate);
        domain.addPredicate(objectAtPredicate);
        domain.addPredicate(carryingObjectPredicate);
        domain.addPredicate(removesTrapPredicate);


        domain.addAction(moveAction);
        domain.addAction(takeObjectAction);
        domain.addAction(setTrapAction);
        domain.addAction(removeTrapAction);
        return domain;
    }

    @Override
    public PDDLProblem getProblem(AgentBody body) {
        PDDLProblem problem = new PDDLProblem("SpyVsSpyProblem", "SpyVsSpy");
        for (int i = 0; i < environment.nodes.size(); i++) {
            problem.addObject(locationConstants[i]);
        }
        for (int i = 0; i < environment.defs.numItemTypes; i++) {
            problem.addObject(itemConstants[i]);
        }

        List<List<PDDLObjectInstance>> trapInstances = new ArrayList<List<PDDLObjectInstance>>();         //first index is the trap type
        List<List<PDDLObjectInstance>> trapRemoverInstances = new ArrayList<List<PDDLObjectInstance>>();
        for (int i = 0; i < environment.defs.numTrapTypes; i++) {
            trapInstances.add(new ArrayList<PDDLObjectInstance>());
            trapRemoverInstances.add(new ArrayList<PDDLObjectInstance>());
        }
        
        SpyVsSpyBodyInfo bodyInfo = environment.bodyInfos.get(body.getId());        

        List<String> initialLiterals = new ArrayList<String>();
        initialLiterals.add(playerAtPredicate.stringAfterSubstitution(locationConstants[bodyInfo.locationIndex]));
        for (SpyVsSpyMapNode n : environment.nodes) {
            PDDLObjectInstance nodeInstance = locationConstants[n.index];
            for (Integer neighbourIndex : environment.defs.neighbours.get(n.index)) {
                initialLiterals.add(adjacentPredicate.stringAfterSubstitution(nodeInstance, locationConstants[neighbourIndex]));
            }
            for (Integer item : n.items) {
                initialLiterals.add(objectAtPredicate.stringAfterSubstitution(itemConstants[item], nodeInstance));
            }
            for (Integer newTrapType : n.traps) {
                PDDLObjectInstance newTrapInstance = addTrap(trapInstances, newTrapType, problem);

                initialLiterals.add(trapSetPredicate.stringAfterSubstitution(newTrapInstance, nodeInstance));
            }

            for (int newTrapRemoverType = 0; newTrapRemoverType < environment.defs.numTrapTypes; newTrapRemoverType++) {
                for (int i = 0; i < n.numTrapRemovers[newTrapRemoverType]; i++) {
                    PDDLObjectInstance newTrapRemoverInstance = addTrapRemover(trapRemoverInstances, newTrapRemoverType, problem);

                    initialLiterals.add(objectAtPredicate.stringAfterSubstitution(newTrapRemoverInstance, nodeInstance));
                }
            }
        }

        for (int carriedItemType : bodyInfo.itemsCarried) {
            initialLiterals.add(carryingObjectPredicate.stringAfterSubstitution(itemConstants[carriedItemType]));
        }

        for (int trapTypeIndex = 0; trapTypeIndex < environment.defs.numTrapTypes; trapTypeIndex++) {
            for (int i = 0; i < bodyInfo.numTrapsCarried[trapTypeIndex]; i++) {
                PDDLObjectInstance newTrapInstance = addTrap(trapInstances, trapTypeIndex, problem);
                initialLiterals.add(carryingObjectPredicate.stringAfterSubstitution(newTrapInstance));
            }
            for (int i = 0; i < bodyInfo.numTrapRemoversCarried[trapTypeIndex]; i++) {
                PDDLObjectInstance newTrapRemoverInstance = addTrapRemover(trapRemoverInstances, trapTypeIndex, problem);
                initialLiterals.add(carryingObjectPredicate.stringAfterSubstitution(newTrapRemoverInstance));
            }
        }

        /*
         * Generate removesTrap predicat
         */
        for (int trapTypeIndex = 0; trapTypeIndex < environment.defs.numTrapTypes; trapTypeIndex++) {
            for (PDDLObjectInstance trapRemover : trapRemoverInstances.get(trapTypeIndex)) {
                for (PDDLObjectInstance trap : trapInstances.get(trapTypeIndex)) {
                    initialLiterals.add(removesTrapPredicate.stringAfterSubstitution(trapRemover, trap));
                }
            }
        }

        problem.setInitialLiterals(initialLiterals);

        List<String> goalConditions = new ArrayList<String>();
        goalConditions.add(playerAtPredicate.stringAfterSubstitution(locationConstants[environment.defs.destination]));
        for (PDDLObjectInstance item : itemConstants) {
            goalConditions.add(carryingObjectPredicate.stringAfterSubstitution(item));
        }

        problem.setGoalCondition(PDDLOperators.makeAnd(goalConditions));
        return problem;
    }

    protected PDDLObjectInstance addTrapRemover(List<List<PDDLObjectInstance>> trapRemoverInstances, int newTrapRemoverType, PDDLProblem problem) {
        int newRemoverIndex = trapRemoverInstances.get(newTrapRemoverType).size();
        String newTrapRemoverName = REMOVER_PREFIX + SEPARATOR + newTrapRemoverType + SEPARATOR + "instance" + SEPARATOR + newRemoverIndex;
        PDDLObjectInstance newTrapRemoverInstance = new PDDLObjectInstance(newTrapRemoverName, trapRemoverType);
        problem.addObject(newTrapRemoverInstance);
        trapRemoverInstances.get(newTrapRemoverType).add(newTrapRemoverInstance);
        return newTrapRemoverInstance;
    }

    protected PDDLObjectInstance addTrap(List<List<PDDLObjectInstance>> trapInstances, Integer newTrapType, PDDLProblem problem) {
        int newTrapIndex = trapInstances.get(newTrapType).size();
        String newTrapName = TRAP_PREFIX + SEPARATOR + newTrapType + SEPARATOR + "instance" + SEPARATOR + newTrapIndex;
        PDDLObjectInstance newTrapInstance = new PDDLObjectInstance(newTrapName, trapType);
        problem.addObject(newTrapInstance);
        trapInstances.get(newTrapType).add(newTrapInstance);
        return newTrapInstance;
    }

    protected int extractActionParameter(ActionDescription desc, int parameterIndex, String typePrefix) {
        String stringAfterPrefix = desc.getParameters().get(parameterIndex).substring(typePrefix.length() + SEPARATOR.length());
        return Integer.parseInt(stringAfterPrefix.split(SEPARATOR)[0]);
    }

    @Override
    public List<? extends SpyVsSpyAction> translateAction(ActionDescription actionFromPlanner) {
        List<SpyVsSpyAction> actions = new ArrayList<SpyVsSpyAction>(1);
        if (actionFromPlanner.getName().equalsIgnoreCase(moveAction.getName())) {
            int targetLocation = extractActionParameter(actionFromPlanner, 1, LOCATION_PREFIX);
            actions.add(new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, targetLocation));

        } else if (actionFromPlanner.getName().equalsIgnoreCase(takeObjectAction.getName())) {
            String objectParameter = actionFromPlanner.getParameters().get(0).toLowerCase();
            if (objectParameter.startsWith(REMOVER_PREFIX.toLowerCase())) {
                int targetRemover = extractActionParameter(actionFromPlanner, 0, REMOVER_PREFIX);
                actions.add(new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_TRAP_REMOVER, targetRemover));
            } else if (objectParameter.startsWith(ITEM_PREFIX.toLowerCase())) {
                int targetItem = extractActionParameter(actionFromPlanner, 0, ITEM_PREFIX);
                actions.add(new SpyVsSpyAction(SpyVsSpyAction.ActionType.PICKUP_ITEM, targetItem));
            } else {
                throw new AisteException("Unrecognized item to pickup: " + objectParameter);
            }

        } else if (actionFromPlanner.getName().equalsIgnoreCase(removeTrapAction.getName())) {
            int targetTrap = extractActionParameter(actionFromPlanner, 1, TRAP_PREFIX);
            actions.add(new SpyVsSpyAction(SpyVsSpyAction.ActionType.REMOVE_TRAP, targetTrap));

        } else if (actionFromPlanner.getName().equalsIgnoreCase(setTrapAction.getName())) {
            int targetTrap = extractActionParameter(actionFromPlanner, 0, TRAP_PREFIX);                
            actions.add(new SpyVsSpyAction(SpyVsSpyAction.ActionType.SET_TRAP, targetTrap));

        } else {
            throw new AisteException("Unrecognized action name: " + actionFromPlanner.getName());
        }
        return actions;
    }

    @Override
    public void setEnvironment(SpyVsSpy env) {
        if(env.defs != this.environment.defs){
            throw new IllegalArgumentException("Environment could only be set to a copy of the original env.");
        }
        this.environment = env;
    }

    @Override
    public boolean lastActionFailed(AgentBody body) {
        return environment.lastActionFailed(body);
    }

    @Override
    public boolean isGoalState(AgentBody body) {
        SpyVsSpyBodyInfo info = environment.bodyInfos.get(body.getId());
        
        if(info.locationIndex != environment.defs.destination){
            return false;
        }
        
        if(info.itemsCarried.size() != environment.defs.numItemTypes){
            return false;
        }
        
        return true;
    }
 
    
    
}
