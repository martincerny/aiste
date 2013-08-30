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

import cz.cuni.amis.aiste.simulations.fps1.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class CGUtils {

    /**
     * Distance between to points. As of now, this is standard euclidean
     * distance
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public static double distance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public static double distance(Loc l1, Loc l2)            {
        return distance(l1.x, l1.y, l2.x, l2.y);
    }
    
    /**
     * Finds points on a line by Bresenham's algorithm, taken from http://tech-algorithm.com/articles/drawing-line-using-bresenham-algorithm/
     * @param from
     * @param to
     * @return 
     */
    public static List<Loc> getPointsOnLine(Loc from, Loc to){
        List<Loc> points = new ArrayList<Loc>();
        int x = from.x;
        int x2 = to.x;
        int y = from.y;
        int y2 = to.y;
        
        int w = x2 - x;
        int h = y2 - y;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if (w < 0) {
            dx1 = -1;
        } else if (w > 0) {
            dx1 = 1;
        }
        if (h < 0) {
            dy1 = -1;
        } else if (h > 0) {
            dy1 = 1;
        }
        if (w < 0) {
            dx2 = -1;
        } else if (w > 0) {
            dx2 = 1;
        }
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0) {
                dy2 = -1;
            } else if (h > 0) {
                dy2 = 1;
            }
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++) {
            points.add(new Loc(x,y));
            numerator += shortest;
            if (!(numerator < longest)) {
                numerator -= longest;
                x += dx1;
                y += dy1;
            } else {
                x += dx2;
                y += dy2;
            }
        }
        
        return points;
    }    
    
    /**
     * Direct visibility is checked by Bresenham's algorithm, taken from http://tech-algorithm.com/articles/drawing-line-using-bresenham-algorithm/
     * @param from
     * @param to
     * @return 
     */
    public static boolean isDirectlyVisible(Loc from, Loc to, CGSquare[][] squares){
        for(Loc pointOnLine : getPointsOnLine(from, to)){
            if(!squares[pointOnLine.x][pointOnLine.y].passable){
                return false;
            }             
        }
        return true;
    }    

    /**
     * Direct visibility implies visibility, but a place is considered visible also if it is directly visible
     * from any of the neighbouring traversable squares.
     * @param from
     * @param to
     * @param squares
     * @return 
     */
    public static boolean isVisible(Loc from, Loc to, CoverGame.StaticDefs defs){
        if(isDirectlyVisible(from, to, defs.squares)){
            return true;
        }
        
        for(CGSquare sq: getNeighbouringSquares(from, defs)){
            if(sq.passable && isDirectlyVisible(sq.loc, to, defs.squares)){
                return true;
            }
        }
        
        return false;
    }    
    
    public static List<CGSquare> getNeighbouringSquares(Loc l, CoverGame.StaticDefs defs){
        List<CGSquare> neighbours = new ArrayList<CGSquare>(4);
        if(l.x > 0 && defs.squares[l.x - 1][l.y].passable){
            neighbours.add(defs.squares[l.x - 1][l.y]);
        }
        if(l.y > 0 && defs.squares[l.x][l.y - 1].passable){
            neighbours.add(defs.squares[l.x][l.y - 1]);
        }
        if(l.x < defs.levelWidth - 1 && defs.squares[l.x + 1][l.y].passable){
            neighbours.add(defs.squares[l.x + 1][l.y]);
        }
        if(l.y < defs.levelHeight - 1 && defs.squares[l.x][l.y + 1].passable){
            neighbours.add(defs.squares[l.x][l.y + 1]);
        }
        return neighbours;
    }
    
    public static boolean isThereNeighbouringCover(Loc l, CoverGame.StaticDefs defs){
        for(CGSquare sq : getNeighbouringSquares(l, defs)){
            if(sq.horizontalCover || sq.verticalCover){
                return true;
            }
        } 
        return false;
    }
    
}
