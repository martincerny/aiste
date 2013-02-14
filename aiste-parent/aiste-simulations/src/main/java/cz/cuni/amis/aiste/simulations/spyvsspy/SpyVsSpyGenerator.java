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

import cz.cuni.amis.aiste.simulations.spyvsspy.SpyVsSpy.MapNode;
import cz.cuni.amis.aiste.simulations.utils.RandomUtils;
import java.util.*;
import org.jgrapht.EdgeFactory;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.RandomGraphGenerator;
import org.jgrapht.graph.SimpleGraph;
import umontreal.iro.lecuyer.probdist.BinomialDist;
import umontreal.iro.lecuyer.probdist.DiscreteDistributionInt;
import umontreal.iro.lecuyer.probdist.NegativeBinomialDist;

/**
 *
 * @author Martin Cerny
 */
public class SpyVsSpyGenerator {
    private int maxPlayers;
    private int numNodes;
    private double meanNodeDegree;
    private int numItemTypes;
    private int numTrapTypes;

    public SpyVsSpyGenerator(int maxPlayers, int numNodes, double meanNodeDegree, int numItemTypes, int numTrapTypes) {
        this.maxPlayers = maxPlayers;
        this.numNodes = numNodes;
        this.meanNodeDegree = meanNodeDegree;
        this.numItemTypes = numItemTypes;
        this.numTrapTypes = numTrapTypes;
    }

    
    
    public SpyVsSpy generateEnvironment(){
        Random random = new Random();
        
        
        RandomGraphGenerator<SpyVsSpy.MapNode, Object> graphGenerator = new RandomGraphGenerator<SpyVsSpy.MapNode, Object>(numNodes, (int)(numNodes * meanNodeDegree));
        UndirectedGraph<SpyVsSpy.MapNode, Object> nodeGraph = new SimpleGraph<SpyVsSpy.MapNode, Object>(new EdgeFactory<SpyVsSpy.MapNode, Object>(){

            @Override
            public Object createEdge(MapNode v, MapNode v1) {
                return new Object();
            }
            
        });

        final List<SpyVsSpy.MapNode> nodes = new ArrayList<SpyVsSpy.MapNode>();
        
        graphGenerator.generateGraph(nodeGraph, new VertexFactory<MapNode>(){

            @Override
            public MapNode createVertex() {
                MapNode newNode = new MapNode(nodes.size(), numTrapTypes);
                nodes.add(newNode);
                return newNode;
            }
            
        },null);
        
        Map<Integer, List<Integer>> neighbours = new HashMap<Integer, List<Integer>>();
        for(int i = 0; i < numNodes; i++){
            List<Integer> nodeNeighbours = new ArrayList<Integer>();
            for(Object e : nodeGraph.edgesOf(nodes.get(i))){
                nodeNeighbours.add(nodeGraph.getEdgeTarget(e).getIndex());
            }
            neighbours.put(i, nodeNeighbours);
        }
        //id                     traps                   items               trap removers
/*        nodes.add(new SpyVsSpy.MapNode(0, Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET, numTrapTypes));
        nodes.add(new SpyVsSpy.MapNode(1, Collections.EMPTY_SET, Collections.EMPTY_SET, Collections.EMPTY_SET, numTrapTypes));
        nodes.add(new SpyVsSpy.MapNode(2, Collections.singleton(0), Collections.singleton(0), Collections.singleton(1), numTrapTypes));
        nodes.add(new SpyVsSpy.MapNode(3, Collections.EMPTY_SET, Collections.singleton(1), Collections.singleton(0), numTrapTypes));
*/
        
        List<Integer> startingLocations = new ArrayList<Integer>(RandomUtils.randomSampleOfIntegerRange(0, numNodes, maxPlayers, random));

        
        int[] trapsCarriedCounts = new int[numTrapTypes];
        double trapsCarriedP = ((double)(numNodes * numTrapTypes)) / ((numNodes  * numTrapTypes) + 10); //the value of p is chosen se that mean number of traps is numNodes / numTrapTypes * 5
        DiscreteDistributionInt trapCountDistribution = new NegativeBinomialDist(2, trapsCarriedP);
        for(int i = 0; i < numTrapTypes; i++){
            trapsCarriedCounts[i] = trapCountDistribution.inverseFInt(random.nextDouble());
        }
        
        int maxTrapsInTheMap[] = new int[numTrapTypes];
        for(int i = 0; i < numTrapTypes; i++){
            maxTrapsInTheMap[i] = trapsCarriedCounts[i] * maxPlayers;
        }
        //Generate items
        
        double itemP = ((double)maxPlayers) / (maxPlayers + 4); //the value of p is chosen se that mean number of items is maxPlayers / 2
        DiscreteDistributionInt itemCountDistribution = new NegativeBinomialDist(2, itemP);
        double itemTrappedProbability = 0.5;
        
        
        for(int itemType = 0; itemType < numItemTypes; itemType++){
            int numItemInstances = Math.min(itemCountDistribution.inverseFInt(random.nextDouble()), numNodes);
            for(MapNode nodeWithItem: RandomUtils.randomSample(nodes, numItemInstances, random)){
                
                nodeWithItem.getItems().add(itemType);
                if(random.nextDouble() < itemTrappedProbability){
                    int trapType = random.nextInt(numTrapTypes);
                    if(!nodeWithItem.getTraps().contains(trapType)){
                        maxTrapsInTheMap[trapType]++;
                        nodeWithItem.getTraps().add(trapType);
                    }
                }
            }
        }
        
        //Generate trap removers
        for(int trapRemoverType = 0; trapRemoverType < numTrapTypes; trapRemoverType++){
            //the parameters are chosen se that mean number of removers is the 0.9 * (maximum number of traps of that type)
            double expectedMean = maxTrapsInTheMap[trapRemoverType] * 0.9;
            double removerP = 0.8;
            DiscreteDistributionInt removerCountDistribution = new BinomialDist((int)Math.floor(expectedMean / removerP) + 1, removerP);
            
            int numRemoverInstances = Math.min(removerCountDistribution.inverseFInt(random.nextDouble()), numNodes);
            for(MapNode nodeWithItem: RandomUtils.randomSample(nodes, numRemoverInstances, random)){                
                nodeWithItem.getNumTrapRemovers()[trapRemoverType]++;
            }
        }
        

        
        int destination = random.nextInt(numNodes);

        SpyVsSpy spyVsSpy = new SpyVsSpy(nodes, maxPlayers, startingLocations, neighbours, numTrapTypes, trapsCarriedCounts, numItemTypes, destination);
        
        return spyVsSpy;
    }
}
