QuPath Weka Extension
=====================

The *QuPath Weka Extension* enables classifiers from the popular machine learning library *Weka* to be used from within QuPath, and also facilitates the use export of QuPath data in a Weka-friendly format.


> The **QuPath Weka Extension** is **not** an 'official' piece of software by the developers of Weka, nor does it include or modify Weka in any way.  Rather, it simply enables some of the useful features of Weka to be accessible from within QuPath's user interface.

> Use of the QuPath Weka Extension requires Weka to be downloaded (and optionally installed) separately from http://www.cs.waikato.ac.nz/ml/weka/downloading.html

## Installation

* Download *Weka* from http://www.cs.waikato.ac.nz/ml/weka/downloading.html
* Download the latest release of the *QuPath Weka Extension*
* Install
* Do *either* one of the following:
	1. Run the *Classify &rarr; Create detection classifier (Weka)* command and, when prompted, point QuPath to the directory where Weka is installed
	2. Find the ```weka.jar``` file from within your Weka installation, and drag it onto QuPath
	
For the final step, depending upon the platform and where Weka is installed, there may be permissions issues that prevent method 1. from being successful... in which case method 2. is required.

If the above steps are successful, then *Classify &rarr; Create detection classifier (Weka)* can be used for classifying detections as an alternative to *Classify &rarr; Create detection classifier*.

> For more information on classifiers and their usefulness, see the [Classifying objects](https://github.com/qupath/qupath/wiki/Classifying-objects) section of the documentation.


## Why use the Weka extension?

Since QuPath already provides some machine learning capabilities with the *Classify &rarr; Create detection classifier* command (using [OpenCV](http://docs.opencv.org/3.1.0/dc/dd6/ml_intro.html)) to enable tasks such as cell classification or region identification, you may wonder why an alternative might be useful.

There are three main reasons for the existence of this Weka extension:

### 1. To provide access to (at least some of) Weka's classifiers

In some cases, there may be advantages to using one of Weka's classifiers (rather than the QuPath's OpenCV-based defaults) - either because of accuracy or performance, or simply because they are written in Java and therefore may be easier to explore, adapt or [serialize](https://docs.oracle.com/javase/tutorial/essential/io/objectstreams.html).


### 2. To enable training data export to use in Weka Explorer

The [Weka Explorer](http://www.cs.waikato.ac.nz/~ml/weka/gui_explorer.html) can be a very handy tool for examining data in more detail - including tasks such as feature selection, or assessing the performance of different classifiers.

Using the Weka extension, QuPath is able to export the data used to train a classifier in Weka's preferred ```.arff``` format, for easy import into Weka Explorer.  This can be used to help gain insights into how best to approach classification in QuPath.

To do this, click the *Export training data for Weka* button at the bottom of the classifier panel.


### 3. To demonstrate how additional machine learning libraries could be used with QuPath

There are many machine learning libraries that could be usefully employed within QuPath, but which are not included by default.  The Weka extension shows one way in which a new library could be integrated with QuPath - giving a pattern to developers who might want to integrate another library.
