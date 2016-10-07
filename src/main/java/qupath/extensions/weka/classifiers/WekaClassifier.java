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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.extensions.weka.WekaHelpers;
import qupath.lib.classifiers.Normalization;
import qupath.lib.classifiers.PathObjectClassifier;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.Summarizable;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * Base implementation of a Weka classifier for QuPath.
 * 
 * @author Pete Bankhead
 *
 */
public abstract class WekaClassifier implements Externalizable, PathObjectClassifier {
	
	private static final long serialVersionUID = 2L;
	
	final private static Logger logger = LoggerFactory.getLogger(WekaClassifier.class);
	
	private long lastModifiedTimestamp;
	
	private ArrayList<Attribute> trainingAttributes = new ArrayList<>();
	private Attribute classAttribute;
	private Classifier classifier;
	private Filter filter;
	private List<String> measurements = new ArrayList<>();
	private Map<String, PathClass> pathClassMap = new TreeMap<>();
	
	@Override
	public List<String> getRequiredMeasurements() {
		return Collections.unmodifiableList(measurements);
	}
	
	@Override
	public Collection<PathClass> getPathClasses() {
		return Collections.unmodifiableCollection(pathClassMap.values());
	}

	@Override
	public boolean isValid() {
		return classifier != null;
	}
		
	private ArrayList<Attribute> getAttributesCopy() {
		if (trainingAttributes == null)
			return null;
		ArrayList<Attribute> attributes = new ArrayList<>(trainingAttributes.size());
		for (Attribute attribute : trainingAttributes)
			attributes.add((Attribute)attribute.copy());
		return attributes;
	}
	
	public Instances createEmptyInstances(int capacity) {
		ArrayList<Attribute> attributes = getAttributesCopy();
		if (attributes == null || attributes.isEmpty())
			return null;
		Instances instances = new Instances("Explorer", attributes, capacity);
		instances.setClass(classAttribute);
		return instances;
	}
	
	
	protected abstract Classifier createClassifier(final Instances trainingInstances) throws Exception;
	
	
	
