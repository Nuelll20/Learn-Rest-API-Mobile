<?php

declare(strict_types=1);

require __DIR__ . '/db.php';

header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    header('Allow: GET');
    http_response_code(405);
    echo json_encode(['pesan' => 'Metode HTTP tidak didukung.']);
    exit;
}

try {
    $db = bukaDatabase();

    $query = $db->query(
        'SELECT id, nama, email, telepon, alamat
         FROM konsumen
         ORDER BY id DESC'
    );

    echo json_encode(
        ['data' => $query->fetchAll()],
        JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR
    );
} catch (Throwable $error) {
    error_log($error->getMessage());
    http_response_code(500);
    echo json_encode(['pesan' => 'Kesalahan server. Periksa log PHP.']);
}