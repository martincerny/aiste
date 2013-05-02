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
     * Visibility is checked by Bresenham's algorithm, taken from http://tech-algorithm.com/articles/drawing-line-using-bresenham-algorithm/
     * @param from
     * @param to
     * @return 
     */
    public static boolean isVisible(Loc from, Loc to, CGSquare[][] squares){
        for(Loc pointOnLine : getPointsOnLine(from, to)){
            if(squares[pointOnLine.x][pointOnLine.y] == null){
                return false;
            }             
        }
        return true;
    }    
    
}
