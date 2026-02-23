-- Fix babies.gender from CHAR(1)/bpchar to VARCHAR(1) to match Hibernate mapping
ALTER TABLE babies ALTER COLUMN gender TYPE VARCHAR(1);
