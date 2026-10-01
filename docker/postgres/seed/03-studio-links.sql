-- 03-studio-links.sql
-- Idempotent seed data for CTI entities and tenanted links.

SET search_path = njall_users, public;

-- CTI Root Entities for Links
INSERT INTO njall_users.entities (id, tenant_id, entity_type, summary) VALUES
    ('018d0000-0000-7000-8000-000000000101', '018d0000-0000-7000-8000-000000000001', 'Link', 'Valkyrie Example Larp Official Website'),
    ('018d0000-0000-7000-8000-000000000102', '018d0000-0000-7000-8000-000000000001', 'Link', 'Valkyrie Example Community Discord Server'),
    ('018d0000-0000-7000-8000-000000000103', '018d0000-0000-7000-8000-000000000001', 'Link', 'Valkyrie Example Larp Core Rules v1'),
    ('018d0000-0000-7000-8000-000000000104', '018d0000-0000-7000-8000-000000000002', 'Link', 'Ironwood Fake Larp Chronicles Portal'),
    ('018d0000-0000-7000-8000-000000000105', '018d0000-0000-7000-8000-000000000002', 'Link', 'Ironwood Fake Rulebook and Setting Lore')
ON CONFLICT (id) DO NOTHING;

-- Links Subtype Records
INSERT INTO njall_users.links (tenant_id, id, link_type, url, media_type) VALUES
    ('018d0000-0000-7000-8000-000000000001', '018d0000-0000-7000-8000-000000000101', 'WEBSITE', 'https://valkyrie.example.com', 'text/html'),
    ('018d0000-0000-7000-8000-000000000001', '018d0000-0000-7000-8000-000000000102', 'DISCORD', 'https://discord.example.com/invite/valkyrie-larp', 'text/html'),
    ('018d0000-0000-7000-8000-000000000001', '018d0000-0000-7000-8000-000000000103', 'RULEBOOK', 'https://rules.valkyrie.example.com/guide.pdf', 'application/pdf'),
    ('018d0000-0000-7000-8000-000000000002', '018d0000-0000-7000-8000-000000000104', 'WEBSITE', 'https://ironwood.fake.org', 'text/html'),
    ('018d0000-0000-7000-8000-000000000002', '018d0000-0000-7000-8000-000000000105', 'RULEBOOK', 'https://lore.ironwood.fake.org/rules.html', 'text/html')
ON CONFLICT (id) DO NOTHING;
