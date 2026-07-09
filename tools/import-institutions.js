#!/usr/bin/env node
/**
 * One-time import: reads D:\Dev\his-map\institutions_final.json (the
 * official MoH institution registry, 1,955 rows) and emits
 * src/main/resources/db/migration/V2__import_institutions.sql.
 *
 * See the architecture doc §7 for the cleanup rules this implements:
 *  - 320 rows with no institution code are imported with code = NULL
 *  - 6 duplicate codes are imported as-is (flagged below, not deduped —
 *    dropping either row would lose a real institution)
 *  - Admin-office rows (MOH/RDHS/PDHS/ADC/CDC/NTS) are imported as
 *    Institution rows, not excluded — they ARE the hierarchy nodes
 *  - type_atomic/type_group classification is reused to seed InstitutionType
 *
 * Reporting-line hierarchy (Institution.parent) constructed here:
 *   Ministry of Health (root, real row "Ministry & Dept. of Health")
 *     -> Provincial Ministry of Health (8 real rows; Western province has
 *        none in the source registry, so its PDHS parents directly to the
 *        national Ministry -- a genuine gap in the source data, not an
 *        import bug)
 *          -> Provincial Department of Health Services (9 real PDHS rows,
 *             one per province)
 *               -> Regional Department of Health Services (26 real RDHS
 *                  rows, matched to province via the standard Sri Lanka
 *                  district/RDHS -> province mapping below)
 *                    -> hospitals, MOH offices, clinics, etc. (line=2,
 *                       matched to their RDHS via the `rdhs` field)
 *     -> hospitals etc. reporting directly to the centre (line=1)
 *     -> "Other Government Health Institutions" (synthetic grouping node,
 *        no source row) -> military/police/prison hospitals (line=3)
 *
 * Area rows (geography, independent of the above): National -> 9 Province
 * -> 26 RDHS Division, built from the same standard mapping.
 *
 * Run: node tools/import-institutions.js
 */
const fs = require('fs');
const path = require('path');

const SOURCE = 'D:/Dev/his-map/institutions_final.json';
const OUT = path.join(__dirname, '..', 'src', 'main', 'resources', 'db', 'migration', 'V2__import_institutions.sql');

const RDHS_TO_PROVINCE = {
    'Colombo': 'Western', 'Gampaha': 'Western', 'Kalutara': 'Western',
    'Kandy': 'Central', 'Matale': 'Central', 'Nuwara Eliya': 'Central',
    'Galle': 'Southern', 'Matara': 'Southern', 'Hambantota': 'Southern',
    'Jaffna': 'Northern', 'Kilinochchi': 'Northern', 'Mannar': 'Northern', 'Vavuniya': 'Northern', 'Mullaitivu': 'Northern',
    'Trincomalee': 'Eastern', 'Batticaloa': 'Eastern', 'Ampara': 'Eastern', 'Kalmunai': 'Eastern',
    'Kurunegala': 'North Western', 'Puttalam': 'North Western',
    'Anuradhapura': 'North Central', 'Polonnaruwa': 'North Central',
    'Badulla': 'Uva', 'Moneragala': 'Uva',
    'Ratnapura': 'Sabaragamuwa', 'Kegalle': 'Sabaragamuwa'
};
const PROVINCES = ['Western', 'Central', 'Southern', 'Northern', 'Eastern', 'North Western', 'North Central', 'Uva', 'Sabaragamuwa'];

