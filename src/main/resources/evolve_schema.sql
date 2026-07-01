CREATE TABLE IF NOT EXISTS vector.evolve_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    module_name VARCHAR(255) NOT NULL,

    file_name VARCHAR(255) NOT NULL,

    file_extension VARCHAR(20) NOT NULL,

    file_size BIGINT NOT NULL,

    minio_object_path TEXT NOT NULL,

    presigned_url TEXT,

    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    status VARCHAR(20) DEFAULT 'PENDING'
);
