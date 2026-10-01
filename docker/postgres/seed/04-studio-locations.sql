-- 04-studio-locations.sql
-- Idempotent seed data for CTI location entities, locations, and spatial addresses.

SET search_path = njall_users, public;

-- CTI Root Entities for Locations
INSERT INTO njall_users.entities (id, tenant_id, entity_type, summary) VALUES
    ('018d0000-0000-7000-8000-000000000201', '018d0000-0000-7000-8000-000000000001', 'Location', 'Outdoor campsite venue for Valkyrie Example Larp weekend games'),
    ('018d0000-0000-7000-8000-000000000202', '018d0000-0000-7000-8000-000000000002', 'Location', 'Historical manor grounds for Ironwood Fake Larp events')
ON CONFLICT (id) DO NOTHING;

-- Locations Subtype Records
INSERT INTO njall_users.locations (tenant_id, id, name) VALUES
    ('018d0000-0000-7000-8000-000000000001', '018d0000-0000-7000-8000-000000000201', 'Camp Example'),
    ('018d0000-0000-7000-8000-000000000002', '018d0000-0000-7000-8000-000000000202', 'Blackthorn Fake Manor Grounds')
ON CONFLICT (id) DO NOTHING;

-- Addresses Records with PostGIS Coordinates
INSERT INTO njall_users.addresses (
    tenant_id, id, location_id, address_type,
    address_line_1, address_line_2, address_line_3,
    locality, administrative_area, postal_code, country_code, geom
) VALUES
    (
        '018d0000-0000-7000-8000-000000000001',
        '018d0000-0000-7000-8000-000000000301',
        '018d0000-0000-7000-8000-000000000201',
        'PHYSICAL',
        '101 Fake Needle Way',
        'Site Example Cabin 4',
        '',
        'Seattle',
        'WA',
        '98101',
        'US',
        ST_SetSRID(ST_MakePoint(-122.3321, 47.6062), 4326)::geography
    ),
    (
        '018d0000-0000-7000-8000-000000000002',
        '018d0000-0000-7000-8000-000000000302',
        '018d0000-0000-7000-8000-000000000202',
        'PHYSICAL',
        '42 Fake Manor Road',
        'Gatehouse Example',
        '',
        'Oxford',
        'Oxfordshire',
        'OX1 1AA',
        'GB',
        ST_SetSRID(ST_MakePoint(-1.2577, 51.7520), 4326)::geography
    )
ON CONFLICT (id) DO NOTHING;
