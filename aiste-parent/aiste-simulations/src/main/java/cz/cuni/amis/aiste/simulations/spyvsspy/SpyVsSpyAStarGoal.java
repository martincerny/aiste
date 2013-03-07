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

import cz.cuni.amis.pathfinding.map.IPFGoal;
import cz.cuni.amis.utils.heap.IHeap;
import java.util.Set;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyAStarGoal implements IPFGoal<Integer> {
    private int start;
    private int goal;
    private SpyVsSpy environment;
    SpyVsSpyMapNode goalNode;

    public SpyVsSpyAStarGoal(int start, int goal, SpyVsSpy environment) {
        this.start = start;
        this.goal = goal;
        this.environment = environment;
        goalNode = environment.nodes.get(goal);
    }

    @Override
    public Integer getStart() {
        return start;
    }

    @Override
    public boolean isGoalReached(Integer node) {
        return node == goal;
    }

    @Override
    public int getEstimatedCostToGoal(Integer node) {
        SpyVsSpyMapNode n = environment.nodes.get(node);
        int xDist = Math.abs(n.posX - goalNode.posX);
        int yDist = Math.abs(n.posY - goalNode.posY);
        return xDist + yDist;
    }

    @Override
    public void setOpenList(IHeap<Integer> iheap) {
    }

    @Override
    public void setCloseList(Set<Integer> set) {
    }
    
}
