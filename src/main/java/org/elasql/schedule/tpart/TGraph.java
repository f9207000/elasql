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
package org.elasql.schedule.tpart;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.elasql.server.Elasql;
import org.elasql.sql.RecordKey;
import org.elasql.storage.metadata.PartitionMetaMgr;

public class TGraph {
	private List<Node> nodes = new LinkedList<Node>();
	/// XXX A Map indicate where are the records' position
	private Map<RecordKey, Node> resPos = new HashMap<RecordKey, Node>();
	private Node[] sinkNodes;
	private PartitionMetaMgr parMeta;

	public TGraph() {
		sinkNodes = new Node[PartitionMetaMgr.NUM_PARTITIONS];
		for (int i = 0; i < sinkNodes.length; i++) {
			Node node = new Node(null);
			node.setPartId(i);
			sinkNodes[i] = node;
		}
		parMeta = Elasql.partitionMetaMgr();
	}

	/**
	 * Insert the new node into the t-graph.
	 * 
	 * @param node
	 */
	public void insertNode(Node node) {
		if (node.getTask() == null)
			return;

		nodes.add(node);

		if (node.getTask().getReadSet() != null) {
			// create a read edge to the latest txn that writes that resource
			for (RecordKey res : node.getTask().getReadSet()) {

				Node targetNode;

				if (parMeta.isFullyReplicated(res))
					targetNode = sinkNodes[node.getPartId()];
				else
					targetNode = getResourcePosition(res);

				node.addReadEdges(new Edge(targetNode, res));
				targetNode.addWriteEdges(new Edge(node, res));
			}
		}
		
		if (node.getTask().getWriteSet() != null) {
			// update the resource position
			for (RecordKey res : node.getTask().getWriteSet())
				resPos.put(res, node);
		}
	}

	/**
	 * Write back all modified data records to their original partitions.
	 */
	public void addWriteBackEdge() {
		// XXX should implement different write back strategy
		for (Entry<RecordKey, Node> resPosPair : resPos.entrySet()) {
			RecordKey res = resPosPair.getKey();
			Node node = resPosPair.getValue();

			if (node.getTask() != null)
				node.addWriteBackEdges(new Edge(sinkNodes[parMeta.getPartition(res)], res));
		}
		resPos.clear();
	}

	public void clearSinkNodeEdges() {
		for (int i = 0; i < sinkNodes.length; i++)
			sinkNodes[i].getWriteEdges().clear();
	}

	public void removeSunkNodes() {
		nodes.clear();
	}

	/**
	 * Get the node that produce the latest version of specified resource.
	 * 
	 * @param res the key of the resource
	 * @return The desired node. If the resource has not been created a new
	 *         version since last sinking, the partition that own the resource
	 *         will be return in a Node format.
	 */
	public Node getResourcePosition(RecordKey res) {
		if (resPos.containsKey(res))
			return resPos.get(res);
		return sinkNodes[parMeta.getPartition(res)];
	}

	public List<Node> getNodes() {
		return nodes;
	}
}
