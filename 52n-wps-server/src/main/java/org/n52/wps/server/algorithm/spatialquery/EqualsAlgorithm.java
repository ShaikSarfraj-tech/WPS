package org.n52.wps.server.algorithm.spatialquery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.server.AbstractAlgorithm;

public class EqualsAlgorithm extends AbstractAlgorithm {

	Logger LOGGER = Logger.getLogger(ContainsAlgorithm.class);
	private final String inputID1 = "LAYER1";
	private final String inputID2 = "LAYER2";
	private final String outputID = "RESULT";
	private List<String> errors = new ArrayList<String>();

	public List<String> getErrors() {
		return errors;
	}

	public Class getInputDataType(String id) {
		if (id.equalsIgnoreCase(inputID1) || id.equalsIgnoreCase(inputID2)) {
			return GTVectorDataBinding.class;
		}
		return null;
	}

	public Class getOutputDataType(String id) {
		if(id.equalsIgnoreCase(outputID)){
			return LiteralBooleanBinding.class;
		}
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData) {

		if(inputData==null || !inputData.containsKey(inputID1)){
			throw new RuntimeException("Error while allocating input parameters");
		}
		if(inputData==null || !inputData.containsKey(inputID2)){
			throw new RuntimeException("Error while allocating input parameters");
		}
		List<IData> firstDataList = inputData.get(inputID1);
		if(firstDataList == null || firstDataList.size() != 1){
			throw new RuntimeException("Error while allocating input parameters");
		}
		IData firstInputData = firstDataList.get(0);
				
		FeatureCollection firstCollection = ((GTVectorDataBinding) firstInputData).getPayload();

		List<IData> secondDataList = inputData.get(inputID2);
		if(secondDataList == null || secondDataList.size() != 1){
			throw new RuntimeException("Error while allocating input parameters");
		}
		IData secondInputData = secondDataList.get(0);
				
		FeatureCollection secondCollection = ((GTVectorDataBinding) secondInputData).getPayload();
		
		FeatureIterator firstIterator = firstCollection.features();
		
		FeatureIterator secondIterator = secondCollection.features();
		
		if(!firstIterator.hasNext()){
			throw new RuntimeException("Error while iterating over features in layer 1");
		}
		
		if(!secondIterator.hasNext()){
			throw new RuntimeException("Error while iterating over features in layer 2");
		}
		
		Feature firstFeature = firstIterator.next();
		
		Feature secondFeature = secondIterator.next();
		
		boolean equals = firstFeature.getDefaultGeometry().equals(secondFeature.getDefaultGeometry());
		
		HashMap<String, IData> result = new HashMap<String, IData>();

		result.put(outputID,
				new LiteralBooleanBinding(equals));
		return result;
	}


}