const TYPE_MAP = {
    'Primary Medical Care Unit': 'Primary_Medical_Care_Unit',
    'PMCU & MH': 'Primary_Medical_Care_Unit',
    'MOH': 'MOH_Office',
    'Divisional Hospital-Type C': 'Divisional_Hospital_Type_C',
    'Divisional Hospital-Type B': 'Divisional_Hospital_Type_B',
    'Divisional Hospital-Type A': 'Divisional_Hospital_Type_A',
    'Other': 'Other',
    'ADC': 'ADC',
    'Base Hospital-Type B': 'Base_Hospital_Type_B',
    'Base Hospital-Type A': 'Base_Hospital_Type_A',
    'STD Clinic': 'STD_Clinic',
    'RDHS': 'Regional_Department_of_Health_Services',
    'Chest Clinic': 'Chest_Clinic',
    'Air Force Hospital': 'Air_Force_Hospital',
    'District General Hospital': 'District_General_Hospital',
    'CDC': 'CDC',
    'NTS': 'NTS',
    'Teaching Hospital': 'Teaching_Hospital',
    'Other Specialized Hospital': 'Other_Specialized_Hospital',
    'Army Hospital': 'Army_Hospital',
    'Prison Hospital': 'Prison_Hospital',
    'PDHS': 'Provincial_Department_of_Health_Services',
    'Navy Hospital': 'Navy_Hospital',
    'Specialized Teaching Hospital': 'Specialized_Teaching_Hospital',
    'National Hospital': 'National_Hospital',
    'Other Hospital': 'Other_Hospital',
    'Police Hospital': 'Police_Hospital',
    'Board Managed Hospital (Tertiary Care)': 'Board_Managed_Hospital_Tertiary_Care',
    'Board Managed Hospital (Secondary Care)': 'Board_Managed_Hospital_Secondary_Care'
};

function sqlStr(v) {
    if (v === null || v === undefined || v === '') {
        return 'NULL';
    }
    return `'${String(v).trim().replace(/'/g, "''")}'`;
}

