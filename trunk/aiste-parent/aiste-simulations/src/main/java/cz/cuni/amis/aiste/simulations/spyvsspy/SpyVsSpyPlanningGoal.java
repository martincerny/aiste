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
package cz.cuni.amis.aiste.simulations.spyvsspy;

import cz.cuni.amis.aiste.environment.impl.AbstractPlanningGoal;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyPlanningGoal extends AbstractPlanningGoal{
    public enum Type {DIRECT_WIN, KILL_OPONENT}
    
    private Type type;
    private int parameter;

    public SpyVsSpyPlanningGoal(Type type, int priority) {
        this(type, -1, priority);
    }

    
    
    public SpyVsSpyPlanningGoal(Type type, int parameter, int priority) {
        super(priority);
        this.type = type;
        this.parameter = parameter;
    }

    public Type getType() {
        return type;
    }

    public int getParameter() {
        return parameter;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 89 * hash + this.parameter;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SpyVsSpyPlanningGoal other = (SpyVsSpyPlanningGoal) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.parameter != other.parameter) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SpyVsSpyPlanningGoal{" + "type=" + type + ", parameter=" + parameter + '}';
    }

    
    
}
