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

import cz.cuni.amis.aiste.environment.*;
import cz.cuni.amis.aiste.environment.impl.AbstractStateVariableRepresentableSynchronizedEnvironment;
import cz.cuni.amis.aiste.environment.impl.EnumStateVariable;
import cz.cuni.amis.aiste.environment.impl.IntegerStateVariable;
import cz.cuni.amis.aiste.environment.impl.SingletonAgentInstantiationDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

/**
 * <p>Represents maze = array of rooms where each room is array of steps</p>
 * 
 * @author Jaroslav Kubat <jk.kubat@seznam.cz>
 */
public class KeyLockMaze extends AbstractStateVariableRepresentableSynchronizedEnvironment<KeyLockAction> {

	/**
	 * <p>Roles of the MazeStep</p>
	 */
	private enum MazeStepRole {

		/**
		 * <p>Empty step.</p>
		 */
		EMPTY(KeyLockStepReward.emptyStep),
		
		/**
		 * <p>Step containing key.</p>
		 */
		KEY(KeyLockStepReward.keyStep),
		
		/**
		 * <p>Step that contained key, but it's already picked up.</p>
		 */
		KEY_PICKED_UP(KeyLockStepReward.emptyStep),
		
		/**
		 * <p>Start step.</p>
		 */
		START(KeyLockStepReward.startStep),
		
		/**
		 * <p>Finish step.</p>
		 */
		FINISH(KeyLockStepReward.finishStep),
		
		/**
		 * <p>Step containing locked door.</p>
		 */
		DOOR_LOCKED(KeyLockStepReward.doorLockedStep),
		
		/**
		 * <p>Step containing unlocked door.</p>
		 */
		DOOR_UNLOCKED(KeyLockStepReward.doorUnlockedStep);
		
		
		/**
		 * <p>Reward to be given when somebody visits the step.</p>
		 */
		private final Double reward;

		/**
		 * <p>Constructor defining the reward.</p>
		 * @param reward Reward to be given when somebody visits the step
		 */
		private MazeStepRole(Double reward) {
			this.reward = reward;
		}

		/**
		 * Returns reward for visiting step with this role.
		 * @return Reward of this role
		 */
		public Double GetReward() {
			return this.reward;
		}
	}

	/**
	 * <p>Role of the MazeRoom</p>
	 */
	private enum MazeRoomRole {

		/**
		 * <p>Empty room (&lt;=&gt; ! start & ! finish)</p>
		 */
		EMPTY,
		
		/**
		 * <p>Start room</p>
		 */
		START,
		
		/**
		 * <p>Finish room</p>
		 */
		FINISH
	}

	/**
	 * <p>States of inventory items</p>
	 */
	private enum InventoryItemState {

		/**
		 * <p>The item is in inventory</p>
		 */
		HAVE,
		
		/**
		 * <p>The item is NOT in inventory</p>
		 */
		DONT_HAVE
	}

	
	/**
	 * <p>Door states</p>
	 */
	private enum DoorState {

		/**
		 * <p>The door is locked</p>
		 */
		LOCKED,
		
		/**
		 * <p>The door is unlocked</p>
		 */
		UNLOCKED
	}

	/**
	 * <p>Represents single step in the maze.</p>
	 * @see KeyLockFourWayPoint
	 */
	private class MazeStep extends KeyLockFourWayPoint {

		/**
		 * <p>Role of this step</p>
		 */
		private MazeStepRole role;
		
		/**
		 * <p>Key that lays on current step.</p>
		 */
		private MazeKey key = null;

		
		/**
		 * <p>Constructor defining the ID.</p>
		 * @param id ID of this step
		 */
		public MazeStep(int id) {
			super(id);
			this.role = MazeStepRole.EMPTY;
		}

		/**
		 * <p>ID getter</p>
		 * @return ID of this step
		 */
		public int getId() {
			return this.id;
		}

		/**
		 * Role getter
		 * @return {@link MazeStepRole} of this step
		 */
		public MazeStepRole getRole() {
			return this.role;
		}

		/**
		 * <p>Role setter.</p>
		 * @param role {@link MazeStepRole} to be the role of this step
		 */
		public void setRole(MazeStepRole role) {
			this.role = role;
		}

		/**
		 * <p>Performs all necessary actions on key picking up and returns the key.</p>
		 * @return {@link MazeKey} that lied on this step
		 */
		public MazeKey pickUpKey() {
			this.role = MazeStepRole.KEY_PICKED_UP;
			return this.key;
		}
		
		/**
		 * <p>Performs all necessary actions on key laying. This method is "extended key setter".</p>
		 * @param k {@link MazeKey} to be laid down on this step
		 */
		public void layKey(MazeKey k) {
			this.key = k;
			this.setRole(MazeStepRole.KEY);
		}

		/**
		 * <p>Returns {@link MazeStep} that is held in specified direction of this step.</p>
		 * @param directionCode Direction, that we want the step from
		 * @return MazeStep that is held in specified direction
		 */
		@Override
		public MazeStep getNeighbour(KeyLockDirection directionCode) {
			return (MazeStep)super.getNeighbour(directionCode);
		}
	}

	/**
	 * <p>Represents maze step that contains door.</p>
	 * @see MazeStep
	 */
	private class MazeDoor extends MazeStep {

		/**
		 * <p>State of this door.</p>
		 */
		private DoorState state;
		
