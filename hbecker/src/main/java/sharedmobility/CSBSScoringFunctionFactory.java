package sharedmobility;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

import sharedmobility.CSBSLegScoringFunction;

public class CSBSScoringFunctionFactory implements ScoringFunctionFactory {

	private final Scenario scenario;
	private final ScoringParametersForPerson params;
	private final DemandHandler demandHandler;
	private final BikeshareDemandHandler bsDemandHandler;
	private final CostsCalculatorContainer costsCalculatorContainer;
	private final CarsharingSupplyInterface carsharingSupplyContainer;
	@Inject
	CSBSScoringFunctionFactory( final Scenario sc, final DemandHandler demandHandler, final BikeshareDemandHandler bsDemandHandler,
			final CostsCalculatorContainer costsCalculatorContainer, final CarsharingSupplyInterface carsharingSupplyContainer) {
		this.scenario = sc;
		this.params = new SubpopulationScoringParameters( sc );
		this.demandHandler = demandHandler;
		this.bsDemandHandler = bsDemandHandler;
		this.costsCalculatorContainer = costsCalculatorContainer;
		this.carsharingSupplyContainer = carsharingSupplyContainer;
	}


	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = new SumScoringFunction();
	    //this is the main difference, since we need a special scoring for carsharing legs

		scoringFunctionSum.addScoringFunction(
	    new CSBSLegScoringFunction( params.getScoringParameters( person ),
	    								 this.scenario.getConfig(),
	    								 this.scenario.getNetwork(), this.demandHandler, this.bsDemandHandler,
	    								 this.costsCalculatorContainer, 
	    								 this.carsharingSupplyContainer, person));
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelLegScoring(
						params.getScoringParameters( person ),
						this.scenario.getNetwork())
			    );
		//the remaining scoring functions can be changed and adapted to the needs of the user
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelActivityScoring(
						params.getScoringParameters(
								person ) ) );
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelAgentStuckScoring(
						params.getScoringParameters(
								person ) ) );
	    return scoringFunctionSum;
	  }
}
