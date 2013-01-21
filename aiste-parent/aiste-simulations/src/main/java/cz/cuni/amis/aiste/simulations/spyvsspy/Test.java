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
package cz.cuni.amis.aiste.simulations.spyvsspy;

import cz.cuni.amis.planning4j.ActionDescription;
import cz.cuni.amis.planning4j.IPlanner;
import cz.cuni.amis.planning4j.IPlanningResult;
import cz.cuni.amis.planning4j.external.ExternalPlanner;
import cz.cuni.amis.planning4j.external.impl.itsimple.ItSimplePlannerExecutor;
import cz.cuni.amis.planning4j.external.impl.itsimple.ItSimplePlannerInformation;
import cz.cuni.amis.planning4j.external.impl.itsimple.PlannerListManager;
import cz.cuni.amis.planning4j.external.plannerspack.PlannersPackUtils;
import cz.cuni.amis.planning4j.impl.PDDLStringDomainProvider;
import cz.cuni.amis.planning4j.impl.PDDLStringProblemProvider;
import cz.cuni.amis.planning4j.pddl.PDDLRequirement;
import cz.cuni.amis.planning4j.utils.Planning4JUtils;
import java.io.File;

/**
 *
 * @author Martin Cerny
 */
public class Test {

    public static void main(String args[]) {
        PlannerListManager plannerManager = PlannersPackUtils.getPlannerListManager();
        
        ItSimplePlannerInformation info = plannerManager.suggestPlanners(PDDLRequirement.FLUENTS).get(0);
        
        File plannersDirectory = new File("target");
        //The planner is extracted (only if it does not exist yet) and exec permissions are set under Linux
        plannerManager.extractAndPreparePlanner(plannersDirectory, info);

        IPlanner planner = new ExternalPlanner(new ItSimplePlannerExecutor(info,plannersDirectory));  
        
         IPlanningResult result = Planning4JUtils.plan(planner, 
/*                new PDDLStringDomainProvider("(define (domain jug-pouring)"
                + "(:requirements :typing :fluents)"
                + "(:types jug)"
                + "(:functors"
                + "(amount ?j - jug)"
                + "(capacity ?j - jug)"
                + "- (fluent number))"
                + "(:action empty"
                + ":parameters (?jug1 ?jug2 - jug)"
                + ":precondition (fluent-test"
                + "(>= (- (capacity ?jug2) (amount ?jug2))"
                + "(amount ?jug1)))"
                + ":effect (and (change (amount ?jug1)"
                + "0)"
                + "(change (amount ?jug2)"
                + "(+ (amount ?jug2)"
                + "(amount ?jug1)))))"
                + ")"),*/
                new PDDLStringDomainProvider("(define (domain jug-pouring)"
                + "(:requirements :typing :fluents)"
                + "(:types jug)"
                + "(:functions"
                + "     (amount ?j - jug)"
                + "     (capacity ?j - jug)"
                + ")"
                + "(:action empty"
                + "     :parameters (?jug1 ?jug2 - jug)"
                + ":precondition "
                + "(>= (- (capacity ?jug2) (amount ?jug2))"
                + "(amount ?jug1))"
                + ":effect (and (assign (amount ?jug1)"
                + "0)"
                + "(assign (amount ?jug2)"
                + ""
                + "(amount ?jug1))))"
                + ")"),
                new PDDLStringProblemProvider("(define (problem jug-test)"
                + "(:domain jug-pouring)"
                + "(:objects j - jug)"
                + "(:init "
                 + "(= (amount j) 10)"
                 + "(= (capacity j) 30)"
                 + ")"
                + "(:goal (= (amount j) 0)))")
                );
        
            if (!result.isSuccess()) {
                System.out.println("No solution found.");
                return;
            } else {
                System.out.println("Found solution. The plan is:");
                for (ActionDescription action : result.getPlan()) {
                    System.out.println(action.getName());
                }
            }

        /*
        SpyVsSpy b = new SpyVsSpy();
        SpyVsSpyReactiveController player1 = new SpyVsSpyReactiveController();
        SpyVsSpyReactiveController player2 = new SpyVsSpyReactiveController();

        SynchronuousEnvironmentExecutor executor = new SynchronuousEnvironmentExecutor();
        executor.setEnvironment(b);
        executor.addAgentController(SpyVsSpyAgentType.getInstance(), player1);
        executor.addAgentController(SpyVsSpyAgentType.getInstance(), player2);

        IEnvironmentExecutionResult result = executor.executeEnvironment();

        System.out.println("Results: ");
        System.out.println("Player1: " + result.getAgentResults().get(0).getTotalReward());
        System.out.println("Player2: "+ result.getAgentResults().get(1).getTotalReward());
        */ 
    }
}
