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

import cz.cuni.amis.aiste.environment.*;
import cz.cuni.amis.aiste.environment.impl.AbstractSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.AgentInstantiationDescriptor;
import cz.cuni.amis.aiste.environment.impl.EnumStateVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    
    private final StaticDefs defs;
    
    /**
     * Informace o jednotlivych agentech. Indexem je id {@link AgentBody#id}
     */
    private List<FPS1BodyInfo> bodyInfos;
    
    public FPS1(StaticDefs defs) {
        super(FPS1Action.class);
        /* Create defs - debug*/
        this.defs = defs;
        
        /* Create empty agent data*/
        bodyInfos = new ArrayList<FPS1BodyInfo>();
    }
    
    
    
    @Override
    protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, FPS1Action> actionsToPerform) {
        
        // informace o prostredi dostanu od agenta takto:
        //bodyInfos.get(body.getId())
        
        //krok simulace vraci reward, ktery dostali agenti za provedene akce
        //v nasem pripade je reward +1 za zabiti oponenta, jinak 0 (tj. klasicky frag count)
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
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
}
