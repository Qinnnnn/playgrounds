package playground.gregor.misanthrope.benchmark;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Provider;

import playground.gregor.misanthrope.events.JumpEventCSVWriter;
import playground.gregor.misanthrope.events.JumpEventHandler;
import playground.gregor.misanthrope.router.CTRoutingModule;
import playground.gregor.misanthrope.run.CTRunner;
import playground.gregor.misanthrope.simulation.CTMobsimFactory;
import playground.gregor.misanthrope.simulation.physics.CTCell;
import playground.gregor.misanthrope.simulation.physics.CTLink;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;

/**
 * Created by laemmel on 20/10/15.
 */
public class DifferentCellSizes {

    private static final double WIDTH = 20;
    private static double AGENTS_LR = 10000;
    private static double AGENTS_RL = 0;
    private static double INV_INFLOW = 0.25;
    private static double LL = 500;

    public static void main(String[] args) throws IOException {
        CTRunner.DEBUG = false;

        List<String> resH = new ArrayList<>();
        List<String> resL = new ArrayList<>();
        resH.add("#width in cells, a , max inflow, avg 1, var 1, avg 2, var 2");
        List<String> texRes = new ArrayList<>();
        for (int widthInCells = 2; widthInCells <= 10; widthInCells++) {
            CTLink.DESIRED_WIDTH_IN_CELLS = widthInCells;
            INV_INFLOW = 0.25; //30 min
            TravelTimeObserver obs = new TravelTimeObserver();
            for (int i = 0; i < 1; i++) {
                runAll(obs);
            }
            resL.add(widthInCells + "," + getA(widthInCells) + "," + INV_INFLOW + "," + obs.getV1().getMean() + "," + obs.getV1().getVar() + "," + obs.getV2().getMean() + "," + obs.getV2().getVar());
            texRes.add("low&" + getA(widthInCells) + "&" + obs.getV1().getMean() + "&" + obs.getV1().getVar() + "&" + obs.getV2().getMean() + "&" + obs.getV2().getVar() + "\\\\");

            INV_INFLOW = 1. / 10000; //max
            obs = new TravelTimeObserver();
            for (int i = 0; i < 1; i++) {
                runAll(obs);
            }
            resH.add(widthInCells + "," + getA(widthInCells) + "," + INV_INFLOW + "," + obs.getV1().getMean() + "," + obs.getV1().getVar() + "," + obs.getV2().getMean() + "," + obs.getV2().getVar());
            resH.forEach(System.out::println);
            texRes.add("high&" + getA(widthInCells) + "&" + obs.getV1().getMean() + "&" + obs.getV1().getVar() + "&" + obs.getV2().getMean() + "&" + obs.getV2().getVar() + "\\\\");
        }

        {

            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/laemmel/scenarios/misanthrope/paper/different_cell_sizes_uni_h")));
            resH.forEach(System.out::println);
            resH.forEach(s -> {
                try {
                    bw.append(s).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            bw.close();
        }
        {

            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/laemmel/scenarios/misanthrope/paper/different_cell_sizes_uni_l")));
            resL.forEach(System.out::println);
            resL.forEach(s -> {
                try {
                    bw.append(s).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            bw.close();
        }
    }

    private static double getA(int widthInCells) {
        double cellWidth = WIDTH / widthInCells;
        double a = cellWidth / 2;
        return a;
    }

    private static void runAll(TravelTimeObserver obs) {
        Config c2 = ConfigUtils.createConfig();
        Scenario sc2 = ScenarioUtils.createScenario(c2);
        createSc(sc2);
        createBigPopLR(sc2);
        createBigPopRL(sc2);
        setupConfig(sc2);
        runScenario(sc2, obs);
    }

    private static void runScenario(Scenario sc, TravelTimeObserver obs) {
        final Controler controller = new Controler(sc);
        controller.getConfig().controler().setOverwriteFileSetting(
                OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        EventsManager em = controller.getEvents();
        //DEBUG

        //		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
        //		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
        //
        //
        //		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
        if (CTRunner.DEBUG) {
            EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
            InfoBox iBox = new InfoBox(dbg, sc);
            dbg.addAdditionalDrawer(iBox);
            //		dbg.addAdditionalDrawer(new Branding());
            //			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
            //			dbg.addAdditionalDrawer(qDbg);


            //			em.addHandler(qDbg);
            em.addHandler(dbg);
            //END_DEBUG
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(new File("/Users/laemmel/scenarios/misanthrope/sc1")));
            JumpEventHandler h = new JumpEventCSVWriter(bw);
            em.addHandler(h);
        } catch (IOException e) {
            e.printStackTrace();
        }

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addRoutingModuleBinding("walkct").toProvider(CTRoutingModule.class);
            }
        });

        final CTMobsimFactory factory = new CTMobsimFactory();

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(obs);
                if (getConfig().controler().getMobsim().equals("ctsim")) {
                    bind(Mobsim.class).toProvider(new Provider<Mobsim>() {
                        @Override
                        public Mobsim get() {
                            return factory.createMobsim(controller.getScenario(), controller.getEvents());
                        }
                    });
                }
            }
        });

        controller.run();

        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setupConfig(Scenario sc) {
        sc.getNetwork().setEffectiveCellSize(.26);
        sc.getNetwork().setEffectiveLaneWidth(.71);
        sc.getNetwork().setCapacityPeriod(1);
        Config c = sc.getConfig();

        c.strategy().addParam("Module_1", "ChangeExpBeta");
        c.strategy().addParam("ModuleProbability_1", ".5");

        c.controler().setMobsim("ctsim");

        c.controler().setLastIteration(0);

        PlanCalcScoreConfigGroup.ActivityParams pre = new PlanCalcScoreConfigGroup.ActivityParams("origin");
        pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
        // running a simulation one gets
        // "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
        // the reason is the double precision. see also comment in
        // ActivityUtilityParameters.java (gl)
        pre.setMinimalDuration(49);
        pre.setClosingTime(49);
        pre.setEarliestEndTime(49);
        pre.setLatestStartTime(49);
        pre.setOpeningTime(49);

        PlanCalcScoreConfigGroup.ActivityParams post = new PlanCalcScoreConfigGroup.ActivityParams("destination");
        post.setTypicalDuration(49); // dito
        post.setMinimalDuration(49);
        post.setClosingTime(49);
        post.setEarliestEndTime(49);
        post.setLatestStartTime(49);
        post.setOpeningTime(49);
        sc.getConfig().planCalcScore().addActivityParams(pre);
        sc.getConfig().planCalcScore().addActivityParams(post);

        sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
        sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);

        QSimConfigGroup qsim = sc.getConfig().qsim();
        // qsim.setEndTime(20 * 60);
        c.controler().setMobsim("ctsim");
        c.global().setCoordinateSystem("EPSG:3395");

        c.qsim().setEndTime(30 * 3600);

        c.controler().setOutputDirectory("/Users/laemmel/tmp" + "/sc2/");
    }