		/**
		 * <p>Key that can unlock this door.</p>
		 */
		public final MazeKey key;

		/**
		 * <p>Constructor that sets ID, door state and key to unlock this door.</p>
		 * @param id ID of this maze step
		 * @param state {@link DoorState} of this door
		 * @param k {@link MazeKey} that is able to unlock this door
		 */
		public MazeDoor(int id, DoorState state, MazeKey k) {
			super(id);
			
			// Set door state
			this.state = state;
			
			// Determine step role
			MazeStepRole r = 
					(state == DoorState.UNLOCKED)
					? 
					MazeStepRole.DOOR_UNLOCKED : MazeStepRole.DOOR_LOCKED;
			// Set step role
			this.setRole(r);
			
			// Set key
			this.key = k;
		}

		/**
		 * <p>Determines whether this door can be unlocked by any item in the inventory</p>
		 * @param inventory {@link List}&lt;{@link MazeKey}&gt; representing inventory
		 * @return True if this door is already unlocked or if there is at least 
		 * one key in the inventory that is able to unlock this door. Otherwise false.
		 */
		public boolean canBeUnlocked(List<MazeKey> inventory) {
			if (this.state == DoorState.UNLOCKED) {
				return true;
			}
			
			ListIterator<MazeKey> iterator = inventory.listIterator();
			while(iterator.hasNext()) {
				int keyID = iterator.next().id;
				if (keyID == this.key.id) {
					return true;
				}
			}

			return false;
		}

		/**
		 * <p>Performs necessary actions when unlocking this door.</p>
		 */
		public void unlock() {
			this.state = DoorState.UNLOCKED;
			this.setRole(MazeStepRole.DOOR_UNLOCKED);
		}
	}

	/**
	 * <p>The key that can be used to unlock some door in the maze.</p>
	 */
	private class MazeKey {

		/**
		 * <p>ID of this key (must be same as ID of door that this key unlocks)</p>
		 */
		int id;

		/**
		 * Constructor that sets ID.
		 * @param id ID of this key (must be same as ID of door that this key unlocks)
		 */
		public MazeKey(int id) {
			this.id = id;
		}
	}

	/**
	 * <p>Representation of maze room.</p>
	 * @see KeyLockFourWayPoint
	 */
	private class MazeRoom extends KeyLockFourWayPoint {

		/**
		 * <p>Grid of {@link MazeStep}s in this room.</p>
		 */
		private MazeStep[][] steps;
		
		/**
		 * <p>Grid of {@link KeyLockFourWayPoint}s representing map of this room.</p>
		 */
		private KeyLockFourWayPoint[][] map;
		
		/**
		 * <p>Width of this room. (number of steps on x-axis)</p>
		 */
		private int width;
		
		/**
		 * <p>Height of this room. (number of steps on y-axis)</p>
		 */
		private int height;
		
		/**
		 * <p>Role of this room.</p>
		 */
		private MazeRoomRole role;
		
		/**
		 * <p>List of tuples <code>(direction, item)</code> where 
		 * <code>direction</code> is direction code and <code>item</code> is 
		 * tuple <code>(door, step)</code> where <code>door</code> is 
		 * {@link MazeDoor} leading to another {@link MazeRoom} and 
		 * <code>step</code> is {@link MazeStep} on which the door is connected 
		 * to. The <code>direction</code> is direction from <code>step</code> to 
		 * <code>door</code>.</p>
		 */
		private List<TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>>> neighboursWithDoors 
				= new ArrayList<TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>>>(4);
		
		/**
		 * <p>Door from current room indexed by direction they are leading to.</p>
		 */
		private MazeDoor[] doors = new MazeDoor[4];

		/**
		 * <p>Constructor setting ID, width and height of this room.</p>
		 * @param id ID of the room
		 * @param width Width of the room (says how many steps will be the room wide)
		 * @param height Height of the room (says how many steps will be the room high)
		 */
		public MazeRoom(int id, int width, int height) {
			super(id);
			
			this.width = width;
			this.height = height;
			this.role = MazeRoomRole.EMPTY;
		}

		/**
		 * <p>Generates randomly the room. (including the room map)</p>
		 */
		public void generate() {
			// Generate map
			this.map = KeyLockFunctions.generateMazeMap(this.width, this.height, KeyLockMaze.this.rand);
			
			// Create steps
			this.steps = new MazeStep[this.map.length][this.map[0].length];
			for (int i = 0; i < this.steps.length; i++) {
				for (int j = 0; j < this.steps[i].length; j++) {
					this.steps[i][j] = new MazeStep(KeyLockMaze.this.numSteps);
					KeyLockMaze.this.numSteps++;
				}
			}
			
			// Connect steps according to map
			// For all steps...
			for (int i = 0; i < this.steps.length; i++) {
				for (int j = 0; j < this.steps[i].length; j++) {
					// For all directions...
					for (int k = 0; k < directions.length; k++) {
						// If there is neighbour in that direction...
						if (!this.map[i][j].isNeighbourSlotFree(directions[k])) {
							int nextX = i + directions[k].getDiffX();
							int nextY = j + directions[k].getDiffY();
							// If those coordinates are valid...
							if (
									KeyLockFunctions.isBetweenOrEqual(nextX, 0, this.steps.length - 1) &&
									KeyLockFunctions.isBetweenOrEqual(nextY, 0, this.steps[nextX].length - 1)
								) {
								// Connect steps
								this.steps[i][j].setNeighbour(this.steps[nextX][nextY], directions[k]);
							}
						}
					}
				}
			}
		}

