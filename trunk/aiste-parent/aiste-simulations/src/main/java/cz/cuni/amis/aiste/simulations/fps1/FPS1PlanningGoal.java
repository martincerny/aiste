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

package cz.cuni.amis.aiste.simulations.fps1;

import cz.cuni.amis.aiste.environment.IPlanningGoal;
import cz.cuni.amis.aiste.environment.impl.AbstractPlanningGoal;

/**
 *
 * @author Martin Cerny
 */
public class FPS1PlanningGoal extends AbstractPlanningGoal {
    public enum Type {WOUND_OPONENT, PICKUP_MEDIKIT};

    private Type type;

    private boolean riskAllowed;

    public FPS1PlanningGoal(Type type, boolean riskAllowed, int priority) {
        super(priority);
        this.type = type;
        this.riskAllowed = riskAllowed;
    }

    public boolean isRiskAllowed() {
        return riskAllowed;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FPS1PlanningGoal other = (FPS1PlanningGoal) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.riskAllowed != other.riskAllowed) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 73 * hash + (this.riskAllowed ? 1 : 0);
        return hash;
    }
    
    
    
}