    private static void createSc(Scenario sc1) {
        Network net1 = sc1.getNetwork();
        NetworkFactory fac1 = net1.getFactory();
        Node n1_0 = fac1.createNode(Id.createNodeId("n1_0"), CoordUtils.createCoord(0, 0));
        Node n1_1 = fac1.createNode(Id.createNodeId("n1_1"), CoordUtils.createCoord(30, 0.1));
        Node n1_9 = fac1.createNode(Id.createNodeId("n1_9"), CoordUtils.createCoord(30 + LL, 0));
        Node n1_10 = fac1.createNode(Id.createNodeId("n1_10"), CoordUtils.createCoord(30 + LL + 30, 0.1));
        net1.addNode(n1_0);
        net1.addNode(n1_1);
        net1.addNode(n1_9);
        net1.addNode(n1_10);
        Link l1_0 = fac1.createLink(Id.createLinkId("l1_0"), n1_0, n1_1);
        Link l1_8 = fac1.createLink(Id.createLinkId("1_8"), n1_1, n1_9);
        Link l1_9 = fac1.createLink(Id.createLinkId("l1_9"), n1_9, n1_10);
        Link l1_0r = fac1.createLink(Id.createLinkId("l1_0r"), n1_1, n1_0);
        Link l1_8r = fac1.createLink(Id.createLinkId("1_8r"), n1_9, n1_1);
        Link l1_9r = fac1.createLink(Id.createLinkId("l1_9r"), n1_10, n1_9);
        net1.addLink(l1_0);
        net1.addLink(l1_8);
        net1.addLink(l1_9);
        net1.addLink(l1_0r);
        net1.addLink(l1_8r);
        net1.addLink(l1_9r);
        Set<String> modes = new HashSet<>();
        modes.add("walkct");
        for (Link l : net1.getLinks().values()) {
            l.setAllowedModes(modes);
            l.setFreespeed(CTCell.V_0);
            l.setLength(30.);
            l.setCapacity(WIDTH * 1.33);
        }
        l1_8.setLength(LL);
        l1_8r.setLength(LL);

    }

