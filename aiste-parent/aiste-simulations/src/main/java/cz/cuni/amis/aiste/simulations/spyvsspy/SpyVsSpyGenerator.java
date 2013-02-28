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

import cz.cuni.amis.aiste.AisteException;
import cz.cuni.amis.aiste.IRandomizable;
import cz.cuni.amis.aiste.environment.AgentBody;
import cz.cuni.amis.aiste.simulations.utils.RandomUtils;
import cz.cuni.amis.aiste.simulations.utils.SeedableRandomGraphGenerator;
import cz.cuni.amis.planning4j.ActionDescription;
import cz.cuni.amis.planning4j.IPlanner;
import cz.cuni.amis.planning4j.IPlanningResult;
import cz.cuni.amis.planning4j.pddl.PDDLDomain;
import cz.cuni.amis.planning4j.pddl.PDDLProblem;
import cz.cuni.amis.planning4j.utils.Planning4JUtils;
import java.util.*;
import org.apache.log4j.Logger;
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
public class SpyVsSpyGenerator implements IRandomizable{

    private final Logger logger = Logger.getLogger(SpyVsSpyGenerator.class);
    
    private int maxPlayers;

    private int numNodes;

    private double meanNodeDegree;

    private int numItemTypes;

    private int numTrapTypes;

    private double itemTrappedProbability;

    private IPlanner plannerToTestDomain;
    /**
     * Maximum number of random generator trials, before giving up, if all
     * created levels are unsolvable
     */
    private static final int MAX_GENERATOR_ROUNDS = 10;

    private Random rand;
    
    public SpyVsSpyGenerator(int maxPlayers, int numNodes, double meanNodeDegree, int numItemTypes, int numTrapTypes, double itemTrappedProbability, IPlanner plannerToTestDomain) {
        this.maxPlayers = maxPlayers;
        this.numNodes = numNodes;
        this.meanNodeDegree = meanNodeDegree;
        this.numItemTypes = numItemTypes;
        this.numTrapTypes = numTrapTypes;
        this.itemTrappedProbability = itemTrappedProbability;
        this.plannerToTestDomain = plannerToTestDomain;
        rand = new Random();
    }

    @Override
    public void setRandomSeed(long seed) {
        rand = new Random(seed);
    }
    
    