		/**
		 * <p>Role setter.</p>
		 * @param r {@link MazeRoomRole} to be the role of this room
		 */
		public void setRole(MazeRoomRole r) {
			this.role = r;
		}
		
		/**
		 * <p>Role getter.</p>
		 * @return {@link MazeRoomRole} of this room
		 */
		public MazeRoomRole getRole() {
			return this.role;
		}

		/**
		 * <p>NeighboursWithDoors getter</p>
		 * @return List of tuples <code>(direction, (door, item))</code>
		 * @see #neighboursWithDoors
		 */
		public List<TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>>> getNeighboursWithDoors() {
			return this.neighboursWithDoors;
		}
		
		/**
		 * <p>Connects door to current room in specified direction on random position on the edge.</p>
		 * @param door {@link MazeDoor} to be connected to current room
		 * @param direction Index of direction in which the door will be connected (north = 0, south = 1, west = 2, east = 3)
		 * @param randCoords Random coordinates
		 */
		public void connectDoor(MazeDoor door, KeyLockDirection direction, TupleInt randCoords) {
			this.connectDoor(door, direction, randCoords, true);
		}
		
		/**
		 * <p>Connects door to current room in specified direction on random position on the edge.</p>
		 * @param door {@link MazeDoor} to be connected to current room
		 * @param direction Index of direction in which the door will be connected (north = 0, south = 1, west = 2, east = 3)
		 * @param randCoords Random coordinates
		 * @param isWithNeighbour Is there a neighbour behind that door?
		 */
		public void connectDoor(MazeDoor door, KeyLockDirection direction, TupleInt randCoords, boolean isWithNeighbour) {
			int coordX = 0;
			int coordY = 0;
			
			switch (direction.getCode()) {
				case 0:// north => x = random & y = max
					coordX = Math.min(randCoords.x(), this.width - 1);
					coordY = this.height - 1;
					break;
				case 1://south => x = random & y = 0
					coordX = Math.min(randCoords.x(), this.width - 1);
					coordY = 0;
					break;
				case 2://west => x = 0 & y = random
					coordX = 0;
					coordY = Math.min(randCoords.y(), this.height - 1);
					break;
				case 3://east => x = max & y = random
					coordX = this.width - 1;
					coordY = Math.min(randCoords.y(), this.height - 1);
			}
			
			// Set the neighbour
			this.steps[coordX][coordY].setNeighbour(door, direction);
			
			// Add item to neighboursWithDoors if there is supposed to be neighbour behind the door.
			if (isWithNeighbour) {
				this.neighboursWithDoors.add(
						new TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>>(
							direction, 
							new TupleComparable<MazeDoor, MazeStep>(
								door, 
								this.steps[coordX][coordY])
						)
					);
			}
			
			this.doors[direction.getCode()] = door;
		}
		
		/**
		 * <p>Returns {@link MazeStep} on specified coordinates.</p>
		 * @param coordX X-axis coordinate of requested step
		 * @param coordY Y-axis coordinate of requested step
		 * @return MazeStep that is on specified coordinates in this room
		 */
		public MazeStep getStepOn(int coordX, int coordY) {
			// Check coords
			if (!KeyLockFunctions.isBetweenOrEqual(coordX, 0, this.width - 1)) { coordX = 0; }
			if (!KeyLockFunctions.isBetweenOrEqual(coordY, 0, this.height - 1)) { coordY = 0; }
			// Return the step
			return this.steps[coordX][coordY];
		}
		
		/**
		 * <p>Returns all keys that are needed to unlock all doors of this room.</p>
		 * @return {@link List}&lt;{@link MazeKey}&gt; containing keys
		 */
		public List<MazeKey> getKeys() {
			List<MazeKey> keysList = new ArrayList<MazeKey>(4);
			// For each door in this room...
			for (MazeDoor d : this.doors) {
				if (d != null) {
					keysList.add(d.key);
				}
			}
			return keysList;
		}
		
		/**
		 * <p>Lays the key on random position in this room.</p>
		 * @param key The {@link MazeKey} to be laid.
		 * @param rand {@link Random} generator
		 * @exception Exception When the key can not be put in the room.
		 */
		public void layKey(MazeKey key, Random rand) throws Exception {
			// Array of coord tuples
			TupleInt[] coords = new TupleInt[this.width * this.height];
			// Array of indexes of 'coords'
			int[] coordsIndexes = new int[coords.length];
			
			// Fill those arrays
			int xx = 0;
			int yy = 0;
			for (int i = 0; i < coords.length; i++) {
				coords[i] = new TupleInt(xx, yy);
				coordsIndexes[i] = i;
				xx++;
				if (xx >= this.width) {
					yy++;
					xx = 0;
				}
			}
			
			// Randomly sort coordsIndexes
			coordsIndexes = KeyLockFunctions.randomSort(coordsIndexes, rand);
			
			// Get random coords
			int index = 0;
			int x = coords[coordsIndexes[index]].x();
			int y = coords[coordsIndexes[index]].y();
			
			// Avoid overwriting already present keys
			while(this.steps[x][y].getRole() != MazeStepRole.EMPTY) {
				index++;
				// If the index is out of range...
				if (! KeyLockFunctions.isBetweenOrEqual(index, 0, coordsIndexes.length - 1)) {
					throw new Exception("There is no place for the key.");
				}
				
				// Get next set of coords
				x = coords[coordsIndexes[index]].x();
				y = coords[coordsIndexes[index]].y();
			}
			
			// Lay down the key
			this.steps[x][y].layKey(key);
		}
		
