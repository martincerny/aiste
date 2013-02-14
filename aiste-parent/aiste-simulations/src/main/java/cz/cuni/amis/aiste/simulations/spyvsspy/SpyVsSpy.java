/*
 * Copyright (C) 2012 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
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
import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IPDDLRepresentableEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractStateVariableRepresentableSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.planning4j.ActionDescription;
import cz.cuni.amis.planning4j.pddl.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpy extends AbstractStateVariableRepresentableSynchronizedEnvironment<SpyVsSpyAgentBody, SpyVsSpyAction>
        implements IPDDLRepresentableEnvironment<SpyVsSpyAgentBody, SpyVsSpyAction> {

    private final Logger logger = Logger.getLogger(SpyVsSpy.class);

    private int maxPlayers;

    /**
     * Number of trap types
     */
    private final int numTrapTypes;

    /**
     * Number of traps of individual types that can be set by an individual
     * agent during one game
     */
    private final int[] trapCounts;

    /**
     * Number of item types. A single instance of every item is required to
     * complete a level.
     */
    private final int numItemTypes;

    /**
     * Reward for dying - usually negative
     */
    private final double rewardDeath;

    /**
     * Reward for reaching the goal first
     */
    private final double rewardReachGoal;

    /**
     * Reward for every step where nothing special happens - usually negative to
     * force agent to escape quickly
     */
    private final double rewardNothing;

    /**
     * The probability a single attack action kills the oponnent
     */
    private final double attackSuccessProbability;

    /**
     * All nodes present in the environment
     */
    private List<MapNode> nodes;

    /**
     * The index of the node, everybody is trying to reach
     */
    private int destination;

    /**
     * The list of starting locations for each agent (should have the same size
     * as {@link #maxPlayers}
     */
    private List<Integer> startingLocations;

    /**
     * Neighbour lists. The values are indices to @{link #nodes}
     */
    private Map<Integer, List<Integer>> neighbours;

    Random rand;

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

    public SpyVsSpy(List<MapNode> nodes, int maxPlayers, List<Integer> startingLocations, Map<Integer, List<Integer>> neighbours, int numTrapTypes, int[]trapCounts, int numItemTypes, int destination) {
        super(SpyVsSpyAgentBody.class, SpyVsSpyAction.class);

        this.nodes = nodes;
        this.maxPlayers = maxPlayers;
        this.startingLocations = startingLocations;
        this.neighbours = neighbours;
        this.numTrapTypes = numTrapTypes;
        this.trapCounts = trapCounts;
        this.numItemTypes = numItemTypes;
        this.destination = destination;
        
        rewardDeath = -50;
        rewardNothing = -1;
        rewardReachGoal = 150;

        attackSuccessProbability = 0.3;

        rand = new Random();

        /*
         * Declare global PDDL objects
         */

        interactiveObjectType = new PDDLType("interactiveObject");
        itemType = new PDDLType("item", interactiveObjectType);
        trapType = new PDDLType("trap", interactiveObjectType);
        trapRemoverType = new PDDLType("trapRemover", interactiveObjectType);


        locationType = new PDDLType("location");

        locationConstants = new PDDLObjectInstance[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
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

        itemConstants = new PDDLObjectInstance[numItemTypes];
        for (int i = 0; i < numItemTypes; i++) {
            itemConstants[i] = new PDDLObjectInstance(ITEM_PREFIX + SEPARATOR + i, itemType);
        }
    }

    @Override
    public PDDLDomain getDomain(SpyVsSpyAgentBody body) {
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
    public PDDLProblem getProblem(SpyVsSpyAgentBody body) {
        PDDLProblem problem = new PDDLProblem("SpyVsSpyProblem", "SpyVsSpy");
        for (int i = 0; i < nodes.size(); i++) {
            problem.addObject(locationConstants[i]);
        }
        for (int i = 0; i < numItemTypes; i++) {
            problem.addObject(itemConstants[i]);
        }

        List<List<PDDLObjectInstance>> trapInstances = new ArrayList<List<PDDLObjectInstance>>();         //first index is the trap type
        List<List<PDDLObjectInstance>> trapRemoverInstances = new ArrayList<List<PDDLObjectInstance>>();
        for (int i = 0; i < numTrapTypes; i++) {
            trapInstances.add(new ArrayList<PDDLObjectInstance>());
            trapRemoverInstances.add(new ArrayList<PDDLObjectInstance>());
        }

        List<String> initialLiterals = new ArrayList<String>();
        initialLiterals.add(playerAtPredicate.stringAfterSubstitution(locationConstants[body.locationIndex]));
        for (MapNode n : nodes) {
            PDDLObjectInstance nodeInstance = locationConstants[n.index];
            for (Integer neighbourIndex : neighbours.get(n.index)) {
                initialLiterals.add(adjacentPredicate.stringAfterSubstitution(nodeInstance, locationConstants[neighbourIndex]));
            }
            for (Integer item : n.items) {
                initialLiterals.add(objectAtPredicate.stringAfterSubstitution(itemConstants[item], nodeInstance));
            }
            for (Integer newTrapType : n.traps) {
                PDDLObjectInstance newTrapInstance = addTrap(trapInstances, newTrapType, problem);

                initialLiterals.add(trapSetPredicate.stringAfterSubstitution(newTrapInstance, nodeInstance));
            }

            for (int newTrapRemoverType = 0; newTrapRemoverType < numTrapTypes; newTrapRemoverType++) {
                for (int i = 0; i < n.numTrapRemovers[newTrapRemoverType]; i++) {
                    PDDLObjectInstance newTrapRemoverInstance = addTrapRemover(trapRemoverInstances, newTrapRemoverType, problem);

                    initialLiterals.add(objectAtPredicate.stringAfterSubstitution(newTrapRemoverInstance, nodeInstance));
                }
            }
        }

        for (int carriedItemType : body.itemsCarried) {
            initialLiterals.add(carryingObjectPredicate.stringAfterSubstitution(itemConstants[carriedItemType]));
        }

        for (int trapTypeIndex = 0; trapTypeIndex < numTrapTypes; trapTypeIndex++) {
            for (int i = 0; i < body.numTrapsCarried[trapTypeIndex]; i++) {
                PDDLObjectInstance newTrapInstance = addTrap(trapInstances, trapTypeIndex, problem);
                initialLiterals.add(carryingObjectPredicate.stringAfterSubstitution(newTrapInstance));
            }
            for (int i = 0; i < body.numTrapRemoversCarried[trapTypeIndex]; i++) {
                PDDLObjectInstance newTrapRemoverInstance = addTrapRemover(trapRemoverInstances, trapTypeIndex, problem);
                initialLiterals.add(carryingObjectPredicate.stringAfterSubstitution(newTrapRemoverInstance));
            }
        }

        /*
         * Generate removesTrap predicat
         */
        for (int trapTypeIndex = 0; trapTypeIndex < numTrapTypes; trapTypeIndex++) {
            for (PDDLObjectInstance trapRemover : trapRemoverInstances.get(trapTypeIndex)) {
                for (PDDLObjectInstance trap : trapInstances.get(trapTypeIndex)) {
                    initialLiterals.add(removesTrapPredicate.stringAfterSubstitution(trapRemover, trap));
                }
            }
        }

        problem.setInitialLiterals(initialLiterals);

        List<String> goalConditions = new ArrayList<String>();
        goalConditions.add(playerAtPredicate.stringAfterSubstitution(locationConstants[destination]));
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
    public boolean checkPlanValidity(List<? extends SpyVsSpyAction> plan) {
        return true;
    }

    /**
     * Kills an agent - i.e. moves it to a starting another location, drops all
     * of its items, assigns a reward and clears its action for this round.
     *
     * @param killedAgent
     * @param actionsToPerform
     */
    protected void killAgent(SpyVsSpyAgentBody killedAgent, Map<SpyVsSpyAgentBody, SpyVsSpyAction> actionsToPerform, Map<SpyVsSpyAgentBody, Double> reward) {
        //drop all items
        MapNode currentNode = nodes.get(killedAgent.locationIndex);
        for (int itemIndex : killedAgent.itemsCarried) {
            currentNode.items.add(itemIndex);
        }
        killedAgent.itemsCarried.clear();
        for (int trapRemoverIndex : killedAgent.numTrapRemoversCarried) {
            currentNode.numTrapRemovers[trapRemoverIndex] += killedAgent.numTrapRemoversCarried[trapRemoverIndex];
            killedAgent.numTrapRemoversCarried[trapRemoverIndex] = 0;
        }
        //traps are not dropped, they are available, until they are all used

        //set reward, clear action
        reward.put(killedAgent, rewardDeath);
        actionsToPerform.put(killedAgent, SpyVsSpyAction.NO_OP_ACTION);

        //move to a random location
        int randomStartLocation = rand.nextInt(startingLocations.size());
        if (startingLocations.size() > 1) {
            while (randomStartLocation == killedAgent.locationIndex) {
                randomStartLocation = rand.nextInt(startingLocations.size());
            }
        }

        killedAgent.locationIndex = randomStartLocation;
        if (logger.isDebugEnabled()) {
            logger.debug("Agent killed, spawned at location: " + randomStartLocation + " body: " + killedAgent);
        }

    }

    /**
     * Checks whether agent has triggered a trap. Called from behaviours that
     * trigger traps (item pickups). If a trap was set, the agent si killed
     *
     * @param agent
     * @param actionsToPerform
     * @param reward
     * @return true, if the agent survived, false otherwise
     */
    protected boolean checkTrapSet(SpyVsSpyAgentBody agent, Map<SpyVsSpyAgentBody, SpyVsSpyAction> actionsToPerform, Map<SpyVsSpyAgentBody, Double> reward) {
        MapNode location = nodes.get(agent.locationIndex);
        if (!location.traps.isEmpty()) {
            killAgent(agent, actionsToPerform, reward);
            //remove a trap (due to hashing this is reasonably random)
            location.traps.remove(location.traps.iterator().next());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected Map<SpyVsSpyAgentBody, Double> simulateOneStepInternal(Map<SpyVsSpyAgentBody, SpyVsSpyAction> actionsToPerform) {
        Map<SpyVsSpyAgentBody, Double> reward = new HashMap<SpyVsSpyAgentBody, Double>();

        if (logger.isDebugEnabled()) {
            logger.debug("============ Map State: ================");
            for (MapNode node : nodes) {
                logger.debug(node);
            }
            logger.debug("=========== MapState End ===============");
        }

        //evaluate attack actions
        for (SpyVsSpyAgentBody body : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(body);
            if (action.getType() == SpyVsSpyAction.ActionType.ATTACK_AGENT) {
                if (action.getActionTarget() > getAllBodies().size()
                        || body.locationIndex != getAllBodies().get(action.getActionTarget()).locationIndex) {
                    //I am attacking an invalid agent or agent at different location
                    logger.info("Invalid action: " + action.getLoggableRepresentation() + " from: " + body);
                    continue;
                }
                if (rand.nextDouble() < attackSuccessProbability) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Succesful attack: " + action.getLoggableRepresentation() + " from: " + body);
                    }
                    killAgent(getAllBodies().get(action.getActionTarget()), actionsToPerform, reward);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unsuccesful attack: " + action.getLoggableRepresentation() + " from: " + body);
                    }
                }
            }
        }

        //evaluate remove trap actions
        for (SpyVsSpyAgentBody body : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(body);
            if (action.getType() == SpyVsSpyAction.ActionType.REMOVE_TRAP) {
                int targetTrap = action.getActionTarget();
                MapNode location = nodes.get(body.locationIndex);
                if (targetTrap < numTrapTypes
                        && body.numTrapRemoversCarried[targetTrap] > 0
                        && location.traps.contains(targetTrap)) {
                    location.traps.remove(targetTrap);
                    body.numTrapRemoversCarried[targetTrap]--;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + body);
                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + body);
                }

            }
        }

        //evaluate set trap actions
        for (SpyVsSpyAgentBody body : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(body);
            if (action.getType() == SpyVsSpyAction.ActionType.SET_TRAP) {
                int targetTrap = action.getActionTarget();
                MapNode location = nodes.get(body.locationIndex);
                if (targetTrap < numTrapTypes
                        && body.numTrapsCarried[targetTrap] > 0) {
                    location.traps.add(targetTrap);
                    body.numTrapsCarried[targetTrap]--;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + body);
                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + body);
                }

            }
        }

        //evaluate pickup trap remover actions
        for (SpyVsSpyAgentBody body : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(body);
            if (action.getType() == SpyVsSpyAction.ActionType.PICKUP_TRAP_REMOVER) {
                int targetTrap = action.getActionTarget();
                MapNode location = nodes.get(body.locationIndex);
                if (targetTrap < numTrapTypes && location.numTrapRemovers[targetTrap] > 0) {
                    if (checkTrapSet(body, actionsToPerform, reward)) {
                        body.numTrapRemoversCarried[targetTrap]++;
                        location.numTrapRemovers[targetTrap]--;
                        if (logger.isDebugEnabled()) {
                            logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + body);
                        }
                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + body);
                }

            }
        }

        //evaluate pickup item actions
        for (SpyVsSpyAgentBody body : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(body);
            if (action.getType() == SpyVsSpyAction.ActionType.PICKUP_ITEM) {
                int targetItem = action.getActionTarget();
                MapNode location = nodes.get(body.locationIndex);
                if (targetItem < numItemTypes
                        && location.items.contains(targetItem)) {
                    if (checkTrapSet(body, actionsToPerform, reward)) {
                        body.itemsCarried.add(targetItem);
                        location.items.remove(targetItem);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + body);
                        }
                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + body);
                }
            }
        }

        //evaluate move actions
        for (SpyVsSpyAgentBody body : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(body);
            if (action.getType() == SpyVsSpyAction.ActionType.MOVE) {
                if (neighbours.get(body.locationIndex).contains(action.getActionTarget())) {
                    body.locationIndex = action.getActionTarget();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + body);
                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + body);
                }

            }
        }

        //check for goal conditions
        for (SpyVsSpyAgentBody body : actionsToPerform.keySet()) {
            if (body.locationIndex == destination && body.itemsCarried.size() == numItemTypes) {
                //agent has reached the destination with all neccessary items, let's finish this
                reward.put(body, rewardReachGoal);
                this.setFinished(true);
                //TODO split reward if two agents reach goal simultaneously
                if (logger.isDebugEnabled()) {
                    logger.debug("Agent reached goal: " + body);
                }
            }
        }



        for (SpyVsSpyAgentBody body : this.getAllBodies()) {
            if (reward.get(body) == null) {
                //set nothing reward, if it was not set previously (for example when the agent was killed)
                reward.put(body, rewardNothing);
            }
        }
        return reward;
    }

    @Override
    protected SpyVsSpyAgentBody createAgentBodyInternal(IAgentType type) {
        if (type != SpyVsSpyAgentType.getInstance()) {
            throw new AgentInstantiationException("Illegal agent type");
        }
        int nextAgentIndex = this.getAllBodies().size();

        int startingLocation = startingLocations.get(nextAgentIndex);

        logger.info("Registered agent at starting position " + startingLocation);
        return new SpyVsSpyAgentBody(nextAgentIndex, startingLocation, numTrapTypes, trapCounts);
    }

    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        return Collections.singletonMap(SpyVsSpyAgentType.getInstance(), new AgentInstantiationDescriptor(1, maxPlayers));
    }

    public static class MapNode {

        /**
         * The indices of traps set at the given location
         */
        private Set<Integer> traps;

        /**
         * The indices of items that can be pickud up at given location
         */
        private Set<Integer> items;

        /**
         * The indices of trap removers that can be picked up at given location
         */
        private int[] numTrapRemovers;

        /**
         * The unique identifier of this node
         */
        private int index;

        public MapNode(int index, Set<Integer> traps, Set<Integer> items, Set<Integer> trapRemovers, int numTraps) {
            this.index = index;
            this.traps = new HashSet<Integer>(traps);
            this.items = new HashSet<Integer>(items);
            numTrapRemovers = new int[numTraps];
            for (int trapRemoverIndex : trapRemovers) {
                numTrapRemovers[trapRemoverIndex]++;
            }
        }

        public MapNode(int index, int numTraps) {
            this.index = index;
            this.traps = new HashSet<Integer>();
            this.items = new HashSet<Integer>();
            numTrapRemovers = new int[numTraps];
        }

        public int getIndex() {
            return index;
        }

        public Set<Integer> getItems() {
            return items;
        }

        public int[] getNumTrapRemovers() {
            return numTrapRemovers;
        }

        public Set<Integer> getTraps() {
            return traps;
        }
                

        @Override
        public String toString() {
            return "MapNode index: " + index + ", traps:" + traps + ", items: " + items + ", trapRemovers: " + Arrays.toString(numTrapRemovers);
        }
    }
}
