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
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import qupath.extensions.weka.classifiers.WekaClassifier;
import qupath.extensions.weka.classifiers.WekaClassifierBayesNet;
import qupath.extensions.weka.classifiers.WekaClassifierJ48;
import qupath.extensions.weka.classifiers.WekaClassifierOneR;
import qupath.extensions.weka.classifiers.WekaClassifierRandomForests;
import qupath.extensions.weka.classifiers.WekaClassifierSMO;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.interfaces.PathCommand;
import qupath.lib.gui.helpers.DisplayHelpers;
import qupath.lib.gui.panels.classify.ClassifierBuilderPanel;
import qupath.lib.gui.prefs.PathPrefs;


/**
 * Command used to create and show a suitable dialog box for interactive display of Weka classifiers.
 * 
 * @author Pete Bankhead
 *
 */
public class WekaClassifierCommand implements PathCommand {
	
	final private static String name = "Create detection classifier (Weka)";

	final private static Logger logger = LoggerFactory.getLogger(WekaClassifierCommand.class);

	final private static StringProperty wekaPath = PathPrefs.createPersistentPreference("wekaPath", null);
	
	
	private QuPathGUI qupath;
	
	private Stage dialog;
	private ClassifierBuilderPanel<WekaClassifier> panel;
	
	public WekaClassifierCommand(final QuPathGUI qupath) {
		this.qupath = qupath;
		// Add Weka path
		updateExtensionPath();
		// Listen for changes to path property
		wekaPath.addListener((v, o, n) -> updateExtensionPath());
	}
	
	private void updateExtensionPath() {
		String path = wekaPath.get();
		if (path != null && new File(path).exists()) {
			qupath.addExtensionJar(new File(path));
		}
	}
	
	public Stage getDialog() {
		return dialog;
	}
	
	public ClassifierBuilderPanel<WekaClassifier> getPanel() {
		return panel;
	}

	@Override
	public void run() {
		try {
			if (dialog == null) {
				dialog = new Stage();
				if (qupath != null)
					dialog.initOwner(qupath.getStage());
				dialog.setTitle(name);
				
				BorderPane pane = new BorderPane();
				WekaClassifierRandomForests defaultClassifier = new WekaClassifierRandomForests();
				panel = new ClassifierBuilderPanel<>(qupath, Arrays.asList(
						new WekaClassifierBayesNet(),
						new WekaClassifierJ48(),
						new WekaClassifierOneR(),
						defaultClassifier,
						new WekaClassifierSMO()
						), defaultClassifier);
				
				ScrollPane scrollPane = new ScrollPane(panel.getPane());
				scrollPane.setFitToWidth(true);
				scrollPane.setFitToHeight(true);
				pane.setCenter(scrollPane);
				
				
				GridPane paneWeka = new GridPane();
				Image img = getWekaIcon(-1, 50);
				Button btnExportToWeka = new Button("Export training data for Weka");
				int col = 0;
				if (img != null) {
					Tooltip t = new Tooltip("Export data in ARFF file format for import in Weka Explorer etc.");
					t.setGraphic(new ImageView(img));
					btnExportToWeka.setTooltip(t);
				}
				btnExportToWeka.setMaxWidth(Double.MAX_VALUE);
				paneWeka.add(btnExportToWeka, col++, 0);
				GridPane.setHgrow(btnExportToWeka, Priority.SOMETIMES);
				btnExportToWeka.setOnAction(e -> saveWekaData());
					
				pane.setBottom(paneWeka);
				
				dialog.setScene(new Scene(pane));
			}
			dialog.show();
			if (dialog.getHeight() < javafx.stage.Screen.getPrimary().getVisualBounds().getHeight())
				dialog.setResizable(false);
		} catch (NoClassDefFoundError e) {
			dialog = null;
			
//			// Since we're running this (i.e. an extension), we really *should* have an extensions directory...
//			File dirExtensions = QuPathGUI.getExtensionDirectory();
//			if (dirExtensions == null || !dirExtensions.isDirectory()) {
//				DisplayHelpers.showErrorMessage("Weka classifier error", "Cannot find either Weka or an extensions directory.  Please reinstall the Weka extension.");				
//				return;
//			}
			
			// Prompt to select path to Weka
			if (!DisplayHelpers.showConfirmDialog("Set path to weka.jar", "Cannot find weka.jar.\n\nDo you want to select it manually from your Weka installation?"))
				return;
			File fileWeka = QuPathGUI.getDialogHelper(qupath.getStage()).promptForFile("Select weka.jar", null, "Weka JAR file", new String[]{".jar"});
			if (fileWeka == null || !fileWeka.isFile()) {
				logger.error("No Weka JAR file selected.");
				return;
			}
			
			// Set the path
			wekaPath.set(fileWeka.getAbsolutePath());
			
//			// Create a symbolic link in the extensions directory, and add refresh extensions to update classpath
//			try {
//				Files.createSymbolicLink(new File(dirExtensions, "weka").toPath(), dirWeka.toPath());
//			} catch (IOException e1) {
//				DisplayHelpers.showErrorNotification("Weka link error", e1);
//				return;
//			}
			qupath.refreshExtensions(false);
			
			// Now try again...
			run();
		}
	}
	
	
	/**
	 * Save the training data in a Weka-friendly format, for open elsewhere in Weka's GUI.
	 */
	private void saveWekaData() {
		if (panel.getSelectedFeatures().isEmpty()) {
			DisplayHelpers.showErrorMessage("Weka export", "No features selected for export!");
			return;
		}
		try {
			File fileOutput = qupath.getDialogHelper().promptToSaveFile("Export for Weka", null, null, "Weka Attribute-Relation File Format", ".arff");
			if (fileOutput == null)
				return;
			WekaHelpers.saveWekaData(fileOutput, panel.getTrainingMap(), panel.getSelectedFeatures());
		} catch (IOException e) {
			DisplayHelpers.showErrorNotification("Export for Weka", e);
		}
	}
	
	
	/**
	 * Try to get an image representing the Weka icon, from the weka.jar file, if possible.
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public static Image getWekaIcon(final int width, final int height) {
		try {
			URL url = weka.core.WekaEnumeration.class.getClassLoader().getResource("weka/gui/images/weka_background.gif");
			return new Image(url.toString(), width, height, true, true);
		} catch (Exception e) {
			logger.error("Unable to load Weka icon!", e);
		}	
		return null;
	}
	
}