		/**
		 * <p>Returns string representation of this room.</p>
		 * @return 
		 */
		public String print() {
			String output = "";
			String eol = System.getProperty("line.separator");
			if (this.north != null) { output += "N "; }
			if (this.south != null) { output += "S "; }
			if (this.west != null) { output += "W "; }
			if (this.east != null) { output += "E "; }
			output += eol + eol;
			for (int j = 0; j < this.steps[0].length; j++) {
				for (int i = 0; i < this.steps.length; i++) {
					if (this.steps[i][j].north != null) { output += "N"; }
					else {output += "_"; }
					if (this.steps[i][j].south != null) { output += "S"; }
					else {output += "_"; }
					if (this.steps[i][j].west != null) { output += "W"; }
					else {output += "_"; }
					if (this.steps[i][j].east != null) { output += "E"; }
					else {output += "_"; }
					output += " ";
				}
				output += eol;
			}
			
			return output;
		}
	}
	
	/**
	 * <p>X-axis coordinate of player's position</p>
	 */
	private IntegerStateVariable playerPositionVariableX;
	
	/**
	 * <p>Y-axis coordinate of player's position</p>
	 */
	private IntegerStateVariable playerPositionVariableY;
	
	/**
	 * <p>Array of items that can be in players inventory</p>
	 */
	private EnumStateVariable[] playerInventoryVariable;
	
	/**
	 * <p>Array of maze doors</p>
	 */
	private EnumStateVariable[] doorsVariable;
	
	/**
	 * <p>List of doors in this maze</p>
	 */
	private List<MazeDoor> doors;
	
	/**
	 * <p>List of keys to doors in this maze</p>
	 */
	private List<MazeKey> keys;
	
	/**
	 * <p>List of keys that player has in his inventory</p>
	 */
	private List<MazeKey> keysInInventory;
	
	/**
	 * <p>Grid of maze rooms = actual maze</p>
	 */
	private MazeRoom[][] maze;
	
	/**
	 * <p>Map of this maze (map of rooms)</p>
	 */
	private KeyLockFourWayPoint[][] map;
	
	/**
	 * <p>Complete map of this maze (map of all steps of all rooms)</p>
	 */
	private KeyLockFourWayPoint[][] mapCompleteWithoutDoors;
	
	/**
	 * <p>Start step</p>
	 */
	private MazeStep start;
	
	/**
	 * <p>Finish step</p>
	 */
	private MazeStep finish;
	
	/**
	 * <p>Coordinates of start room</p>
	 */
	private TupleInt startRoomCoords;
	
	/**
	 * <p>Coordinates of start step in start room</p>
	 */
	private TupleInt startStepCoords;
	
	/**
	 * <p>Coordinates of finish room</p>
	 */
	private TupleInt finishRoomCoords;
	
	/**
	 * <p>Options of this maze.</p>
	 */
	private KeyLockOptions options;
	
	/**
	 * <p>Number of steps in this maze (&lt;=&gt; smallest available step ID)</p>
	 */
	int numSteps = 0;
	
	/**
	 * <p>Number of doors in this maze (&lt;=&gt; smallest available door and key ID)</p>
	 */
	int numDoors = 0;
	
	/**
	 * <p>Number of rooms in this maze (&lt;=&gt; smallest available room ID)</p>
	 */
	int numRooms = 0;
	
	/**
	 * <p>Random generator</p>
	 */
	public final Random rand;
	
	/**
	 * <p>All directions of each maze point.</p>
	 */
	public final KeyLockDirection[] directions = {
		KeyLockDirection.NORTH, 
		KeyLockDirection.SOUTH, 
		KeyLockDirection.WEST, 
		KeyLockDirection.EAST
	};
	
	/**
	 * <p>The agent's body</p>
	 */
	AgentBody theBody;

	public KeyLockMaze(KeyLockOptions options) {
		super(KeyLockAction.class);
		
		KeyLockFunctions.printDebugMsg("Maze initialization...");
		
		this.options = options;
		
		// This seed is generated just because of knowing which seed was used 
		// when the seed is supposed to be random.
		long seed = (new Random()).nextLong();
		
		// Determine whether use seed defined in options or randomly generated seed.
		this.rand = (this.options.randomSeed == null) ? new Random(seed) : new Random(this.options.randomSeed);
		
		// Print the seed to output if in DEBUG.
		if (this.options.randomSeed == null) {
			KeyLockFunctions.printDebugMsg("Random seed = " + seed);
		} else {
			KeyLockFunctions.printDebugMsg("Random seed = " + this.options.randomSeed);
		}
		KeyLockFunctions.printDebugMsg("");
		
		// Initialize door and key lists
		this.doors = new ArrayList<MazeDoor>(this.options.roomsWide * this.options.roomsHigh);
		this.keys = new ArrayList<MazeKey>(this.options.roomsWide * this.options.roomsHigh);
		this.keysInInventory = new ArrayList<MazeKey>(this.options.roomsWide * this.options.roomsHigh);
	}

