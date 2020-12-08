package cloud.gks29;

import java.util.Arrays;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.sql.SparkSession;

import scala.Tuple2;

public class CloudMLTester {
	public static void main(String[] args) {
		int modelType = 0;
		String modelPath = "/Users/gksingh/model/";
		String testFilePath = "/Users/gksingh/Test.csv";
		
		if(args.length < 3)
		{
			System.out.println("Usage: testFilePath");
			return;
		}
		
		System.out.println("CloudMLTester started");
		
		modelType = Integer.parseInt(args[0]);
		modelPath = args[1];
		testFilePath = args[2];

		SparkSession spark = SparkSession.builder().appName("CloudMLTrainer").getOrCreate();
		JavaSparkContext sc = JavaSparkContext.fromSparkContext(spark.sparkContext());
		spark.sparkContext().setLogLevel("ERROR");
		
		JavaRDD<String> rawData = sc.textFile(testFilePath).filter((val)->{ return !val.contains("\"\"");});
		rawData.cache();
		
		System.out.println("TestData has " + rawData.count() + " data samples");
		
		JavaRDD<Tuple2<Double, double[]>> testData = rawData.map((val) -> {
			return val.split(";");
		}).map((val) -> {
			return new Tuple2<>(Double.parseDouble(val[val.length - 1]) - 1,
					Arrays.stream(val, 0, val.length - 1).mapToDouble(Double::parseDouble).toArray());
		});
		
		testData.cache();
		
		
		/*System.out.println("Top 10 Test Records");
		List<Tuple2<Double, double[]>> tmpvalData = testData.take(10);
		List<String> tmpValRaw = rawData.take(10);
		
		for(int i = 0; i < tmpvalData.size(); i++)
		{	
			Tuple2<Double, double[]> tp = tmpvalData.get(i);
			Double[] what = Arrays.stream(tp._2).boxed().toArray( Double[]::new );
			System.out.println("(" + Arrays.asList(what) + ", " + tp._1 + ") <<<===" + tmpValRaw.get(i));
		}*/
		
		JavaRDD<Tuple2<Double, Double>> predictionAndLabels = null;
		if(modelType == 0)
		{
			LogisticRegressionModel lrModel = LogisticRegressionModel.load(spark.sparkContext(), modelPath + "/lrModel");
			predictionAndLabels = testData.map((val) -> {
				return new Tuple2<Double, Double>(lrModel.predict(Vectors.dense(val._2)), val._1);
			});
		}
		else if(modelType == 1)
		{
			NaiveBayesModel nbModel = NaiveBayesModel.load(spark.sparkContext(), modelPath + "/nbModel");
			predictionAndLabels = testData.map((val) -> {
				return new Tuple2<Double, Double>(nbModel.predict(Vectors.dense(val._2)), val._1);
			});
		}
		else if(modelType == 2)
		{
			DecisionTreeModel dtModel = DecisionTreeModel.load(spark.sparkContext(), modelPath + "/dtModel");
			predictionAndLabels = testData.map((val) -> {
				return new Tuple2<Double, Double>(dtModel.predict(Vectors.dense(val._2)), val._1);
			});
		}
		else
		{
			RandomForestModel rndfModel = RandomForestModel.load(spark.sparkContext(), modelPath + "/rndfModel");
			predictionAndLabels = testData.map((val) -> {
				return new Tuple2<Double, Double>(rndfModel.predict(Vectors.dense(val._2)), val._1);
			});
		}
		
		MulticlassMetrics metrics = new MulticlassMetrics(JavaRDD.toRDD(predictionAndLabels));
		System.out.println("F1 Score is " + metrics.accuracy());
		System.out.println("CloudMLTester exiting");	
	}
}
