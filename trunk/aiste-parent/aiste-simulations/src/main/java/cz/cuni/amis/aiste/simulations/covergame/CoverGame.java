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
package cz.cuni.amis.aiste.simulations.covergame;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import cz.cuni.amis.aiste.environment.*;
import cz.cuni.amis.aiste.environment.impl.AbstractSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.aiste.simulations.utils.RandomUtils;
import cz.cuni.amis.experiments.ILoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.experiments.impl.LoggingHeadersConcatenation;
import cz.cuni.amis.pathfinding.map.IPFKnownMap;
import cz.cuni.amis.utils.collections.ListConcatenation;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.log4j.Logger;

/**
 * 
 * @author 
 */
public class CoverGame extends AbstractSynchronizedEnvironment<CGPairAction> implements 
        IEnvironmentRepresentation, //it is a represenation of itself for reactive controller
        ISimulableEnvironment<CGPairAction> 
{


    

    private final Logger logger = Logger.getLogger(CoverGame.class);
    
    final StaticDefs defs;
           
    /**
     * Information on individual agents. Indexed by id {@link AgentBody#id}
     */
    List<CGBodyInfo> bodyInfos;
    
    List<CGBodyPair> bodyPairs;
    
    /**
     * Cache for opponent data.
     * */
    List<Long> lastOpponentTeamDataEvalStep;
    List<OpponentTeamData> lastOpponentTeamData;
    
    List<MarkerData> markerData;
    /**
     * True if this environment is a clone, created only for simulation
     */
    boolean isSimulation;    
    
    List<CGBodyInfo> agentsKilledLastRound = new CopyOnWriteArrayList<CGBodyInfo>();
    
    /**
     * Create a duplicate of the environment. (defs are linked directly, rest of the data is copied).
     * @param original 
     */
    private CoverGame(CoverGame original){
        super(original);
        defs = original.defs;
        isSimulation = original.isSimulation;
        
        bodyInfos = new ArrayList<CGBodyInfo>();
        //prefill the array so that I can call set on correct locations
        for(int i = 0; i < original.bodyInfos.size(); i++){
            bodyInfos.add(null);
        }
        bodyPairs = new ArrayList<CGBodyPair>();
        lastOpponentTeamData = new ArrayList<OpponentTeamData>();
        lastOpponentTeamDataEvalStep = new ArrayList<Long>();
        markerData = new ArrayList<MarkerData>();
        
        agentsKilledLastRound = new CopyOnWriteArrayList<CGBodyInfo>(original.agentsKilledLastRound);
        
        for(CGBodyPair originalPair : original.bodyPairs ){
            CGBodyPair copyPair = new CGBodyPair(originalPair.body);
            
            CGBodyInfo body0 = new CGBodyInfo(originalPair.bodyInfo0, copyPair);            
            CGBodyInfo body1 = new CGBodyInfo(originalPair.bodyInfo1, copyPair);
            
            bodyInfos.set(body0.id, body0);
            bodyInfos.set(body1.id, body1);
                    
            copyPair.bodyInfo0 = body0;
            copyPair.bodyInfo1 = body1;
            copyPair.copyStatisticsFromOriginal(originalPair);            
            
            bodyPairs.add(copyPair);            
            lastOpponentTeamData.add(null);
            lastOpponentTeamDataEvalStep.add(-1L);
            markerData.add(new MarkerData(original.markerData.get(originalPair.getId())));
        }
        
    }
    
    public CoverGame(StaticDefs defs) {
        super(CGPairAction.class);
        this.defs = defs;
        
        
        
        registerRepresentation(this);
        registerRepresentation(new CGPDDLRepresentation(this, true));        
        registerRepresentation(new CGPDDLRepresentation(this, false));        
        registerRepresentation(new CGJSHOPRepresentationWithRoles(this));        
        //registerRepresentation(new CGJSHOPRepresentation(this));        
    }      

    @Override
    public ILoggingHeaders getEnvironmentParametersHeaders() {
        return LoggingHeadersConcatenation.concatenate(super.getEnvironmentParametersHeaders(), new LoggingHeaders("levelName")); 
    }

    @Override
    public List<Object> getEnvironmentParametersValues() {
        return ListConcatenation.concatenate(super.getEnvironmentParametersValues(), Arrays.asList(new Object[] {defs.levelName}));         
    }

    @Override
    public ILoggingHeaders getPerAgentAndExperimentLoggingHeaders() {
        return LoggingHeadersConcatenation.concatenate(super.getPerAgentAndExperimentLoggingHeaders(), 
                new LoggingHeaders("kills", "deaths", "numShots", "numShotsHit"));
    }

    @Override
    public List<Object> getPerAgentAndExperimentLoggingData(AgentBody agentBody) {
        CGBodyPair pair = bodyPairs.get(agentBody.getId());
        return ListConcatenation.concatenate(super.getPerAgentAndExperimentLoggingData(agentBody), 
                Arrays.asList(new Object[] {
                    pair.kills, pair.deaths, pair.numShots, pair.numShotsHit
                })
                ); 
    }

    
    
    
    @Override
    public void init() {
        super.init();
        /* Create empty agent data*/
        bodyInfos = new ArrayList<CGBodyInfo>();
        bodyPairs = new ArrayList<CGBodyPair>();
        lastOpponentTeamData = new ArrayList<OpponentTeamData>();
        lastOpponentTeamDataEvalStep = new ArrayList<Long>();

        isSimulation = false;

        markerData = new ArrayList<MarkerData>();
    }

    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, CGPairAction> actionsToPerform) {
        
        if(logger.isDebugEnabled() && !isSimulation){
            logger.debug("\n==== Step " + getTimeStep() + " ======");
        }
        
        agentsKilledLastRound.clear();
        
        Map<CGBodyInfo, CGAction> individualActionsToPerform = new HashMap<CGBodyInfo, CGAction>();
        
        //calculate possible threats for simulation
        for(AgentBody body : getActiveBodies()){
            for(int bodyIndex = 0; bodyIndex < 2; bodyIndex++){
                for(int opponentIndex = 0; opponentIndex < 2; opponentIndex++){
                    double hitProbability = getHitProbability(bodyPairs.get(body.getId()).getBodyInfo(bodyIndex), bodyInfos.get(getOpponentIds(body.getId())[opponentIndex]));
                    markerData.get(body.getId()).expectedWorstCaseShotsReceivedSinceMarker[bodyIndex] += hitProbability;                               
                    if(hitProbability > markerData.get(body.getId()).maxShotProbabilitySinceMarker[bodyIndex]){
                        markerData.get(body.getId()).maxShotProbabilitySinceMarker[bodyIndex] = hitProbability;
                    }
                }
            }
        }
        
        for(AgentBody body: actionsToPerform.keySet()){
            CGBodyPair bodyPair = bodyPairs.get(body.getId());
            individualActionsToPerform.put(bodyPair.bodyInfo0, actionsToPerform.get(body).getAction1());
            individualActionsToPerform.put(bodyPair.bodyInfo1, actionsToPerform.get(body).getAction2());
        }
    
        
        
        double [] rewards = new double[] { 0d, 0d}; //indexed by team id - the same rewards are given to all agents in the team

        //agents that are not hit this round heal
        Set<Integer> agentsNotHit = new HashSet<Integer>();
        for(AgentBody body : getActiveBodies()){
            agentsNotHit.add(body.getId());
        }
        
        //first come supress actions. The suppressed flag is cleared each round and cooldown is lowered
        for(CGBodyInfo bodyInfo : bodyInfos){
            bodyInfo.suppressed = false;
            if(bodyInfo.suppressCooldown > 0){
                bodyInfo.suppressCooldown--;
            }
        }
        
        for(CGBodyInfo bodyInfo : individualActionsToPerform.keySet()){
            CGAction action = individualActionsToPerform.get(bodyInfo);
            if(action.act == CGAction.Action.SUPPRESS){                
                CGBodyInfo targetInfo = bodyInfos.get((Integer)action.target);                
                if(bodyInfo.suppressCooldown > 0){
                    if(!isSimulation){
                        logger.info(bodyInfo.id + ": Invalid supress. Cooldown not zero: " + bodyInfo.suppressCooldown);
                    }
                    agentFailedAction(bodyInfo.team.body);                    
                }
                else if(!isVisible(bodyInfo.loc, targetInfo.loc)){
                    if(!isSimulation){
                        logger.info(bodyInfo.id + ": Invalid supress. Target not visible. From: " + bodyInfo.getLoc() + " to: " + targetInfo.getLoc());                    
                    }
                    agentFailedAction(bodyInfo.team.body);                    
                } else {
                    targetInfo.suppressed = true;
                    if(logger.isDebugEnabled()  && !isSimulation){
                        logger.debug(bodyInfo.id + ": Succesful suppress on: " + targetInfo.id);                            
                    }   
                    bodyInfo.suppressCooldown = defs.supressCooldown;
                }
            }
        }
        
        //Uncovering due to shooting is evaluated AFTER all shots (i.e. is effective for next round)
        List<CGBodyInfo> agentsToUncover = new ArrayList<CGBodyInfo>();
        
        //evaluate attack actions, movement is updated after that
        for(CGBodyInfo bodyInfo : individualActionsToPerform.keySet()){
            CGAction action = individualActionsToPerform.get(bodyInfo);
            if(action.act == CGAction.Action.SHOOT){
                CGBodyInfo targetInfo = bodyInfos.get((Integer) action.target);
                agentsToUncover.add(bodyInfo);
                if (!isVisible(bodyInfo.getLoc(), targetInfo.getLoc())) {
                    if(!isSimulation){
                        logger.info(bodyInfo.id + ": Invalid ranged attack. Target not visible. From: " + bodyInfo.getLoc() + " to: " + targetInfo.getLoc());
                    }
                    agentFailedAction(bodyInfo.team.body);                    
                } else {
                    bodyInfo.team.numShots++;
                    double hitProbability = getHitProbability(bodyInfo, targetInfo);
                    if (rand.nextDouble() <= hitProbability || isSimulation) {
                        //In simulation, I always hit
                        int damage = rand.nextInt(defs.shootDamageVariability * 2) + defs.shootDamage - defs.shootDamageVariability;
                        targetInfo.health -= damage;
                        targetInfo.numTurnsNotHit = 0;
                        agentsNotHit.remove(targetInfo.id);                        
                        bodyInfo.team.numShotsHit++;
                        if(logger.isDebugEnabled()  && !isSimulation){
                            logger.debug(bodyInfo.id + ": Succesful ranged attack on:" + targetInfo.id + " damage: " + damage);                            
                        }
                    } else {
                        if(logger.isDebugEnabled()  && !isSimulation){
                            logger.debug(bodyInfo.id + ": Ranged attack on:" + targetInfo.id + " missed.");                            
                        }                        
                    }
                }
            }
        }
        
        for(CGBodyInfo agentToUncover : agentsToUncover){
            agentToUncover.takingFullCover = false;
        }
        
        //evaluate deaths, assess rewards and respawn agents               
        for(CGBodyInfo bodyInfo : bodyInfos){
            int id = bodyInfo.id;
            if(bodyInfo.health <= 0){
                //assess -1 reward for getting killed
                rewards[bodyInfo.team.body.getId()] -= 1; 
                bodyInfo.team.deaths++;
                //the +1 reward to the other team
                rewards[1 - bodyInfo.team.body.getId()] += 1;
                bodyPairs.get(1 - bodyInfo.team.body.getId()).kills++;
                
                //update markers
                markerData.get(bodyInfo.getTeamId()).diedSinceMarker = true;
                
                //clear agent action for this round
                individualActionsToPerform.put(bodyInfo, new CGAction(CGAction.Action.NO_OP, null));
                                
                //the agent has failed any action it wanted to do
                agentFailedAction(bodyInfo.team.body);                
                
                agentsKilledLastRound.add(bodyInfo);
                        
                //respawn agent
                respawnAgent(bodyInfo);                
            }
        }

        //evaluate movement and cover        
        for(CGBodyInfo bodyInfo : individualActionsToPerform.keySet()){
            CGAction action = individualActionsToPerform.get(bodyInfo);
            if(action.act == CGAction.Action.MOVE){
                Loc targetLocation = (Loc)action.target;
                bodyInfo.takingFullCover = false;
                if(bodyInfo.getLoc().distanceTo(targetLocation) > defs.maxDistancePerTurn){
                    if(!isSimulation){
                        logger.info(bodyInfo.id + ": Invalid movement - to far. From: " + bodyInfo.getLoc() + " To:" + targetLocation);
                    }
                    agentFailedAction(bodyInfo.team.body);                    
                } else if(!isVisible(bodyInfo.getLoc(), targetLocation)) {
                    if(!isSimulation){
                        logger.info(bodyInfo.id + ": Invalid movement - impassable terrain. To:" + targetLocation);
                    }
                    agentFailedAction(bodyInfo.team.body);                    
                }
                else {
                    bodyInfo.loc = targetLocation;
                    if(logger.isDebugEnabled()  && !isSimulation){
                        logger.debug(bodyInfo.id + ": Succesful move to:" + targetLocation);                            
                    }
                }
            } else if(action.act == CGAction.Action.TAKE_FULL_COVER){
                if(!isThereNeighbouringCover(bodyInfo.loc)){
                    if(!isSimulation){
                        logger.info(bodyInfo.id + ": Invalid full cover - no cover near " + bodyInfo.loc);                    
                    }
                    agentFailedAction(bodyInfo.team.body);                    
                } else {
                    bodyInfo.takingFullCover = true;
                    if(logger.isDebugEnabled() && !isSimulation){
                        logger.debug(bodyInfo.id + ": Succesful full cover");                            
                    }
                }
            }
        }
        
        //evaluate healing
        for(int agentNotHit : agentsNotHit){
            CGBodyInfo notHitInfo = bodyInfos.get(agentNotHit);
            notHitInfo.numTurnsNotHit++;
            if(notHitInfo.numTurnsNotHit >= defs.healHeatup){
                notHitInfo.health += defs.healPerRound;
                if(notHitInfo.health > defs.maxHealth){
                    notHitInfo.health = defs.maxHealth;
                }
            }
        }
        
        
        Map<AgentBody, Double> rewardsMap = new HashMap<AgentBody, Double>(getActiveBodies().size());
        for(CGBodyPair bodypair : bodyPairs){
            rewardsMap.put(bodypair.body, rewards[bodypair.body.getId()]);
            if(logger.isDebugEnabled() && !isSimulation){
                logger.debug("Reward " + bodypair.body.getId() + ": " + rewards[bodypair.body.getId()]);
            }
        }
        
        if(logger.isDebugEnabled()  && !isSimulation){
            outputEnvironmentStateToLogger();            
        }
        
        return rewardsMap;
    }
    
    public List<CGSquare> getNeighbouringSquares(Loc l){
        return CGUtils.getNeighbouringSquares(l, defs);
    }
    
    public boolean isThereNeighbouringCover(Loc l){
        return CGUtils.isThereNeighbouringCover(l, defs);
    }
    

    
    /**
     * Return cached visibility from visibility matrix.
     * Visibility is checked by Bresenham's algorithm, taken from http://tech-algorithm.com/articles/drawing-line-using-bresenham-algorithm/
     * @param from
     * @param to
     * @return 
     */
    public boolean isVisible(Loc from, Loc to){
        return defs.visibilityMatrix[from.x][from.y][to.x][to.y];
       // return CGUtils.isVisible(from, to, defs);
    }
    
    /**
     * Checks whether location to is covered if shooting from location from.
     * Does not check visibility!
     * @param from
     * @param to
     * @return 
     */
    public boolean isCovered(Loc from, Loc to){
        if(CGUtils.distance(from,to) < 2){
            //at close combat distance, there is no cover
            return false;
        }
        if(from.x < to.x - 1  && defs.squares[to.x - 1][to.y].verticalCover){
            return true;
        }
        if(from.x > to.x + 1  && defs.squares[to.x + 1][to.y].verticalCover){
            return true;
        }
        if(from.y < to.y - 1  && defs.squares[to.x][to.y - 1].horizontalCover){
            return true;
        }
        if(from.y > to.y + 1  && defs.squares[to.x][to.y + 1].horizontalCover){
            return true;
        }
        return false;
    }
    
    private void respawnAgent(CGBodyInfo bodyInfo) {
        Collection<Loc> freeSpawningPoints = Collections2.filter(defs.playerSpawningLocations, new Predicate<Loc>(){

            @Override
            public boolean apply(Loc t) {
                for(CGBodyInfo bodyInfo: bodyInfos){
                    if(t.equals(bodyInfo.loc)){
                        return false;
                    }
                }
                return true;
            }

        });
        Loc spawningPoint = RandomUtils.randomElementLinearAccess(freeSpawningPoints, rand);
        bodyInfo.loc = spawningPoint;                
        bodyInfo.health = defs.maxHealth;
        bodyInfo.suppressed = false;
        bodyInfo.takingFullCover = false;
        bodyInfo.suppressCooldown = 0;
        bodyInfo.numTurnsNotHit = 0;
    }
    
    @Override
    protected AgentBody createAgentBodyInternal(IAgentType type) {
        if(!(type instanceof CGAgentType)){
            throw new AgentInstantiationException("Illegal agent type");
        }
        int newID = bodyPairs.size();

        AgentBody newBody = new AgentBody(newID, type);
        CGBodyPair newPair = new CGBodyPair(newBody);
        bodyPairs.add(newPair);
        
        int newBodyId = bodyInfos.size();
        CGBodyInfo newBodyInfo0 = new CGBodyInfo(newBodyId, newPair);
        bodyInfos.add(newBodyInfo0);
        newPair.bodyInfo0 = newBodyInfo0;
        respawnAgent(newBodyInfo0);        

        CGBodyInfo newBodyInfo1 = new CGBodyInfo(newBodyId + 1, newPair);
        bodyInfos.add(newBodyInfo1);
        newPair.bodyInfo1 = newBodyInfo1;
        respawnAgent(newBodyInfo1);        
        
        lastOpponentTeamData.add(null);
        lastOpponentTeamDataEvalStep.add(-1L);
        
        markerData.add(createMarkerData(newBody));        
        
        return newBody;
    }

    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        Map<CGAgentType, IAgentInstantiationDescriptor> descriptors = new HashMap<CGAgentType, IAgentInstantiationDescriptor>(2);
        descriptors.put(CGAgentType.getInstance(), new AgentInstantiationDescriptor(2, 2));
        return descriptors;
    }

    @Override
    public ISimulableEnvironment<CGPairAction> cloneForSimulation() {
        CoverGame clone = new CoverGame(this);
        clone.isSimulation = true;
        //reset marker so that I can count possible risks
        for(AgentBody body : getActiveBodies()){
            clone.setMarker(body);
        }
        return clone;
    }

    @Override
    public Map<AgentBody, Double> simulateOneStep(Map<AgentBody, CGPairAction> actions) {
        return nextStepWithGivenActions(actions);
    }
    
    protected MarkerData createMarkerData(AgentBody body){
        MarkerData data = new MarkerData();
        for(int oppId : getOpponentIds(body.getId())){
            data.opponentLocationsAtMarker.add(bodyInfos.get(oppId).loc);
        }
        return data;
    }
    
    void setMarker(AgentBody body){
        markerData.set(body.getId(), createMarkerData(body));
    }
    
    MarkerData getMarkerData(AgentBody body){
        return markerData.get(body.getId());
    }
    
    /**
     * Helper method for representations. Purposefully package visibility
     * @param teamNo
     * @return 
     */
    int[] getOpponentIds(int teamNo){
        int[] ids = new int[2];
        int idIndex = 0;
        for(CoverGame.CGBodyInfo bodyInfo : bodyInfos){
            if(bodyInfo.getTeamId() != teamNo){
                ids[idIndex] = bodyInfo.id;
                idIndex++;
            }
        }
        return ids;
    }

    /**
     * Helper method for representations. Purposefully package visibility
     * @param teamNo
     * @return 
     */
    int[] getTeamIds(int teamNo){
        int[] ids = new int[2];
        int idIndex = 0;
        for(CoverGame.CGBodyInfo bodyInfo : bodyInfos){
            if(bodyInfo.getTeamId() == teamNo){
                ids[idIndex] = bodyInfo.id;
                idIndex++;
            }
        }
        return ids;
    }
    
    
    /**
     * Helper method for representations. Purposefully package visibility
     * @param teamNo
     * @return 
     */
    OpponentTeamData getOpponentTeamData(int teamNo){
        if(lastOpponentTeamDataEvalStep.get(teamNo) == getTimeStep()){
            return lastOpponentTeamData.get(teamNo);
        }
        int [] ids = getOpponentIds(teamNo);
        OpponentData[] opponentData = new OpponentData[ids.length];
        OpponentTeamData data = new OpponentTeamData();
        
        for(int i = 0; i < ids.length; i++){
            opponentData[i] = new OpponentData();
            Loc oponentLocation = bodyInfos.get(ids[i]).loc;
            for(Loc navPoint : defs.navGraph.keySet()){
                if(isVisible(navPoint, oponentLocation)){
                    opponentData[i].visibleNavpoints.add(navPoint);
                    if(!isCovered(navPoint, oponentLocation)){
                        opponentData[i].navpointsInvalidatingCover.add(navPoint);
                    }
                    if(!isCovered(oponentLocation, navPoint)){
                        opponentData[i].uncoveredNavpoints.add(navPoint);
                    }
                }
            }
            data.allUncoveredNavPoints.addAll(opponentData[i].uncoveredNavpoints);            
            data.allVisibleNavPoints.addAll(opponentData[i].visibleNavpoints);            
        }
        
        lastOpponentTeamDataEvalStep.set(teamNo, getTimeStep());
        
        data.opponentData = opponentData;
        
        lastOpponentTeamData.set(teamNo, data);
        return data;
        
    }    
    
    /**
     * Get id of the least covered visible member of opposing team - helper for representations.
     * @return the id, or -1 if no visible opponent.
     */
    //TODO rewrite to "get best target for team" - include possibility to shoot from my collegue, health and so on
    //to minimize expected number of turns before the opponent dies
    public int getBestTarget(int bodyId){
        int leastCoverId = -1;
        int leastCoverLevel = Integer.MAX_VALUE;
        CGBodyInfo bodyInfo = bodyInfos.get(bodyId);
        for(int oppId : getOpponentIds(bodyInfo.team.getId())){
            CGBodyInfo oppInfo = bodyInfos.get(oppId);
            if(!isVisible(bodyInfo.loc, oppInfo.loc)){
                continue;
            } else {
                int coverLevel;
                if(isCovered(bodyInfo.loc, oppInfo.loc)){
                    if(oppInfo.takingFullCover){
                        coverLevel = 2;
                    } else {
                        coverLevel = 1;
                    }
                } else {
                    coverLevel = 0;
                }
                if(coverLevel < leastCoverLevel){
                    leastCoverLevel = coverLevel;
                    leastCoverId = oppId;
                }
            }
        }
        return leastCoverId;
    }
    
    /**
     * Get number of threats (agents with uncovered shot) to a specified body at given location
     * @param bodyId
     * @param node
     * @return 
     */
    int getNumThreats(int bodyId, Loc node) {
        int threats = 0;
        CGBodyInfo info = bodyInfos.get(bodyId);
        for (OpponentData oppData : getOpponentTeamData(info.getTeamId()).opponentData) {
            if (oppData.uncoveredNavpoints.contains(node)) {
                threats++;
            }
        }
        return threats;
    }

    double getHitProbability(CGBodyInfo bodyInfo, CGBodyInfo targetInfo) {
        if(!isVisible(bodyInfo.loc, targetInfo.loc)){
            return 0;
        }
        double distance = bodyInfo.getLoc().distanceTo(targetInfo.getLoc());
        if(distance < 1){
            distance = 1;
        }
        double hitProbability = defs.basicAim * (1 / (0.1 * distance + 0.9));
        boolean covered = isCovered(bodyInfo.getLoc(), targetInfo.getLoc());
        boolean fullCover = covered && targetInfo.takingFullCover;
        boolean supressed = bodyInfo.suppressed;
        double multiplier = 1;
        if(covered){
            if(fullCover){
                multiplier *= defs.fullCoverAimPenalty;
            } else {
                multiplier *= defs.partialCoverAimPenalty;
            }
        }
        if(supressed){
            multiplier *= defs.supressedAimPenalty;
        }
        hitProbability *= multiplier;
//        if (logger.isTraceEnabled() && !isSimulation) {
//            logger.trace("Distance: " + distance + " Covered: " + covered + " Full cover: " + fullCover + " Supressed:" + supressed + ", hit probability: " + hitProbability);
//        }
        return hitProbability;
    }

    protected void outputEnvironmentStateToLogger() {
        //Draw the playground
        char[][] display = new char[defs.levelWidth][defs.levelHeight];
        for(int x = 0; x < defs.levelWidth; x++){
            for(int y = 0; y < defs.levelHeight;y++){
                if(!defs.squares[x][y].passable){
                    if(defs.squares[x][y].horizontalCover && defs.squares[x][y].verticalCover){
                        display[x][y] = 'H';                            
                    } else {
                        display[x][y] = '#';
                    }
                }
                else if(defs.squares[x][y].horizontalCover){
                    if(defs.squares[x][y].verticalCover){
                        display[x][y] = '*';
                    } else {
                        display[x][y] = '-';
                    }
                } else if(defs.squares[x][y].verticalCover){
                    display[x][y] = '|';
                } else {
                    display[x][y] = '.';
                }
            }
        }
        for(CGBodyInfo bodyInfo : bodyInfos){
            display[bodyInfo.loc.x][bodyInfo.loc.y] = Integer.toString(bodyInfo.id).charAt(0);                
        }
        
        StringBuilder infoBuilder = new StringBuilder("Environment state:\n");
        infoBuilder.append("\t");
        for (int x = 0; x < defs.levelWidth; x++) {
                infoBuilder.append(x % 10);                
        }
        infoBuilder.append("\n");
        
        for (int y = 0; y < defs.levelHeight; y++) {
            infoBuilder.append(y % 100).append("\t");
            for (int x = 0; x < defs.levelWidth; x++) {
                infoBuilder.append(display[x][y]);
            }
            infoBuilder.append("\n");
        }
        infoBuilder.append("\t");
        for (int x = 0; x < defs.levelWidth; x++) {
                infoBuilder.append(x % 10);                
        }
        infoBuilder.append("\n");
        
        for(CGBodyInfo bodyInfo : bodyInfos){
            infoBuilder.append(bodyInfo.id).append(": ").append(bodyInfo.toString()).append("\n");
        }
        logger.debug(infoBuilder.toString());
    }

    
    static class CGBodyInfo {
        final int id;
        
        final CGBodyPair team;
        
        Loc loc;
        
        int health;
        boolean takingFullCover;
        boolean suppressed;
        
        /**
         * Number of turns before the agent can supress again
         */
        int suppressCooldown;
        
        int numTurnsNotHit;
        
        /**
         * A copy constructor
         * @param original
         * @param copiedPair 
         */
        protected CGBodyInfo(CGBodyInfo original, CGBodyPair copiedPair){
            this(original.id, copiedPair);
            this.loc = original.loc;
            this.health = original.health;
            this.takingFullCover = original.takingFullCover;
            this.suppressed = original.suppressed;
            this.suppressCooldown = original.suppressCooldown;
            this.numTurnsNotHit = original.numTurnsNotHit;
        }
        
        public CGBodyInfo(int id, CGBodyPair team) {
            this.id = id;
            this.team = team;
        }

        public Loc getLoc() {
            return loc;
        }

        public void setLoc(Loc loc) {
            this.loc = loc;
        }

        public int getTeamId(){
            return team.getId();
        }
       
        @Override
        public String toString() {
            return "CGBodyInfo{" + "id=" + id + ", team=" + team.body.getId() + ", loc=" + loc + ", health=" + health + ", takingFullCover=" + takingFullCover + ", suppressed=" + suppressed + ", suppressCooldown=" + suppressCooldown + ", numTurnsNotHit=" + numTurnsNotHit + '}';
        }
    }
    
    static class CGBodyPair {
        /**
         * The corresponding body.
         */
        final AgentBody body;
        
        CGBodyInfo bodyInfo0;
        CGBodyInfo bodyInfo1;
        
        /**
         * Statistics for logging
         */
        int kills = 0;
        int deaths = 0;
        int numShots = 0;
        int numShotsHit = 0;

        public CGBodyPair(AgentBody body) {
            this.body = body;
        }                

        public int getId() {
            return body.getId();
        }
        
        public int[] getBodyIds(){
            return new int[] {bodyInfo0.id, bodyInfo1.id};
        }
        
        public CGBodyInfo getBodyInfo(int order){
            switch(order){
                case 0 : return bodyInfo0;
                case 1 : return bodyInfo1;
                default : throw new IllegalArgumentException("Body order outside range");
            }
        }
        
        public CGBodyInfo getOtherInfo(CGBodyInfo info){
            if(info == bodyInfo0){
                return bodyInfo1;
            } else if (info == bodyInfo1){
                return bodyInfo0;
            } else {
                throw new IllegalStateException("Given body is not a member of this pair.");
            }
        }
        
        void copyStatisticsFromOriginal(CGBodyPair original){
            this.kills = original.kills;
            this.deaths = original.deaths;
            this.numShots = original.numShots;
            this.numShotsHit = original.numShotsHit;
        }

        @Override
        public String toString() {
            return "Pair_" + body.getId();
        }
        
        
    }
    
    public static class StaticDefs {
        String levelName;
        
        int levelWidth;
        int levelHeight;
        
        CGSquare[][] squares;
        
        /**
         * Visibility matrix. Keys are (in this order): fromX, fromY, toX, toY
         */
        boolean [][][][] visibilityMatrix;
        
        List<Loc> playerSpawningLocations;
        
        Map<Loc,List<Loc>> navGraph;
        
        double maxDistancePerTurn = 3.5;
        
        int maxHealth = 100;
        
        /**
         * Supression can only be used once in supressCooldown turns
         */
        int supressCooldown = 2;
        
        /**
         * Amount of health given for each round the agent was not hit.
         */
        int healPerRound = 10;

        /**
         * Number of turns the agent must not be hit before it starts to heal
         */
        int healHeatup = 2;
        
        int shootDamage = 45;
        
        
        
        /**
         * The actual damage is chosen uniformly between rangeDamage - rangeDamageVariability and rangeDamage + rangeDamageVariability
         */
        int shootDamageVariability = 15;

        
        /**
         * The base probability of range weapon to hit (for distance 1).
         * 0 - cannot hit ... 1 hits always
         */
        double basicAim = 0.95;
        
        /**
         * The aim is multiplied by this constant, if target is in partial cover
         */
        double partialCoverAimPenalty = 0.5;
        
        /**
         * The aim is multiplied by this constant, if target is in full cover
         */
        double fullCoverAimPenalty = 0.1;
        
        /**
         * The aim is multiplied by this constant, if the shooting agent is suppressed
         */
        double supressedAimPenalty = 0.2;
        
        IPFKnownMap<Loc> navGraphMap = new IPFKnownMap<Loc>() {

            @Override
            public Collection<Loc> getNodes() {
                return navGraph.keySet();
            }

            @Override
            public Collection<Loc> getNeighbors(Loc node) {
                return navGraph.get(node);
            }

            @Override
            public int getNodeCost(Loc node) {
                return 0;
            }

            @Override
            public int getArcCost(Loc node, Loc node1) {
                return 1;
                //return (int)Math.ceil(CGUtils.distance(node1, node));
            }
        };
    }
        
    protected static class OpponentTeamData {
        OpponentData [] opponentData;
        Set<Loc> allUncoveredNavPoints = new HashSet<Loc>();        
        Set<Loc> allVisibleNavPoints = new HashSet<Loc>();        
    }
        
    protected static class OpponentData {
        /**
         * Navpoints that are not covered when oponent attacks
         */
        Set<Loc> uncoveredNavpoints = new HashSet<Loc>();
        
        /**
         * Navpoints that are visible to oponent (and oponent is visible from there)
         */
        List<Loc> visibleNavpoints = new ArrayList<Loc>();
        
        /**
         * Navpoints from which the oponent has no cover
         */
        List<Loc> navpointsInvalidatingCover = new ArrayList<Loc>();
    }
        
    static class MarkerData {
        boolean diedSinceMarker = false;
        List<Loc> opponentLocationsAtMarker;
        double[] expectedWorstCaseShotsReceivedSinceMarker = new double[2];
        double[] maxShotProbabilitySinceMarker = new double[2];

        public MarkerData() {
            opponentLocationsAtMarker = new ArrayList<Loc>();
        }
        
        /**
         * A copy constructor
         * @param original 
         */
        public MarkerData(MarkerData original){
            diedSinceMarker = original.diedSinceMarker;
            opponentLocationsAtMarker = new ArrayList<Loc>(original.opponentLocationsAtMarker);
            for(int i = 0; i < original.expectedWorstCaseShotsReceivedSinceMarker.length;i++){
                expectedWorstCaseShotsReceivedSinceMarker[i] = original.expectedWorstCaseShotsReceivedSinceMarker[i];
            }
            for(int i = 0; i < original.maxShotProbabilitySinceMarker.length;i++){
                maxShotProbabilitySinceMarker[i] = original.maxShotProbabilitySinceMarker[i];
            }
        }
    }
}
