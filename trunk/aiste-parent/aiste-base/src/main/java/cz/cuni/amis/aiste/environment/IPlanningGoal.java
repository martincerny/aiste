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
package cz.cuni.amis.aiste.environment;

/**
 * A common interface for all objects that represent a planning goal.
 * Implementations need to work well with equals and hashCode! Priority should NOT
 * be included in equals and hashCode, because goals are (usually) equal without respect to their priorities.
 * @author Martin Cerny
 */
public interface IPlanningGoal {
    /**
     * Priority of the goal. If the controller using this representation
     * reasons about goals, it uses priority as its base. Good priority value is
     * close to expected difference between reward received 
     * in current environment run if the goal is achieved and the reward if
     * nothing is done. The priority should take into account possilbe longer term
     * advantage achieved by the goal, that might not directly propagate in short term
     * reward.
     * @return 
     */
    public int getPriority();
}
