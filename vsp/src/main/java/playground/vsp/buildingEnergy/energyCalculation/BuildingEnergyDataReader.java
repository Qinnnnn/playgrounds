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
package playground.vsp.buildingEnergy.energyCalculation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.vsp.buildingEnergy.linkOccupancy.LinkActivityOccupancyCounter;

/**
 * @author droeder
 *
 */
class BuildingEnergyDataReader {

	private static final Logger log = Logger
			.getLogger(BuildingEnergyDataReader.class);
	private List<Integer> timeBins;
	private Set<String> activityTypes;
	private double tmax;
	private double td;
	
	private HashMap<String, LinkOccupancyStats> type2OccupancyStats;
	private PopulationStats populationStats;
	private ArrayList<Id> links;

	BuildingEnergyDataReader(List<Integer> timeBins,
							double td,
							double tmax,
							Set<String> activityTypes) {
		this.timeBins = timeBins;
		this.activityTypes = activityTypes;
		this.td = td;
		this.tmax = tmax;
	}
	
	void run(String net, String plans, String events, String homeType, String workType){
		BuildingEnergyPlansAnalyzer plansAna = new BuildingEnergyPlansAnalyzer(homeType, workType);
		prepareScenario(plans, net, plansAna);
		log.warn("only persons with work- and home-Activities will be handled!");
		this.populationStats = plansAna.getStats();
		this.type2OccupancyStats = new HashMap<String, LinkOccupancyStats>();
		for(String s: activityTypes){
			this.type2OccupancyStats.put(s, initOccupancyCounter(s, plansAna.getPopulation()));
		}
		parseEvents(events);
	}
	
	PopulationStats getPStats(){
		return populationStats;
	}
	
	Map<String, LinkOccupancyStats> getLinkActivityStats(){
		return this.type2OccupancyStats;
	}
	
	List<Id> getLinkIds(){
		return this.links;
	}
	
	/**
	 * @param events
	 */
	private void parseEvents(String events) {
		EventsManager manager = EventsUtils.createEventsManager();
		MyEventsFilter filter = new MyEventsFilter(this.populationStats.getHomeAndWorkAgentIds());
		for(LinkOccupancyStats los: this.type2OccupancyStats.values()){
			for(LinkActivityOccupancyCounter laoc: los.getStats().values()){
				filter.addCounter(laoc);
			}
		}
		manager.addHandler(filter);
		new MatsimEventsReader(manager).readFile(events);
		for(LinkOccupancyStats los: this.type2OccupancyStats.values()){
			for(LinkActivityOccupancyCounter laoc: los.getStats().values()){
				laoc.finish();
			}
		}		
	}
	
	/*
	 * this is very quick and dirty hack but I don't want to change the other impl here... //dr, nov'13
	 */
	static class MyEventsFilter implements ActivityStartEventHandler, ActivityEndEventHandler{
		
		private List<Id> personFilter;
		private List<LinkActivityOccupancyCounter> handler = new ArrayList<LinkActivityOccupancyCounter>();

		public MyEventsFilter(List<Id> personFilter) {
			log.warn("filtering events --- functionality is only ensured for Berlin-Scenario!");
			this.personFilter = personFilter;
		}
		
		void addCounter(LinkActivityOccupancyCounter counter){
			handler.add(counter);
		}

		@Override
		public void reset(int iteration) {
			
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if(personFilter.contains(event.getPersonId())){
				for(LinkActivityOccupancyCounter laoc: handler){
					laoc.handleEvent(event);
				}
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if(personFilter.contains(event.getPersonId())){
				for(LinkActivityOccupancyCounter laoc: handler){
					laoc.handleEvent(event);
				}
			}
		}
		
	}

