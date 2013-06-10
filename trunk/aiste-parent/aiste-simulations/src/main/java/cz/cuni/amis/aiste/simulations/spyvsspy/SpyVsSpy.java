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

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.AgentInstantiationException;
import cz.cuni.amis.aiste.environment.IAgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.IAgentType;
import cz.cuni.amis.aiste.environment.IPlanningRepresentation;
import cz.cuni.amis.aiste.environment.IReactivePlan;
import cz.cuni.amis.aiste.environment.ISimulableEnvironment;
import cz.cuni.amis.aiste.environment.ReactivePlanStatus;
import cz.cuni.amis.aiste.environment.impl.AbstractReactivePlan;
import cz.cuni.amis.aiste.environment.impl.AbstractSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.experiments.ILoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeadersConcatenation;
import cz.cuni.amis.pathfinding.alg.astar.AStar;
import cz.cuni.amis.pathfinding.alg.astar.AStarResult;
import cz.cuni.amis.pathfinding.map.IPFKnownMap;
import cz.cuni.amis.pathfinding.map.IPFMap;
import cz.cuni.amis.utils.collections.ListConcatenation;
import java.util.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpy extends AbstractSynchronizedEnvironment<SpyVsSpyAction>
        implements ISimulableEnvironment<SpyVsSpyAction>
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

    SpyVsSpyPDDLRepresentation pDDLRepresentation;
    SpyVsSpyJShop2Representation jShop2Representation;
    
    Map<AgentBody, ChangesSinceMarker> markerData;
    Set<AgentBody> agentsKilledThisRound;
    
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
        markerData = new HashMap<AgentBody, ChangesSinceMarker>();
        agentsKilledThisRound = new HashSet<AgentBody>(original.agentsKilledThisRound);
    }

    public SpyVsSpy(SpyVsSpyEnvironmentDefinition definition) {
        super(SpyVsSpyAction.class);

        int rewardDeath = -50;
        int rewardReachedGoal = 150;
        int rewardNothing = -1;
        double attackSuccessProbability = 0.5;

        defs = new StaticDefs(definition.maxPlayers, definition.numTrapTypes, definition.trapCounts, definition.numItemTypes, rewardDeath, rewardReachedGoal, rewardNothing, attackSuccessProbability, definition.destination, definition.startingLocations, definition.nodes, definition.neighbours);

        rand = new Random();
        
        
        pDDLRepresentation = new SpyVsSpyPDDLRepresentation(this);
        registerRepresentation(pDDLRepresentation);
        
        jShop2Representation = new SpyVsSpyJShop2Representation(this);
        registerRepresentation(jShop2Representation);
        
        registerRepresentation(new SpyVsSpyReactiveRepresentation(this));        

        
    }

    @Override
    public void init() {
        super.init();

        this.nodes = new ArrayList<SpyVsSpyMapNode>(defs.nodesInStartingPosition.size());
        for(SpyVsSpyMapNode node : defs.nodesInStartingPosition){
            nodes.add(new SpyVsSpyMapNode(node));
        }


        isSimulation = false;

        bodyInfos = new ArrayList<SpyVsSpyBodyInfo>();

        markerData = new HashMap<AgentBody, ChangesSinceMarker>();
        agentsKilledThisRound = new HashSet<AgentBody>();
        
    }
    
    
    
    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        return Collections.singletonMap(SpyVsSpyAgentType.getInstance(), new AgentInstantiationDescriptor(1, defs.maxPlayers));
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
        for (int trapRemoverIndex = 0; trapRemoverIndex < defs.numTrapTypes ; trapRemoverIndex++) {
            currentNode.numTrapRemovers[trapRemoverIndex] += killedAgentInfo.numTrapRemoversCarried[trapRemoverIndex];
            killedAgentInfo.numTrapRemoversCarried[trapRemoverIndex] = 0;
        }
        
        currentNode.numWeapons += killedAgentInfo.numWeapons;
        killedAgentInfo.numWeapons = 0;
        
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
        
        //note changes for markers
        for(Map.Entry<AgentBody, ChangesSinceMarker> markerEntry : markerData.entrySet()){
            if(markerEntry.getKey().equals(killedAgent)){
                markerEntry.getValue().agentHasDied = true;
            } else {
                markerEntry.getValue().numOtherAgentsDeaths++;
            }
        }

        killedAgentInfo.locationIndex = randomStartLocation;
        if (logger.isDebugEnabled() && !isSimulation) {
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

        //When an agent gets killed, I overwrite its actions, so I need a copy of the actions.
        Map<AgentBody, SpyVsSpyAction> actionsToPerformCopy = new HashMap<AgentBody, SpyVsSpyAction>(actionsToPerform);        
        
/*        if (logger.isDebugEnabled() && !isSimulation) {
            logger.debug("============ Map State: ================");
            for (SpyVsSpyMapNode node : nodes) {
                logger.debug(node);
            }
            logger.debug("=========== MapState End ===============");
        }
  */      
        if (logger.isDebugEnabled() && !isSimulation) {
            logger.debug("=== Step: " + getTimeStep()+ " =================");
        }
        //evaluate attack actions
        //agents killed by attack are first gathered and then all killed instantly, to properly resolve mutual attacks
        agentsKilledThisRound.clear();
        for (AgentBody agentBody : actionsToPerformCopy.keySet()) {
            SpyVsSpyAction action = actionsToPerformCopy.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.ATTACK_AGENT) {
                if (bodyInfo.numWeapons <= 0 || action.getActionTarget() > getAllBodies().size()
                        || bodyInfo.locationIndex != bodyInfos.get(action.getActionTarget()).locationIndex) {
                    //I am attacking an invalid agent or agent at different location or I do not have weapons
                    logger.info(agentBody.getId() + ": Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    agentFailedAction(agentBody);
                    continue;
                }
                bodyInfo.numWeapons--; //weapon is for one use only
                if (rand.nextDouble() < defs.attackSuccessProbability) {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Succesful attack: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    
                    AgentBody targetBody = getAllBodies().get(action.getActionTarget());                    
                    agentsKilledThisRound.add(targetBody);
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Unsuccesful attack: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }
            }
        }
        
        for(AgentBody bodyToKill : agentsKilledThisRound){
            killAgent(bodyToKill, actionsToPerformCopy, reward);
        }

        //evaluate remove trap actions
        for (AgentBody agentBody : actionsToPerformCopy.keySet()) {
            SpyVsSpyAction action = actionsToPerformCopy.get(agentBody);
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
                        logger.debug(agentBody.getId() + ": Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }

            }
        }

        //evaluate set trap actions
        for (AgentBody agentBody : actionsToPerformCopy.keySet()) {
            SpyVsSpyAction action = actionsToPerformCopy.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.SET_TRAP) {
                int targetTrap = action.getActionTarget();
                SpyVsSpyMapNode location = nodes.get(bodyInfo.locationIndex);
                if (targetTrap < defs.numTrapTypes
                        && bodyInfo.numTrapsCarried[targetTrap] > 0) {
                    location.traps.add(targetTrap);
                    bodyInfo.numTrapsCarried[targetTrap]--;
                    
                    //update marker data
                    for(ChangesSinceMarker changes : markerData.values()){
                        changes.numTrapsSet++;
                    }
                    
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }

            }
        }

        //evaluate pickup trap remover actions
        for (AgentBody agentBody : actionsToPerformCopy.keySet()) {
            SpyVsSpyAction action = actionsToPerformCopy.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.PICKUP_TRAP_REMOVER) {
                int targetTrap = action.getActionTarget();
                SpyVsSpyMapNode location = nodes.get(bodyInfo.locationIndex);
                if (targetTrap < defs.numTrapTypes && location.numTrapRemovers[targetTrap] > 0) {
                    if (checkTrapSet(agentBody, actionsToPerformCopy, reward)) {
                        bodyInfo.numTrapRemoversCarried[targetTrap]++;
                        location.numTrapRemovers[targetTrap]--;
                        
                        //update marker data
                        for(ChangesSinceMarker changes : markerData.values()){
                            changes.numRemoversTaken++;
                        }
                        
                        if (logger.isDebugEnabled() && !isSimulation) {
                            logger.debug(agentBody.getId() + ": Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                        }
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }

            }
        }

        //evaluate pickup item actions
        for (AgentBody agentBody : actionsToPerformCopy.keySet()) {
            SpyVsSpyAction action = actionsToPerformCopy.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.PICKUP_ITEM) {
                int targetItem = action.getActionTarget();
                SpyVsSpyMapNode location = nodes.get(bodyInfo.locationIndex);
                if (targetItem < defs.numItemTypes
                        && location.items.contains(targetItem)) {
                    if (checkTrapSet(agentBody, actionsToPerformCopy, reward)) {
                        bodyInfo.itemsCarried.add(targetItem);
                        location.items.remove(targetItem);
                        
                        //update marker data
                        for(ChangesSinceMarker changes : markerData.values()){
                            changes.numItemsTaken++;
                        }
                        
                        if (logger.isDebugEnabled() && !isSimulation) {
                            logger.debug(agentBody.getId() + ": Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                        }
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }
            }
        }
        
        //evaluate pickup weapon actions
        for (AgentBody agentBody : actionsToPerformCopy.keySet()) {
            SpyVsSpyAction action = actionsToPerformCopy.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.PICKUP_WEAPON) {
                SpyVsSpyMapNode location = nodes.get(bodyInfo.locationIndex);
                if (location.numWeapons > 0) {
                    if (checkTrapSet(agentBody, actionsToPerformCopy, reward)) {
                        bodyInfo.numWeapons++;
                        location.numWeapons--;
                        
                        //update marker data
                        for(ChangesSinceMarker changes : markerData.values()){
                            changes.numWeaponsTaken++;
                        }
                        
                        if (logger.isDebugEnabled() && !isSimulation) {
                            logger.debug(agentBody.getId() + ": Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                        }
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }
            }
        }

        //evaluate move actions
        for (AgentBody agentBody : actionsToPerformCopy.keySet()) {
            SpyVsSpyAction action = actionsToPerformCopy.get(agentBody);
            SpyVsSpyBodyInfo bodyInfo = bodyInfos.get(agentBody.getId());
            if (action.getType() == SpyVsSpyAction.ActionType.MOVE) {
                if (defs.neighbours.get(bodyInfo.locationIndex).contains(action.getActionTarget())) {
                    bodyInfo.locationIndex = action.getActionTarget();

                    //update marker data
                    for(ChangesSinceMarker changes : markerData.values()){
                        changes.numAgentMoves++;
                    }
                    
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Succesful action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                } else {
                    if (logger.isDebugEnabled() && !isSimulation) {
                        logger.debug(agentBody.getId() + ": Invalid action: " + action.getLoggableRepresentation() + " from: " + bodyInfo);
                    }
                    agentFailedAction(agentBody);
                }

            }
        }

        //check for goal conditions
        for (AgentBody agentBody : actionsToPerformCopy.keySet()) {
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

    public SpyVsSpyJShop2Representation getjShop2Representation() {
        return jShop2Representation;
    }

    @Override
    public ILoggingHeaders getEnvironmentParametersHeaders() {
        return LoggingHeadersConcatenation.concatenate(super.getEnvironmentParametersHeaders(), 
                new LoggingHeaders("numNodes", "numTrapTypes", "numItemTypes", "attackSuccesProb"));
    }

    @Override
    public List<Object> getEnvironmentParametersValues() {
        return ListConcatenation.concatenate(super.getEnvironmentParametersValues(), Arrays.asList(new Object[]{defs.nodesInStartingPosition.size(), defs.numTrapTypes, defs.numItemTypes, defs.attackSuccessProbability }));
    }

    public synchronized void setMarker(AgentBody body){
        markerData.put(body, new ChangesSinceMarker());
    }
    
    public ChangesSinceMarker getChangesSinceMarker(AgentBody body){
        if(markerData.containsKey(body)){
            return markerData.get(body);
        } else {
            return new ChangesSinceMarker();
        }
    }

    /**
     * Some representations exploit this class's random generator.
     * @return 
     */
    Random getRand() {
        return rand;
    }
    
    
    
    
    /**
     * Class that allows the implementation of markers for {@link IPlanningRepresentation#environmentChangedConsiderablySinceLastMarker() }
     */
    public static class ChangesSinceMarker {
        int numWeaponsTaken = 0;
        int numItemsTaken = 0;
        int numTrapsSet = 0;
        int numAgentMoves = 0;
        int numOtherAgentsDeaths = 0;
        int numRemoversTaken = 0;
        boolean agentHasDied = false;                
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
        
        IPFKnownMap<Integer> mapForPathFinding;

        /**
        * "Original" for all the map nodes - every time the environment is reused,
        * nodes are recreated from this original.
        */
        List<SpyVsSpyMapNode> nodesInStartingPosition;

        
        public StaticDefs(int maxPlayers, int numTrapTypes, int[] trapCounts, int numItemTypes, double rewardDeath, double rewardReachGoal, double rewardNothing, double attackSuccessProbability, int destination, List<Integer> startingLocations, List<SpyVsSpyMapNode> nodesInStartingPosition, Map<Integer, List<Integer>> neighbours) {
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
            this.nodesInStartingPosition = nodesInStartingPosition;
            this.neighbours = neighbours;
            
            this.mapForPathFinding = new IPFKnownMap<Integer>() {
                @Override
                public Collection<Integer> getNeighbors(Integer node) {
                    return defs.neighbours.get(node);
                }

                @Override
                public int getNodeCost(Integer node) {
                    //default cost is 0
                    return 0;
                }

                @Override
                public int getArcCost(Integer node, Integer node1) {
                    //default cost is 1
                    return 1;
                }

                @Override
                public Collection<Integer> getNodes() {
                    return StaticDefs.this.neighbours.keySet();
                }
                
                
            };
            
        }
    }
    
    public abstract static class AbstractPathFindingPlan extends AbstractReactivePlan<SpyVsSpyAction> {
        protected int agentId;
        
        private int lastPathFindingTarget;
        private boolean lastPathFindingSuccess = false;
        private List<Integer> foundPath;
        private int foundPathIndex;
        
        private AStar<Integer> astar;

        final SpyVsSpy env;
        
        public AbstractPathFindingPlan(SpyVsSpy env, int agentId) {
            this.agentId = agentId;
            lastPathFindingTarget = -1;
            this.env = env;
        }

        protected abstract int getTargetLocationId();
        
        protected AStar<Integer> createAStar(){
            return new AStar<Integer>(env.defs.mapForPathFinding);            
        }
        
        @Override
        protected void updateStepForNextAction() {
            foundPathIndex++;
        }

        @Override
        public SpyVsSpyAction peek() {
            refreshPathIfNeccessary();
            if(!lastPathFindingSuccess){
                throw new IllegalStateException(agentId + ": Path finding was not succesful, no actions available");
            }
            if(foundPathIndex >= foundPath.size()){
                throw new IllegalStateException(agentId + ": There are no more path elements available");
            }
            return new SpyVsSpyAction(SpyVsSpyAction.ActionType.MOVE, foundPath.get(foundPathIndex));
        }

        @Override
        public ReactivePlanStatus getStatus() {
            refreshPathIfNeccessary();
            int currentLocationIndex = env.bodyInfos.get(agentId).locationIndex;
            if(currentLocationIndex == lastPathFindingTarget){
                return ReactivePlanStatus.COMPLETED;
            }
            else if(!lastPathFindingSuccess){                
                return ReactivePlanStatus.FAILED;
            } 
            /*  The check does not work very well...
            else if(foundPath.get(foundPathIndex - 1) != currentLocationIndex && foundPathIndex >= 2 && foundPath.get(foundPathIndex - 2) != currentLocationIndex){                
                //I do not know, whether last action I sent was acutally performed or not, so I rather check both variants and report failure only if both of them are wrong
                env.logger.info(agentId + ": Path following failed - location not properly updated. Expected: " + foundPath.get(foundPathIndex - 1) + " or " +  foundPath.get(foundPathIndex - 2) + " got: " + currentLocationIndex);
                 return ReactivePlanStatus.FAILED;
            }*/
            else {
                return ReactivePlanStatus.EXECUTING;
            }
        }

        protected void refreshPathIfNeccessary() {
            int newPathFindingTarget = getTargetLocationId();
            if(newPathFindingTarget != lastPathFindingTarget || foundPath == null){
                if(astar == null){
                    astar =  createAStar();
                }
                AStarResult<Integer> result = astar.findPath(new SpyVsSpyAStarGoal(env.bodyInfos.get(agentId).locationIndex, newPathFindingTarget, env));
                if(result.isSuccess()){
                    lastPathFindingSuccess = true;
                    foundPath = result.getPath();
                } else {
                    lastPathFindingSuccess = false;
                    foundPath = null;
                }
                //the first element is always the agent's current location, so we skip it
                foundPathIndex = 1;
                lastPathFindingTarget = newPathFindingTarget;
            }
        }
    }
    
    public static class PursueOponentPlan extends AbstractPathFindingPlan {
        private int oponentId;

        public PursueOponentPlan(SpyVsSpy env, int agentId, int oponentId) {
            super(env, agentId);
            this.oponentId = oponentId;
        }

        @Override
        protected int getTargetLocationId() {
            return env.bodyInfos.get(oponentId).locationIndex;
        }

        @Override
        public IReactivePlan<SpyVsSpyAction> cloneForSimulation(ISimulableEnvironment<SpyVsSpyAction> environmentCopy) {
            return new PursueOponentPlan((SpyVsSpy)environmentCopy, agentId, oponentId);
        }
        
        
    }
}
