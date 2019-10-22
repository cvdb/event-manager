package com.transixs.event.store.service.data;

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.SqlScript;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import java.util.List;

@RegisterBeanMapper(CommitDto.class)
public interface CommitDao {

  @SqlScript("CREATE TABLE IF NOT EXISTS commits " +
  "( " +
  "    txnReference BIGINT NOT NULL, " +
  "    commitId char(40) NOT NULL, " +
  "    dispatchedAt bigint NOT NULL DEFAULT 0, " +
  "    commitSequence int NOT NULL, " +
  "    revision int NOT NULL, " +
  "    items tinyint NOT NULL, " +
  "    commitStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
  "    checkpointNumber bigint AUTO_INCREMENT, " +
  "    payload TEXT NOT NULL, " +
  "    PRIMARY KEY (checkpointNumber) " +
  ")")
  @SqlScript("CREATE UNIQUE INDEX IX_Commits ON commits (txnReference, commitSequence)")
  @SqlScript("CREATE UNIQUE INDEX IX_Commits_CommitId ON commits (txnReference, commitId)")
  @SqlScript("CREATE UNIQUE INDEX IX_Commits_Revisions ON commits (txnReference, revision, items)")
  @SqlScript("CREATE INDEX IX_Idspatched_At ON commits (dispatchedAt)")
  void createTable();

  @SqlUpdate("INSERT INTO commits (txnReference, commitId, commitSequence, revision, items, payload) " +
  "VALUES (:txnReference, :commitId, :commitSequence, :revision, :items, :payload)")
  @GetGeneratedKeys("checkpointNumber")
  long insert(@BindBean CommitDto commit);

  @SqlQuery("SELECT COUNT(*) FROM commits WHERE txnReference = :txnReference AND commitId = :commitId")
  int isDuplicateCommit(@Bind("txnReference") long txnReference, @Bind("commitId") String commitId);

  @SqlQuery("select * from commits where txnReference = :txnReference order by commitStamp, commitSequence")
  List<CommitDto> getCommits(@Bind("txnReference") long txnReference);

  @SqlQuery("select * from commits where dispatchedAt = 0 order by checkpointNumber ASC")
  List<CommitDto> getNonDispatched();

  @SqlUpdate("UPDATE commits SET dispatchedAt  = :timestamp WHERE checkpointNumber = :checkpointNumber")
  void updateDispatched(@Bind("timestamp") long timestamp, @Bind("checkpointNumber") long checkpointNumber);

}
