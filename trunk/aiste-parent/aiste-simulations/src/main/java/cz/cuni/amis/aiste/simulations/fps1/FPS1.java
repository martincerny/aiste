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

import cz.cuni.amis.aiste.IRandomizable;
import cz.cuni.amis.aiste.environment.*;
import cz.cuni.amis.aiste.environment.impl.AbstractSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
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
    }

    
    
    
    
    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, FPS1Action> actionsToPerform) {
        
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
                    //if(body.)
                }
            }
        }
        
        //assess rewards for all deaths and respawn agents               

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
    
    
    @Override
    protected AgentBody createAgentBodyInternal(IAgentType type) {
        if(type != FPS1AgentType.getInstance()){
            throw new AgentInstantiationException("Illegal agent type");
        }
        AgentBody newBody = new AgentBody(bodyInfos.size() /*nove id v rade*/, type);
        bodyInfos.add(new FPS1BodyInfo(newBody));
        
        //cokoliv dalsiho potrebujes, dopis sem (a smaz tento bordel :-)
        if(true){            
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
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
        
        int x;
        int y;
        
        int health;
        int ammo;
        boolean hasRangedWeapon;
        boolean hasMeleeWeapon;
        
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
