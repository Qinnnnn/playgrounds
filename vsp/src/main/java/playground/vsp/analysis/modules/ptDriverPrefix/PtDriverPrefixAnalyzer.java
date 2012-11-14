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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.ptDriverPrefix;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * 
 * @author ikaddoura
 *
 */
public class PtDriverPrefixAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(PtDriverPrefixAnalyzer.class);
	private ScenarioImpl scenario;
	
	private PtDriverPrefixHandler ptDriverPrefixHandler;
	
	public PtDriverPrefixAnalyzer() {
		super(PtDriverPrefixAnalyzer.class.getSimpleName());
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.ptDriverPrefixHandler = new PtDriverPrefixHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.ptDriverPrefixHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
	}

	@Override
	public void postProcessData() {
		// Analyzing the ptDriverPrefix here would be to late. Getting the ptDriverPrefix while parsing the events.
	}

	@Override
	public void writeResults(String outputFolder) {
		System.out.println(this.ptDriverPrefixHandler.getPtDriverPrefix());
	}
	
	public String getPtDriverPrefix() {
		return this.ptDriverPrefixHandler.getPtDriverPrefix();
	}
	
}
