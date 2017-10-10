package jp.mzw.vtr.cluster.machine_larning;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class MachineLearning {

	public static void main(String[] args) throws Exception {
		
		// arrange
		ClassLoader cl = MachineLearning.class.getClassLoader();
		CSVLoader loader = new CSVLoader();
		
		// read training data
		loader.setSource(cl.getResourceAsStream("training-data.csv"));
		Instances train = loader.getDataSet();

		// read test data
//		loader.setSource(cl.getResourceAsStream("training-data.csv"));
//		Instances test = loader.getDataSet();
		
		train.setClassIndex(1);
		
		Classifier classifier = new J48();
		classifier.buildClassifier(train);
		
		Evaluation evaluation = new Evaluation(train);
        evaluation.evaluateModel(classifier, train);
        
        System.out.println(evaluation.toMatrixString());
	}
	
}