	public boolean updateClassifier(final Map<PathClass, List<PathObject>> map, final List<String> measurements, Normalization normalization) {
		
		if (map == null || map.size() < 2) {
			logger.error("At two classes of labelled objects are required!");
			return false;
		}
		
		// Reset variables in case we don't make it the whole way through...
		classifier = null;
		filter = null;
		this.measurements.clear();
		this.measurements.addAll(measurements);
		this.trainingAttributes.clear();
		
		
		if (map.size() <= 1) {
			logger.error("At least two different classes required to train a classifier!");
			return false;
		}
		// Determine the 'training increment' based on the maximum number of training instances
		double trainingIncrement = 1;
		
		// Create a map connecting PathClasses to the class names
		// TODO: Consider using an unique identifier rather than names
		pathClassMap.clear();
		for (PathClass pathClass : map.keySet())
			pathClassMap.put(pathClass.getName(), pathClass);
		
		// Create attributes
		trainingAttributes = WekaHelpers.createAttributes(measurements, map.keySet(), trainingAttributes);
		classAttribute = trainingAttributes.get(trainingAttributes.size()-1);
		
		// Create training instances
		Instances trainingInstances = new Instances("Training", trainingAttributes, map.size());
		trainingInstances.setClassIndex(trainingInstances.numAttributes()-1);
		for (Map.Entry<PathClass, List<PathObject>> entry : map.entrySet()) {
			PathClass pathClass = entry.getKey();
			List<PathObject> list = entry.getValue();
			for (double i = 0; i < list.size(); i += trainingIncrement) {
				PathObject pathObject = list.get((int)i);
				Instance instance = WekaHelpers.createInstance(pathObject.getMeasurementList(), measurements, pathClass, classAttribute);
				if (instance != null)
					trainingInstances.add(instance);
			}
		}
		
//		// TODO: Support normalization properly using Weka
//		logger.warn("Weka classifiers do not yet support normalization options!");
		
		// Perform feature normalization
		if (normalization != null && normalization != Normalization.NONE) {
			try {
				if (normalization == Normalization.MIN_MAX)
					filter = new Normalize();
				else
					filter = new Standardize();
				filter.setInputFormat(trainingInstances);
				trainingInstances = Filter.useFilter(trainingInstances, filter);
				logger.debug("Training classifier with normalization: {}", normalization);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			logger.debug("Training classifier without normalization");
			filter = null;
		}
		
	    // Perform classification
		try {
			classifier = createClassifier(trainingInstances);
//		    classifier = builder.buildClassifier(trainingInstances);
			System.out.println(classifier);
			if (classifier instanceof Summarizable)
				logger.info(((Summarizable)classifier).toSummaryString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		lastModifiedTimestamp = System.currentTimeMillis();
		
		return true;
	}
	
	
	@Override
	public int classifyPathObjects(final Collection<PathObject> pathObjects) {
		if (!isValid())
			return 0;
		return classifyDetectionObjects(pathObjects);
	}
	
	
	// Apply classification, having previously stripped out any non-detection objects
	int classifyDetectionObjects(final Collection<PathObject> pathObjects) {
		if (!isValid() || pathObjects == null || pathObjects.isEmpty())
			return 0;
		
		// Apply filter if we have one
		int nClassified = 0;
		Instances classificationInstances = new Instances("Classification", trainingAttributes, 1);
		classificationInstances.setClassIndex(classificationInstances.numAttributes()-1);
		
		// In the case that we have 'many' path objects, try to see if they all share the same measurement names -
		// if so, we can get away with determining the indices only once (and potentially classifying a bit faster)
		// TODO: Do more benchmarking to see how useful this really is (if at all)
		int[] indices = null;
		List<String> namesRef = null;
		if (pathObjects.size() > 100 && pathObjects.size() > measurements.size() * 2 && pathObjects instanceof List) {
			namesRef = ((List<PathObject>)pathObjects).get(0).getMeasurementList().getMeasurementNames();
			if (namesRef != null && namesRef.equals(((List<PathObject>)pathObjects).get(1).getMeasurementList().getMeasurementNames())) {
				indices = new int[measurements.size()];
				for (int i = 0; i < measurements.size(); i++) {
					indices[i] = namesRef.indexOf(measurements.get(i));
				}
			} else
				namesRef = null;
		}
		
		try {
			for (PathObject pathObject : pathObjects) {
//				if (!(pathObject instanceof PathDetectionObject))
//					continue;
				Instance instance;
				if (namesRef == null || !namesRef.equals(pathObject.getMeasurementList().getMeasurementNames()))
					instance = WekaHelpers.createInstance(pathObject.getMeasurementList(), measurements, null, classAttribute);
				else
					instance = WekaHelpers.createInstance(pathObject.getMeasurementList(), indices, null, classAttribute);
				if (instance == null)
					continue;
				instance.setDataset(classificationInstances);
				if (filter != null) {
					filter.input(instance);
					instance = filter.output();
				}

//				System.out.println(instance);
				double[] classDistribution = classifier.distributionForInstance(instance);
				int classIndex = WekaHelpers.getMaxIndex(classDistribution);
				String className = classAttribute.value(classIndex);
				PathClass pathClass = pathClassMap.get(className);
				double probability = classDistribution[classIndex];
				pathObject.setPathClass(pathClass, probability);
				nClassified++;
			}
		} catch (Exception e) {
			logger.error("Problem applying Weka classifier {}", getName(), e);
		}

		return nClassified;
	}
		
	
	@Override
	public String getDescription() {
		if (classifier == null)
			return "No classifier set!";
		
		StringBuilder sb = new StringBuilder();
		sb.append("Classifier:\t").append(classifier.getClass().getSimpleName()).append("\n\n");
		sb.append("Classes:\t[");
		Iterator<PathClass> iterClasses = getPathClasses().iterator();
		while (iterClasses.hasNext()) {
			sb.append(iterClasses.next());			
			if (iterClasses.hasNext())
				sb.append(", ");
			else
				sb.append("]\n\n");
		}
		Normalization normalization;
		if (filter instanceof Normalize)
			normalization = Normalization.MIN_MAX;
		else if (filter instanceof Standardize)
			normalization = Normalization.MEAN_VARIANCE;
		else
			normalization = Normalization.NONE;
		sb.append("Normalization:\t").append(normalization).append("\n\n");
		List<String> measurements = getRequiredMeasurements();
		sb.append("Required measurements (").append(measurements.size()).append("):\n");
		Iterator<String> iter = getRequiredMeasurements().iterator();
		while (iter.hasNext()) {
			sb.append("    ");
			sb.append(iter.next());			
			sb.append("\n");
		}
		
		sb.append("\n");
		sb.append(classifier.toString());
		
		return sb.toString();
	}
		
	
	
	@Override
	public long getLastModifiedTimestamp() {
		return lastModifiedTimestamp;
	}

	
	
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(serialVersionUID);
		out.writeLong(lastModifiedTimestamp);
		out.writeObject(trainingAttributes);
		out.writeObject(classAttribute);
		out.writeObject(classifier);
		out.writeObject(filter);
		out.writeObject(measurements);
		out.writeObject(pathClassMap);
	}


	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readLong();
		lastModifiedTimestamp = in.readLong();
		trainingAttributes = (ArrayList<Attribute>)in.readObject();
		classAttribute = (Attribute)in.readObject();
		classifier = (Classifier)in.readObject();
		filter = (Filter)in.readObject();
		measurements = (List<String>)in.readObject();
		pathClassMap = (HashMap<String, PathClass>)in.readObject();
	}
	
}