	@Override
	protected Map<AgentBody, Double> nextStepInternal(Map<AgentBody, KeyLockAction> actionsToPerform) {
		//krok simulace vraci reward, ktery dostali agenti za provedene akce
		//v nasem pripade je reward +100 za dojiti do cile oponenta, jinak -1 za kazdy krok (aby to motivovalo k rychlemu reseni)
		Map<AgentBody, Double> result = new HashMap<AgentBody, Double>();
		
		// If there is no action for current body...
		if (!actionsToPerform.containsKey(theBody)) {
			result.put(theBody, 0.0);
			return result;
		}
		
		KeyLockFunctions.printDebugMsg("## Spy is performing the action... ##");
		
		KeyLockAction action = actionsToPerform.get(theBody);
		Double reward = 0.0;
		
		
		Integer x = (Integer)getStateVariableValue(playerPositionVariableX);
		Integer y = (Integer)getStateVariableValue(playerPositionVariableY);
		KeyLockDirection direction = action.getDirection();
		
		TupleInt roomCoord = new TupleInt(
				x / this.options.roomWidth, 
				y / this.options.roomHeight
				);
		TupleInt stepCoord = new TupleInt(
				x % this.options.roomWidth, 
				y % this.options.roomHeight
				);
		
		MazeStep step = 
				this.maze[roomCoord.x()][roomCoord.y()]
				.getStepOn(stepCoord.x(), stepCoord.y())
				.getNeighbour(direction);
		
		if (this.canAccess(direction)) {
			// Let the spy go to next step
			TupleInt coords = new TupleInt(
					x + direction.getDiffX(), 
					y + direction.getDiffY()
					);
			
			setStateVariableValue(playerPositionVariableX, coords.x());
			setStateVariableValue(playerPositionVariableY, coords.y());
			
			KeyLockFunctions.printDebugMsg("Gone to: (" + coords.x() + ", " + coords.y() + ")");
			
			MazeStepRole role = step.getRole();
			
			// Get the reward
			reward = role.GetReward();
			
			// Unlock the door (if any)
			if (role == MazeStepRole.DOOR_LOCKED) {
				MazeDoor door = (MazeDoor) step;
				// If the spy has the key to this door...
				if (door.canBeUnlocked(this.keysInInventory)) {
					door.unlock();
					setStateVariableValue(doorsVariable[door.getId()], DoorState.UNLOCKED);
					// Review the reward
					reward = door.getRole().GetReward();
					KeyLockFunctions.printDebugMsg("\tUnlocked door id=" + door.getId());
				} else {
					KeyLockFunctions.printDebugMsg("\tUnable to unlock door id=" + door.getId());
				}
			}
			
			if (role == MazeStepRole.DOOR_UNLOCKED) {
				KeyLockFunctions.printDebugMsg("\tDoor id=" + step.getId() + " already unlocked");
			}

			// Pick up the key (if any)
			if (role == MazeStepRole.KEY) {
				MazeKey key = step.pickUpKey();
				this.keysInInventory.add(key);
				setStateVariableValue(playerInventoryVariable[key.id], InventoryItemState.HAVE);
				KeyLockFunctions.printDebugMsg("\tPicked up key id=" + key.id);
			}
			// Check finish
			if (role == MazeStepRole.FINISH) {
				setFinished(true);
				KeyLockFunctions.printDebugMsg("\tFINISH");
			}
		}
		
		result.put(theBody, reward);
		KeyLockFunctions.printDebugMsg("\tGot reward " + reward);
		KeyLockFunctions.printDebugMsg("");
		return result;
	}

	@Override
	protected AgentBody createAgentBodyInternal(IAgentType type) {
		if (type != KeyLockAgentType.getInstance()) {
			throw new AgentInstantiationException("Illegal agent type");
		}
		theBody = new AgentBody(0, type);
		return theBody;
	}

	@Override
	public Map<? extends IAgentType, ? extends IAgentInstantiationDescriptor> getInstantiationDescriptors() {
		return Collections.singletonMap(KeyLockAgentType.getInstance(), new SingletonAgentInstantiationDescriptor());
	}
	
	/**
	 * <p>Full map getter.</p>
	 * @return Grid of {@link KeyLockFourWayPoint} representing map of all steps in the maze
	 */
	public KeyLockFourWayPoint[][] getMap() {
		return this.mapCompleteWithoutDoors;
	}
	
	/**
	 * <p>Player position getter</p>
	 * @return Returns {@link TupleInt} representing coords of the player on the full map
	 */
	public TupleInt getPosition() {
		Integer x = (Integer)getStateVariableValue(playerPositionVariableX);
		Integer y = (Integer)getStateVariableValue(playerPositionVariableY);
		return new TupleInt(x, y);
	}
	
