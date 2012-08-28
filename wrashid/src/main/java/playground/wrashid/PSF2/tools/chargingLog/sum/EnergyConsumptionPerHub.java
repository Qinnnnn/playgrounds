package playground.wrashid.PSF2.tools.chargingLog.sum;

import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class EnergyConsumptionPerHub {

	public static void main(String[] args) {
		String chargingLogFileNamePath = "c:/tmp/chargingLog.txt";
		//String chargingLogFileNamePath = "H:/data/experiments/ArtemisAug2011/runs/run1/output/ITERS/it.0/0.chargingLog.txt";
		String linkHubMappingTable = "H:/data/experiments/ARTEMIS/zh/dumb charging/input/run1/linkHub_orig.mappingTable.txt";
		
		DoubleValueHashMap<String> energyConsumptionPerHub = getEnergyConsumptionPerHub(chargingLogFileNamePath, linkHubMappingTable);

		System.out.println("hubId\tenergyConsumption");
		energyConsumptionPerHub.printToConsole();
		
	}

	private static DoubleValueHashMap<String> getEnergyConsumptionPerHub(String chargingLogFileNamePath, String linkHubMappingTable) {
		DoubleValueHashMap<String> energyConsumptionPerLink = EnergyConsumptionPerLink.readChargingLog(chargingLogFileNamePath);
		StringMatrix matrix = GeneralLib.readStringMatrix(linkHubMappingTable);
		DoubleValueHashMap<String> energyConsumptionPerHub = new DoubleValueHashMap<String>();

		for (int i = 1; i < matrix.getNumberOfRows(); i++) {
			String hubId = matrix.getString(i, 0);
			String linkId = matrix.getString(i, 1);
			energyConsumptionPerHub.incrementBy(hubId, energyConsumptionPerLink.get(linkId));
		}
		return energyConsumptionPerHub;
	}

}
