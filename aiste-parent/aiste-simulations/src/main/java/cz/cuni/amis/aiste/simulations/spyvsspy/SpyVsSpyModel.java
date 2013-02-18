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

import cz.cuni.amis.aiste.IRandomizable;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IEnvironmentModel;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyModel implements IEnvironmentModel<SpyVsSpyAction>, IRandomizable {
    private final Logger logger = Logger.getLogger(SpyVsSpyModel.class);

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
     * Reward for every step where nothing special happens - usually negative to
     * force agent to escape quickly
     */
    final double rewardNothing;

    /**
     * The probability a single attack action kills the oponnent
     */
    final double attackSuccessProbability;

    /**
     * All nodes present in the environment
     */
    List<SpyVsSpyMapNode> nodes;

    /**
     * The index of the node, everybody is trying to reach
     */
    int destination;

    /**
     * The list of starting locations for each agent (should have the same size
     * as {@link #maxPlayers}
     */
    List<Integer> startingLocations;

    /**
     * Neighbour lists. The values are indices to @{link #nodes}
     */
    Map<Integer, List<Integer>> neighbours;

    /**
     * Informations about individual agent, indexed by agentBody.id.
     */
    List<SpyVsSpyBodyInfo> bodyInfos;
    
    Random rand;

    public SpyVsSpyModel(List<SpyVsSpyMapNode> nodes, int maxPlayers, List<Integer> startingLocations, Map<Integer, List<Integer>> neighbours, int numTrapTypes, int[]trapCounts, int numItemTypes, int destination) {

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

        bodyInfos = new ArrayList<SpyVsSpyBodyInfo>();
        
        rand = new Random();
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
        reward.put(killedAgent, rewardDeath);
        actionsToPerform.put(killedAgent, SpyVsSpyAction.NO_OP_ACTION);

        //move to a random location
        int randomStartLocation = rand.nextInt(startingLocations.size());
        if (startingLocations.size() > 1) {
            while (randomStartLocation == killedAgentInfo.locationIndex) {
                randomStartLocation = rand.nextInt(startingLocations.size());
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
    public Map<AgentBody, Double> simulateOneStep(Map<AgentBody, SpyVsSpyAction> actionsToPerform) {
        Map<AgentBody, Double> reward = new HashMap<AgentBody, Double>();

        if (logger.isDebugEnabled()) {
            logger.debug("============ Map State: ================");
            for (SpyVsSpyMapNode node : nodes) {
                logger.debug(node);
            }
            logger.debug("=========== MapState End ===============");
        }

        //evaluate attack actions
        for (AgentBody body : actionsToPerform.keySet()) {
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
        for (AgentBody body : actionsToPerform.keySet()) {
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
        for (AgentBody body : actionsToPerform.keySet()) {
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
        for (AgentBody body : actionsToPerform.keySet()) {
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
        for (AgentBody body : actionsToPerform.keySet()) {
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
        for (AgentBody body : actionsToPerform.keySet()) {
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
        for (AgentBody body : actionsToPerform.keySet()) {
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



        for (AgentBody body : this.getAllBodies()) {
            if (reward.get(body) == null) {
                //set nothing reward, if it was not set previously (for example when the agent was killed)
                reward.put(body, rewardNothing);
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

        int startingLocation = startingLocations.get(nextAgentIndex);

        logger.info("Registered agent at starting position " + startingLocation);
        return new AgentBody(nextAgentIndex, startingLocation, numTrapTypes, trapCounts);
    }
    
    
}