	/**
	 * <p>Determines whether the player is able to access step laying in given direction from his current position.</p>
	 * @param direction Direction code of direction to access
	 * @return True if the requested step is empty step, unlocked door or locked door to which the player has key
	 */
	public boolean canAccess(KeyLockDirection direction) {
		// Get actual position
		Integer x = (Integer)getStateVariableValue(playerPositionVariableX);
		Integer y = (Integer)getStateVariableValue(playerPositionVariableY);
		
		// Convert position to room and step coordinates
		TupleInt fromRoom = new TupleInt(
				x / this.options.roomWidth, 
				y / this.options.roomHeight
				);
		TupleInt fromStep = new TupleInt(
				x % this.options.roomWidth, 
				y % this.options.roomHeight
				);
		
		// Get steps
		MazeStep from = this.maze[fromRoom.x()][fromRoom.y()].getStepOn(fromStep.x(), fromStep.y());
		MazeStep to = from.getNeighbour(direction);
		
		// If there is no neighbour in specified direction, return false.
		if (to == null) {
			return false;
		}
		
		KeyLockFunctions.printDebugMsg("\tTrying to access direction '" + direction + "'");
		
		// If the step is "locked door"...
		if (to.getRole() == MazeStepRole.DOOR_LOCKED) {
			MazeDoor door = (MazeDoor) to;
			KeyLockFunctions.printDebugMsg("\t\t=> Locked door id=" + door.getId());
			// If the spy has the key to this door...
			if (door.canBeUnlocked(this.keysInInventory)) {
				KeyLockFunctions.printDebugMsg("\t\t\tCan be unlocked");
				KeyLockFunctions.printDebugMsg("");
				return true;
			} else {
				KeyLockFunctions.printDebugMsg("\t\t\tCan NOT be unlocked");
				KeyLockFunctions.printDebugMsg("");
				return false;
			}
		}
		
		KeyLockFunctions.printDebugMsg("\t=> Direction can be accessed");
		KeyLockFunctions.printDebugMsg("");
		return true;
	}
	
	/**
	 * <p>Generates maze and prepares it for spy.</p>
	 * @param preparedMap Map to be used when generating the maze. (If null => randomly generated map is used)
	 */
	public void generateMaze(KeyLockFourWayPoint[][] preparedMap) {
		
		KeyLockFunctions.printDebugMsg("");
		KeyLockFunctions.printDebugMsg("Generating maze...");

		// Get map
		if (preparedMap == null) {
			// Generate map
			this.map = KeyLockFunctions.generateMazeMap(this.options.roomsWide, this.options.roomsHigh, rand);
		} else {
			this.map = preparedMap;
		}
		
		
		// Try to generate maze
		try {
			generateMaze(rand);
		} catch (Exception e) {
			System.err.println("Maze can't be generated because of exception with message: '" + e.getMessage() + "'");
			System.exit(1);
		}
		
		KeyLockFunctions.printDebugMsg("");
		KeyLockFunctions.printDebugMsg("Creating maze full map...");
		
		generateCompleteMap();
		
		KeyLockFunctions.printDebugMsg("");
		KeyLockFunctions.printDebugMsg("Initializing state variables...");
		
		// Init state variables
		playerInventoryVariable = new EnumStateVariable[this.numDoors];
		doorsVariable = new EnumStateVariable[this.numDoors];
		
		// Put all keys to the player's inventory with state "dont't have"
		for (int i = 0; i < playerInventoryVariable.length; i++) {
			MazeKey key = keys.get(i);
			playerInventoryVariable[key.id] = new EnumStateVariable("key:" + key.id, InventoryItemState.class);
			addStateVariable(playerInventoryVariable[key.id]);
			setStateVariableValue(playerInventoryVariable[key.id], InventoryItemState.DONT_HAVE);
		}
		
		// Register all doors to the state variables with state matching their inner state
		for (int i = 0; i < doorsVariable.length; i++) {
			MazeDoor d = doors.get(i);
			doorsVariable[d.id] = new EnumStateVariable("door:" + d.id, DoorState.class);
			addStateVariable(doorsVariable[d.id]);
			setStateVariableValue(doorsVariable[d.id], d.state);
		}
		
		// Register player's position
		playerPositionVariableX = new IntegerStateVariable("playerPosition", 0, this.options.roomsWide * this.options.roomWidth);
		addStateVariable(playerPositionVariableX);
		setStateVariableValue(
				playerPositionVariableX, 
				this.startRoomCoords.x() * this.options.roomWidth + this.startStepCoords.x()
				);

		playerPositionVariableY = new IntegerStateVariable("playerPosition", 0, this.options.roomsHigh * this.options.roomHeight);
		addStateVariable(playerPositionVariableY);
		setStateVariableValue(
				playerPositionVariableY, 
				this.startRoomCoords.y() * this.options.roomHeight + this.startStepCoords.y()
				);
		
		// Register representation
		this.registerRepresentation(this);
		
		KeyLockFunctions.printDebugMsg("");
		KeyLockFunctions.printDebugMsg("Maze done.");
		KeyLockFunctions.printDebugMsg("");
		KeyLockFunctions.printDebugMsg("###########################################");
		KeyLockFunctions.printDebugMsg("");
		
		KeyLockFunctions.printDebugMsg(
				"Player starting at (" 
				+ getStateVariableValue(playerPositionVariableX) 
				+ ", "
				+ getStateVariableValue(playerPositionVariableY)
				+ ")"
				);
		KeyLockFunctions.printDebugMsg("");
	}
	
