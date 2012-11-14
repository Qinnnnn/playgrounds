/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.welfareAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.transferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsAnalyzer;

/**
 * 
 * @author ikaddoura
 *
 */
public class WelfareAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(WelfareAnalyzer.class);
	private ScenarioImpl scenario;

	private List<AbstractAnalyisModule> anaModules = new LinkedList<AbstractAnalyisModule>();
	private MonetaryPaymentsAnalyzer transferAna;
	private UserBenefitsAnalyzer userBenefitsAna;
	
	private double totalWelfare;
	
	public WelfareAnalyzer() {
		super(WelfareAnalyzer.class.getSimpleName());
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		
		// (sub-modules)
			
		this.transferAna =  new MonetaryPaymentsAnalyzer();
		this.transferAna.init(scenario);
		this.anaModules.add(transferAna);
		
		this.userBenefitsAna = new UserBenefitsAnalyzer();
		this.userBenefitsAna.init(scenario);
		this.anaModules.add(userBenefitsAna);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();
		for (AbstractAnalyisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				allEventHandler.add(handler);
			}
		}
		return allEventHandler;
	}

	@Override
	public void preProcessData() {
		log.info("Preprocessing all (sub-)modules...");
		for (AbstractAnalyisModule module : this.anaModules) {
			module.preProcessData();
		}
		log.info("Preprocessing all (sub-)modules... done.");
		
	}

	@Override
	public void postProcessData() {		
		log.info("Postprocessing all (sub-)modules...");
		for (AbstractAnalyisModule module : this.anaModules) {
			module.postProcessData();
		}
		log.info("Postprocessing all (sub-)modules... done.");
		
		// own postProcessing
		log.info("all users logsum: " + this.transferAna.getAllUsersAmount());
		log.info("transfer payments sum : " + this.userBenefitsAna.getAllUsersLogSum());
		
		this.totalWelfare = this.userBenefitsAna.getAllUsersLogSum() + this.transferAna.getAllUsersAmount();
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "welfare.txt";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("userBenefits: " + this.userBenefitsAna.getAllUsersLogSum());
			bw.newLine();
			bw.write("transferPayments: " + this.transferAna.getAllUsersAmount());
			bw.newLine();
			bw.write("totalWelfare: " + this.totalWelfare);
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