	/**
	 * @param plansFile
	 * @param plansAna 
	 * @return
	 */
	private void prepareScenario(String plansFile, String networkFile, BuildingEnergyPlansAnalyzer plansAna) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		plansAna.setPopulation(new PopulationImpl((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
		new MatsimNetworkReader(sc).readFile(networkFile);
		if(links == null){
			this.links = new ArrayList<Id>(sc.getNetwork().getLinks().keySet());
		}
		Collections.sort(links);
		// TODO[dr] this slows down the very drastic. Streaming is only necessary here because we need a 
		// filtered population for the berlin-scenario. 
		((PopulationImpl) sc.getPopulation()).addAlgorithm(plansAna);
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		new MatsimPopulationReader(sc).readFile(plansFile);
	}

	/**
	 * @param string
	 * @param td
	 * @param tmax
	 * @return
	 */
	private LinkOccupancyStats initOccupancyCounter(
			String string, Population p) {
		LinkOccupancyStats stats = new LinkOccupancyStats();
		for(int i : timeBins){
			stats.add(String.valueOf(i), new LinkActivityOccupancyCounter(p, i, i + td , string));
		}
		stats.add(BuildingEnergyAnalyzerMain.all , new LinkActivityOccupancyCounter(p, 0, tmax , string));
		return stats;
	}
	
	
//	
	protected class LinkOccupancyStats{
		
		private Map<String, LinkActivityOccupancyCounter> l = new HashMap<String, LinkActivityOccupancyCounter>();
		
		public LinkOccupancyStats() {
		}
		
		private void add(String s, LinkActivityOccupancyCounter l){
			this.l.put(s, l);
		}
		
		final Map<String, LinkActivityOccupancyCounter> getStats(){
			return l;
		}
	}
	
	/**
	 * 
	 * Analyzes the number of persons working/ performing home activities. 
	 * Note, a person performing a certain activity-type more than once, will
	 * be counted only once
	 * @author droeder
	 *
	 */
	private class BuildingEnergyPlansAnalyzer extends AbstractPersonAlgorithm {

		private String homeType;
		private String workType;
		private List<Id> home;
		private List<Id> work;
		private Population population;

		BuildingEnergyPlansAnalyzer(String homeType, String workType) {
			this.homeType = homeType;
			this.workType = workType;
			this.work = new ArrayList<Id>();
			this.home = new ArrayList<Id>();
			log.warn("currently activityType ``not specified'' is handled equals to ``home'' (necessary for Berlin-Scennrio)");
		}

		/**
		 * @param population
		 */
		public void setPopulation(Population population) {
			this.population = population;
		}
		
		public Population getPopulation(){
			return population;
		}

		@Override
		public void run(Person person) {
			boolean work = false;
			boolean home = false;
			List<PlanElement> pe = person.getSelectedPlan().getPlanElements();
			for(int i = 0; i< pe.size(); i += 2){
				Activity a = (Activity) pe.get(i);
				if(!work){
					if(a.getType().equals(workType)){
						work = true;
					}
				}
				if(!home){
					if(a.getType().equals(homeType) || a.getType().equals("not specified")){
						home = true;
					}
				}
				// we know everything we need to know, return
				if(home && work){
					this.population.addPerson(person);
					this.home.remove(person.getId());
					this.work.remove(person.getId());
					return;
				}
			}
			if(home){
				this.home.add(person.getId());
			} 
			if (work){
				this.work.add(person.getId());
			}
		}
		
		public PopulationStats getStats(){
			return new PopulationStats(new ArrayList<Id>(this.population.getPersons().keySet()), home, work);
		}

		
	}
	
	protected class PopulationStats{
		
		private List<Id> work;
		private List<Id> home;
		private List<Id> homeWork;

		private PopulationStats(List<Id> homeWork, List<Id> home, List<Id> work){
			this.homeWork = homeWork;
			this.home = home;
			this.work = work;
			
		}
		
		public final int getWorkCnt() {
			return (work.size() + homeWork.size());
		}
		
		public int getHomeAndWorkCnt() {
			return homeWork.size();
		}
		
		public final int getHomeCnt() {
			return (home.size() + homeWork.size());
		}
		
		public final List<Id> getHomeOnlyAgentIds(){
			return home;
		}
		
		public final List<Id> getWorkOnlyAgentIds(){
			return work;
		}
		
		public final List<Id> getHomeAndWorkAgentIds(){
			return homeWork;
		}
		
	}

}

