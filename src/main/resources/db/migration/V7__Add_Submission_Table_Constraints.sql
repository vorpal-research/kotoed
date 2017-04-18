DELETE FROM Project WHERE courseId IS NULL;
DELETE FROM Project WHERE denizenId IS NULL;

ALTER TABLE Project ALTER courseId SET NOT NULL;
ALTER TABLE Project ALTER denizenId SET NOT NULL;

DELETE FROM Submission WHERE projectId IS NULL;
ALTER TABLE Submission ALTER projectId SET NOT NULL;

DELETE FROM Submission WHERE state IS NULL;
ALTER TABLE Submission ALTER state SET NOT NULL;
ALTER TABLE Submission ALTER state SET DEFAULT 'pending';

DELETE FROM Submission sub WHERE EXISTS(
    SELECT * FROM SUBMISSION sub2 WHERE sub.id != sub2.id AND sub.parentSubmissionId = sub2.parentSubmissionId
);
ALTER TABLE Submission ADD UNIQUE (parentSubmissionId);
