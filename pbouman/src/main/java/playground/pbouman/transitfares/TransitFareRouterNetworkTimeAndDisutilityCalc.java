package playground.pbouman.transitfares;

import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;

import playground.pbouman.agentproperties.AgentProperties;
import playground.pbouman.transitfares.FarePolicies.DiscountInterval;

public class TransitFareRouterNetworkTimeAndDisutilityCalc implements TravelTime, TravelDisutility
{

	private double currentFactor = 1;
	
	private ScenarioImpl scenario;
	private TransitRouterNetworkTravelTimeAndDisutility calc;
	
	private Map<String,AgentProperties> agentProps;
	private double minimumMoneyUtility;
	
	public TransitFareRouterNetworkTimeAndDisutilityCalc(final TransitRouterConfig config, final ScenarioImpl scenario, Map<String,AgentProperties> props)
	{
		this(config,scenario);
		if (props != null)
		{
			agentProps = props;
			minimumMoneyUtility = Double.POSITIVE_INFINITY;
			for (AgentProperties ap : props.values())
			{
				minimumMoneyUtility = Math.min(minimumMoneyUtility,ap.getMoneyUtility());
			}
		}
	}
	
	public TransitFareRouterNetworkTimeAndDisutilityCalc(final TransitRouterConfig config, final ScenarioImpl scenario)
	{
		calc = new TransitRouterNetworkTravelTimeAndDisutility(config);
		this.scenario = scenario;
	}

	@Override
	public double getLinkTravelTime(final Link link, final double time)
	{
		double time2 = calc.getLinkTravelTime(link, time);
		
		// Do some stuff
		
		return time2;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle)
	{
		double cost = calc.getLinkTravelDisutility(link, time, person, vehicle);
		
		// Do some stuff here
		FarePolicies policies = scenario.getScenarioElement(FarePolicies.class);
		
		if (link instanceof TransitRouterNetworkLink)
		{
			TransitRouterNetworkLink trnl = (TransitRouterNetworkLink) link;
			if (trnl.getRoute() == null)
			{
				//System.out.println("Transfer from "+trnl.fromNode.route.getId()+" of line "+trnl.fromNode.line.getId()+" to "+trnl.toNode.route.getId()+" of line "+trnl.toNode.line.getId());
				
			}
			else
			{
				String line = trnl.getLine().getId().toString();
				String route = trnl.getLine().getId().toString();
				double distPrice = policies.getPolicyDistancePrice(line, route);
				DiscountInterval discount = policies.getDiscountInterval(time, line, route);
				
				if (discount == null)
				{
					cost += distPrice * trnl.getLength() * currentFactor;
				}
				else
				{
					double fare = distPrice * trnl.getLength() * currentFactor;
					fare = fare + (fare * discount.discount);
					cost += fare;
				}
			}
		}
		else
		{
			//System.out.println("Routing "+link+" for "+currentPerson.getId());		
		}
		
		double fac = 1;
		double costDisutility = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		
		if (agentProps != null && person != null)
		{
			costDisutility = agentProps.get(person.getId().toString()).getMoneyUtility();
		}
		else
		{
			AgentSensitivities as = null;
			if (scenario.getScenarioElement(AgentSensitivities.class) != null)
				as = (AgentSensitivities) scenario.getScenarioElement(AgentSensitivities.class);
			if (as != null)
			{
				fac = as.getSensitivity(person.getId());
			}
		}

		
		
		return fac * cost * costDisutility;

	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link)
	{
		double cost = calc.getLinkMinimumTravelDisutility(link);
		
		// Do some stuff here
		FarePolicies policies = scenario.getScenarioElement(FarePolicies.class);

		
		if (link instanceof TransitRouterNetworkLink)
		{
			TransitRouterNetworkLink trnl = (TransitRouterNetworkLink) link;
			if (trnl.getRoute() == null)
			{
				//System.out.println("Transfer from "+trnl.fromNode.route.getId()+" of line "+trnl.fromNode.line.getId()+" to "+trnl.toNode.route.getId()+" of line "+trnl.toNode.line.getId());
				
			}
			else
			{
				String line = trnl.getLine().getId().toString();
				String route = trnl.getLine().getId().toString();
				double distPrice = policies.getPolicyDistancePrice(line, route);
				
				DiscountInterval discount = policies.getMinimumDiscountInterval(line, route);
				
				if (discount == null)
				{
					cost += distPrice * trnl.getLength() * currentFactor;
				}
				else
				{
					double fare = distPrice * trnl.getLength() * currentFactor;
					fare = fare + (fare * discount.discount);
					cost += fare;
				}
			}
		}
		else
		{
			//System.out.println("Routing "+link+" for "+currentPerson.getId());		
		}
		
		double fac = 1;
		double costDisutility = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		
		if (agentProps != null)
		{
			costDisutility = minimumMoneyUtility;
		}
		else
		{
			AgentSensitivities as = null;
			if (scenario.getScenarioElement(AgentSensitivities.class) != null)
				as = (AgentSensitivities) scenario.getScenarioElement(AgentSensitivities.class);
			if (as != null)
			{
				fac = as.getMinimumSensitivity();
			}
		}

		
		
		return fac * cost * costDisutility;

	}

}
