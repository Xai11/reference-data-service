ALTER TABLE dictionary
    ADD COLUMN hierarchical BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE dictionary_item
    ADD COLUMN parent_id  BIGINT REFERENCES dictionary_item (id),
    ADD COLUMN valid_from DATE,
    ADD COLUMN valid_to   DATE,
    ADD CONSTRAINT ck_dictionary_item_period
        CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_from <= valid_to);

CREATE INDEX idx_dictionary_item_parent ON dictionary_item (parent_id);
