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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author Martin Cerny
 */
public class CGMapReader {
    public static CoverGame.StaticDefs readMap(InputStream is) throws IOException {
        CoverGame.StaticDefs defs = new CoverGame.StaticDefs();
        
        Scanner sc = new Scanner(is);
        defs.levelWidth = sc.nextInt();
        defs.levelHeight = sc.nextInt();
        sc.nextLine();
        
        defs.playerSpawningLocations = new ArrayList<Loc>();
        defs.squares = new CGSquare[defs.levelWidth][defs.levelHeight];

        defs.navGraph = new HashMap<Loc, List<Loc>>();
        
        Set<Loc> navPoints = new HashSet<Loc>();
        
        //----- Read the map ----
        for(int y = 0; y < defs.levelHeight; y++){
            String line = sc.nextLine();
            for(int x = 0; x < defs.levelWidth; x++){
                CGSquare newSquare = new CGSquare(x,y);
                defs.squares[x][y] = newSquare;
                newSquare.passable = true;
                char squareChar = line.charAt(x);
                boolean isNavPoint = false;
                switch(squareChar){
                    case '#' : {
                        newSquare.passable = false;                            
                        break;
                    }
                    case 'H' : {
                        newSquare.passable = false;
                        newSquare.horizontalCover = true;
                        newSquare.verticalCover = true;
                        break;
                    }
                    case '-' : {
                        newSquare.horizontalCover = true;
                        break;
                    }
                    case '|' : {
                        newSquare.verticalCover = true;
                        break;
                    }                            
                    case '*' : {
                        newSquare.horizontalCover = true;
                        newSquare.verticalCover = true;
                        break;
                    }
                    case 'o' : {
                        isNavPoint = true;
                        break;
                    }
                    case 's' : {
                        defs.playerSpawningLocations.add(new Loc(x,y));
                        isNavPoint = true;
                        break;
                    }
                }
                newSquare.isNavPoint = isNavPoint;
                if(isNavPoint){
                    navPoints.add(new Loc(x,y));
                }
            }
        }
        
        //find navpoints close to covers
        for(int x = 0; x < defs.levelWidth; x++){
            for(int y = 0; y < defs.levelHeight; y++){
                if(defs.squares[x][y].verticalCover){
                    if(x > 0 && defs.squares[x - 1][y].passable){
                        navPoints.add(new Loc(x - 1, y));
                    }
                    if(x < defs.levelWidth - 1 && defs.squares[x + 1][y].passable){
                        navPoints.add(new Loc(x + 1, y));
                    }
                }
                if(defs.squares[x][y].horizontalCover){
                    if(y > 0 && defs.squares[x][y - 1].passable){
                        navPoints.add(new Loc(x, y - 1));
                    }
                    if(y < defs.levelHeight - 1 && defs.squares[x][y + 1].passable){
                        navPoints.add(new Loc(x, y + 1));
                    }
                }
            }
        }
        
        //generate nav graph - which pairs are reachable by single movement
        for(Loc np1 : navPoints){
            //Some navpoints were added - mark them as nav points
            defs.squares[np1.x][np1.y].isNavPoint = true;
            
            List<Loc> neighbours = new ArrayList<Loc>();
            for(Loc np2: navPoints){
                if(!np1.equals(np2) && CGUtils.distance(np1, np2) <= defs.maxDistancePerTurn && CGUtils.isVisible(np1, np2, defs)){
                    neighbours.add(np2);
                }
            }
            defs.navGraph.put(np1, neighbours);
        }
                
        return defs;
    }
}
