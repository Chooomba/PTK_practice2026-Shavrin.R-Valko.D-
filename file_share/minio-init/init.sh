#!/bin/sh
set -e

echo "Waiting for MinIO to be ready..."
until mc alias set local http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" 2>/dev/null; do
  sleep 2
done

echo "MinIO is ready. Creating bucket..."
mc mb --ignore-existing local/files

echo "Bucket 'files' is ready."
