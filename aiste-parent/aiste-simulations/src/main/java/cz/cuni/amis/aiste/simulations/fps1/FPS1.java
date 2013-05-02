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
package cz.cuni.amis.aiste.simulations.fps1;

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
public class FPS1 extends AbstractSynchronizedEnvironment<FPS1Action> implements 
        IEnvironmentRepresentation, //it is a represenation of itself for reactive controller
        ISimulableEnvironment<FPS1Action> 
{


    
    public enum ItemType {
        NOTHING, RANGED_WEAPON, MEELEE_WEAPON, RANGED_AMMO, MEDIKIT
    }

    private final Logger logger = Logger.getLogger(FPS1.class);
    
    private final StaticDefs defs;
    
    /**
     * Informace o jednotlivych agentech. Indexem je id {@link AgentBody#id}
     */
    private List<FPS1BodyInfo> bodyInfos;
    
    private FPS1SquareState[][] squareStates;
    
    private PriorityQueue<ScheduledRespawn> scheduledRespawns;
        
    public FPS1(StaticDefs defs) {
        super(FPS1Action.class);
        /* Create defs - debug*/
        this.defs = defs;
        
        /* Create empty agent data*/
        bodyInfos = new ArrayList<FPS1BodyInfo>();
        
        copySquareStates(defs.initialSquareStates);        
        
        registerRepresentation(this);
    }

    
    private void copySquareStates(FPS1SquareState[][] originalStates){
        squareStates = new FPS1SquareState[defs.levelWidth][defs.levelHeight];
        for(int x = 0; x < defs.levelWidth; x++){
            for(int y = 0; y < defs.levelHeight; y++){
                if(originalStates[x][y] != null)
                squareStates[x][y] = new FPS1SquareState(originalStates[x][y]);
            }
        }
        
    }
    
    
    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, FPS1Action> actionsToPerform) {
    
        if(logger.isDebugEnabled()){
            //Draw the playground
            char[][] display = new char[defs.levelWidth][defs.levelHeight];
            for(int x = 0; x < defs.levelWidth; x++){
                for(int y = 0; y < defs.levelHeight;y++){
                    if(defs.squares[x][y] == null){
                        display[x][y] = '#';
                    }
                    else if(squareStates[x][y].items.contains(ItemType.MEDIKIT)){
                        display[x][y] = 'h';
                    } 
                    else if(squareStates[x][y].items.contains(ItemType.RANGED_AMMO)){
                        display[x][y] = 'a';
                    } 
                    else if(squareStates[x][y].items.contains(ItemType.RANGED_WEAPON)){
                        display[x][y] = 'r';
                    } 
                    else if(squareStates[x][y].items.contains(ItemType.MEELEE_WEAPON)){
                        display[x][y] = 'm';
                    } else {
                        display[x][y] = '.';
                    }
                }
            }
            for(FPS1BodyInfo bodyInfo : bodyInfos){
                display[bodyInfo.x][bodyInfo.y] = Integer.toString(bodyInfo.body.getId()).charAt(0);                
            }
            
            StringBuilder infoBuilder = new StringBuilder("==== Step " + getTimeStep() + " ======\n");
            for (int y = 0; y < defs.levelHeight; y++) {
                for (int x = 0; x < defs.levelWidth; x++) {
                    infoBuilder.append(display[x][y]);
                }
                infoBuilder.append("\n");
            }
            for(FPS1BodyInfo bodyInfo : bodyInfos){
                infoBuilder.append(bodyInfo.body.getId()).append(": ").append(bodyInfo.toString()).append("\n");
            }
            logger.debug(infoBuilder.toString());            
        }
        
        
        double [] rewards = new double[getAllBodies().size()]; //indexed by agent id
        
        //first index is who was attacked. At each slot, list of agents that dealt demage to this agent is kept        
        List<List<Integer>> succesfulAttackers = new ArrayList<List<Integer>>(getAllBodies().size()); 
        
        
        for(int i = 0; i < getAllBodies().size(); i++){
            rewards[i] = 0; //zero is the default reward
            succesfulAttackers.add(new ArrayList<Integer>()); //add empty attackers
        }

        //evaluate attack actions first, movement is updated after that
        for(Map.Entry<AgentBody, FPS1Action> actionEntry : actionsToPerform.entrySet()){
            AgentBody body = actionEntry.getKey();
            FPS1Action action = actionEntry.getValue();
            FPS1BodyInfo bodyInfo = bodyInfos.get(body.getId());
            switch(action.act){
                case ATTACK_MELEE : {
                    FPS1BodyInfo targetInfo = bodyInfos.get((Integer)action.target);
                    if(targetInfo.getLoc().distanceTo(bodyInfo.getLoc()) > 1){
                        logger.info(body.getId() + ": Invalid melee attack. Target to far. From: " + bodyInfo.getLoc() + " to: " + targetInfo.getLoc());
                    } else {
                        int damage = rand.nextInt(defs.meleeDamageVariability * 2) + defs.meleeDamage - defs.meleeDamageVariability;
                        targetInfo.health -= damage;
                        succesfulAttackers.get(targetInfo.body.getId()).add(body.getId());
                    }
                    break;
                }
                case ATTACK_RANGED : {
                    FPS1BodyInfo targetInfo = bodyInfos.get((Integer)action.target);                    
                    if(!isVisible(bodyInfo.getLoc(), targetInfo.getLoc())){
                        logger.info(body.getId() + ": Invalid ranged attack. Target not visible. From: " + bodyInfo.getLoc() + " to: " + targetInfo.getLoc());
                    } else {
                        double distance = bodyInfo.getLoc().distanceTo(targetInfo.getLoc());
                        double hitProbability = defs.rangeBasicAim * (1 / Math.sqrt(distance));
                        if(logger.isTraceEnabled()){
                            logger.trace("Distance: " + distance + ", hit probability: " + hitProbability);
                        }
                        if(rand.nextDouble() <= hitProbability){
                            int damage = rand.nextInt(defs.rangeDamageVariability * 2) + defs.rangeDamage - defs.rangeDamageVariability;
                            targetInfo.health -= damage;
                            succesfulAttackers.get(targetInfo.body.getId()).add(body.getId());
                        }
                    }
                    break;
                }
            }
        }
        
        //evaluate deaths, assess rewards and respawn agents               
        for(FPS1BodyInfo bodyInfo : bodyInfos){
            int id = bodyInfo.body.getId();
            if(bodyInfo.health <= 0){
                //assess -1 reward for getting killed
                rewards[id] -= 1; 
                //the +1 reward is divided among all succesfull attackers
                for(int attacker : succesfulAttackers.get(id)){
                    rewards[attacker] += 1 / succesfulAttackers.size();
                }
                
                //clear agent action for this round
                actionsToPerform.put(bodyInfo.body, new FPS1Action(FPS1Action.Action.NO_OP, null));
                
                //if agent had weapon, drop it
                if(bodyInfo.hasRangedWeapon){
                    squareStates[bodyInfo.x][bodyInfo.y].items.add(ItemType.RANGED_WEAPON);
                } else if(bodyInfo.hasMeleeWeapon){
                    squareStates[bodyInfo.x][bodyInfo.y].items.add(ItemType.MEELEE_WEAPON);                    
                }
                
                //respawn agent
                respawnAgent(bodyInfo);                
            }
        }

        //evaluate movement        
        //first check all moves validity and clear invalid. Only after alter all locations of succesfully moving agents
        Map<FPS1BodyInfo, Loc> validMoves = new HashMap<FPS1BodyInfo, Loc>();
        for(AgentBody body : actionsToPerform.keySet()){
            FPS1Action action = actionsToPerform.get(body);
            FPS1BodyInfo bodyInfo = bodyInfos.get(body.getId());
            if(action.act == FPS1Action.Action.MOVE){
                Loc targetLocation = (Loc)action.target;
                boolean moveValid = true;
                if(bodyInfo.getLoc().distanceTo(targetLocation) > 1){
                    logger.info(body.getId() + ": Invalid movement - to far. From: " + bodyInfo.getLoc() + " To:" + targetLocation);
                    moveValid = false;
                } else if(defs.squares[targetLocation.x][targetLocation.y] == null){
                    logger.info(body.getId() + ": Invalid movement - unpassable terrain. To:" + targetLocation);
                    moveValid = false;                    
                } else {
                    for(FPS1BodyInfo otherAgent : bodyInfos){
                        if(otherAgent == bodyInfo){
                            continue;
                        } else if(bodyInfo.getLoc().equals(targetLocation)){
                            logger.info(body.getId() + ": Invalid movement - occuppied by agent " + otherAgent.body.getId() + ". To:" + targetLocation);                            
                            moveValid = false;
                        }
                    }
                }
                
                if(moveValid){
                    if(logger.isDebugEnabled()){
                        logger.debug(body.getId() + ": Succesful move to:" + targetLocation);                            
                    }
                    validMoves.put(bodyInfo, targetLocation);
                }
            }
        }
        
        //update position of agents by all valid moves
        for(FPS1BodyInfo bodyInfo : validMoves.keySet()){
            bodyInfo.setLoc(validMoves.get(bodyInfo));            
        }
        
        //agents automatically gather items that are at their locations, if they can (are not over limits)
        for(FPS1BodyInfo bodyInfo : bodyInfos){
            Set<ItemType> itemsNotTaken = EnumSet.noneOf(ItemType.class);
            FPS1Square currentSquare = defs.squares[bodyInfo.x][bodyInfo.y];
            FPS1SquareState currentSquareState = squareStates[bodyInfo.x][bodyInfo.y];
            for(ItemType item: currentSquareState.items){
                switch(item){
                    case MEDIKIT: {
                        if(bodyInfo.health >= defs.maxHealth){
                            itemsNotTaken.add(item);
                        } else {
                            bodyInfo.health = Math.max(defs.maxHealth, bodyInfo.health + defs.healthRestoredByHealthPack);
                            afterItemTaken(bodyInfo, currentSquare, item);
                        }
                        break;
                    }
                    case MEELEE_WEAPON:{
                        if(bodyInfo.hasMeleeWeapon){
                            itemsNotTaken.add(item);
                        } else {
                            bodyInfo.hasMeleeWeapon = true;
                            afterItemTaken(bodyInfo,currentSquare, item);
                        }
                        break;
                    }
                    case RANGED_WEAPON : {
                        if(bodyInfo.hasRangedWeapon && bodyInfo.ammo >= defs.maximumAmmo){
                            itemsNotTaken.add(item);
                        } else {
                            bodyInfo.ammo = Math.max(defs.maximumAmmo, bodyInfo.ammo + defs.ammoInWeapon);
                            afterItemTaken(bodyInfo,currentSquare, item);
                        }     
                        break;
                    }
                    case RANGED_AMMO : {
                        if(bodyInfo.ammo >= defs.maximumAmmo){
                            itemsNotTaken.add(item);
                        } else {
                            bodyInfo.ammo = Math.max(defs.maximumAmmo, bodyInfo.ammo + defs.ammoInAmmoPack);
                            afterItemTaken(bodyInfo,currentSquare, item);
                        }
                        break;
                    }
                }
            }
        }
        
        Map<AgentBody, Double> rewardsMap = new HashMap<AgentBody, Double>(getActiveBodies().size());
        for(AgentBody activeBody : getActiveBodies()){
            rewardsMap.put(activeBody, rewards[activeBody.getId()]);
        }
        return rewardsMap;
    }

    private void afterItemTaken(FPS1BodyInfo bodyInfo, FPS1Square square, ItemType item) {
        if(square.spawnedItem == item){
            scheduledRespawns.add(new ScheduledRespawn(getTimeStep() + defs.itemRespawnTime, square));
        }
        if(logger.isTraceEnabled()){
            logger.trace(bodyInfo.body.getId() + ": Item taken: " + item);            
        }
    }
    
    /**
     * Visibility is checked by Bresenham's algorithm, taken from http://tech-algorithm.com/articles/drawing-line-using-bresenham-algorithm/
     * @param from
     * @param to
     * @return 
     */
    public boolean isVisible(Loc from, Loc to){
        int x = from.x;
        int x2 = to.x;
        int y = from.y;
        int y2 = to.y;
        
        int w = x2 - x;
        int h = y2 - y;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if (w < 0) {
            dx1 = -1;
        } else if (w > 0) {
            dx1 = 1;
        }
        if (h < 0) {
            dy1 = -1;
        } else if (h > 0) {
            dy1 = 1;
        }
        if (w < 0) {
            dx2 = -1;
        } else if (w > 0) {
            dx2 = 1;
        }
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0) {
                dy2 = -1;
            } else if (h > 0) {
                dy2 = 1;
            }
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++) {
            if(defs.squares[x][y] == null){
                //traversing an impassable square
                return false;
            }
            numerator += shortest;
            if (!(numerator < longest)) {
                numerator -= longest;
                x += dx1;
                y += dy1;
            } else {
                x += dx2;
                y += dy2;
            }
        }
        
        return true;
    }
    
    private void respawnAgent(FPS1BodyInfo bodyInfo) {
        Collection<Loc> freeSpawningPoints = Collections2.filter(defs.playerSpawningLocations, new Predicate<Loc>(){

            @Override
            public boolean apply(Loc t) {
                for(FPS1BodyInfo bodyInfo: bodyInfos){
                    if(bodyInfo.getLoc().equals(t)){
                        return false;
                    }
                }
                return true;
            }

        });
        Loc spawningPoint = RandomUtils.randomElementLinearAccess(freeSpawningPoints, rand);
        bodyInfo.setLoc(spawningPoint);                
        bodyInfo.ammo = 0;
        bodyInfo.hasMeleeWeapon = false;
        bodyInfo.hasRangedWeapon = false;
        bodyInfo.health = defs.maxHealth;
    }
    
    @Override
    protected AgentBody createAgentBodyInternal(IAgentType type) {
        if(type != FPS1AgentType.getInstance()){
            throw new AgentInstantiationException("Illegal agent type");
        }
        AgentBody newBody = new AgentBody(bodyInfos.size() /*nove id v rade*/, type);
        FPS1BodyInfo newBodyInfo = new FPS1BodyInfo(newBody);
        bodyInfos.add(newBodyInfo);

        respawnAgent(newBodyInfo);        
        
        return newBody;
    }

    @Override
    public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
        return Collections.singletonMap(FPS1AgentType.getInstance(), new AgentInstantiationDescriptor(1, defs.maxPlayers));
    }

    @Override
    public ISimulableEnvironment<FPS1Action> cloneForSimulation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<AgentBody, Double> simulateOneStep(Map<AgentBody, FPS1Action> actions) {
        return nextStepInternal(actions);
    }
    
    
    
    private static class FPS1BodyInfo {
        /**
         * Body, ktere reprezentuji.
         */
        AgentBody body;
        
        int x = - 1;
        int y = - 1;
        
        int health;
        boolean hasRangedWeapon;
        boolean hasMeleeWeapon;
        int ammo;
        
        public FPS1BodyInfo(AgentBody body) {
            this.body = body;
        }
        
        public Loc getLoc(){
            return new Loc(x, y);
        }
        
        public void setLoc(Loc loc){
            this.x = loc.x;
            this.y = loc.y;
        }

        @Override
        public String toString() {
            return "FPS1BodyInfo " + "id=" + body.getId() + ", x=" + x + ", y=" + y + ", health=" + health + ", hasRangedWeapon=" + hasRangedWeapon + ", hasMeleeWeapon=" + hasMeleeWeapon + ", ammo=" + ammo + '}';
        }
        
        
    }
    
    static class StaticDefs {
        int maxPlayers;        
        
        int levelWidth;
        int levelHeight;
        
        FPS1Square[][] squares;
        
        FPS1SquareState[][] initialSquareStates;
        
        List<FPS1Room> rooms;
        
        List<Loc> playerSpawningLocations;
        
        int maxHealth = 100;
        
        int meleeDamage = 80;
        
        /**
         * The actual damage is chosen uniformly between meleeDamage - meleeDamageVariability and meleeDamage + meleeDamageVariability
         */
        int meleeDamageVariability = 40;
        
        int rangeDamage = 30;
        
        /**
         * The actual damage is chosen uniformly between rangeDamage - rangeDamageVariability and rangeDamage + rangeDamageVariability
         */
        int rangeDamageVariability = 10;

        
        /**
         * The base probability of range weapon to hit (for distance 1).
         * 0 - cannot hit ... 1 hits always
         */
        double rangeBasicAim = 0.95;
        
        int maximumAmmo = 15;
        
        /**
         * The ammount of ammo present with the weapon itself {@link  ItemType#RANGED_WEAPON}.
         */
        int ammoInWeapon = 3;        
        
        /**
         * The ammount of ammo in a single ammo pack {@link  ItemType#RANGED_AMMO}.
         */
        int ammoInAmmoPack = 5;
        
        int healthRestoredByHealthPack = 50;
        
        int itemRespawnTime = 10;
        
    }
    
    private static class ScheduledRespawn implements Comparable<ScheduledRespawn> {
        private final long timeStep;
        private final FPS1Square square;

        public ScheduledRespawn(long timeStep, FPS1Square square) {
            this.timeStep = timeStep;
            this.square = square;
        }               

        public FPS1Square getSquare() {
            return square;
        }

        public long getTimeStep() {
            return timeStep;
        }

        
        
        @Override
        public int compareTo(ScheduledRespawn o) {
            if(timeStep < o.timeStep){
                return -1;
            } else if(timeStep > o.timeStep){
                return 1;
            } else {
                return 0;
            }
        }
        
        
    }
}
