UPDATE submission_comment cmnt
  SET previous_comment_id = (
      SELECT prev.id
      FROM submission_comment prev
      WHERE prev.submission_id = cmnt.submission_id
      AND prev.sourcefile = cmnt.sourcefile
      AND prev.sourceline = cmnt.sourceline
      AND prev.datetime < cmnt.datetime
      ORDER BY prev.datetime DESC
      LIMIT 1
  )
WHERE cmnt.previous_comment_id IS NULL;
