CREATE TABLE IF NOT EXISTS `JSON_SCHEMA_LATEST_VERSION` (
  `SCHEMA_ID` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  `VERSION_ID` BIGINT NOT NULL,
  PRIMARY KEY (`SCHEMA_ID`),
  CONSTRAINT FOREIGN KEY (`SCHEMA_ID`) REFERENCES `JSON_SCHEMA` (`SCHEMA_ID`) ON DELETE RESTRICT,
  CONSTRAINT FOREIGN KEY (`VERSION_ID`) REFERENCES `JSON_SCHEMA_VERSION` (`VERSION_ID`) ON DELETE RESTRICT
)