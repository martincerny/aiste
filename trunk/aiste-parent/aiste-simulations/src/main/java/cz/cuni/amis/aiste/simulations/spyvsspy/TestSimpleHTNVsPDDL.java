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

import JSHOP2.JSHOP2;
import JSHOP2.Predicate;
import JSHOP2.State;
import JSHOP2.TaskAtom;
import JSHOP2.TaskList;
import cz.cuni.amis.aiste.SimpleTest;
import cz.cuni.amis.aiste.environment.impl.JShop2Utils;
import cz.cuni.amis.experiments.impl.CSVLoggingOutput;
import cz.cuni.amis.experiments.impl.LoggingHeaders;
import cz.cuni.amis.planning4j.IPDDLFileDomainProvider;
import cz.cuni.amis.planning4j.IPDDLObjectProblemProvider;
import cz.cuni.amis.planning4j.IPlanner;
import cz.cuni.amis.planning4j.IPlanningResult;
import cz.cuni.amis.planning4j.external.ExternalPlanner;
import cz.cuni.amis.planning4j.external.impl.itsimple.ItSimplePlannerExecutor;
import cz.cuni.amis.planning4j.external.impl.itsimple.ItSimplePlannerInformation;
import cz.cuni.amis.planning4j.external.plannerspack.PlannersPackUtils;
import cz.cuni.amis.planning4j.impl.PDDLFileDomainProvider;
import cz.cuni.amis.planning4j.impl.PDDLObjectProblemProvider;
import cz.cuni.amis.planning4j.pddl.PDDLObjectInstance;
import cz.cuni.amis.planning4j.pddl.PDDLProblem;
import cz.cuni.amis.planning4j.pddl.PDDLType;
import cz.cuni.amis.planning4j.utils.Planning4JUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Martin Cerny
 */
public class TestSimpleHTNVsPDDL {

    public static void main(String args[]) throws IOException {
        JSHOP2 jshop = new JSHOP2();
        SimpleTest testJShopDomain = new SimpleTest(jshop);

        CSVLoggingOutput jshopOutput = new CSVLoggingOutput(new File("jshop.csv"));
        jshopOutput.init(new LoggingHeaders("Size", "Repetition", "Success",  "Total", "Init", "Creation"));

        CSVLoggingOutput pddlOutput = new CSVLoggingOutput(new File("pddl.csv"));
        pddlOutput.init(new LoggingHeaders("Size", "Repetition", "Success", "Total", "Creation"));

        ItSimplePlannerInformation plannerInfo = PlannersPackUtils.getMetricFF();
        PlannersPackUtils.getPlannerListManager().extractAndPreparePlanner(plannerInfo);
        IPlanner<IPDDLFileDomainProvider, IPDDLObjectProblemProvider> planner = Planning4JUtils.getTranslatingPlanner(new ExternalPlanner(new ItSimplePlannerExecutor(plannerInfo)), IPDDLFileDomainProvider.class, IPDDLObjectProblemProvider.class);
        IPDDLFileDomainProvider pddlDomain = new PDDLFileDomainProvider(new File("classes/testDomain.pddl"));

        try {
            for (int num_positions = 100; num_positions < 5000; num_positions += 100) {
                for (int repetition = 0; repetition < 1; repetition++) {
                    //measure JSHOP
                    long startTimeJSHOP = System.currentTimeMillis();
                    jshop.initialize(testJShopDomain, num_positions * 2 + 20);
                    long initTimeJSHOP = System.currentTimeMillis() - startTimeJSHOP;
                    State initState = new State(testJShopDomain);
                    for (int i = 0; i < num_positions - 1; i++) {
                        initState.add(new Predicate(SimpleTest.CONST_ADJACENT, 0, JShop2Utils.createTermList(jshop, i, i + 1)));
                        initState.add(new Predicate(SimpleTest.CONST_ADJACENT, 0, JShop2Utils.createTermList(jshop, i + 1, i)));

                        initState.add(new Predicate(SimpleTest.CONST_VALUE_AT, 0, JShop2Utils.createTermList(jshop, i, num_positions + i)));
                    }
                    initState.add(new Predicate(SimpleTest.CONST_EMPTY, 0, JShop2Utils.createTermList(jshop, num_positions - 1)));
                    jshop.setState(initState);
                    TaskList task = new TaskList(new TaskAtom(new Predicate(SimpleTest.METHOD_MAKE_EMPTY, 0, JShop2Utils.createTermList(jshop, 0)), false, false));

                    long problemCreationTimeJSHOP = System.currentTimeMillis() - startTimeJSHOP - initTimeJSHOP;
                    LinkedList plansJSHOP = jshop.findPlans(task, 1);

                    long totalSolvingTimeJSHOP = System.currentTimeMillis() - startTimeJSHOP;
                    
                    boolean successJShop = !plansJSHOP.isEmpty();

                    jshopOutput.logData(Arrays.asList(new Object[]{num_positions, repetition, successJShop,  totalSolvingTimeJSHOP, initTimeJSHOP, problemCreationTimeJSHOP}));

                    //measure PDDL
                    long startTimePDDL = System.currentTimeMillis();
                    PDDLProblem problem = new PDDLProblem("testProblem", "TestDomain");

                    PDDLType locationType = new PDDLType("location");
                    PDDLType valueType = new PDDLType("value");

                    List<String> initialLiterals = new ArrayList<String>();
                    for (int i = 0; i < num_positions - 1; i++) {
                        PDDLObjectInstance locationInstance = new PDDLObjectInstance("loc_" + i, locationType);
                        problem.addObject(locationInstance);

                        PDDLObjectInstance valueInstance = new PDDLObjectInstance("val_" + i, valueType);
                        problem.addObject(valueInstance);
                        initialLiterals.add("value_at " + locationInstance.getNameForPDDL() + " " + valueInstance.getNameForPDDL() + "");
                        initialLiterals.add("adjacent " + locationInstance.getNameForPDDL() + " loc_" + (i + 1) + "");
                        initialLiterals.add("adjacent loc_" + (i + 1) + " " + locationInstance.getNameForPDDL() + "");
                    }
                    initialLiterals.add("empty loc_" + (num_positions - 1));
                    problem.addObject(new PDDLObjectInstance("loc_" + (num_positions - 1), locationType));
                    problem.setInitialLiterals(initialLiterals);
                    problem.setGoalCondition("empty loc_0");

                    long problemCreationTimePDDL = System.currentTimeMillis() - startTimePDDL;
                    IPlanningResult planPDDL = planner.plan(pddlDomain, new PDDLObjectProblemProvider(problem));

                    long totalSolvingTimePDDL = System.currentTimeMillis() - startTimePDDL;

                    pddlOutput.logData(Arrays.asList(new Object[]{num_positions, repetition,planPDDL.isSuccess(), totalSolvingTimePDDL, problemCreationTimePDDL}));

                }
            }

        }
        finally {
            jshopOutput.close();
            pddlOutput.close();
        }
    }
}
