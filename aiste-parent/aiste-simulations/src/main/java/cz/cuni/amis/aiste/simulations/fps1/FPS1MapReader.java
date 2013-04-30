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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author Martin Cerny
 */
public class FPS1MapReader {
    public static FPS1.StaticDefs readMap(InputStream is) throws IOException {
        FPS1.StaticDefs defs = new FPS1.StaticDefs();
        
        Scanner sc = new Scanner(is);
        defs.maxPlayers = sc.nextInt();
        defs.levelWidth = sc.nextInt();
        defs.levelHeight = sc.nextInt();
        sc.nextLine();
        
        defs.playerSpawningLocations = new ArrayList<Loc>();
        defs.rooms = new ArrayList<FPS1Room>();
        defs.squares = new FPS1Square[defs.levelWidth][defs.levelHeight];
        defs.initialSquareStates = new FPS1SquareState[defs.levelWidth][defs.levelHeight];
        
        Map<Character, FPS1Room> roomCharsToRooms = new HashMap<Character, FPS1Room>();
        
        //----- Read room infos ----
        for(int y = 0; y < defs.levelWidth; y++){
            String line = sc.nextLine();
            for(int x = 0; x < defs.levelHeight; x++){
                char roomChar = line.charAt(x);
                if(roomChar == '#'){
                    continue; //square not passable, it stays null
                }
                else { 
                    FPS1Square sq = new FPS1Square();
                    sq.location = new Loc(x,y);
                    if(roomChar == ' '){
                        //a corridor
                        sq.room = null;
                    } else {
                        FPS1Room room;
                        if(roomCharsToRooms.containsKey(roomChar)){
                            room = roomCharsToRooms.get(roomChar);
                        } else {
                            int roomId = defs.rooms.size();
                            room = new FPS1Room(roomId);
                            defs.rooms.add(room);
                            roomCharsToRooms.put(roomChar, room);
                        }
                        sq.room = room;
                    }
                    defs.squares[x][y] = sq;
                    defs.initialSquareStates[x][y] = new FPS1SquareState();
                }
            }
        }
        
        sc.nextLine();//one line is ignored
        
        //----- Read item infos, spawn places and create initial state ----
        for(int y = 0; y < defs.levelWidth; y++){
            String line = sc.nextLine();
            for(int x = 0; x < defs.levelHeight; x++){
                FPS1.ItemType item = null;                
                char itemChar = line.charAt(x);
                
                switch(itemChar){
                    case 'm' : {
                        item = FPS1.ItemType.MEELEE_WEAPON;
                        break;
                    }
                    case 'r' : {
                        item = FPS1.ItemType.RANGED_WEAPON;
                        break;
                    }
                    case 'a' : {
                        item = FPS1.ItemType.RANGED_AMMO;
                        break;
                    }
                    case 'h' : {
                        item = FPS1.ItemType.MEDIKIT;
                        break;
                    }
                    case 's' : {
                        defs.playerSpawningLocations.add(new Loc(x,y));
                        break;
                    }
                }
                if( (item != null || itemChar == 's') && (defs.squares[x][y] == null || defs.squares[x][y].room == null)){
                    throw new IllegalStateException("Pos:" + x+ "," + y +": Item or spawning point outside of room.");                    
                }
                
                if(item != null){
                    defs.squares[x][y].spawnedItem = item;
                    defs.initialSquareStates[x][y].items.add(item);
                }
            }
        }
        
        return defs;
    }
}
