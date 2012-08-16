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
package playground.droeder.southAfrica.replanning;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;

/**
 * @author droeder
 *
 */
public class FixedPtSubModePtInteractionRemover implements PlanAlgorithm {
	private static final Logger log = Logger
			.getLogger(FixedPtSubModePtInteractionRemover.class);
	
	public FixedPtSubModePtInteractionRemover(){
		//do nothing
	}

	@Override
	public void run(Plan plan) {
		List<PlanElement> newPlanElements = new ArrayList<PlanElement>();
		newPlanElements.add( plan.getPlanElements().get(0));
		List<PlanElement> temp = new ArrayList<PlanElement>();
		PlanElement e;
		for(int i = 1; i < plan.getPlanElements().size(); i++){
			e = plan.getPlanElements().get(i);
			temp.add(e);
			// a plan needs at least 3 PlanElements to work
			if(temp.size() > 1){
				if(e instanceof Activity){
					// a 'subtour' ends, when a non-'pt interaction' occurs
					if(!((Activity) e).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
						// this might be a non-pt-chain or it is a single-transit_walk
						if(temp.size() == 2){
							if(((Leg) temp.get(0)).getMode().equals(TransportMode.transit_walk)){
								log.warn("found act-transitWalk-act without any real pt-leg. LegMode set to pt." +
										" Can not guarantee that PtSubMode is still fixed!");
								((Leg) temp.get(0)).setMode(TransportMode.pt);
							}
							newPlanElements.addAll(temp);
						}
						// this is "pt-chain". Throw away all unnecessary pt legs and activities...
						else{
							for(PlanElement ee: temp){
								if(ee instanceof Activity){
									if(!((Activity) ee).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
										newPlanElements.add(ee);
									}
								}else if(ee instanceof Leg){
									if(!((Leg) ee).getMode().equals(TransportMode.transit_walk)){
										newPlanElements.add(ee);
									}
								}
							}
						}
						// clear the temp-list, because all temp-PlanElements are added to the new PlanElements
						temp.clear();
					}
				}
			}
		}
		plan.getPlanElements().clear();
		plan.getPlanElements().addAll(newPlanElements);
	}

}
