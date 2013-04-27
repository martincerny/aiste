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

import cz.cuni.amis.aiste.environment.IAction;

/**
 *
 * @author 
 */
public class SpyVsSpyAction implements IAction{

    public enum ActionType {
        MOVE(true),
        PICKUP_ITEM(true),
        PICKUP_TRAP_REMOVER(true),
        PICKUP_WEAPON(false),
        SET_TRAP(true),
        REMOVE_TRAP(true),
        ATTACK_AGENT(true),        
        NO_OP(false);
        
        boolean requiresTarget;

        private ActionType(boolean requiresTarget) {
            this.requiresTarget = requiresTarget;
        }
        
    }
    
    public static final SpyVsSpyAction NO_OP_ACTION = new SpyVsSpyAction(ActionType.NO_OP, -1);
    public static final SpyVsSpyAction PICKUP_WEAPON_ACTION = new SpyVsSpyAction(ActionType.PICKUP_WEAPON, -1);
    
    private ActionType type;
    
    /**
     * The target of action, i.e. the id of trap to remove, id of room to move to, etc.
     */
    private int actionTarget;

    public SpyVsSpyAction(ActionType type, int actionTarget) {
        this.type = type;
        this.actionTarget = actionTarget;
    }

    public int getActionTarget() {
        return actionTarget;
    }

    public ActionType getType() {
        return type;
    }
    
    
    
    @Override
    public String getLoggableRepresentation() {
        return type + ": " + actionTarget;
    }

    @Override
    public String toString() {
        return getLoggableRepresentation();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SpyVsSpyAction other = (SpyVsSpyAction) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.type.requiresTarget && this.actionTarget != other.actionTarget) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.type != null ? this.type.hashCode() : 0);
        if(this.type.requiresTarget){
            hash = 17 * hash + this.actionTarget;
        }
        return hash;
    }
 
    
}
