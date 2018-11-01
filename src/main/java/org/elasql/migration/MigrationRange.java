package org.elasql.migration;

import java.util.Set;

import org.elasql.sql.RecordKey;

public interface MigrationRange {
	
	boolean contains(RecordKey key);
	
	boolean isMigrated(RecordKey key);
	
	void setMigrated(RecordKey key);
	
	Set<RecordKey> generateNextMigrationChunk(int maxChunkSize);
	
	int getSourcePartId();
	
	int getDestPartId();
}