	/**
	 * <p>Generates whole maze.</p>
	 * @param rand {@link Random} generator
	 * @throws Exception When there occured problem in maze generation.
	 */
	private void generateMaze(Random rand) throws Exception {
		
		/***************
		 * Build rooms *
		 ***************/
		
		this.maze = new MazeRoom[this.options.roomsWide][this.options.roomsHigh];
		for (int x = 0; x < this.maze.length; x++) {
			this.maze[x] = new MazeRoom[this.options.roomsHigh];
			for (int y = 0; y < this.maze[x].length; y++) {
				int id = this.map[x][y].id;
				this.maze[x][y] = new MazeRoom(id, this.options.roomWidth, this.options.roomHeight);
				this.maze[x][y].generate();
				this.numRooms++;
			}
		}
		
		
		/**************************
		 * Connect rooms by doors *
		 **************************/
		
		for (int x = 0; x < this.maze.length; x++) {
			for (int y = 0; y < this.maze[x].length; y++) {
				MazeRoom currentRoom = this.maze[x][y];

				// For all non-diagonal neighbours of 'currentRoom'...
				for (int n = 0; n < directions.length; n++) {
					int coordX = x + directions[n].getDiffX();
					int coordY = y + directions[n].getDiffY();

					if (// If coords are not valid => skip
							!KeyLockFunctions.isBetweenOrEqual(coordX, 0, this.options.roomsWide - 1)
							|| !KeyLockFunctions.isBetweenOrEqual(coordY, 0, this.options.roomsHigh - 1)) {
						continue;
					}

					// Get neighbour
					MazeRoom neighbour = this.maze[coordX][coordY];

					// If there is no wall between currentRoom and neighbour
					KeyLockDirection dir = this.map[x][y].hasNeighbourInDirection(this.map[coordX][coordY]);
					if (dir != null && currentRoom.isNeighbourSlotFree(dir)) {
						// Count index for door at neighbour
						KeyLockDirection oppositeDir = KeyLockDirection.NORTH;
						switch (dir.getCode()) {
							case 0:
								oppositeDir = KeyLockDirection.SOUTH;
								break;
							case 1:
								oppositeDir = KeyLockDirection.NORTH;
								break;
							case 2:
								oppositeDir = KeyLockDirection.EAST;
								break;
							case 3:
								oppositeDir = KeyLockDirection.WEST;
								break;
						}
						// Connect currentRoom with neighbour by new door
						this.connectRooms(currentRoom, dir, neighbour, oppositeDir);
					}
				}
			}
		}

		
		/************************
		 * Set start and finish *
		 ************************/
		
		// Set start step
		this.startRoomCoords = new TupleInt(0, this.rand.nextInt(this.options.roomsHigh));
		MazeRoom room = this.maze[this.startRoomCoords.x()][this.startRoomCoords.y()];
		this.startStepCoords = new TupleInt(0, this.rand.nextInt(room.height));
		this.start = room.getStepOn(startStepCoords.x(), startStepCoords.y());
		this.start.setRole(MazeStepRole.START);
		
		// Make last door
		// Determine if last door will be locked or not
		DoorState doorState = lockedOrUnlocked();
		// Make a key to it
		MazeKey doorKey = new MazeKey(this.numDoors);
		// Make last door
		MazeDoor lastDoor = new MazeDoor(this.numDoors, doorState, doorKey);
		this.numDoors++;
		this.doors.add(lastDoor);
		this.keys.add(doorKey);
		
		// Pick randomly last room and connect last door to it
		this.finishRoomCoords = new TupleInt(this.options.roomsWide - 1, this.rand.nextInt(this.options.roomsHigh));
		room = this.maze[this.finishRoomCoords.x()][this.finishRoomCoords.y()];
		TupleInt randCoords = new TupleInt(
				this.rand.nextInt(room.width), 
				this.rand.nextInt(room.height));
		room.connectDoor(lastDoor, KeyLockDirection.EAST, randCoords, false);
		
		// Set last door step as finish step
		this.finish = lastDoor;
		this.finish.setRole(MazeStepRole.FINISH);

		
		/********************************************************
		 * Lay keys to rooms on random way from start to finish *
		 ********************************************************/
		
		// Initialize
		Stack<TupleInt> roomStack = new Stack<TupleInt>();
		Stack<MazeKey> keysStack = new Stack<MazeKey>();
		boolean[][] visited = new boolean[this.options.roomsWide][this.options.roomsHigh];
		
		// Insert startRoom into stack
		roomStack.push(startRoomCoords);
		keysStack.addAll(this.maze[startRoomCoords.x()][startRoomCoords.y()].getKeys());
		
		// While there is room that has not been visited...
		while(!roomStack.empty()) {
			TupleInt coord = roomStack.pop();
			MazeRoom current = this.maze[coord.x()][coord.y()];
			visited[coord.x()][coord.y()] = true;
			
			// Get all room's neighbours with doors to them
			List<TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>>> neighboursWithDoors = current.getNeighboursWithDoors();
			
			// Remove those, that were visited
			ListIterator<TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>>> iterator = neighboursWithDoors.listIterator();
			while(iterator.hasNext()) {
				KeyLockDirection direction = iterator.next().x;
				int x = coord.x() + direction.getDiffX();
				int y = coord.y() + direction.getDiffY();
				if (visited[x][y]) {
					iterator.remove();
				}
			}
			if (neighboursWithDoors.isEmpty()) {
				if (!keysStack.empty()) {
					current.layKey(keysStack.pop(), rand);
				}
				continue;
			}
			
			// Randomly sort them
			List<TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>>> neighboursWithDoorsRandom = 
					new ArrayList<TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>>>(neighboursWithDoors.size());
			
			int[] indexes = new int[neighboursWithDoors.size()];
			for (int i = 0; i < indexes.length; i++) {
				indexes[i] = i;
			}
			
			indexes = KeyLockFunctions.randomSort(indexes, rand);
			
			for (int i = 0; i < indexes.length; i++) {
				neighboursWithDoorsRandom.add(neighboursWithDoors.get(indexes[i]));
			}
			
			// Add neighbours and keys to stacks and lay down the key for the right door
			for (int i = neighboursWithDoorsRandom.size() - 1; i >= 0; i--) {
				TupleComparable<KeyLockDirection, TupleComparable<MazeDoor, MazeStep>> tuple = neighboursWithDoorsRandom.get(i);
				KeyLockDirection direction = tuple.x;
				roomStack.push(new TupleInt(
						coord.x() + direction.getDiffX(), 
						coord.y() + direction.getDiffY()
						));
				if (i == 0) {
					current.layKey(tuple.y.x.key, rand);
				} else {
					keysStack.push(tuple.y.x.key);
				}
			}
		}
	}
	
