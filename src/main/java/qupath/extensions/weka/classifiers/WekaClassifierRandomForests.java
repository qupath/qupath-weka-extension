/*-
 * #%L
 * This file is part of a QuPath extension.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.extensions.weka.classifiers;

import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.plugins.parameters.Parameterizable;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

/**
 * QuPath wrapper for a classifier using Weka's Random Forests implementation.
 * 
 * @author Pete Bankhead
 *
 */
public class WekaClassifierRandomForests extends WekaClassifier implements Parameterizable {
	
	private ParameterList params;
	
	@Override
	public String getName() {
		return "Random Forests";
	}

	@Override
	public boolean supportsAutoUpdate() {
		return true;
	}

	@Override
	protected Classifier createClassifier(Instances trainingInstances) throws Exception {
		RandomForest classifier = new RandomForest();
		ParameterList params = getParameterList();
		// Seem to be better off (in general) to not do it in parallel...?
		if (params.getBooleanParameterValue("doParallel"))
			classifier.setNumExecutionSlots(Runtime.getRuntime().availableProcessors());
		else
			classifier.setNumExecutionSlots(1);
		classifier.setNumIterations(params.getIntParameterValue("nIterations"));
		classifier.setMaxDepth(params.getIntParameterValue("maxDepth"));
		classifier.setNumFeatures(params.getIntParameterValue("nFeatures"));
		classifier.setBagSizePercent(params.getIntParameterValue("bagSizePercent"));
		classifier.setSeed(params.getIntParameterValue("seed"));
		
		classifier.buildClassifier(trainingInstances);
		return classifier;
	}

	@Override
	public ParameterList getParameterList() {
		if (params == null) {
			params = new ParameterList().addIntParameter("nIterations", "Number of iterations", 50, null, "Set the number of bagging iterations")
					.addIntParameter("maxDepth", "Max tree depth", 0, null, "The maximum depth of each tree (0 for unlimited)")
					.addIntParameter("nFeatures", "Number of features", 0, null, "The number of randomly-chosen features")
					.addIntParameter("bagSizePercent", "Bag size percent", 100, null, "Size of each bag, as a percentage of training set")
					.addIntParameter("seed", "Random seed", 1, null, "Seed for random number generator - keep the same for reproducibility, or vary to explore robustness")
					.addBooleanParameter("doParallel", "Use parallelized training", false, "Use multiple CPUs for training - may help (or harm) performance");
		}
		return params;
	}

	@Override
	public void resetParameterList() {
		params = null;
	}
	
	
}