    private static void createBigPopLR(Scenario sc1) {
        Population pop1 = sc1.getPopulation();
        PopulationFactory popFac1 = pop1.getFactory();

        int offset = pop1.getPersons().size();

        for (int i = 0; i < AGENTS_LR; i++) {
            Person pers = popFac1.createPerson(Id.create("b" + (i + offset), Person.class));
            Plan plan = popFac1.createPlan();
            pers.addPlan(plan);
            Activity act0;
            act0 = popFac1.createActivityFromLinkId("origin",
                    Id.create("l1_0", Link.class));
            act0.setEndTime(i * INV_INFLOW);
            plan.addActivity(act0);
            Leg leg = popFac1.createLeg("walkct");
            plan.addLeg(leg);
            Activity act1 = popFac1.createActivityFromLinkId("destination",
                    Id.create("l1_9", Link.class));
            plan.addActivity(act1);
            pop1.addPerson(pers);
        }

    }

    private static void createBigPopRL(Scenario sc1) {
        Population pop1 = sc1.getPopulation();
        PopulationFactory popFac1 = pop1.getFactory();

        int offset = pop1.getPersons().size();


        for (int i = 0; i < AGENTS_RL; i++) {
            Person pers = popFac1.createPerson(Id.create("r" + (i + offset), Person.class));
            Plan plan = popFac1.createPlan();
            pers.addPlan(plan);
            Activity act0;
            act0 = popFac1.createActivityFromLinkId("origin",
                    Id.create("l1_9r", Link.class));
            act0.setEndTime(i * INV_INFLOW);
            plan.addActivity(act0);
            Leg leg = popFac1.createLeg("walkct");
            plan.addLeg(leg);
            Activity act1 = popFac1.createActivityFromLinkId("destination",
                    Id.create("l1_0r", Link.class));
            plan.addActivity(act1);
            pop1.addPerson(pers);
        }

    }

    private static LeastCostPathCalculatorFactory createDefaultLeastCostPathCalculatorFactory(
            Scenario scenario) {
        Config config = scenario.getConfig();
        if (config.controler().getRoutingAlgorithmType()
                .equals(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
            return new DijkstraFactory();
        } else {
            if (config
                    .controler()
                    .getRoutingAlgorithmType()
                    .equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
                return new AStarLandmarksFactory();
            } else {
                if (config.controler().getRoutingAlgorithmType()
                        .equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
                    return new FastDijkstraFactory();
                } else {
                    if (config
                            .controler()
                            .getRoutingAlgorithmType()
                            .equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
                        return new FastAStarLandmarksFactory();
                    } else {
                        throw new IllegalStateException(
                                "Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
                    }
                }
            }
        }
    }
}
