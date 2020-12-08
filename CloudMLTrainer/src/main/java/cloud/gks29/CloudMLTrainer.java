package cloud.gks29;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.sql.SparkSession;

import scala.Tuple2;

public class CloudMLTrainer {
	public static void main(String[] args) throws URISyntaxException {
		/*String trainingDataSetPath = "hdfs://HDFS-NAME:9000/cloud/TrainingDataset_noHeader.csv";
		String validationDataSetPath = "hdfs://HDFS-NAME:9000/cloud/ValidationDataset_noHeader.csv";
		String outputModelPath = "hdfs://HDFS-NAME:9000/cloud/models";*/

		if (args.length < 3) {
			System.out.println("Usage : trainingDataSetPath validationDataSetPath outputModelPath");
			return;
		}
		
		
		String trainingDataSetPath = args[0];
		String validationDataSetPath = args[1];
		String outputModelPath = args[2];

		//SparkConf conf = new SparkConf().setAppName("CloudMLTrainer").setMaster(sparkMaster).setJars(new String[] {"/Users/gksingh/Desktop/CloudMLTrainer.jar"});
		SparkSession spark = SparkSession.builder().appName("CloudMLTrainer").getOrCreate();
	
		JavaSparkContext sc = JavaSparkContext.fromSparkContext(spark.sparkContext());

		JavaRDD<String> rawData = sc.textFile(trainingDataSetPath).filter((val)->{ return !val.contains("\"\"");});
		rawData.cache();

		JavaRDD<Tuple2<Double, double[]>> trainingRecords = rawData.map((val) -> {
			return val.split(";");
		}).map((val) -> {
			return new Tuple2<>(Double.parseDouble(val[val.length - 1]) - 1,
					Arrays.stream(val, 0, val.length - 1).mapToDouble(Double::parseDouble).toArray());
		});
		trainingRecords.cache();

		JavaRDD<LabeledPoint> trainingLabelPoint = trainingRecords.map(new Function<Tuple2<Double, double[]>, LabeledPoint>() {

			private static final long serialVersionUID = 1L;

			@Override
			public LabeledPoint call(Tuple2<Double, double[]> val) throws Exception {
				return new LabeledPoint(val._1, Vectors.dense(val._2));
			}
		});
		trainingLabelPoint.cache();

		/*System.out.println("Top 10 Training Records");
		List<LabeledPoint> temp = trainingLabelPoint.take(10);
		List<String> rtemp = rawData.take(10);
		for(int i = 0; i < temp.size(); i++)
			System.out.println(temp.get(i)  + "<<<===" + rtemp.get(i));*/


		/**
		 *  NaiveBayes Model does not accept negative values in the samples hence making all negative values to 0.0.
		 */
		
		JavaRDD<LabeledPoint> naiveBayesTrainingLabelPoint = trainingRecords.map((val) -> {
			return new LabeledPoint(val._1, Vectors.dense(Arrays.stream(val._2).map((val2) -> {
				return (val2 < 0.0) ? 0.0 : val2;
			}).toArray()));
		});
		naiveBayesTrainingLabelPoint.cache();

		JavaRDD<String> valRawData = sc.textFile(validationDataSetPath).filter((val)->{ return !val.contains("\"\"");});
		valRawData.cache();

		JavaRDD<Tuple2<Double, double[]>> validationData = valRawData.map((val) -> {
			return val.split(";");
		}).map((val) -> {
			return new Tuple2<>(Double.parseDouble(val[val.length - 1]) - 1,
					Arrays.stream(val, 0, val.length - 1).mapToDouble(Double::parseDouble).toArray());
		});
		validationData.cache();
		
		/*System.out.println("Top 10 Validation Records");
		List<Tuple2<Double, double[]>> tmpvalData = validationData.take(10);
		List<String> tmpValRaw = valRawData.take(10);
		
		for(int i = 0; i < tmpvalData.size(); i++)
		{	
			Tuple2<Double, double[]> tp = tmpvalData.get(i);
			Double[] what = Arrays.stream(tp._2).boxed().toArray( Double[]::new );
			System.out.println("(" + Arrays.asList(what) + ", " + tp._1 + ") <<<===" + tmpValRaw.get(i));
		}*/
		
		/**
		 *  NaiveBayes Model does not accept negative values in the samples hence making all negative values to 0.0.
		 */
		
		JavaRDD<Tuple2<Double, double[]>> naiveBayesValidationData = validationData.map((val) -> {
			return new Tuple2<Double, double[]>(val._1, Arrays.stream(val._2).map((val2) -> {
				return (val2 < 0.0) ? 0.0 : val2;
			}).toArray());
		});
		naiveBayesValidationData.cache();
		
		
		/* Training the data with various models */

		LogisticRegressionModel lrModel = new LogisticRegressionWithLBFGS().setNumClasses(10).run(JavaRDD.toRDD(trainingLabelPoint));

		NaiveBayesModel nbModel = new NaiveBayes().run(JavaRDD.toRDD(trainingLabelPoint));

		DecisionTreeModel dtModel = DecisionTree.trainClassifier(trainingLabelPoint, 10, new HashMap<Integer, Integer>(), "gini", 30,
				32);
		
		RandomForestModel rndfModel = RandomForest.trainClassifier(trainingLabelPoint, 10, new HashMap<Integer, Integer>(), 500, "auto",
				"gini", 30, 32, 12345);
		
		JavaRDD<Tuple2<Double, Double>> lrPredictionAndLabels = validationData.map((val) -> {
			return new Tuple2<Double, Double>(lrModel.predict(Vectors.dense(val._2)), val._1);
		});
		JavaRDD<Tuple2<Double, Double>> nbPredictionAndLabels = validationData.map((val) -> {
			return new Tuple2<Double, Double>(nbModel.predict(Vectors.dense(val._2)), val._1);
		});
		JavaRDD<Tuple2<Double, Double>> dtPredictionAndLabels = validationData.map((val) -> {
			return new Tuple2<Double, Double>(dtModel.predict(Vectors.dense(val._2)), val._1);
		});
		JavaRDD<Tuple2<Double, Double>> rndfPredictionAndLabels = validationData.map((val) -> {
			return new Tuple2<Double, Double>(rndfModel.predict(Vectors.dense(val._2)), val._1);
		});

		/** I read one documentation is that since 2.2.0 spark = recall = precision = accuracy since
		 *  F1 = 2 * (recall * precision) / (recall + precision )
		 *  Hence F1 = recall = precision = accuracy
		 *  https://spark.apache.org/docs/2.1.0/api/java/org/apache/spark/mllib/evaluation/MulticlassMetrics.html#recall()
		 */
		
		MulticlassMetrics lrMetrics = new MulticlassMetrics(JavaRDD.toRDD(lrPredictionAndLabels));
		System.out.println("Logistic Regression Model F1 Score = " + lrMetrics.accuracy());

		MulticlassMetrics nbMetrics = new MulticlassMetrics(JavaRDD.toRDD(nbPredictionAndLabels));
		System.out.println("NaiveBayes Model F1 Score = " + nbMetrics.accuracy());

		MulticlassMetrics dtMetrics = new MulticlassMetrics(JavaRDD.toRDD(dtPredictionAndLabels));
		System.out.println("Decision Tree F1 Score = " + dtMetrics.accuracy());

		MulticlassMetrics rndfMetrics = new MulticlassMetrics(JavaRDD.toRDD(rndfPredictionAndLabels));
		System.out.println("Random Forest F1 Score = " + rndfMetrics.accuracy());
		
		
		try {
			FileSystem fs = FileSystem.get(new java.net.URI(outputModelPath), sc.hadoopConfiguration());
			fs.delete(new Path(outputModelPath), true);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		lrModel.save(spark.sparkContext(), outputModelPath + "/lrModel");
		nbModel.save(spark.sparkContext(), outputModelPath + "/nbModel");
		dtModel.save(spark.sparkContext(), outputModelPath + "/dtModel");
		rndfModel.save(spark.sparkContext(), outputModelPath + "/rndfModel");
		sc.close();
	}
}
