CREATE DATABASE IF NOT EXISTS praktikum_android
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE praktikum_android;

CREATE TABLE IF NOT EXISTS konsumen (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telepon VARCHAR(25) NOT NULL,
    alamat VARCHAR(255) NOT NULL
);

INSERT INTO konsumen (nama, email, telepon, alamat)
SELECT 'Dina Putri', 'dina@example.test', '081234567890', 'Bandar Lampung'
WHERE NOT EXISTS (
    SELECT 1 FROM konsumen WHERE email = 'dina@example.test'
);