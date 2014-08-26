/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.agarwalamit.marginalTesting;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.internalizationCar.MarginalCostPricing;
import playground.ikaddoura.internalizationCar.TollDisutilityCalculatorFactory;
import playground.ikaddoura.internalizationCar.TollHandler;
import playground.ikaddoura.internalizationCar.WelfareAnalysisControlerListener;

/**
 * @author amit
 */
public class CompareTwoMethodsControler {

	public static void main(String[] args) {

		boolean existingMethod = Boolean.valueOf(args [0]);
		boolean newMethod = Boolean.valueOf(args [1]);

		String configFile = args[2];


		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(args[3]);

		//===vsp defaults
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.ABORT);

		Controler controler = new Controler(config);

		if(existingMethod) 
		{
			//=== internalization of congestion implV3
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			controler.addControlerListener(new MarginalCostPricing((ScenarioImpl) controler.getScenario(), tollHandler ));
		}
		
		if(newMethod) {
			//=== internalization of congestion implV4
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			controler.addControlerListener(new MarginalCostPricingControlerListner((ScenarioImpl) controler.getScenario(), tollHandler ));
		}

		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		controler.setDumpDataAtEnd(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		
		controler.run();	

	}
}