	/**
	 * <p>Takes care about connection of two rooms (including door creation).</p>
	 * @param room1 First room
	 * @param direction1 Direction code of direction from the first room to the second one
	 * @param room2 Second room
	 * @param direction2 Direction code of direction from the second room to the first one
	 */
	private void connectRooms(MazeRoom room1, KeyLockDirection direction1, MazeRoom room2, KeyLockDirection direction2) {
		// Determine if current door will be locked or not
		DoorState doorState = lockedOrUnlocked();
		// Make a key to it
		MazeKey doorKey = new MazeKey(this.numDoors);
		// Make a door
		MazeDoor door = new MazeDoor(this.numDoors, doorState, doorKey);
		this.numDoors++;
		this.doors.add(door);
		this.keys.add(doorKey);
		
		TupleInt randCoords = new TupleInt(
				this.rand.nextInt(Math.min(room1.width, room2.width)), 
				this.rand.nextInt(Math.min(room1.height, room2.height)));
		
		// Connect door to first room
		room1.connectDoor(door, direction1, randCoords);
		// Connect door to second room
		room2.connectDoor(door, direction2, randCoords);
		// Set rooms as neighbours
		room1.setNeighbour(room2, direction1);
	}
	
	/**
	 * <p>Determines if next door will be locked or not.</p>
	 * @return {@link DoorState} that is expected to be set to newly created door
	 * @see KeyLockOptions#lockedRatio
	 * @see KeyLockOptions#lockedRatioMax
	 */
	private DoorState lockedOrUnlocked() {
		if (this.rand.nextInt(this.options.lockedRatioMax) < this.options.lockedRatio) {
			return DoorState.LOCKED;
		}
		return DoorState.UNLOCKED;
	}
	
	/**
	 * <p>Generates map of all steps of all rooms in this maze</p>
	 */
	private void generateCompleteMap() {
		this.mapCompleteWithoutDoors = 
				new KeyLockFourWayPoint
						[this.options.roomsWide * this.options.roomWidth]
						[this.options.roomsHigh * this.options.roomHeight];
		
		
		int id = 0;
		// For all rooms in maze...
		for (int xRoom = 0; xRoom < this.options.roomsWide; xRoom++) {
			for (int yRoom = 0; yRoom < this.options.roomsHigh; yRoom++) {
				// For all steps in each room...
				for (int xStep = 0; xStep < this.options.roomWidth; xStep++) {
					for (int yStep = 0; yStep < this.options.roomHeight; yStep++) {
						int x = xRoom * this.options.roomWidth + xStep;
						int y = yRoom * this.options.roomHeight + yStep;
						this.mapCompleteWithoutDoors[x][y] = new KeyLockFourWayPoint(id);
						id++;
					}
				}
			}
		}
		
		// For all rooms in maze...
		for (int xRoom = 0; xRoom < this.options.roomsWide; xRoom++) {
			for (int yRoom = 0; yRoom < this.options.roomsHigh; yRoom++) {
				// For all steps in each room...
				for (int xStep = 0; xStep < this.options.roomWidth; xStep++) {
					for (int yStep = 0; yStep < this.options.roomHeight; yStep++) {
						// Count coords
						int x = xRoom * this.options.roomWidth + xStep;
						int y = yRoom * this.options.roomHeight + yStep;
						
						// For all directions of that step...
						for (KeyLockDirection direction : directions) {
							// If the step does'n have neighbour in current direction => continue
							if (this.maze[xRoom][yRoom]  // Room in maze
									.steps[xStep][yStep] // Step in room
									.isNeighbourSlotFree(direction)
									) {
								continue;
							}
							
							// Otherwise => 
							KeyLockFourWayPoint neighbour = 
									this.maze[xRoom][yRoom]  // Room in maze
									.steps[xStep][yStep] // Step in room
									.getNeighbour(direction);
							
							this.mapCompleteWithoutDoors[x][y].setNeighbour(neighbour, direction);
						}
					}
				}
			}
		}
	}
	
	/**
	 * <p>Prints string representation of this maze to the 'OutFile.txt'.</p>
	 */
	public final void print() {
		try {
			PrintStream out = new PrintStream(new FileOutputStream("OutFile.txt"));
			
			for (int i = 0; i < this.maze.length; i++) {
				for (int j = 0; j < this.maze[i].length; j++) {
					out.println("Room ID: " + this.maze[i][j].id);
					out.println(this.maze[i][j].print());
					out.println("----------------------------------------------------------------");
				}
			}

			out.close();

		} catch (FileNotFoundException e) {
			System.err.println("Print failed with message: " + e.getMessage());
		}
	}
        
    @Override
    public String getLoggableRepresentation() {
        return "Default";
    }
        
}