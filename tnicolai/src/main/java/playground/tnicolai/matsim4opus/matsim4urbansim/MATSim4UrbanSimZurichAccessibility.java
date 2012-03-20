
/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimERSA.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
 */
package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.scenario.ZurichUtilities;
import playground.tnicolai.matsim4opus.scenario.ZurichUtilitiesIVTCHNetwork;
import playground.tnicolai.matsim4opus.utils.network.NetworkBoundaryBox;

import com.vividsolutions.jts.geom.Geometry;

/**
 * This class extends MATSim4UrbanSimV2 including two extra accessibility measurements
 * 1) Grid-based accessibility using a shape file (better for plotting)
 * 2) Grid-based accessibility using network only, dumping out different girdsize resolutions (easier to use)
 * tnicolai feb'12
 * 
 * Added custom boundig box for Grid-based accessibility using the network only
 * 	- reason: avoiding "out of memory" issues 
 * tnicolai march'12
 * 
 * @author thomas
 *
 */
class MATSim4UrbanSimZurichAccessibility extends MATSim4UrbanSimParcelV2{
	
	// Logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimZurichAccessibility.class);
	
	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimZurichAccessibility(String args[]){
		super(args);
		// set the resolution, this is used for setting 
		// the starting points for accessibility measures
//		checkAndSetShapeFile(args);
//		checkAndSetGridSize(args);
//		checkAndSetJobSample(args);
//		checkAndSetBoundingBox(args);
	}

//	/**
//	 * Set the shape file path in order to determine 
//	 * the starting points for accessibility computation
//	 * 
//	 * @param args
//	 */
//	private void checkAndSetShapeFile(String[] args) {
//
//		if( args.length >= 2 ){
//			shapeFile = args[1].trim();
//			log.info("The shape file path was set to " + shapeFile);
//			if(!Paths.pathExsits(shapeFile))
//				throw new RuntimeException("Given path to shape file does not exist: " + shapeFile);
//		} else{
//			log.error("Missing shape file!!!");
//			System.exit(-1);
//		}
//	}
//	
//	/**
//	 * Set the grid size for the starting points
//	 * 
//	 * @param args
//	 */
//	private void checkAndSetGridSize(String[] args) {
//		
//		if(args.length >= 3){
//			gridSizeInMeter = Double.parseDouble( args[2].trim() );
//			log.info("The resolution was set to " + String.valueOf(gridSizeInMeter) );
//		} else{
//			log.error("Missing resolution!!!");
//			System.exit(-1);
//		}
//	}
	
//	/**
//	 * Set the jobSample for the starting points
//	 * 
//	 * @param args
//	 */
//	private void checkAndSetJobSample(String[] args) {
//
//		if (args.length >= 4) {
//			jobSample = Double.parseDouble(args[3].trim());
//			log.info("The jobSample was set to " + String.valueOf(jobSample));
//		} else {
//			log.error("Missing jobSample!!!");
//			System.exit(-1);
//		}
//	}
	
//	/**
//	 * Set custom bounding box to determine the area to process
//	 * 
//	 * @param args
//	 */
//	private void checkAndSetBoundingBox(String args[]){
//		// tnicolai: get bounding box via config
//		// minX, minY, maxX, maxY
//		NetworkBoundaryBox.setCustomBoundaryBox(676223.42, 241583.83, 689664.05, 254305.72); // this is for the Zurich application
//	}
	
	/**
	 * This modifies the MATSim network according to the given
	 * test parameter in the MATSim config file (from UrbanSim)
	 */
	@Override
	void modifyNetwork(Network network){
		log.info("");
		log.info("Checking for network modifications ...");
		// check given test parameter for desired modifications
		String testParameter = scenario.getConfig().getParam(Constants.URBANSIM_PARAMETER, Constants.TEST_PARAMETER_PARAM);
		if(testParameter.equals("")){
			log.info("No modifications to perform.");
			log.info("");
			return;
		}
		else{
			String scenarioArray[] = testParameter.split(",");
			ZurichUtilitiesIVTCHNetwork.modifyNetwork(network, scenarioArray);
//			ZurichUtilitiesZurichBigRoads.modifyNetwork(network, scenarioArray);
			log.info("Done modifying network.");
			log.info("");
		}
	}
	
