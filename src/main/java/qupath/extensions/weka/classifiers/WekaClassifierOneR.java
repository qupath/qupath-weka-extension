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

import weka.classifiers.Classifier;
import weka.classifiers.rules.OneR;
import weka.core.Instances;

/**
 * QuPath wrapper for a classifier using Weka's (minimalist) One-R implementation.
 * 
 * @author Pete Bankhead
 *
 */
public class WekaClassifierOneR extends WekaClassifier {
	
	@Override
	public String getName() {
		return "OneR (minimal classifier)";
	}

	@Override
	public boolean supportsAutoUpdate() {
		return true;
	}

	@Override
	protected Classifier createClassifier(Instances trainingInstances) throws Exception {
		OneR classifier = new OneR();
		classifier.buildClassifier(trainingInstances);
		return classifier;
	}
	
	
}
