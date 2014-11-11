import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd._

object ClusterContextClueAggregator {
    def main(args: Array[String]) {
        if (args.size < 3) {
            println("Usage: ClusterContextClueAggregator cluster-file feature-file output [min. probability] [min. coverage] [wordlist]")
            return
        }

        val param_s = if (args.length > 3) args(3).toDouble else 0.0
        val param_p = if (args.length > 4) args(4).toDouble else 0.0

        val words:Set[String] = if (args.length > 5) args(5).split(",").toSet else null

        val conf = new SparkConf().setAppName("ClusterContextClueAggregator")
        val sc = new SparkContext(conf)

        val clusterFile = sc.textFile(args(0))
        val featureFile = sc.textFile(args(1))
        val outputFile = args(2)

        val clusterSimWords:RDD[((String, String), Array[String])] = clusterFile
            .map(line => line.split("\t"))
            .map(cols => ((cols(0), cols(1) + "\t" + cols(2)), cols(3).split("  ")))
            .filter({case ((word, sense), simWords) => words == null || words.contains(word)})

        val clusterWords:RDD[(String, (String, String, Int))] = clusterSimWords
            .flatMap({case ((word, sense), simWords) => for(simWord <- simWords) yield (simWord, (word, sense, simWords.size))})

        val wordFeatures = featureFile
            .map(line => line.split("\t"))
            .map(cols => (cols(0), (cols(1), cols(4).toFloat / cols(3).toFloat, cols(4).toFloat / cols(2).toFloat))) // prob = wfc / fc, coverage = wfc / wc
            .filter({case (word, (feature, prob, coverage)) => prob >= param_s && coverage >= param_p})

        val featuresPerWord = wordFeatures
            .mapValues(feature => 1)
            .reduceByKey((sum1, sum2) => sum1 + sum2)

        clusterWords
            .join(wordFeatures)
            .map({case (simWord, ((word, sense, numSimWords), (feature, prob, coverage))) => ((word, sense, feature), (prob, coverage, numSimWords))})
            .reduceByKey({case ((p1, c1, n), (p2, c2, _)) => (p1 + p2, c1 + c2, n)})
            .map({case ((word, sense, feature), (probSum, coverageSum, numSimWords)) => ((word, sense), (feature, probSum / numSimWords, coverageSum / numSimWords))})
            .groupByKey()
            .mapValues(featureScores => featureScores.toArray.sortWith({case ((_, _, c1), (_, _, c2)) => c1 > c2}))
            .join(clusterSimWords)
            .map({case ((word, sense), (featureScores, simWords)) => word + "\t" + sense + "\t" + simWords.mkString("  ") + "\t" + featureScores.map({case (feature, prob, coverage) => feature + ":" + prob + ":" + coverage}).mkString("  ")})
            .saveAsTextFile(outputFile)
    }
}