    public SpyVsSpy generateEnvironment() {

        generateCycle: for (int trial = 0; trial < MAX_GENERATOR_ROUNDS; trial++) {

            //create nodes on a grid
            final List<SpyVsSpyMapNode> nodes = new ArrayList<SpyVsSpyMapNode>();
            Map<Integer, List<Integer>> neighbours = new HashMap<Integer, List<Integer>>();
            
            int gridSize = (int) (Math.sqrt(numNodes) + 1 );
            
            SpyVsSpyMapNode nodesOnGrid[][] = new SpyVsSpyMapNode[gridSize][gridSize];
            
            
            for(int i = 0; i < numNodes; i++){
                int emptySquares = (gridSize * gridSize) - i;
                int nextSquare = rand.nextInt(emptySquares);
                int squareNo = 0;
                
                searchForEmptyCycle:
                for(int x = 0; x < gridSize; x++){
                    for(int y = 0; y < gridSize; y++){
                        if(nodesOnGrid[x][y] == null){
                            if(squareNo == nextSquare){
                                SpyVsSpyMapNode newNode = new SpyVsSpyMapNode(i, numTrapTypes, x, y);
                                nodesOnGrid[x][y] = newNode;
                                nodes.add(newNode);
                                break searchForEmptyCycle;
                            }
                            squareNo++;
                        }
                    }
                }
            }
            
            //generate neighbours
            for(int x = 0; x < gridSize; x++){
                for(int y = 0; y < gridSize; y++){
                    if(nodesOnGrid[x][y] != null){
                        SpyVsSpyMapNode currentNode = nodesOnGrid[x][y];
                        List<Integer> nodeNeighbours = new ArrayList<Integer>();
                        for(int distance = 1; distance < 3; distance++){
                            boolean foundNeighbour = false;
                            if(x - distance >= 0 && nodesOnGrid[x - distance][y] != null){
                                nodeNeighbours.add(nodesOnGrid[x - distance][y].index);
                                foundNeighbour = true;
                            }
                            if(x + distance < gridSize && nodesOnGrid[x + distance][y] != null){
                                nodeNeighbours.add(nodesOnGrid[x + distance][y].index);
                                foundNeighbour = true;
                            }
                            if(y - distance >= 0 && nodesOnGrid[x][y  - distance] != null){
                                nodeNeighbours.add(nodesOnGrid[x][y - distance].index);
                                foundNeighbour = true;
                            }
                            if(y + distance < gridSize && nodesOnGrid[x][y  + distance] != null){
                                nodeNeighbours.add(nodesOnGrid[x][y + distance].index);
                                foundNeighbour = true;
                            }
                            if(foundNeighbour){
                                break;
                            }
                        }
                        neighbours.put(currentNode.index, nodeNeighbours);
                    }
                }
            }
            
            /*
             * Graph generating withou position is obsolete. 
            SeedableRandomGraphGenerator<SpyVsSpyMapNode, Object> graphGenerator = new SeedableRandomGraphGenerator<SpyVsSpyMapNode, Object>(numNodes, (int) ((numNodes * meanNodeDegree) / 2));
            UndirectedGraph<SpyVsSpyMapNode, Object> nodeGraph = new SimpleGraph<SpyVsSpyMapNode, Object>(new EdgeFactory<SpyVsSpyMapNode, Object>() {

                @Override
                public Object createEdge(SpyVsSpyMapNode v, SpyVsSpyMapNode v1) {
                    return new Object();
                }
            });

            final List<SpyVsSpyMapNode> nodes = new ArrayList<SpyVsSpyMapNode>();

            graphGenerator.setRandomSeed(rand.nextLong());            
            graphGenerator.generateGraph(nodeGraph, new VertexFactory<SpyVsSpyMapNode>() {

                @Override
                public SpyVsSpyMapNode createVertex() {
                    SpyVsSpyMapNode newNode = new SpyVsSpyMapNode(nodes.size(), numTrapTypes);
                    nodes.add(newNode);
                    return newNode;
                }
            }, null);

            Map<Integer, List<Integer>> neighbours = new HashMap<Integer, List<Integer>>();
            for (int i = 0; i < numNodes; i++) {
                List<Integer> nodeneighbours = new ArrayList<Integer>();
                for (Object e : nodeGraph.edgesOf(nodes.get(i))) {
                    nodeneighbours.add(nodeGraph.getEdgeTarget(e).getIndex());
                }
                neighbours.put(i, nodeneighbours);
            }
            */            

            List<Integer> startingLocations = new ArrayList<Integer>(RandomUtils.randomSampleOfIntegerRange(0, numNodes, maxPlayers, rand));


            int[] trapsCarriedCounts = new int[numTrapTypes];
            double trapsCarriedP = ((double) (numNodes * numTrapTypes)) / ((double)(numNodes * numTrapTypes) + 10); //the value of p is chosen se that mean number of traps is numNodes / numTrapTypes * 5
            DiscreteDistributionInt trapCountDistribution = new NegativeBinomialDist(2, trapsCarriedP);
            for (int i = 0; i < numTrapTypes; i++) {
                trapsCarriedCounts[i] = trapCountDistribution.inverseFInt(rand.nextDouble());
            }

            int maxTrapsInTheMap[] = new int[numTrapTypes];
            for (int i = 0; i < numTrapTypes; i++) {
                maxTrapsInTheMap[i] = trapsCarriedCounts[i] * maxPlayers;
            }
            //Generate items

            /**
             * the value of p is chosen so that mean number of items is maxPlayers / 2  
             * given r = 2 and desired mean x, we get p = x / (2 - x)
             * our desired mean is (max_players / 2) - 0.5 , so we get:
             */
            double itemP = ((double) maxPlayers - 1) / (double)(maxPlayers + 3); 
            DiscreteDistributionInt itemCountDistribution = new NegativeBinomialDist(2, itemP);


            for (int itemType = 0; itemType < numItemTypes; itemType++) {
                int numItemInstances = Math.min(itemCountDistribution.inverseFInt(rand.nextDouble()) , numNodes);
                if(numItemInstances == 0){
                    numItemInstances = 1; //without a single instance of the item, the map is not solvable
                }
                for (SpyVsSpyMapNode nodeWithItem : RandomUtils.randomSample(nodes, numItemInstances, rand)) {

                    nodeWithItem.getItems().add(itemType);
                    if (rand.nextDouble() < itemTrappedProbability) {
                        int trapType = rand.nextInt(numTrapTypes);
                        if (!nodeWithItem.getTraps().contains(trapType)) {
                            maxTrapsInTheMap[trapType]++;
                            nodeWithItem.getTraps().add(trapType);
                        }
                    }
                }
            }

            //Generate trap removers
            for (int trapRemoverType = 0; trapRemoverType < numTrapTypes; trapRemoverType++) {
                //the parameters are chosen se that mean number of removers is the 0.9 * (maximum number of traps of that type)
                double expectedMean = maxTrapsInTheMap[trapRemoverType] * 0.9;
                double removerP = 0.8;
                DiscreteDistributionInt removerCountDistribution = new BinomialDist((int) Math.floor(expectedMean / removerP) + 1, removerP);

                int numRemoverInstances = Math.min(removerCountDistribution.inverseFInt(rand.nextDouble()), numNodes);
                for (SpyVsSpyMapNode nodeWithItem : RandomUtils.randomSample(nodes, numRemoverInstances, rand)) {
                    nodeWithItem.getNumTrapRemovers()[trapRemoverType]++;
                }
            }



            int destination = rand.nextInt(numNodes);

            /**
             * I create one instance to test (I will need to create a body and mess with it) and another identical to return, if the test is succesful
             */
            SpyVsSpy spyVsSpyToTest = new SpyVsSpy(nodes, maxPlayers, startingLocations, neighbours, numTrapTypes, trapsCarriedCounts, numItemTypes, destination);
            SpyVsSpy spyVsSpyToReturn = new SpyVsSpy(nodes, maxPlayers, startingLocations, neighbours, numTrapTypes, trapsCarriedCounts, numItemTypes, destination);
            
            
            if(plannerToTestDomain != null){
                logger.info("Testing domain with planner: " + plannerToTestDomain);
                spyVsSpyToTest.init();
                SpyVsSpyPDDLRepresentation representation = new SpyVsSpyPDDLRepresentation(spyVsSpyToTest);
                //test for all possible bodies
                for(int player = 0; player < maxPlayers; player++){
                    spyVsSpyToTest.createAgentBody(SpyVsSpyAgentType.getInstance());
                }
                
                for(AgentBody body : spyVsSpyToTest.getActiveBodies()){
                    PDDLDomain domain = representation.getDomain(body);
                    PDDLProblem problem = representation.getProblem(body);
                    IPlanningResult testResult = Planning4JUtils.plan(plannerToTestDomain, domain, problem);
                    if(!testResult.isSuccess()){
                        logger.info("Domain could not be solved for player " + body.getId() + ", generating new one.");
                        continue generateCycle;
                    } else {
                        logger.info("Domain solvable for player " + body.getId() + " in " + testResult.getPlan().size() + " actions.");
                        if(logger.isDebugEnabled()){
                            StringBuilder solvingPlan = new StringBuilder();
                            for(ActionDescription ad: testResult.getPlan()){
                                solvingPlan.append(ad.toString()).append(" ");
                            }
                            logger.debug("Plan to solve: " + solvingPlan.toString());
                        }
                    }
                }
                logger.info("Domain succesfully tested");
            }
            
            if(logger.isDebugEnabled()){
                logger.debug("====== Map ==========");
                for(int i = 0; i < nodes.size(); i++){
                    StringBuilder nodeInfo = new StringBuilder();
                    nodeInfo.append("Node ").append(i).append(" -> ");
                    for(int nodeIndex : neighbours.get(i)){
                        nodeInfo.append(nodeIndex).append(" ");
                    }
                    logger.debug(nodeInfo.toString());
                }
                logger.debug("Destination: " + destination);
                logger.debug("====== Map end ======");
            }
            
            return spyVsSpyToReturn;
        }
        
        throw new AisteException("After " + MAX_GENERATOR_ROUNDS + " trials, no environment created that would be solvable from all starting positions. Bad parameters?");
    }
}
