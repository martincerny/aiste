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
package cz.cuni.amis.aiste.simulations.keylockmaze;

import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.environment.IEnvironment;
import cz.cuni.amis.aiste.environment.impl.AbstractAgentController;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * <p>Represents spy's brain and takes care of his shortest way to finish.</p>
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public class KeyLockNaiveController extends AbstractAgentController<KeyLockAction, KeyLockMaze> {

	/**
	 * <p>Map of the maze that the spy is in</p>
	 */
	private KeyLockFourWayPoint[][] map;
	
	/**
	 * <p>Array that holds number of visits for each step</p>
	 */
	private int[][] visitCounter;
	
	/**
	 * <p>Property that holds the shortest way from start to actual position</p>
	 */
	private Stack<TupleInt> way;
	
	/**
	 * <p>Actual position of the spy</p>
	 */
	private TupleInt position;
	
	/**
	 * <p>Coordinates of previous position</p>
	 */
	private TupleInt previousStep;
	
	
	/**
	 * <p>All directions of each maze point.</p>
	 */
	public final KeyLockDirection[] directions = {
		KeyLockDirection.NORTH, 
		KeyLockDirection.SOUTH, 
		KeyLockDirection.WEST, 
		KeyLockDirection.EAST
	};

    @Override
    public void init(IEnvironment<KeyLockAction> environment, KeyLockMaze representation, AgentBody body, long stepDelay) {
        super.init(environment, representation, body, stepDelay);
        
		this.map = this.representation.getMap();
		this.visitCounter = new int[this.map.length][this.map[0].length];
		this.way = new Stack<TupleInt>();
		this.position = this.representation.getPosition();
		this.previousStep = new TupleInt(this.position.x(), this.position.y());
    }

    @Override
    public void onSimulationStep(double reward) throws RuntimeException {
        super.onSimulationStep(reward);
		
		// Get actual position
		position = this.representation.getPosition();
		
		// Check finished
		if (this.representation.isFinished()) {
			return;
		}
		
		KeyLockFunctions.printDebugMsg("## Spy is thinking... ##");
		
		// Increase visit counter
		visitCounter[position.x()][position.y()]++;
		
		// Get next step direction
		KeyLockDirection nextStepDirection = getNextStepDirection();
		
        // Do the action (<=> go the computed direction)
        act(new KeyLockAction(nextStepDirection));
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public Class getRepresentationClass() {
        return KeyLockMaze.class;
    }

    @Override
    public String getLoggableRepresentation() {
        return "KeyLockNaive";
    }
	/**
	 * <p>Computes the direction of next step</p>
	 * @return Direction code of next step
	 */
    private KeyLockDirection getNextStepDirection() {
		
		/**********************************
		 * Sort neighbours by visit count *
		 **********************************/
		
		// Prepare list of neighbours
		List<TupleInt> neighbourCoords = new ArrayList<TupleInt>(4);
		
		// For all directions... (<=> for all neighbours...)
		for (int i = 0; i < directions.length; i++) {
			KeyLockDirection direction = directions[i];
			
			// If there is no neighbour in that direction... check another one
			if (map[position.x()][position.y()].isNeighbourSlotFree(direction)) {
				continue;
			}
			
			// Get coords of current neighbour
			TupleInt neighbour = new TupleInt(
					position.x() + direction.getDiffX(), 
					position.y() + direction.getDiffY()
					);
			
			// If coords are not valid on current map... (<= the neighbour is final step)
			if (
					! KeyLockFunctions.isBetweenOrEqual(neighbour.x(), 0, map.length - 1) 
					||
					! KeyLockFunctions.isBetweenOrEqual(neighbour.y(), 0, map[neighbour.x()].length - 1)
				) {
				
				// If the agent can access the finish...
				if (this.representation.canAccess(direction)) {
					// Set finish as the only posibility to continue and stop searching neighbours
					neighbourCoords.clear();
					neighbourCoords.add(neighbour);
					break;
				} else {
					// The agent have to search maze to get the key to the finish
					continue;
				}
			}
			
			// If the neighbour is previous step, don't put it into neighbourCoords
			if (neighbour.compareTo(previousStep) == 0) {
				continue;
			}
			
			// If the spy can't access that neighbour step, don't put it into neighbourCoords
			if ( ! this.representation.canAccess(direction)) {
				continue;
			}
			
			// If neighbours list is empty, jut add current neighbour and continue
			if (neighbourCoords.isEmpty()) {
				neighbourCoords.add(neighbour);
				continue;
			}
			
			// Insert-sort of current neighbour into the neighbours list
			int count = neighbourCoords.size();
			// For all neighbours in the list...
			for (int index = 0; index < count; index++) {
				// Get neighbour with index 'index'
				TupleInt tmp = neighbourCoords.get(index);
				// If the tmp neighbour was visited more times than the current one...
				if (visitCounter[tmp.x()][tmp.y()] > visitCounter[neighbour.x()][neighbour.y()]) {
					// Inser the current before tmp
					neighbourCoords.add(index, neighbour);
					break;
				}
			}
			// If the neighbourCoords list is same size as before the sort, put the neighbour to the end
			if (neighbourCoords.size() == count) {
				neighbourCoords.add(neighbour);
			}
		}
		
		
		/****************************************
		 * Determine the direction of next step *
		 ****************************************/
		
		KeyLockDirection nextStepDirection;
		// If there are neighbours in the list...
		if (neighbourCoords.size() > 0) {
			// Get the least visited step that is on the first position of list
			nextStepDirection = KeyLockFunctions.getDirection(position, neighbourCoords.get(0));
		} else {
			// If the way from start to current step is empty...
			if (way.empty()) {
				// There is something wrong (probably the maze is ilegal).
				throw new RuntimeException("There is no way to go.");
			}
			// Spy will go one step backwards (to the previous position)
			nextStepDirection = KeyLockFunctions.getDirection(position, way.pop());
		}
		
		// Remember this position as previous
		previousStep = position;
		
		// Put step to shortest way
		way.push(position);
		
		return nextStepDirection;
	}


    

}
