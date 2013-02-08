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
import cz.cuni.amis.aiste.environment.IAgentBody;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IPDDLRepresentableEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractStateVariableRepresentableSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.planning4j.ActionDescription;
import cz.cuni.amis.planning4j.pddl.PDDLAction;
import cz.cuni.amis.planning4j.pddl.PDDLConstant;
import cz.cuni.amis.planning4j.pddl.PDDLDomain;
import cz.cuni.amis.planning4j.pddl.PDDLOperators;
import cz.cuni.amis.planning4j.pddl.PDDLParameter;
import cz.cuni.amis.planning4j.pddl.PDDLPredicate;
import cz.cuni.amis.planning4j.pddl.PDDLProblem;
import cz.cuni.amis.planning4j.pddl.PDDLRequirement;
import cz.cuni.amis.planning4j.pddl.PDDLSimpleAction;
import cz.cuni.amis.planning4j.pddl.PDDLType;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpy extends AbstractStateVariableRepresentableSynchronizedEnvironment<SpyVsSpyAgentBody, SpyVsSpyAction> 
    implements IPDDLRepresentableEnvironment<SpyVsSpyAgentBody, SpyVsSpyAction>
{

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
    
    PDDLType itemType;
    PDDLType locationType;
    PDDLType trapTypes;
    PDDLType trapRemoverTypes;
    
    PDDLConstant[] itemConstants;
    PDDLConstant[] locationConstants;
    PDDLConstant[] trapConstants;
    PDDLConstant[] trapRemoverConstants;

    PDDLPredicate atPredicate;
    protected PDDLPredicate adjacentPredicate;
    PDDLSimpleAction moveAction;
        
    public static final String LOCATION_PREFIX = "location";
    public static final String SEPARATOR = "_";

    public SpyVsSpy() {
        super(SpyVsSpyAgentBody.class, SpyVsSpyAction.class);
        numTrapTypes = 2;
        trapCounts = new int[]{1, 1};
        numItemTypes = 2;

        destination = 3;

        maxPlayers = 2;
        
        nodes = new ArrayList<MapNode>();
        //id                     traps                   items               trap removers
        nodes.add(new MapNode(0, Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET, numTrapTypes));
        nodes.add(new MapNode(1, Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET, numTrapTypes));
        nodes.add(new MapNode(2, Collections.EMPTY_SET, Collections.singleton(0), Collections.singleton(1), numTrapTypes));
        nodes.add(new MapNode(3, Collections.EMPTY_SET, Collections.singleton(1), Collections.singleton(0), numTrapTypes));

        rewardDeath = -50;
        rewardNothing = -1;
        rewardReachGoal = 150;
        startingLocations = Arrays.asList(new Integer[]{0, 1});

        neighbours = new HashMap<Integer, List<Integer>>();
        neighbours.put(0, Arrays.asList(new Integer[]{1, 2, 3}));
        neighbours.put(1, Arrays.asList(new Integer[]{0, 2}));
        neighbours.put(2, Arrays.asList(new Integer[]{0, 1}));
        neighbours.put(3, Arrays.asList(new Integer[]{0}));

        attackSuccessProbability = 0.3;

        rand = new Random();
        
        itemType = new PDDLType("item");
        locationType = new PDDLType("location");
        
        locationConstants = new PDDLConstant[nodes.size()];
        for(int i = 0; i < nodes.size(); i++){
            locationConstants[i] = new PDDLConstant(LOCATION_PREFIX + SEPARATOR  + i, locationType);
        }
        
        atPredicate = new PDDLPredicate("at", new PDDLParameter("loc", locationType));
        adjacentPredicate = new PDDLPredicate("adjacent", new PDDLParameter("loc1", locationType), new PDDLParameter("loc2", locationType));
        moveAction = new PDDLSimpleAction("move", new PDDLParameter("from", locationType), new PDDLParameter("to", locationType));
        moveAction.setPreconditionList(
                atPredicate.stringAfterSubstitution("?from"),
                adjacentPredicate.stringAfterSubstitution("?from", "?to"));
        moveAction.setPositiveEffects(atPredicate.stringAfterSubstitution("?to"));
        moveAction.setNegativeEffects(atPredicate.stringAfterSubstitution("?from"));
        
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
                if (targetTrap < numTrapTypes
                        && checkTrapSet(body, actionsToPerform, reward)
                        && location.numTrapRemovers[targetTrap] > 0) {
                    body.numTrapRemoversCarried[targetTrap]++;
                    location.numTrapRemovers[targetTrap]--;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + body);
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
                        && checkTrapSet(body, actionsToPerform, reward)
                        && location.items.contains(targetItem)) {
                    body.itemsCarried.add(targetItem);
                    location.items.remove(targetItem);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + body);
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

    @Override
    public PDDLDomain getDomain(SpyVsSpyAgentBody body) {
        PDDLDomain domain = new PDDLDomain("SpyVsSpy", EnumSet.of(PDDLRequirement.TYPING, PDDLRequirement.STRIPS));
        domain.addType(itemType);
        domain.addType(locationType);
        for(int i = 0; i < nodes.size(); i++){
            domain.addConstant(locationConstants[i]);
        }
        domain.addPredicate(atPredicate);        
        domain.addPredicate(adjacentPredicate);
        
        
        
        domain.addAction(moveAction);
        return domain;
    }

    @Override
    public PDDLProblem getProblem(SpyVsSpyAgentBody body) {
        PDDLProblem problem = new PDDLProblem("problem", "SpyVsSpy");
        List<String> initialLiterals = new ArrayList<String>();
        initialLiterals.add(atPredicate.stringAfterSubstitution(locationConstants[body.locationIndex].getName()));
        for(MapNode n : nodes){
            for(Integer neighbourIndex : neighbours.get(n.index)){
                initialLiterals.add(adjacentPredicate.stringAfterSubstitution(locationConstants[n.index].getName(), locationConstants[neighbourIndex].getName()));
            }
        }
        problem.setInitialLiterals(initialLiterals);
        problem.setGoalCondition(atPredicate.stringAfterSubstitution(locationConstants[destination].getName()));
        return problem;
    }

    @Override
    public List<? extends SpyVsSpyAction> convertPlanToActions(List<ActionDescription> planFromPlanner) {
        List<SpyVsSpyAction> actions = new ArrayList<SpyVsSpyAction>(planFromPlanner.size());
        for(ActionDescription desc : planFromPlanner){
            if(desc.getName().equalsIgnoreCase(moveAction.getName())){
                int targetLocation = Integer.parseInt(desc.getParameters().get(1).substring(LOCATION_PREFIX.length() + SEPARATOR.length()));
                actions.add(new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, targetLocation));
            } else {
                throw new AisteException("Unrecognized action name: " + desc.getName());
            }
        }
        return actions;
    }

    @Override
    public boolean checkPlanValidity(List<? extends SpyVsSpyAction> plan) {
        return true;
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

        @Override
        public String toString() {
            return "MapNode index: " + index + ", traps:" + traps + ", items: " + items + ", trapRemovers: " + Arrays.toString(numTrapRemovers);
        }
    }
}
