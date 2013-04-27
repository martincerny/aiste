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

package cz.cuni.amis.aiste.simulations.utils;

import java.util.*;

/**
 * Utils for working with randomness
 * @author Martin Cerny
 */
public class RandomUtils {

        /**
         * Code adapted from http://eyalsch.wordpress.com/2010/04/01/random-sample/ - Floyd algorithm
         * @param <T>
         * @param items
         * @param m
         * @return 
         */
        public static <T> Set<T> randomSample(List<T> items, int m, Random random) {
            HashSet<T> res = new HashSet<T>(m);
            randomSample(items, m, random, res);            
            return res;
        }    
        
        /**
         * Code adapted from http://eyalsch.wordpress.com/2010/04/01/random-sample/ - Floyd algorithm
         * @param <T>
         * @param items
         * @param m
         * @param random Random generator to use
         * @param target collection to add the subset. The collection will be repeatedly queried m-times whether
         * it contains an element, so it should do that quickly
         * @return 
         */
        public static <T> void randomSample(List<T> items, int m, Random random, Collection<T> target) {
            if(m < 0){
                throw new IllegalArgumentException("Sample size must be positive");
            }
            if(items.size() < m){
                throw new IllegalArgumentException("Sample size must less than population size");                
            }
            int n = items.size();
            for (int i = n - m; i < n; i++) {
                int pos = random.nextInt(i + 1);
                T item = items.get(pos);
                if (target.contains(item)) {
                    target.add(items.get(i));
                } else {
                    target.add(item);
                }
            }
        }    


        /**
         * Code adapted from http://eyalsch.wordpress.com/2010/04/01/random-sample/ - Floyd algorithm
         * @param start start of the integer range
         * @param end end of the integer range
         * @param m subset size
         * @param random random generator to use
         * @return 
         */
        public static Set<Integer> randomSampleOfIntegerRange(int start,  int end, int m, Random random) {
            if(m < 0){
                throw new IllegalArgumentException("Sample size must be positive");
            }
            if(end - start < m){
                throw new IllegalArgumentException("Sample size must less than range size");                
            }
            Set<Integer> target = new HashSet<Integer>();
            int n = end - start;
            for (int i = n - m; i < n; i++) {
                int pos = random.nextInt(i + 1);
                int item = start + pos;
                if (target.contains(item)) {
                    target.add(start + i);
                } else {
                    target.add(item);
                }
            }
            return target;
        }    
        
        /**
         * Gets a random element of a collection that does not provide random access 
         *(i.e. the elements need to be traversed).
         * @param <T>
         * @param collection
         * @return 
         */
        public static <T> T randomElementLinearAccess(Collection<T> collection, Random random){
            int index = random.nextInt(collection.size());
            Iterator<T> it = collection.iterator();
            for(int i = 0; i < index; i++){
                it.next();
            }
            return it.next();
        }
        
}
