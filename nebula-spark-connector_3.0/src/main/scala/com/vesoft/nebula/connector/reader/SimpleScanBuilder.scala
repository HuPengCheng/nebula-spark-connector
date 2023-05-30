/* Copyright (c) 2022 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.connector.reader

import java.util

import com.vesoft.nebula.connector.NebulaOptions
import org.apache.spark.sql.connector.read.{
  Batch,
  InputPartition,
  PartitionReaderFactory,
  Scan,
  ScanBuilder,
  SupportsPushDownFilters,
  SupportsPushDownRequiredColumns
}
import org.apache.spark.sql.sources.Filter
import org.apache.spark.sql.types.StructType

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.asScalaBufferConverter

class SimpleScanBuilder(nebulaOptions: NebulaOptions, schema: StructType)
    extends ScanBuilder
    with SupportsPushDownFilters
    with SupportsPushDownRequiredColumns {

  private var filters: Array[Filter] = Array[Filter]()

  override def build(): Scan = {
    new SimpleScan(nebulaOptions, 10, schema)
  }

  override def pushFilters(pushFilters: Array[Filter]): Array[Filter] = {
    if (nebulaOptions.pushDownFiltersEnabled) {
      filters = pushFilters
    }
    pushFilters
  }

  override def pushedFilters(): Array[Filter] = filters

  override def pruneColumns(requiredColumns: StructType): Unit = {
    if (!nebulaOptions.pushDownFiltersEnabled || requiredColumns == schema) {
      new StructType()
    } else {
      requiredColumns
    }
  }
}

class SimpleScan(nebulaOptions: NebulaOptions, nebulaTotalPart: Int, schema: StructType)
    extends Scan
    with Batch {
  override def toBatch: Batch = this

  override def planInputPartitions(): Array[InputPartition] = {
    val partitionSize                                   = nebulaTotalPart
    val inputPartitions: java.util.List[InputPartition] = new util.ArrayList[InputPartition]()
    for (i <- 1 to partitionSize) {
      inputPartitions.add(NebulaPartition(i))
    }
    inputPartitions.asScala.toArray
  }

  override def readSchema(): StructType = schema

  override def createReaderFactory(): PartitionReaderFactory =
    new NebulaPartitionReaderFactory(nebulaOptions, schema)
}