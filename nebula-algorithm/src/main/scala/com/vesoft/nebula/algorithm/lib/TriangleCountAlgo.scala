/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License,
 * attached with Common Clause Condition 1.0, found in the LICENSES directory.
 */

package com.vesoft.nebula.algorithm.lib

import com.vesoft.nebula.algorithm.config.AlgoConstants
import com.vesoft.nebula.algorithm.utils.NebulaUtil
import org.apache.log4j.Logger
import org.apache.spark.graphx.{Graph, VertexRDD}
import org.apache.spark.graphx.lib.TriangleCount
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.sql.types.{IntegerType, LongType, StructField, StructType}

object TriangleCountAlgo {
  private val LOGGER = Logger.getLogger(this.getClass)

  val ALGORITHM: String = "TriangleCount"

  /**
    * run the TriangleCount algorithm for nebula graph
    *
    * compute each vertex's triangle count
    */
  def apply(spark: SparkSession, dataset: Dataset[Row]): DataFrame = {

    val graph: Graph[None.type, Double] = NebulaUtil.loadInitGraph(dataset, false)

    val triangleResultRDD = execute(graph)

    val schema = StructType(
      List(
        StructField(AlgoConstants.ALGO_ID_COL, LongType, nullable = false),
        StructField(AlgoConstants.TRIANGLECOUNT_RESULT_COL, IntegerType, nullable = true)
      ))
    val algoResult = spark.sqlContext
      .createDataFrame(triangleResultRDD, schema)

    algoResult
  }

  def execute(graph: Graph[None.type, Double]): RDD[Row] = {
    val resultRDD: VertexRDD[Int] = TriangleCount.run(graph).vertices
    resultRDD.map(row => Row(row._1, row._2))
  }
}
