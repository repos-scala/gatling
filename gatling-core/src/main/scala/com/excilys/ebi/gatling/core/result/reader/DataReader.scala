/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.result.reader

import scala.collection.immutable.SortedMap

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RunRecord

object DataReader {
	def newInstance(runOn: String) = configuration.dataReaderClass.getConstructor(classOf[String]).newInstance(runOn)
}

abstract class DataReader(runUuid: String) {

	def runRecord: RunRecord
	def realChartRequestRecords: Seq[ChartRequestRecord]

	def requestNames: Seq[String]
	def scenarioNames: Seq[String]

	def requestRecordsGroupByExecutionStartDateInSeconds: SortedMap[Long, Seq[ChartRequestRecord]]
	def scenarioChartRequestRecordsGroupByExecutionStartDateInSeconds(scenarioName: String): SortedMap[Long, Seq[ChartRequestRecord]]

	def realChartRequestRecordsGroupByExecutionStartDateInSeconds: SortedMap[Long, Seq[ChartRequestRecord]]
	def realChartRequestRecordsGroupByExecutionEndDateInSeconds: SortedMap[Long, Seq[ChartRequestRecord]]

	def requestRecords(requestName: String): Seq[ChartRequestRecord]
	def requestRecordsGroupByExecutionStartDate(requestName: String): SortedMap[Long, Seq[ChartRequestRecord]]
	def requestRecordsGroupByExecutionStartDateInSeconds(requestName: String): SortedMap[Long, Seq[ChartRequestRecord]]

}