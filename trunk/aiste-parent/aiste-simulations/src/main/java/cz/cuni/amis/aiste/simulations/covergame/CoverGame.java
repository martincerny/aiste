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
import java.util.*;
import org.apache.log4j.Logger;

/**
 * 
 * @author 
 */
public class CoverGame extends AbstractSynchronizedEnvironment<CGAction> implements 
        IEnvironmentRepresentation, //it is a represenation of itself for reactive controller
        ISimulableEnvironment<CGAction> 
{


    

    private final Logger logger = Logger.getLogger(CoverGame.class);
    
    final StaticDefs defs;
    
    /**
     * Informace o jednotlivych agentech. Indexem je id {@link AgentBody#id}
     */
    List<CGBodyInfo> bodyInfos;
    
    Map<Integer, List<CGBodyInfo>> agentsByTeamNo;
            
    public CoverGame(StaticDefs defs) {
        super(CGAction.class);
        /* Create defs - debug*/
        this.defs = defs;
        
        /* Create empty agent data*/
        bodyInfos = new ArrayList<CGBodyInfo>();
        agentsByTeamNo = new HashMap<Integer, List<CGBodyInfo>>();
        
        
        registerRepresentation(this);
    }      
    
    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, CGAction> actionsToPerform) {
    
        if(logger.isDebugEnabled()){
            //Draw the playground
            char[][] display = new char[defs.levelWidth][defs.levelHeight];
            for(int x = 0; x < defs.levelWidth; x++){
                for(int y = 0; y < defs.levelHeight;y++){
                    if(defs.squares[x][y] == null){
                        display[x][y] = '#';
                    }
                    else if(defs.squares[x][y].horizontalCover){
                        if(defs.squares[x][y].verticalCover){
                            display[x][y] = '-';
                        } else {
                            display[x][y] = '*';
                        }
                    } else if(defs.squares[x][y].verticalCover){
                        display[x][y] = '|';
                    } else {
                        display[x][y] = '.';
                    }
                }
            }
            for(CGBodyInfo bodyInfo : bodyInfos){
                display[bodyInfo.loc.x][bodyInfo.loc.y] = Integer.toString(bodyInfo.body.getId()).charAt(0);                
            }
            
            StringBuilder infoBuilder = new StringBuilder("==== Step " + getTimeStep() + " ======\n");
            for (int y = 0; y < defs.levelHeight; y++) {
                for (int x = 0; x < defs.levelWidth; x++) {
                    infoBuilder.append(display[x][y]);
                }
                infoBuilder.append("\n");
            }
            for(CGBodyInfo bodyInfo : bodyInfos){
                infoBuilder.append(bodyInfo.body.getId()).append(": ").append(bodyInfo.toString()).append("\n");
            }
            logger.debug(infoBuilder.toString());            
        }
        
        
        double [] rewards = new double[] { 0d, 0d}; //indexed by team id - the same rewards are given to all agents in the team

        //agents that are not hit this round heal
        Set<Integer> agentsNotHit = new HashSet<Integer>();
        for(AgentBody body : getActiveBodies()){
            agentsNotHit.add(body.getId());
        }
        
        //first come supress actions. The supressed flag is cleared each round and cooldown is lowered
        for(CGBodyInfo bodyInfo : bodyInfos){
            bodyInfo.supressed = false;
            if(bodyInfo.supressCooldown > 0){
                bodyInfo.supressCooldown--;
            }
        }
        
        for(AgentBody body : actionsToPerform.keySet()){
            CGAction action = actionsToPerform.get(body);
            CGBodyInfo bodyInfo = bodyInfos.get(body.getId());
            if(action.act == CGAction.Action.SUPRESS){                
                CGBodyInfo targetInfo = bodyInfos.get((Integer)action.target);                
                if(bodyInfo.supressCooldown > 0){
                    logger.info(body.getId() + ": Invalid supress. Cooldown not zero: " + bodyInfo.supressCooldown) ;                                        
                }
                else if(!isVisible(bodyInfo.loc, targetInfo.loc)){
                    logger.info(body.getId() + ": Invalid supress. Target not visible. From: " + bodyInfo.getLoc() + " to: " + targetInfo.getLoc());                    
                } else {
                    targetInfo.supressed = true;
                }
            }
        }
        
        //Uncovering due to shooting is evaluated AFTER all shots (i.e. is effective for next round)
        List<CGBodyInfo> agentsToUncover = new ArrayList<CGBodyInfo>();
        
        //evaluate attack actions, movement is updated after that
        for(AgentBody body : actionsToPerform.keySet()){
            CGAction action = actionsToPerform.get(body);
            CGBodyInfo bodyInfo = bodyInfos.get(body.getId());
            if(action.act == CGAction.Action.SHOOT){
                CGBodyInfo targetInfo = bodyInfos.get((Integer) action.target);
                agentsToUncover.add(bodyInfo);
                if (!isVisible(bodyInfo.getLoc(), targetInfo.getLoc())) {
                    logger.info(body.getId() + ": Invalid ranged attack. Target not visible. From: " + bodyInfo.getLoc() + " to: " + targetInfo.getLoc());
                } else {
                    double distance = bodyInfo.getLoc().distanceTo(targetInfo.getLoc());
                    double hitProbability = defs.basicAim * (1 / Math.sqrt(distance));
                    boolean covered = isCovered(bodyInfo.getLoc(), targetInfo.getLoc());
                    boolean fullCover = covered && targetInfo.takingFullCover;
                    boolean supressed = bodyInfo.supressed;
                    
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
                    
                    if (logger.isTraceEnabled()) {
                        logger.trace("Distance: " + distance + " Covered: " + covered + " Full cover: " + fullCover + " Supressed:" + supressed + ", hit probability: " + hitProbability);
                    }
                    if (rand.nextDouble() <= hitProbability) {
                        int damage = rand.nextInt(defs.shootDamageVariability * 2) + defs.shootDamage - defs.shootDamageVariability;
                        targetInfo.health -= damage;
                    }
                }
                break;
            }
        }
        
        for(CGBodyInfo agentToUncover : agentsToUncover){
            agentToUncover.takingFullCover = false;
        }
        
        //evaluate deaths, assess rewards and respawn agents               
        for(CGBodyInfo bodyInfo : bodyInfos){
            int id = bodyInfo.body.getId();
            if(bodyInfo.health <= 0){
                //assess -1 reward for getting killed
                rewards[bodyInfo.teamNo] -= 1; 
                //the +1 reward to the other team
                rewards[1 - bodyInfo.teamNo] += 1;
                
                //clear agent action for this round
                actionsToPerform.put(bodyInfo.body, new CGAction(CGAction.Action.NO_OP, null));
                                
                //respawn agent
                respawnAgent(bodyInfo);                
            }
        }

        //evaluate movement and cover        
        for(AgentBody body : actionsToPerform.keySet()){
            CGAction action = actionsToPerform.get(body);
            CGBodyInfo bodyInfo = bodyInfos.get(body.getId());
            if(action.act == CGAction.Action.MOVE){
                Loc targetLocation = (Loc)action.target;
                bodyInfo.takingFullCover = false;
                if(bodyInfo.getLoc().distanceTo(targetLocation) > defs.maxDistancePerTurn){
                    logger.info(body.getId() + ": Invalid movement - to far. From: " + bodyInfo.getLoc() + " To:" + targetLocation);
                } else if(!isVisible(bodyInfo.getLoc(), targetLocation)) {
                    logger.info(body.getId() + ": Invalid movement - impassable terrain. To:" + targetLocation);
                }
                else {
                    bodyInfo.loc = targetLocation;
                    if(logger.isDebugEnabled()){
                        logger.debug(body.getId() + ": Succesful move to:" + targetLocation);                            
                    }
                }
            } else if(action.act == CGAction.Action.TAKE_FULL_COVER){
                if(!isThereNeighbouringCover(bodyInfo.loc)){
                    logger.info(body.getId() + ": Invalid full cover - no cover near " + bodyInfo.loc);                    
                } else {
                    bodyInfo.takingFullCover = true;
                }
            }
        }
        
        
        
        Map<AgentBody, Double> rewardsMap = new HashMap<AgentBody, Double>(getActiveBodies().size());
        for(CGBodyInfo bodyInfo : bodyInfos){
            rewardsMap.put(bodyInfo.body, rewards[bodyInfo.teamNo]);
        }
        return rewardsMap;
    }
    
    public List<CGSquare> getNeighbouringSquares(Loc l){
        List<CGSquare> neighbours = new ArrayList<CGSquare>(4);
        if(l.x > 0 && defs.squares[l.x - 1][l.y] != null){
            neighbours.add(defs.squares[l.x - 1][l.y]);
        }
        if(l.y > 0 && defs.squares[l.x][l.y - 1] != null){
            neighbours.add(defs.squares[l.x][l.y - 1]);
        }
        if(l.x < defs.levelWidth - 1 && defs.squares[l.x + 1][l.y] != null){
            neighbours.add(defs.squares[l.x + 1][l.y]);
        }
        if(l.x < defs.levelHeight && defs.squares[l.x][l.y + 1] != null){
            neighbours.add(defs.squares[l.x][l.y + 1]);
        }
        return neighbours;
    }
    
    public boolean isThereNeighbouringCover(Loc l){
        for(CGSquare sq : getNeighbouringSquares(l)){
            if(sq.horizontalCover || sq.verticalCover){
                return true;
            }
        } 
        return false;
    }
    

    
    /**
     * Visibility is checked by Bresenham's algorithm, taken from http://tech-algorithm.com/articles/drawing-line-using-bresenham-algorithm/
     * @param from
     * @param to
     * @return 
     */
    public boolean isVisible(Loc from, Loc to){
        return CGUtils.isVisible(from, to, defs.squares);
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
        if(from.x < to.x && defs.squares[to.x - 1][to.y].verticalCover){
            return true;
        }
        if(from.x > to.x && defs.squares[to.x + 1][to.y].verticalCover){
            return true;
        }
        if(from.y < to.y && defs.squares[to.y - 1][to.x].horizontalCover){
            return true;
        }
        if(from.y > to.y && defs.squares[to.y + 1][to.x].horizontalCover){
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
        bodyInfo.supressed = false;
        bodyInfo.takingFullCover = false;
        bodyInfo.supressCooldown = 0;
    }
    
    @Override
    protected AgentBody createAgentBodyInternal(IAgentType type) {
        if(!(type instanceof CGAgentType)){
            throw new AgentInstantiationException("Illegal agent type");
        }
        int teamNo = ((CGAgentType)type).getTeamNo();

        AgentBody newBody = new AgentBody(bodyInfos.size() /*nove id v rade*/, type);
        CGBodyInfo newBodyInfo = new CGBodyInfo(newBody, teamNo);
        bodyInfos.add(newBodyInfo);
        if(!agentsByTeamNo.containsKey(teamNo)){
            agentsByTeamNo.put(teamNo, new ArrayList<CGBodyInfo>());
        }
        agentsByTeamNo.get(teamNo).add(newBodyInfo);

        respawnAgent(newBodyInfo);        
        
        return newBody;
    }

    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        Map<CGAgentType, IAgentInstantiationDescriptor> descriptors = new HashMap<CGAgentType, IAgentInstantiationDescriptor>(2);
        descriptors.put(CGAgentType.getTeam0Instance(), new AgentInstantiationDescriptor(2, 2));
        descriptors.put(CGAgentType.getTeam1Instance(), new AgentInstantiationDescriptor(2, 2));
        return descriptors;
    }

    @Override
    public ISimulableEnvironment<CGAction> cloneForSimulation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<AgentBody, Double> simulateOneStep(Map<AgentBody, CGAction> actions) {
        return nextStepInternal(actions);
    }
    
    
    
    static class CGBodyInfo {
        /**
         * The corresponding body.
         */
        AgentBody body;

        int teamNo;
        
        Loc loc;
        
        int health;
        boolean takingFullCover;
        boolean supressed;
        
        /**
         * Number of turns before the agent can supress again
         */
        int supressCooldown;
        
        
        public CGBodyInfo(AgentBody body, int teamNo) {
            this.body = body;
            this.teamNo = teamNo;
        }

        public Loc getLoc() {
            return loc;
        }

        public void setLoc(Loc loc) {
            this.loc = loc;
        }

        @Override
        public String toString() {
            return "CGBodyInfo{id=" + body.getId() + ", teamNo=" + teamNo + ", loc=" + loc + ", health=" + health + ", takingFullCover=" + takingFullCover + ", supressed=" + supressed +", supressCooldown=" + supressCooldown + '}';
        }
        
        
        
    }
    
    static class StaticDefs {
        int levelWidth;
        int levelHeight;
        
        CGSquare[][] squares;
        
        List<Loc> playerSpawningLocations;
        
        Map<Loc,List<Loc>> navGraph;
        
        double maxDistancePerTurn = 3;
        
        int maxHealth = 100;
        
        /**
         * Supression can only be used once in supressCooldown turns
         */
        int supressCooldown = 2;
        
        /**
         * Amount of health given for each round the agent was not hit.
         */
        int healPerRound = 10;
        
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
    }
    
        
        
    
}
