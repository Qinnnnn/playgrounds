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
package playground.agarwalamit.siouxFalls.legModeDistributions;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.legModeHandler.LegModeActivityEndTimeAndActDurationHandler;
import playground.vsp.analysis.modules.AbstractAnalyisModule;


/**
 * @author amit
 */
public class LegModeDistributionForActivityEndTimeAndActivityDuration extends AbstractAnalyisModule {

	private final Logger logger = Logger.getLogger(LegModeDistributionForActivityEndTimeAndActivityDuration.class);
	private LegModeActivityEndTimeAndActDurationHandler actStrEndur;
	private SortedMap<String, Map<Id, List<Double>>> mode2PersonId2ActEndTimes;
	private SortedMap<String, Map<Id, List<Double>>> mode2PersonId2ActDurations;
	private List<Integer> timeStepClasses;
	private List<String> travelModes;
	private SortedMap<String, Map<Integer, Integer>> mode2ActEndTimeClasses2LegCount;
	private SortedMap<String, Map<Integer, Integer>> mode2ActDurationClasses2LegCount;
	private String eventsFile;
	private String configFile;

	public LegModeDistributionForActivityEndTimeAndActivityDuration(String eventsFile, String configFile, String plansFile) {
		super(LegModeDistributionForActivityEndTimeAndActivityDuration.class.getSimpleName());

		this.eventsFile = eventsFile;
		this.configFile=configFile;
		this.timeStepClasses=new ArrayList<Integer>();
		this.travelModes = new ArrayList<String>();

		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(plansFile);
		sc.getConfig().qsim().setEndTime(LoadMyScenarios.getSimulationEndTime(configFile));
		this.actStrEndur=new LegModeActivityEndTimeAndActDurationHandler(sc);
	}

	public static void main(String[] args) {
		String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMC/";
		String [] runs = {"run33","run201","run202","run203","run204"};

		for(String run:runs){
			String configFile = runDir+run+"/output_config.xml";
			int lastIteration = LoadMyScenarios.getLastIteration(configFile);
			String eventsFile = runDir+run+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
			String plansFile = runDir+run+"/output_plans.xml";
			LegModeDistributionForActivityEndTimeAndActivityDuration lmdatd = new LegModeDistributionForActivityEndTimeAndActivityDuration(eventsFile, configFile,plansFile);
			lmdatd.preProcessData();
			lmdatd.postProcessData();
			new File(runDir+run+"/analysis/legModeDistributions/").mkdir();
			lmdatd.writeResults(runDir+"/analysis500Its/legModeDistributions/"+run);
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(this.actStrEndur);
		reader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
		this.mode2PersonId2ActEndTimes = this.actStrEndur.getLegMode2PesonId2ActEndTimes();
		this.mode2PersonId2ActDurations = this.actStrEndur.getLegMode2PesonId2ActDurations();
		initializeTimeStepClasses();
		getTravelModes();
		this.mode2ActEndTimeClasses2LegCount = calculateMode2ActTimeClases2LegCount(this.mode2PersonId2ActEndTimes);
		this.mode2ActDurationClasses2LegCount = calculateMode2ActTimeClases2LegCount(mode2PersonId2ActDurations);
	}

	private SortedMap<String, Map<Integer, Integer>> calculateMode2ActTimeClases2LegCount (SortedMap<String,Map<Id,List<Double>>> mode2PersonId2ActEndTimes2) {
		SortedMap<String, Map<Integer, Integer>> mode2ActEndTime2LegCount= new TreeMap<String, Map<Integer,Integer>>();

		for(String mode:mode2PersonId2ActEndTimes2.keySet()){
			Map<Integer, Integer> timeClasses2LegCount = new HashMap<Integer,Integer>();

			for(Integer i:this.timeStepClasses){
				timeClasses2LegCount.put(i, 0);
			}
			mode2ActEndTime2LegCount.put(mode, timeClasses2LegCount);

			for(Id personId:mode2PersonId2ActEndTimes2.get(mode).keySet()){
				List<Double> actTimes = mode2PersonId2ActEndTimes2.get(mode).get(personId);
				for(double time : actTimes){
					time = time/(1*3600);
					for(int j=0; j< this.timeStepClasses.size()-1;j++){
						if(time>=this.timeStepClasses.get(j) && time<timeStepClasses.get(j+1)){
							Integer countSoFar = timeClasses2LegCount.get(j);
							Integer newCount = countSoFar+1;
							timeClasses2LegCount.put(this.timeStepClasses.get(j), newCount);
						}
					}

				}
			}
			mode2ActEndTime2LegCount.put(mode, timeClasses2LegCount);
		}

		return mode2ActEndTime2LegCount;
	}

	@Override
	public void writeResults(String outputFolder) {

		writeActType2LegMode2TimeClass2LegCountDistribution(outputFolder, this.mode2ActEndTimeClasses2LegCount, "ActEndTime");
		writeActType2LegMode2TimeClass2LegCountDistribution(outputFolder, this.mode2ActDurationClasses2LegCount, "ActDuration");

	}

	private void writeActType2LegMode2TimeClass2LegCountDistribution(String outputFolder,SortedMap<String,Map<Integer,Integer>> mode2ActEndTimeClasses2LegCount2, String actEndOrActDuration){

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+".legMode"+actEndOrActDuration+"Distribution.txt");
		try {
			writer.write("# \t");
			for(String mode:this.travelModes){
				writer.write(mode+"\t");
			}
			writer.newLine();
			for(int i=0; i<this.timeStepClasses.size();i++){
				writer.write(this.timeStepClasses.get(i)+"\t");
				for(String mode:this.travelModes){
					writer.write(mode2ActEndTimeClasses2LegCount2.get(mode).get(this.timeStepClasses.get(i))+"\t");
				}
				writer.newLine();
			}
			writer.close();

			logger.info("Data file is writted at "+outputFolder+"legModeActEndTimeDistribution.txt");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in File. Reason : "+e);
		}
	}

	private void initializeTimeStepClasses() {
		double simulationEndTime = LoadMyScenarios.getSimulationEndTime(configFile);

		for(int endOfTimeStep =0; endOfTimeStep<=(int)simulationEndTime/(1*3600);endOfTimeStep++){
			this.timeStepClasses.add(endOfTimeStep);
		}

		logger.info("The following time classes were defined: " + this.timeStepClasses);
	}

	private void getTravelModes(){
		travelModes = new ArrayList<String>(this.mode2PersonId2ActEndTimes.keySet());
		logger.info("Travel modes are "+this.travelModes.toString());
	}
}