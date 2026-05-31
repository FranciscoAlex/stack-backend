-- Create banner_config table
CREATE TABLE IF NOT EXISTS banner_config (
    id BIGSERIAL PRIMARY KEY,
    page_key VARCHAR(50) UNIQUE NOT NULL,
    image_url VARCHAR(1000),
    title VARCHAR(200),
    subtitle VARCHAR(500),
    link_url VARCHAR(1000),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create slideshow_config table
CREATE TABLE IF NOT EXISTS slideshow_config (
    id BIGSERIAL PRIMARY KEY,
    page_key VARCHAR(50) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    display_order INTEGER,
    title VARCHAR(200),
    subtitle VARCHAR(500),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_slideshow_page_key ON slideshow_config(page_key);
CREATE INDEX idx_slideshow_display_order ON slideshow_config(display_order);

-- Insert default banner for home page
INSERT INTO banner_config (page_key, image_url, title, subtitle, link_url, enabled)
VALUES (
    'home',
    'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600&h=250&fit=crop',
    'Comunidade de Stakeholders',
    'Plataforma privada para stakeholders financeiros e investidores',
    '#',
    true
) ON CONFLICT (page_key) DO NOTHING;

-- Insert default slideshows for crowdfunding page
INSERT INTO slideshow_config (page_key, image_url, display_order, title, subtitle, enabled)
VALUES
    ('crowdfunding', 'https://images.unsplash.com/photo-1579532537598-459ecdaf39cc?w=800&auto=format&fit=crop', 0, NULL, NULL, true),
    ('crowdfunding', 'https://images.unsplash.com/photo-1560520653-9e0e4c89eb11?w=800&auto=format&fit=crop', 1, NULL, NULL, true),
    ('crowdfunding', 'https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=800&auto=format&fit=crop', 2, NULL, NULL, true)
ON CONFLICT DO NOTHING;

-- Insert default slideshows for formation page
INSERT INTO slideshow_config (page_key, image_url, display_order, title, subtitle, enabled)
VALUES
    ('formation', 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&auto=format&fit=crop', 0, NULL, NULL, true),
    ('formation', 'https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=800&auto=format&fit=crop', 1, NULL, NULL, true),
    ('formation', 'https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&auto=format&fit=crop', 2, NULL, NULL, true)
ON CONFLICT DO NOTHING;