function main() {
    const raw = JSON.parse(fs.readFileSync(SOURCE, 'utf8'));

    // ---- Areas: National -> Province -> RDHS Division ----
    let areaId = 1;
    const nationalAreaId = areaId++;
    const provinceAreaId = {};
    PROVINCES.forEach((p) => { provinceAreaId[p] = areaId++; });
    const rdhsAreaId = {};
    Object.keys(RDHS_TO_PROVINCE).forEach((r) => { rdhsAreaId[r] = areaId++; });

    const areaRows = [];
    areaRows.push([nationalAreaId, 'NATIONAL', 'Sri Lanka', null]);
    PROVINCES.forEach((p) => areaRows.push([provinceAreaId[p], 'PROVINCE', `${p} Province`, nationalAreaId]));
    Object.entries(RDHS_TO_PROVINCE).forEach(([rdhs, province]) =>
        areaRows.push([rdhsAreaId[rdhs], 'RDHS_DIVISION', rdhs, provinceAreaId[province]]));

    // ---- Institutions ----
    let insId = 1;
    const institutionRows = []; // [id, type, name, code, address, phone, email, parentId, areaId]

    const mohRootId = insId++;
    const mohRow = raw.find((r) => r.health_inst_no === 'LCB0000331');
    institutionRows.push([mohRootId, 'Ministry_of_Health', 'Ministry of Health', mohRow.health_inst_no,
        mohRow.address, mohRow.phone, mohRow.email, null, nationalAreaId]);

    const otherPlaceholderId = insId++;
    institutionRows.push([otherPlaceholderId, 'Other', 'Other Government Health Institutions (military/police/prison)',
        null, '(synthetic grouping node — no source row, see tools/import-institutions.js)', null, null, mohRootId, nationalAreaId]);

    // 8 real "Provincial Ministry" rows -> province, via their rdhs field
    const provincialMinistryId = {};
    raw.filter((r) => r.type_atomic === 'Other' && r.name.trim() === 'Provincial Ministry').forEach((r) => {
        const province = RDHS_TO_PROVINCE[r.rdhs.trim()];
        const id = insId++;
        provincialMinistryId[province] = id;
        institutionRows.push([id, 'Provincial_Ministry_of_Health', `Provincial Ministry of Health - ${province} Province`,
            r.health_inst_no, r.address, r.phone, r.email, mohRootId, provinceAreaId[province]]);
    });

    // 9 PDHS rows, name = "<Province> Province" -> parent is that province's Provincial Ministry if it
    // exists, else the national Ministry directly (true for Western province in this registry).
    const pdhsIdByProvince = {};
    raw.filter((r) => r.type_atomic === 'PDHS').forEach((r) => {
        const province = r.name.trim().replace(/ Province$/, '');
        const id = insId++;
        pdhsIdByProvince[province] = id;
        const parent = provincialMinistryId[province] || mohRootId;
        institutionRows.push([id, 'Provincial_Department_of_Health_Services', `PDHS ${r.name.trim()}`,
            r.health_inst_no, r.address, r.phone, r.email, parent, provinceAreaId[province]]);
    });

    // 26 RDHS rows -> parent is that RDHS's province's PDHS
    const rdhsIdByDivision = {};
    raw.filter((r) => r.type_atomic === 'RDHS').forEach((r) => {
        const division = r.rdhs.trim();
        const province = RDHS_TO_PROVINCE[division];
        const id = insId++;
        rdhsIdByDivision[division] = id;
        institutionRows.push([id, 'Regional_Department_of_Health_Services', `RDHS ${division}`,
            r.health_inst_no, r.address, r.phone, r.email, pdhsIdByProvince[province], rdhsAreaId[division]]);
    });

    // Everything else: hospitals, MOH offices, clinics, PMCUs, ADC/CDC/NTS, misc "Other".
    // Tracked by row identity, not institution code -- codes can be blank (Kilinochchi's
    // RDHS row has none) or duplicated elsewhere in the dataset, so they're not a safe key.
    const handledRows = new Set([mohRow,
        ...raw.filter((r) => r.type_atomic === 'Other' && r.name.trim() === 'Provincial Ministry'),
        ...raw.filter((r) => r.type_atomic === 'PDHS'),
        ...raw.filter((r) => r.type_atomic === 'RDHS')]);

    const duplicateCodes = [];
    const seenCodes = new Set();
    let missingCodeCount = 0;

    raw.forEach((r) => {
        if (handledRows.has(r)) {
            return; // already imported above as a hierarchy node
        }
        const type = TYPE_MAP[r.type_atomic.trim()] || 'Other';
        const line = r.line.trim();
        const division = r.rdhs.trim();
        let parent;
        if (line === '1') {
            parent = mohRootId;
        } else if (line === '3') {
            parent = otherPlaceholderId;
        } else {
            parent = rdhsIdByDivision[division] || mohRootId; // fallback, should not trigger
        }
        const area = rdhsAreaId[division] || null;
        const code = r.health_inst_no || null;
        if (code) {
            if (seenCodes.has(code)) duplicateCodes.push(code);
            seenCodes.add(code);
        } else {
            missingCodeCount++;
        }
        const id = insId++;
        institutionRows.push([id, type, r.name.trim(), code, r.address, r.phone, r.email, parent, area]);
    });

    console.log(`Areas: ${areaRows.length} (1 national, ${PROVINCES.length} province, ${Object.keys(RDHS_TO_PROVINCE).length} RDHS division)`);
    console.log(`Institutions: ${institutionRows.length} (source rows: ${raw.length}, +1 synthetic: Other-placeholder)`);
    console.log(`Missing institution codes (imported as NULL): ${missingCodeCount}`);
    console.log(`Duplicate codes encountered (imported as-is, needs manual review): ${[...new Set(duplicateCodes)].join(', ') || 'none'}`);

    // ---- Self-validation: fail loudly rather than emit a broken migration ----
    const errors = [];
    if (institutionRows.length !== raw.length + 1) {
        errors.push(`expected ${raw.length + 1} institution rows (${raw.length} source + 1 synthetic), got ${institutionRows.length}`);
    }
    const insIds = new Set(institutionRows.map((r) => r[0]));
    if (insIds.size !== institutionRows.length) {
        errors.push('duplicate institution id detected');
    }
    const areaIds = new Set(areaRows.map((r) => r[0]));
    institutionRows.forEach(([id, , name, , , , , parentId, areaIdVal]) => {
        if (parentId !== null && !insIds.has(parentId)) {
            errors.push(`institution ${id} (${name}) has dangling parent_id ${parentId}`);
        }
        if (areaIdVal !== null && !areaIds.has(areaIdVal)) {
            errors.push(`institution ${id} (${name}) has dangling area_id ${areaIdVal}`);
        }
    });
    institutionRows.forEach(([id, type]) => {
        if (!Object.values(TYPE_MAP).includes(type) && !['Ministry_of_Health', 'Provincial_Ministry_of_Health', 'Other'].includes(type)) {
            errors.push(`institution ${id} has unmapped type ${type}`);
        }
    });
    // Spot checks against known records
    const castleSt = institutionRows.find((r) => r[3] === 'LCB0000166');
    if (!castleSt || castleSt[1] !== 'Specialized_Teaching_Hospital' || castleSt[7] !== mohRootId) {
        errors.push('spot check failed: Castle Street Hospital for Women (LCB0000166) should be Specialized_Teaching_Hospital reporting directly to Ministry of Health');
    }
    const dupCodeRows = institutionRows.filter((r) => r[3] === 'PCB0004630');
    if (dupCodeRows.length !== 2) {
        errors.push(`spot check failed: expected 2 institutions with duplicate code PCB0004630, found ${dupCodeRows.length}`);
    }
    const kilinochchiRdhsId = rdhsIdByDivision['Kilinochchi'];
    const kilinochchiChild = institutionRows.find((r) => r[8] === rdhsAreaId['Kilinochchi'] && r[7] === kilinochchiRdhsId && r[0] !== kilinochchiRdhsId);
    if (!kilinochchiChild) {
        errors.push('spot check failed: expected at least one facility parented to the Kilinochchi RDHS node');
    }

    if (errors.length) {
        console.error('\nVALIDATION FAILED, not writing migration file:');
        errors.forEach((e) => console.error(' - ' + e));
        process.exit(1);
    }
    console.log('Self-validation passed (row counts, no dangling FKs, all types mapped, spot checks OK).');

    // ---- Emit SQL ----
    const lines = [];
    lines.push('-- Institution registry import from D:\\Dev\\his-map\\institutions_final.json');
    lines.push('-- Generated by tools/import-institutions.js -- see that script for the full mapping');
    lines.push('-- rules and the reporting-line hierarchy this constructs.');
    lines.push('--');
    lines.push(`-- Duplicate source codes (imported as-is, flagged for manual review): ${[...new Set(duplicateCodes)].join(', ') || 'none'}`);
    lines.push(`-- Source rows with no institution code (imported with code = NULL): ${missingCodeCount}`);
    lines.push('');

    lines.push('INSERT INTO area (id, type, name, parent_area_id) VALUES');
    lines.push(areaRows.map(([id, type, name, parentId]) =>
        `  (${id}, ${sqlStr(type)}, ${sqlStr(name)}, ${parentId === null ? 'NULL' : parentId})`).join(',\n') + ';');
    lines.push('');

    // institutionRows is built in dependency order (Ministry -> Other-placeholder ->
    // Provincial Ministry -> PDHS -> RDHS -> everything else); InnoDB validates FK
    // constraints row-by-row as it processes a multi-row INSERT, so a single statement
    // with parents listed before children is safe here.
    lines.push('INSERT INTO institution (id, type, name, code, address, phone, email, parent_id, area_id) VALUES');
    lines.push(institutionRows.map(([id, type, name, code, address, phone, email, parentId, areaIdVal]) =>
        `  (${id}, ${sqlStr(type)}, ${sqlStr(name)}, ${sqlStr(code)}, ${sqlStr(address)}, ${sqlStr(phone)}, ${sqlStr(email)}, ${parentId === null ? 'NULL' : parentId}, ${areaIdVal === null ? 'NULL' : areaIdVal})`
    ).join(',\n') + ';');
    lines.push('');
    lines.push(`ALTER TABLE institution AUTO_INCREMENT = ${insId};`);
    lines.push(`ALTER TABLE area AUTO_INCREMENT = ${areaId};`);
    lines.push('');

    fs.writeFileSync(OUT, lines.join('\n'), 'utf8');
    console.log(`Wrote ${OUT}`);
}

main();
