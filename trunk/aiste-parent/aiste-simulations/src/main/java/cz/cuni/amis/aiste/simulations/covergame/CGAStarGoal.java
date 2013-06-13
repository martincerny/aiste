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

import cz.cuni.amis.pathfinding.map.IPFGoal;
import cz.cuni.amis.utils.heap.IHeap;
import java.util.Set;

/**
 *
 * @author Martin Cerny
 */
public abstract class CGAStarGoal implements IPFGoal<Loc> {
    private CoverGame env;
    private Loc start;

    public CGAStarGoal(CoverGame env, Loc start) {
        this.env = env;
        this.start = start;
    }

    @Override
    public Loc getStart() {
        return start;
    }

    @Override
    public void setOpenList(IHeap<Loc> openList) {
    }

    @Override
    public void setCloseList(Set<Loc> closedList) {
    }
    
    
    
}
