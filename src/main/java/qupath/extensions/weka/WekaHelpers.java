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

package qupath.extensions.weka;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qupath.lib.classifiers.PathClassificationLabellingHelper;
import qupath.lib.measurements.MeasurementList;
import qupath.lib.objects.PathDetectionObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 * Helper methods for connecting QuPath and Weka.
 * 
 * @author Pete Bankhead
 *
 */
public class WekaHelpers {

	/**
	 * Create an Instance for Weka from a PathObject, using the requested measurements.
	 * 
	 * @param pathObject
	 * @param measurements
	 * @param pathClass
	 * @param classAttribute
	 * @return
	 */
	public static Instance createInstance(final MeasurementList measurementList, final List<String> measurements, final PathClass pathClass, final Attribute classAttribute) {
		int n = measurements.size();
		if (classAttribute != null)
			n++;
		double[] vals = new double[n];
		int i = 0;
//		measurementList.ensureListOpen();
//		measurementList.closeList();
		boolean hasMeasurements = false; // Check if we have any measurements - if not, no point trying to classify
		for (String measurement : measurements) {
			double value = measurementList.getMeasurementValue(measurement);
			if (!hasMeasurements && !Double.isNaN(value))
				hasMeasurements = true;
			vals[i++] = value;
		}
		if (!hasMeasurements)
			return null;
		if (classAttribute != null) {
			if (pathClass != null)
				vals[vals.length-1] = classAttribute.indexOfValue(pathClass.getName());
			else
				vals[vals.length-1] = Double.NaN;
		}
		// Add to data
		return new DenseInstance(1.0, vals);
	}
	
	/**
	 * Create an Instance for Weka from a PathObject, using the requested measurements specified accoring to 
	 * their indices.
	 * 
	 * @param measurementList
	 * @param indices
	 * @param pathClass
	 * @param classAttribute
	 * @return
	 */
	public static Instance createInstance(final MeasurementList measurementList, final int[] indices, final PathClass pathClass, final Attribute classAttribute) {
//		int n = indices.length;
//		if (pathClass != null)
//			n++;
		double[] vals = new double[indices.length+1];
		int i = 0;
		for (int ind : indices) {
			vals[i++] = measurementList.getMeasurementValue(ind);
		}
		if (pathClass != null)
			vals[vals.length-1] = classAttribute.indexOfValue(pathClass.getName());
		else
			vals[vals.length-1] = Double.NaN;
		// Add to data
		return new DenseInstance(1.0, vals);
	}

	/**
	 * Create a list of attributes useful when training a classifier for PathObjects based on measurement names
	 * and available PathClasses.
	 * 
	 * @param measurements
	 * @param pathClasses
	 * @param attributes Optional list of attributes to reuse (may be null)
	 * @return
	 */
	public static ArrayList<Attribute> createAttributes(List<String> measurements, Set<PathClass> pathClasses, ArrayList<Attribute> attributes) {
		// Set up attributes
		if (attributes == null)
			attributes = new ArrayList<>();
		else
			attributes.clear();

		// Create attributes for each measurement
		for (String measurement : measurements)
			attributes.add(new Attribute(measurement));

		// Create attribute for path class
		if (pathClasses != null) {
			ArrayList<String> attributeClasses = new ArrayList<>();
			for (PathClass pathClass : pathClasses)
				attributeClasses.add(pathClass.getName());
			Attribute classAttribute = new Attribute("PathClass", attributeClasses);
			attributes.add(classAttribute);
		}

		return attributes;
	}
	
	
	
	/**
	 * Populate Weka instances from a list of PathObjects.
	 * Note: Only PathDetectionObjects will be created as instances,
	 * 
	 * @param pathObjects
	 * @param instances - Instances object to which new instances should be added, one per compatible PathObject
	 * @param pathClass - the PathClass for all the objects in this list that should be added
	 * @param measurements - list of measurements (optional - will be determined from instances if not present)
	 * @param doRecursive - if child objects should be included
	 */
	private static void populateInstances(Collection<? extends PathObject> pathObjects, Instances instances, PathClass pathClass, List<String> measurements, boolean doRecursive) {
		Attribute classAttribute = instances.classAttribute();
		// If we don't have a measurements array, infer these from the Instances
		if (measurements == null) {
			measurements = new ArrayList<>(instances.numAttributes());
			for (int i = 0; i < instances.numAttributes(); i++) {
				Attribute att = instances.attribute(i);
				if (!att.equals(classAttribute))
					measurements.add(att.name());
			}
		}
		for (PathObject pathObject : pathObjects) {
			if (pathObject instanceof PathDetectionObject) {
				Instance instance = createInstance(pathObject.getMeasurementList(), measurements, pathClass, classAttribute);
				if (instance != null)
					instances.add(instance); 
			}
			if (doRecursive && pathObject.hasChildren())
				populateInstances(pathObject.getChildObjects(), instances, pathClass, measurements, doRecursive);
		}
	}

	/**
	 * Get the index of the maximum value in an array, or -1 if the array is null or empty.
	 * 
	 * @param arr
	 * @return
	 */
	public static int getMaxIndex(double[] arr) {
		if (arr == null || arr.length == 0)
			return -1;
		int maxInd = 0;
		double max = arr[0];
		for (int i = 1; i < arr.length; i++) {
			double val = arr[i];
			if (val > max) {
				maxInd = i;
				max = val;
			}
		}
		return maxInd;
	}


	/**
	 * Save a map containing PathDetectionObjects and their ground truth classifications to an output file in Weka's preferred ARFF format.
	 * 
	 * @param fileOutput
	 * @param classificationMap
	 * @param featureNames
	 * @throws IOException
	 */
	public static void saveWekaData(final File fileOutput, final Map<PathClass, ? extends Collection<? extends PathObject>> classificationMap, final List<String> featureNames) throws IOException {
		
		ArrayList<Attribute> attributes = createAttributes(featureNames, classificationMap.keySet(), null);
		int n = PathClassificationLabellingHelper.countObjectsInMap(classificationMap);
		Instances instances = new Instances("QuPath objects", attributes, n);
		instances.setClass(attributes.get(attributes.size()-1));
		
		ArffSaver saver = new ArffSaver();
		saver.setCompressOutput(false); // Tried compression, but couldn't import afterwards? Possibly was a version thing...
//		Saver saver = new SerializedInstancesSaver();
		saver.setFile(fileOutput);
//		saver.setStructure(instances);
		for (PathClass pathClass : classificationMap.keySet()) {
			Collection<? extends PathObject> pathObjects = classificationMap.get(pathClass);
			populateInstances(pathObjects, instances, pathClass, featureNames, false);
		}
		saver.setInstances(instances);
		saver.writeBatch();		
	}
	

}
