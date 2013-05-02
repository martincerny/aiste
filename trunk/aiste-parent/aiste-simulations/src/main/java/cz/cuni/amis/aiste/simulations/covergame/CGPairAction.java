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
package cz.cuni.amis.aiste.simulations.covergame;

import cz.cuni.amis.aiste.environment.IAction;

/**
 *
 * @author Martin Cerny
 */
public class CGPairAction implements IAction {
    private CGAction action1;
    private CGAction action2;

    public CGPairAction(CGAction action1, CGAction action2) {
        this.action1 = action1;
        this.action2 = action2;
    }

    public CGAction getAction1() {
        return action1;
    }

    public CGAction getAction2() {
        return action2;
    }

    @Override
    public String getLoggableRepresentation() {
        return "[" + action1.getLoggableRepresentation() + ", " + action2.getLoggableRepresentation() + "]";
    }
    
    
}
