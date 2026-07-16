-- Machine name (equipment_type.name, e.g. "PET Scanner") and procedure/test name
-- (e.g. "PET Scans") are different concepts shown in different parts of the UI —
-- see EquipmentType.procedureName. Column is nullable: the app falls back to
-- `name` wherever procedure_name is blank, so existing types keep working as-is
-- until an admin fills in a procedure name for them.
ALTER TABLE equipment_type ADD COLUMN procedure_name VARCHAR(150) NULL AFTER name;

-- Backfill the one pattern we can derive with confidence from the existing
-- "<Modality> Scanner" naming convention (CT Scanner, MRI Scanner, PET Scanner, ...).
-- Types that don't follow this pattern (e.g. Cath Lab, DSA) are left NULL for an
-- admin to fill in via the Equipment Types page, rather than guessing wording.
UPDATE equipment_type
SET procedure_name = REPLACE(name, 'Scanner', 'Scans')
WHERE name LIKE '%Scanner%';
