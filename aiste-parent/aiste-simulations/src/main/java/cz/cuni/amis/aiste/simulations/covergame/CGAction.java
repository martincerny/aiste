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

import cz.cuni.amis.aiste.environment.IAction;

/**
 *
 * @author 
 */
public class CGAction implements IAction{

    public enum Action {MOVE, SHOOT, SUPRESS, TAKE_FULL_COVER, NO_OP};
    
    /**
     * Static instance for parameterless actions
     */
    public static final CGAction NO_OP_ACTION = new CGAction(Action.NO_OP, null);
    public static final CGAction TAKE_FULL_COVER_ACTION = new CGAction(Action.TAKE_FULL_COVER, null);
            
    Action act;
    
    
    /**
     * The target of the action - a {@link Loc} object for MOVE or oponnent id for attack actions, take full cover
     * action takes no parameters
     */
    Object target;

    public CGAction(Action act, Object target) {
        this.act = act;
        this.target = target;
    }
    
    
    
    @Override
    public String getLoggableRepresentation() {
        if(target != null){
            return act.toString() + "_" + target.toString();
        } else {
            return act.toString();
        }
    }
    
}
