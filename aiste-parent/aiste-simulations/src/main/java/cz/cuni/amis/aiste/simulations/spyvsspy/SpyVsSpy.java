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

import cz.cuni.amis.aiste.IRandomizable;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IEnvironmentRepresentation;
import cz.cuni.amis.aiste.environment.ISimulableEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpy extends AbstractSynchronizedEnvironment<SpyVsSpyAction>
        implements ISimulableEnvironment<SpyVsSpyAction>, 
        IRandomizable,
        IEnvironmentRepresentation //it is a represenation of itself for reactive controller
        {

    private final Logger logger = Logger.getLogger(SpyVsSpy.class);
    final StaticDefs defs;
    /**
     * All nodes present in the environment
     */
    List<SpyVsSpyMapNode> nodes;
    /**
     * Informations about individual agent, indexed by agentBody.id.
     */
    List<SpyVsSpyBodyInfo> bodyInfos;
    Random rand;

    SpyVsSpyPDDLRepresentation pDDLRepresentation;
    
    /**
     * True if this environment is a clone, created only for simulation
     */
    boolean isSimulation;
    
    /**
     * Create a shallow copy of the environment with the same defs
     * @param original 
     */
    private SpyVsSpy(SpyVsSpy original) {
        super(original);
        this.defs = original.defs;

        this.nodes = new ArrayList<SpyVsSpyMapNode>(original.nodes.size());
        for (SpyVsSpyMapNode node : original.nodes) {
            this.nodes.add(new SpyVsSpyMapNode(node));
        }

        this.bodyInfos = new ArrayList<SpyVsSpyBodyInfo>(original.bodyInfos.size());
        for (SpyVsSpyBodyInfo bi : original.bodyInfos) {
            this.bodyInfos.add(new SpyVsSpyBodyInfo(bi));
        }

        this.rand = new Random();
        this.isSimulation = original.isSimulation;
    }

    public SpyVsSpy(List<SpyVsSpyMapNode> nodes, int maxPlayers, List<Integer> startingLocations, Map<Integer, List<Integer>> neighbours, int numTrapTypes, int[] trapCounts, int numItemTypes, int destination) {
        super(SpyVsSpyAction.class);

        defs = new StaticDefs(maxPlayers, numTrapTypes, trapCounts, numItemTypes, -50, 150, -1, 0.3, destination, startingLocations, neighbours);

        this.nodes = nodes;


        isSimulation = false;

        bodyInfos = new ArrayList<SpyVsSpyBodyInfo>();

        rand = new Random();
        registerRepresentation(this);
        
        pDDLRepresentation = new SpyVsSpyPDDLRepresentation(this);
        registerRepresentation(pDDLRepresentation);

    }

    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        return Collections.singletonMap(SpyVsSpyAgentType.getInstance(), new AgentInstantiationDescriptor(1, defs.maxPlayers));
    }

    @Override
    public void setRandomSeed(long seed) {
        rand = new Random(seed);
    }

    /**
     * Kills an agent - i.e. moves it to a starting another location, drops all
     * of its items, assigns a reward and clears its action for this round.
     *
     * @param killedAgent
     * @param actionsToPerform
     */
    protected void killAgent(AgentBody killedAgent, Map<AgentBody, SpyVsSpyAction> actionsToPerform, Map<AgentBody, Double> reward) {
        //drop all items
        SpyVsSpyBodyInfo killedAgentInfo = bodyInfos.get(killedAgent.getId());

        SpyVsSpyMapNode currentNode = nodes.get(killedAgentInfo.locationIndex);
        for (int itemIndex : killedAgentInfo.itemsCarried) {
            currentNode.items.add(itemIndex);
        }
        killedAgentInfo.itemsCarried.clear();
        for (int trapRemoverIndex : killedAgentInfo.numTrapRemoversCarried) {
            currentNode.numTrapRemovers[trapRemoverIndex] += killedAgentInfo.numTrapRemoversCarried[trapRemoverIndex];
            killedAgentInfo.numTrapRemoversCarried[trapRemoverIndex] = 0;
        }
        //traps are not dropped, they are available, until they are all used

        //set reward, clear action
        reward.put(killedAgent, defs.rewardDeath);
        actionsToPerform.put(killedAgent, SpyVsSpyAction.NO_OP_ACTION);

        //move to a random location
        int randomStartLocation = rand.nextInt(defs.startingLocations.size());
        if (defs.startingLocations.size() > 1) {
            while (randomStartLocation == killedAgentInfo.locationIndex) {
                randomStartLocation = rand.nextInt(defs.startingLocations.size());
            }
        }

        killedAgentInfo.locationIndex = randomStartLocation;
        if (logger.isDebugEnabled()) {
            logger.debug("Agent killed, spawned at location: " + randomStartLocation + " body: " + killedAgentInfo);
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
    protected boolean checkTrapSet(AgentBody agent, Map<AgentBody, SpyVsSpyAction> actionsToPerform, Map<AgentBody, Double> reward) {
        SpyVsSpyBodyInfo agentInfo = bodyInfos.get(agent.getId());
        SpyVsSpyMapNode location = nodes.get(agentInfo.locationIndex);
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
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, SpyVsSpyAction> actionsToPerform) {
        Map<AgentBody, Double> reward = new HashMap<AgentBody, Double>();

        if (logger.isDebugEnabled() && !isSimulation) {
            logger.debug("============ Map State: ================");
            for (SpyVsSpyMapNode node : nodes) {
                logger.debug(node);
            }
            logger.debug("=========== MapState End ===============");
        }
        
        //evaluate attack actions
        for (AgentBody agentBody : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.ATTACK_AGENT) {
                if (action.getActionTarget() > getAllBodies().size()
                        || bodyInfo.locationIndex != bodyInfos.get(action.getActionTarget()).locationIndex) {
                    //I am attacking an invalid agent or agent at different location
                    logger.info("Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    agentFailedAction(agentBody);
                    continue;
                }
                if (rand.nextDouble() < defs.attackSuccessProbability) {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Succesful attack: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    killAgent(getAllBodies().get(action.getActionTarget()), actionsToPerform, reward);
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Unsuccesful attack: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }
            }
        }

        //evaluate remove trap actions
        for (AgentBody agentBody : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.REMOVE_TRAP) {
                int targetTrap = action.getActionTarget();
                SpyVsSpyMapNode location = nodes.get(bodyInfo.locationIndex);
                if (targetTrap < defs.numTrapTypes
                        && bodyInfo.numTrapRemoversCarried[targetTrap] > 0
                        && location.traps.contains(targetTrap)) {
                    location.traps.remove(targetTrap);
                    bodyInfo.numTrapRemoversCarried[targetTrap]--;
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }

            }
        }

        //evaluate set trap actions
        for (AgentBody agentBody : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.SET_TRAP) {
                int targetTrap = action.getActionTarget();
                SpyVsSpyMapNode location = nodes.get(bodyInfo.locationIndex);
                if (targetTrap < defs.numTrapTypes
                        && bodyInfo.numTrapsCarried[targetTrap] > 0) {
                    location.traps.add(targetTrap);
                    bodyInfo.numTrapsCarried[targetTrap]--;
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }

            }
        }

        //evaluate pickup trap remover actions
        for (AgentBody agentBody : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.PICKUP_TRAP_REMOVER) {
                int targetTrap = action.getActionTarget();
                SpyVsSpyMapNode location = nodes.get(bodyInfo.locationIndex);
                if (targetTrap < defs.numTrapTypes && location.numTrapRemovers[targetTrap] > 0) {
                    if (checkTrapSet(agentBody, actionsToPerform, reward)) {
                        bodyInfo.numTrapRemoversCarried[targetTrap]++;
                        location.numTrapRemovers[targetTrap]--;
                        if (logger.isDebugEnabled() && !isSimulation) {
                            logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                        }
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }

            }
        }

        //evaluate pickup item actions
        for (AgentBody agentBody : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.PICKUP_ITEM) {
                int targetItem = action.getActionTarget();
                SpyVsSpyMapNode location = nodes.get(bodyInfo.locationIndex);
                if (targetItem < defs.numItemTypes
                        && location.items.contains(targetItem)) {
                    if (checkTrapSet(agentBody, actionsToPerform, reward)) {
                        bodyInfo.itemsCarried.add(targetItem);
                        location.items.remove(targetItem);
                        if (logger.isDebugEnabled() && !isSimulation) {
                            logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                        }
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }
            }
        }

        //evaluate move actions
        for (AgentBody agentBody : actionsToPerform.keySet()) {
            SpyVsSpyAction action = actionsToPerform.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.MOVE) {
                if (defs.neighbours.get(bodyInfo.locationIndex).contains(action.getActionTarget())) {
                    bodyInfo.locationIndex = action.getActionTarget();
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug("Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }

            }
        }

        //check for goal conditions
        for (AgentBody agentBody : actionsToPerform.keySet()) {
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (bodyInfo.locationIndex == defs.destination && bodyInfo.itemsCarried.size() == defs.numItemTypes) {
                //agent has reached the destination with all neccessary items, let's finish this
                reward.put(agentBody, defs.rewardReachGoal);
                this.setFinished(true);
                //TODO split reward if two agents reach goal simultaneously
                if (logger.isDebugEnabled() && !isSimulation) {
                    logger.debug("Agent reached goal: " + bodyInfo);
                }
            }
        }



        for (AgentBody body : this.getAllBodies()) {
            if (reward.get(body) == null) {
                //set nothing reward, if it was not set previously (for example when the agent was killed)
                reward.put(body, defs.rewardNothing);
            }
        }
        return reward;
    }

    @Override
    protected AgentBody createAgentBodyInternal(IAgentType type) {
        if (type != SpyVsSpyAgentType.getInstance()) {
            throw new AgentInstantiationException("Illegal agent type");
        }
        int nextAgentIndex = this.getAllBodies().size();

        int startingLocation = defs.startingLocations.get(nextAgentIndex);

        logger.info("Registered agent at starting position " + startingLocation);
        AgentBody body = new AgentBody(nextAgentIndex, type);
        SpyVsSpyBodyInfo bodyInfo = new SpyVsSpyBodyInfo(body, startingLocation, defs.numTrapTypes, defs.trapCounts);
        bodyInfos.add(bodyInfo);
        return body;
    }

    @Override
    public SpyVsSpy cloneForSimulation() {
        SpyVsSpy copy =  new SpyVsSpy(this);
        copy.isSimulation = true;
        return copy;
    }

    @Override
    public Map<AgentBody, Double> simulateOneStep(Map<AgentBody, SpyVsSpyAction> actions) {
        return nextStepInternal(actions);
    }

    public SpyVsSpyPDDLRepresentation getpDDLRepresentation() {
        return pDDLRepresentation;
    }

    
    
    /**
     * Static definitions that do not change throughout simulation - used for creating simulable copies
     */
    class StaticDefs {

        int maxPlayers;
        /**
         * Number of trap types
         */
        final int numTrapTypes;
        /**
         * Number of traps of individual types that can be set by an individual
         * agent during one game
         */
        final int[] trapCounts;
        /**
         * Number of item types. A single instance of every item is required to
         * complete a level.
         */
        final int numItemTypes;
        /**
         * Reward for dying - usually negative
         */
        final double rewardDeath;
        /**
         * Reward for reaching the goal first
         */
        final double rewardReachGoal;
        /**
         * Reward for every step where nothing special happens - usually
         * negative to force agent to escape quickly
         */
        final double rewardNothing;
        /**
         * The probability a single attack action kills the oponnent
         */
        final double attackSuccessProbability;
        /**
         * The index of the node, everybody is trying to reach
         */
        int destination;
        /**
         * The list of starting locations for each agent (should have the same
         * size as {@link #maxPlayers}
         */
        List<Integer> startingLocations;
        /**
         * Neighbour lists. The values are indices to
         *
         * @{link #nodes}
         */
        Map<Integer, List<Integer>> neighbours;

        public StaticDefs(int maxPlayers, int numTrapTypes, int[] trapCounts, int numItemTypes, double rewardDeath, double rewardReachGoal, double rewardNothing, double attackSuccessProbability, int destination, List<Integer> startingLocations, Map<Integer, List<Integer>> neighbours) {
            this.maxPlayers = maxPlayers;
            this.numTrapTypes = numTrapTypes;
            this.trapCounts = trapCounts;
            this.numItemTypes = numItemTypes;
            this.rewardDeath = rewardDeath;
            this.rewardReachGoal = rewardReachGoal;
            this.rewardNothing = rewardNothing;
            this.attackSuccessProbability = attackSuccessProbability;
            this.destination = destination;
            this.startingLocations = startingLocations;
            this.neighbours = neighbours;
        }
    }
}