	/**
	 * This removes plan elements from existing plans that
	 * contain a removed link
	 */
	@Override
	void modifyPopulation(Population population){
		ZurichUtilities.deleteRoutesContainingRemovedLinks(population);
	}
	
	/**
	 * This method allows to add additional listener
	 * to the super class
	 */
	@Override
	void addFurtherControlerListener(Controler controler, ActivityFacilitiesImpl parcels){
		
		// set spatial reference id (not necessary but needed to match the outcomes with google maps)
		int srid = Constants.SRID_SWITZERLAND; // Constants.SRID_WASHINGTON_NORTH
		
		// The following lines register what should be executed _after_ the iterations are done:
		
		if(computeAgentPerformance){ // tnicolai: todo
			log.warn("Computation of AgentPerformance under development !!! No output yet.");
		}
		
		if(computeGridBasedAccessibilitiesShapeFile){
			
			// init aggregatedWorkplaces
			if(aggregatedOpportunities == null)
				aggregatedOpportunities = readUrbansimJobs(parcels, jobSampleRate);

			
			Geometry boundary = GridUtils.getBoundary(shapeFile, srid);
			
			SpatialGrid<Double> congestedTravelTimeAccessibilityGrid = GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
			SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid = GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
			SpatialGrid<Double> walkTravelTimeAccessibilityGrid  	 = GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
			
			controler.addControlerListener( new CellBasedAccessibilityShapeControlerListener(GridUtils.createGridLayerByGridSizeByShapeFile(cellSizeInMeter, boundary, srid), 
																	  						 aggregatedOpportunities,
																	  						 congestedTravelTimeAccessibilityGrid, 
																	  						 freespeedTravelTimeAccessibilityGrid, 
																	  						 walkTravelTimeAccessibilityGrid, 
																	  						 benchmark) );
		}
		
		if(computeGridBasedAccessibilitiesNetwork){
			
			// init aggregatedWorkplaces
			if(aggregatedOpportunities == null)
				aggregatedOpportunities = readUrbansimJobs(parcels, jobSampleRate);
			
			// set default boundary box for accessibility computation
			// if a bounding box is already set it won't be overwritten!
			NetworkBoundaryBox.setDefaultBoundaryBox(controler.getNetwork());
			
			SpatialGrid<Double> congestedTravelTimeAccessibilityGrid = new SpatialGrid<Double>(NetworkBoundaryBox.getBoundingBox(), cellSizeInMeter);
			SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid = new SpatialGrid<Double>(NetworkBoundaryBox.getBoundingBox(), cellSizeInMeter);
			SpatialGrid<Double> walkTravelTimeAccessibilityGrid  	 = new SpatialGrid<Double>(NetworkBoundaryBox.getBoundingBox(), cellSizeInMeter);
			
			controler.addControlerListener( new CellBasedAccessibilityNetworkControlerListener(GridUtils.createGridLayerByGridSizeByNetwork(cellSizeInMeter, NetworkBoundaryBox.getBoundingBox(), srid), 
																							   aggregatedOpportunities, 
																							   congestedTravelTimeAccessibilityGrid, 
																							   freespeedTravelTimeAccessibilityGrid, 
																							   walkTravelTimeAccessibilityGrid, 
																							   benchmark));
		}
	}
	
	/**
	 * This is the program entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		long startTime;
		long endTime;
		long time;
		
		startTime = System.currentTimeMillis();
		
		MATSim4UrbanSimZurichAccessibility m4uZurich = new MATSim4UrbanSimZurichAccessibility(args);
		m4uZurich.runMATSim();
		
		endTime = System.currentTimeMillis();
		time = (endTime - startTime) / 60000;
		
		log.info("Computation took " + time + " minutes. Computation done!");
	}

}

