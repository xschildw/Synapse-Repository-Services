CREATE TABLE IF NOT EXISTS `RESEARCH_PROJECT` (
  `ID` BIGINT NOT NULL,
  `ACCESS_REQUIREMENT_ID` BIGINT NOT NULL,
  `CREATED_BY` BIGINT NOT NULL,
  `CREATED_ON` BIGINT NOT NULL,
  `MODIFIED_BY` BIGINT NOT NULL,
  `MODIFIED_ON` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  `PROJECT_LEAD` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `INSTITUTION` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `IDU` blob NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE (`ACCESS_REQUIREMENT_ID`, `CREATED_BY`),
  CONSTRAINT `RESEARCH_PROJECT_ACCESS_REQUIREMENT_ID_FK` FOREIGN KEY (`ACCESS_REQUIREMENT_ID`) REFERENCES `ACCESS_REQUIREMENT` (`ID`) ON DELETE RESTRICT
)
