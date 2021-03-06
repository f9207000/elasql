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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasql.sql.RecordKey;

public class SunkPlan {
	private int sinkProcessId;
	private boolean isLocalTask;

	// key->srcTxNum
	private Map<RecordKey, Long> readingInfoMap;

	// destServerId -> PushInfos
	private Map<Integer, Set<PushInfo>> pushingInfoMap;

	private List<RecordKey> localWriteBackInfo = new ArrayList<RecordKey>();

	private Map<Integer, List<RecordKey>> remoteWriteBackInfo;

	private Map<RecordKey, Set<Long>> writeDestMap = new HashMap<RecordKey, Set<Long>>();

	private Map<Integer, Set<PushInfo>> sinkPushingInfoMap = new HashMap<Integer, Set<PushInfo>>();

	private Set<RecordKey> sinkReadingSet = new HashSet<RecordKey>();

	public SunkPlan(int sinkProcessId, boolean isLocalTask) {
		this.sinkProcessId = sinkProcessId;
		this.isLocalTask = isLocalTask;
	}

	public void addReadingInfo(RecordKey key, long srcTxNum) {
		// not need to specify dest, that is the owner tx num

		if (readingInfoMap == null)
			readingInfoMap = new HashMap<RecordKey, Long>();
		readingInfoMap.put(key, srcTxNum);
	}

	public void addPushingInfo(RecordKey key, int targetNodeId, long srcTxNum, long destTxNum) {
		if (pushingInfoMap == null)
			pushingInfoMap = new HashMap<Integer, Set<PushInfo>>();
		Set<PushInfo> pushInfos = pushingInfoMap.get(targetNodeId);
		if (pushInfos == null) {
			pushInfos = new HashSet<PushInfo>();
			pushingInfoMap.put(targetNodeId, pushInfos);
		}
		pushInfos.add(new PushInfo(destTxNum, targetNodeId, key));
	}

	public void addWritingInfo(RecordKey key, long destTxNum) {
		if (writeDestMap.get(key) == null)
			writeDestMap.put(key, new HashSet<Long>());
		writeDestMap.get(key).add(destTxNum);
	}

	public void addSinkPushingInfo(RecordKey key, int destNodeId, long srcTxNum, long destTxNum) {
		Set<PushInfo> pushInfos = sinkPushingInfoMap.get(destNodeId);
		if (pushInfos == null) {
			pushInfos = new HashSet<PushInfo>();
			sinkPushingInfoMap.put(destNodeId, pushInfos);
		}
		pushInfos.add(new PushInfo(destTxNum, destNodeId, key));
	}

	public void addSinkReadingInfo(RecordKey key) {
		sinkReadingSet.add(key);
	}

	public Map<Integer, Set<PushInfo>> getSinkPushingInfo() {
		return sinkPushingInfoMap;
	}

	public Set<RecordKey> getSinkReadingInfo() {
		return sinkReadingSet;
	}

	public Long[] getWritingDestOfRecord(RecordKey key) {
		Set<Long> set = writeDestMap.get(key);
		return (set == null) ? null : set.toArray(new Long[0]);
	}

	public int sinkProcessId() {
		return sinkProcessId;
	}

	public boolean isLocalTask() {
		return isLocalTask;
	}

	public void addLocalWriteBackInfo(RecordKey key) {
		localWriteBackInfo.add(key);
	}

	public long getReadSrcTxNum(RecordKey key) {
		return readingInfoMap.get(key);
	}

	public Map<Integer, Set<PushInfo>> getPushingInfo() {
		return pushingInfoMap;
	}

	public List<RecordKey> getLocalWriteBackInfo() {
		return localWriteBackInfo;
	}

	public Map<Integer, List<RecordKey>> getRemoteWriteBackInfo() {
		return remoteWriteBackInfo;
	}

	public boolean hasLocalWriteBack() {
		return localWriteBackInfo.size() > 0;
	}

	public boolean hasSinkPush() {
		return sinkPushingInfoMap.size() > 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Sink Process Id: ");
		sb.append(sinkProcessId);
		sb.append("\n");

		sb.append("Is Local: ");
		sb.append(isLocalTask);
		sb.append("\n");

		sb.append("Reading Info: ");
		sb.append(readingInfoMap);
		sb.append("\n");

		sb.append("Pushing Info: ");
		sb.append(pushingInfoMap);
		sb.append("\n");

		sb.append("Local Writing Back Info: ");
		sb.append(localWriteBackInfo);
		sb.append("\n");

		sb.append("Remote Writing Back Info: ");
		sb.append(remoteWriteBackInfo);
		sb.append("\n");

		sb.append("Write Dest: ");
		Iterator<?> iterator = null;
		if (writeDestMap != null) {
			iterator = writeDestMap.keySet().iterator();

			while (iterator.hasNext()) {
				RecordKey key = (RecordKey) iterator.next();
				Set<Long> value = writeDestMap.get(key);
				sb.append(key + " : [");
				for(Long p : value)
					sb.append(p+",");
				sb.append("]");
			}
		}

		sb.append("\n");

		sb.append("Sink Pushing Info: ");
		if (sinkPushingInfoMap != null) {
			iterator = sinkPushingInfoMap.keySet().iterator();
			while (iterator.hasNext()) {
				Integer key = (Integer) iterator.next();
				Set<PushInfo> value = sinkPushingInfoMap.get(key);
				sb.append(key + " : [");
				for(PushInfo p : value)
					sb.append(p+",");
				sb.append("]");
				
			}
		}
		sb.append("\n");

		sb.append("Sink Reading Info: ");
		if (sinkReadingSet != null) {
			iterator = sinkReadingSet.iterator();
			while (iterator.hasNext()) {
				sb.append(iterator.next() + ",");

			}
		}

		sb.append("\n");

		return sb.toString();
	}
}