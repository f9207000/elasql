/*******************************************************************************
 * Copyright 2016, 2018 elasql.org contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.elasql.schedule.tpart.sink;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.elasql.cache.tpart.TPartCacheMgr;
import org.elasql.procedure.tpart.TPartStoredProcedureTask;
import org.elasql.schedule.tpart.Edge;
import org.elasql.schedule.tpart.Node;
import org.elasql.schedule.tpart.TGraph;
import org.elasql.schedule.tpart.TPartPartitioner;
import org.elasql.server.Elasql;
import org.elasql.sql.RecordKey;

public class CacheOptimizedSinker extends Sinker {
	// private static long sinkPushTxNum;
	private static int sinkProcessId = 0;
	private int myId = Elasql.serverId();

	private TPartCacheMgr cm = (TPartCacheMgr) Elasql.remoteRecReceiver();

	public CacheOptimizedSinker() {
		// the tx numbers of sink flush task are unused negative value
		// sinkPushTxNum = -(TPartPartitioner.NUM_PARTITIONS) - 1;
	}

	@Override
	public Iterator<TPartStoredProcedureTask> sink(TGraph graph, long txNum) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<TPartStoredProcedureTask> sink(TGraph graph) {

		// add write back edges
		graph.addWriteBackEdge();

		// create sunk plan list
		List<TPartStoredProcedureTask> plans;
		plans = createSunkPlan(graph);

		// clean up this sink's info
		graph.clearSinkNodeEdges();
		graph.removeSunkNodes();
		TPartPartitioner.costFuncCal.reset();

		sinkProcessId++;

		return plans.iterator();

	}

	/**
	 * Note: only sink the task that will be executed in this machine to result
	 * set.
	 * 
	 * @param node
	 * @return
	 */
	private List<TPartStoredProcedureTask> createSunkPlan(TGraph graph) {

		/*
		 * Prepare the cache info and sink plan.
		 */

		// create procedure tasks
		List<TPartStoredProcedureTask> localTasks = new LinkedList<TPartStoredProcedureTask>();

		for (Node node : graph.getNodes()) {
			// System.out.println("Tx: " + node.getTxNum());

			// task is local if the tx logic should be executed locally
			boolean taskIsLocal = (node.getPartId() == myId);
			boolean replicated = false;

			long txNum = node.getTxNum();
			SunkPlan plan = new SunkPlan(sinkProcessId, taskIsLocal);
			node.getTask().setSunkPlan(plan);

			// readings

			for (Edge e : node.getReadEdges()) {
				// System.out.println("key:" + e.getResourceKey() + ",target:"
				// + e.getTarget().getTxNum());
				long srcTxn = e.getTarget().getTxNum();
				boolean isLocalResource = (e.getTarget().getPartId() == myId);
				if (taskIsLocal) {
					plan.addReadingInfo(e.getResourceKey(), srcTxn);

					if (isLocalResource && e.getTarget().isSinkNode()) {
						cm.registerSinkReading(e.getResourceKey(), txNum);
						plan.addSinkReadingInfo(e.getResourceKey());
					}

				} else if (isLocalResource && e.getTarget().isSinkNode()) {
					// if is not local task and the source of the edge is sink
					// node add the push tag to sinkPushTask
					cm.registerSinkReading(e.getResourceKey(), txNum);
					plan.addSinkPushingInfo(e.getResourceKey(), node.getPartId(), TPartCacheMgr.toSinkId(myId),
							node.getTxNum());
				}
			}

			// for every local task, push to remote if dest. node not in local
			// System.out.println("Write edges: ");
			if (taskIsLocal) {
				for (Edge e : node.getWriteEdges()) {

					int targetServerId = e.getTarget().getPartId();
					// Since Local Cache will take care of push rec in the
					// reading phase
					// there is no need to add WriteingInfo
					// See TPartStoredProcedure pushing
					if (targetServerId != myId)
						plan.addPushingInfo(e.getResourceKey(), targetServerId, txNum, e.getTarget().getTxNum());
					else
						plan.addWritingInfo(e.getResourceKey(), e.getTarget().getTxNum());
				}
			}

			// write back
			// System.out.println("Write back edges: ");
			if (node.getWriteBackEdges().size() > 0 && !replicated) {
				for (Edge e : node.getWriteBackEdges()) {

					int targetServerId = e.getTarget().getPartId();
					RecordKey k = e.getResourceKey();

					if (taskIsLocal) {
						if (targetServerId == myId) {
							// tell the task to write back local
							plan.addLocalWriteBackInfo(k);
							cm.registerSinkWriteback(e.getResourceKey(), txNum);
						} else {
							// push the data if write back to remote
							plan.addPushingInfo(k, targetServerId, txNum, TPartCacheMgr.toSinkId(targetServerId));

						}
						plan.addWritingInfo(e.getResourceKey(), TPartCacheMgr.toSinkId(targetServerId));
					} else {
						if (targetServerId == myId) {
							cm.registerSinkWriteback(e.getResourceKey(), txNum);
							plan.addLocalWriteBackInfo(k);
						}
					}
				}
			}

			/*
			 * For each tx node, create a procedure task for it. The task will
			 * be scheduled locally if 1) the task is partitioned into current
			 * server or 2) the task needs to write back records to this server.
			 */
			if (taskIsLocal || plan.hasLocalWriteBack() || plan.hasSinkPush()) {
				// System.out.println("Task: " + node.getTxNum());
				localTasks.add(node.getTask());
			}

			node.setSunk(true);
		}

		return localTasks;
	}
}
