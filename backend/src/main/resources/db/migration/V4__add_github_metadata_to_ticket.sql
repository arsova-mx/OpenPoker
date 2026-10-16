-- Metadatos para integración con GitHub Issues
ALTER TABLE ticket ADD COLUMN external_source VARCHAR(50) DEFAULT NULL;
ALTER TABLE ticket ADD COLUMN external_id VARCHAR(100) DEFAULT NULL;
ALTER TABLE ticket ADD COLUMN external_url VARCHAR(500) DEFAULT NULL;
ALTER TABLE ticket ADD COLUMN external_repo VARCHAR(255) DEFAULT NULL;

CREATE INDEX idx_ticket_external_repo_id ON ticket (external_repo, external_id);
CREATE INDEX idx_ticket_external_source ON ticket (external_source);