package org.elasql.schedule.calvin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasql.sql.RecordKey;

public class ExecutionPlan {
	
	public enum ParticipantRole { ACTIVE, PASSIVE, IGNORE };
	
	public static class PushSet {
		Set<RecordKey> keys;
		Set<Integer> nodeIds;
		
		public PushSet(Set<RecordKey> keys, Set<Integer> nodeIds) {
			this.keys = keys;
			this.nodeIds = nodeIds;
		}
		
		public Set<RecordKey> getPushKeys() {
			return keys;
		}
		
		public Set<Integer> getPushNodeIds() {
			return nodeIds;
		}
		
		@Override
		public String toString() {
			return "Keys: " + keys + ", targets: " + nodeIds;
		}
	}
	
	private ParticipantRole role = ParticipantRole.IGNORE;
	
	// Record keys
	private Set<RecordKey> localReadKeys = new HashSet<RecordKey>();
	private Set<RecordKey> remoteReadKeys = new HashSet<RecordKey>();
	private Set<RecordKey> localUpdateKeys = new HashSet<RecordKey>();
	private Set<RecordKey> localInsertKeys = new HashSet<RecordKey>();
	private Set<RecordKey> localDeleteKeys = new HashSet<RecordKey>();
	private Set<RecordKey> insertsForMigration = new HashSet<RecordKey>();
	private Map<Integer, Set<RecordKey>> pushSets = new HashMap<Integer, Set<RecordKey>>();

	public void addLocalReadKey(RecordKey key) {
		localReadKeys.add(key);
	}
	
	public void addRemoteReadKey(RecordKey key) {
		remoteReadKeys.add(key);
	}
	
	public void addLocalUpdateKey(RecordKey key) {
		localUpdateKeys.add(key);
	}

	public void addLocalInsertKey(RecordKey key) {
		localInsertKeys.add(key);
	}

	public void addLocalDeleteKey(RecordKey key) {
		localDeleteKeys.add(key);
	}
	
	public void addInsertForMigration(RecordKey key) {
		insertsForMigration.add(key);
	}
	
	public void addPushSet(Integer targetNodeId, RecordKey key) {
		Set<RecordKey> keys = pushSets.get(targetNodeId);
		if (keys == null) {
			keys = new HashSet<RecordKey>();
			pushSets.put(targetNodeId, keys);
		}
		keys.add(key);
	}
	
	public void removeFromPushSet(Integer targetNodeId, RecordKey key) {
		Set<RecordKey> keys = pushSets.get(targetNodeId);
		if (keys != null) {
			keys.remove(key);
			
			if (keys.size() == 0)
				pushSets.remove(targetNodeId);
		}
	}
	
	public Set<RecordKey> getLocalReadKeys() {
		return localReadKeys;
	}
	
	public Set<RecordKey> getRemoteReadKeys() {
		return remoteReadKeys;
	}
	
	public boolean isLocalUpdate(RecordKey key) {
		return localUpdateKeys.contains(key);
	}
	
	public Set<RecordKey> getLocalUpdateKeys() {
		return localUpdateKeys;
	}
	
	public boolean isLocalInsert(RecordKey key) {
		return localInsertKeys.contains(key);
	}
	
	public Set<RecordKey> getLocalInsertKeys() {
		return localInsertKeys;
	}
	
	public boolean isLocalDelete(RecordKey key) {
		return localDeleteKeys.contains(key);
	}
	
	public Set<RecordKey> getLocalDeleteKeys() {
		return localDeleteKeys;
	}
	
	public Set<RecordKey> getInsertsForMigration() {
		return insertsForMigration;
	}
	
	public Map<Integer, Set<RecordKey>> getPushSets() {
		return pushSets;
	}
	
	public void setParticipantRole(ParticipantRole role) {
		this.role = role;
	}
	
	public ParticipantRole getParticipantRole() {
		return role;
	}
	
	public boolean isReadOnly() {
		return localUpdateKeys.isEmpty() && localInsertKeys.isEmpty() &&
				localDeleteKeys.isEmpty();
	}
	
	public boolean hasLocalReads() {
		return !localReadKeys.isEmpty();
	}
	
	public boolean hasRemoteReads() {
		return !remoteReadKeys.isEmpty();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Role: " + role);
		sb.append('\n');
		sb.append("Local Reads: " + localReadKeys);
		sb.append('\n');
		sb.append("Remote Reads: " + remoteReadKeys);
		sb.append('\n');
		sb.append("Local Updates: " + localUpdateKeys);
		sb.append('\n');
		sb.append("Local Inserts: " + localInsertKeys);
		sb.append('\n');
		sb.append("Local Deletes: " + localDeleteKeys);
		sb.append('\n');
		sb.append("Inserts for migrations: " + insertsForMigration);
		sb.append('\n');
		sb.append("Push Sets: " + pushSets);
		sb.append('\n');
		
		return sb.toString();
	}
}
