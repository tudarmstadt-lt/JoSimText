import scala.collection.immutable.Iterable
println("this is a worksheeet")
val x = 0.0001
//def foo = { Set(0,1,3,3,2,1) }
//
//val x = foo
val m = Map[String, Double]()
val m2 = Map[Int,Int](0->1, 1->0)

val features = Map[String, Double]()
val clusterFeatures = Map[Int, Map[String, Double]](
    1 -> features,
    2 -> features,
    3 -> features
)
println(clusterFeatures)
for (sense <- clusterFeatures.keys) {
    println(sense)
}
val sense1 = Map[String, Double](
    "language"-> 0.9,
    "programming"-> 0.9,
    "java"-> 0.9,
    "c++"-> 0.9,
    "ruby"->0.9)


val sense2 = Map[String, Double](
    "snake"-> 0.9,
    "animal"-> 0.9)

val inventory = Map[Int, Map[String, Double]](
    1-> sense1,
    2-> sense2)

val senseProbs = collection.mutable.Map[Int, Double]()

val allClusterWordsNum = inventory
  .map{ case (senseId, senseCluster) => senseCluster.size }
  .sum
  .toDouble

println("total number of words", allClusterWordsNum)

for (sense <- inventory.keys){
    senseProbs(sense) = math.log(inventory(sense).size/allClusterWordsNum)
}
println(senseProbs)


val y = inventory(1).getOrElse("java",0.0)

val PRIOR_PROB = 0.000001
val featureProb = 0.001
if (featureProb >= PRIOR_PROB) math.log(featureProb) else math.log(PRIOR_PROB)

