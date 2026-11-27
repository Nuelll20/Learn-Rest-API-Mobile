<?php

declare(strict_types=1);

require __DIR__ . '/db.php';

function balas(int $status, array $isi): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($isi, JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR);
    exit;
}

function bacaKonsumen(): array
{
    $input = json_decode(file_get_contents('php://input'));
    if (!is_object($input)) {
        balas(400, ['pesan' => 'Body harus berupa objek JSON.']);
    }

    $nilai = [];
    $batas = ['nama' => 100, 'email' => 150, 'telepon' => 25, 'alamat' => 255];

    foreach ($batas as $kolom => $maksimum) {
        $teks = $input->$kolom ?? null;
        $teks = is_string($teks) ? trim($teks) : '';

        if ($teks === '' || mb_strlen($teks, 'UTF-8') > $maksimum) {
            balas(422, ['pesan' => "$kolom wajib diisi, maksimal $maksimum karakter."]);
        }

        $nilai[$kolom] = $teks;
    }

    if (!filter_var($nilai['email'], FILTER_VALIDATE_EMAIL)) {
        balas(422, ['pesan' => 'Format email tidak valid.']);
    }

    return $nilai;
}

$metode = $_SERVER['REQUEST_METHOD'];
if (!in_array($metode, ['GET', 'POST', 'PUT', 'DELETE'], true)) {
    header('Allow: GET, POST, PUT, DELETE');
    balas(405, ['pesan' => 'Metode HTTP tidak didukung.']);
}

$id = null;
if (isset($_GET['id']) || in_array($metode, ['PUT', 'DELETE'], true)) {
    $id = filter_input(INPUT_GET, 'id', FILTER_VALIDATE_INT, [
        'options' => ['min_range' => 1, 'max_range' => 4294967295],
    ]);

    if (!$id || $metode === 'POST') {
        balas(400, ['pesan' => 'Periksa parameter id.']);
    }
}

try {
    if ($metode === 'POST') {
        $nilai = bacaKonsumen();
        $db = bukaDatabase();
        $sql = 'INSERT INTO konsumen (nama, email, telepon, alamat)
                VALUES (:nama, :email, :telepon, :alamat)';
        $db->prepare($sql)->execute($nilai);
        balas(201, ['id' => (int) $db->lastInsertId()]);
    }

    $db = bukaDatabase();
        if ($metode === 'PUT' || $metode === 'DELETE') {
            if ($metode === 'PUT') {
                $nilai = bacaKonsumen();
                $sql = 'UPDATE konsumen
                        SET nama = :nama, email = :email, telepon = :telepon, alamat = :alamat
                        WHERE id = :id';
                $query = $db->prepare($sql);
                $query->execute($nilai + ['id' => $id]);
                $pesan = 'Data diperbarui.';
            } else {
                $query = $db->prepare('DELETE FROM konsumen WHERE id = ?');
                $query->execute([$id]);
                $pesan = 'Data dihapus.';
            }

            if ($query->rowCount() === 0) {
                balas(404, ['pesan' => 'Konsumen tidak ditemukan.']);
            }

            balas(200, ['pesan' => $pesan]);
        }

    $sql = 'SELECT id, nama, email, telepon, alamat FROM konsumen';

    if ($id === null) {
        $data = $db->query($sql . ' ORDER BY id DESC')->fetchAll();
        balas(200, ['data' => $data]);
    }

    $query = $db->prepare($sql . ' WHERE id = ?');
    $query->execute([$id]);
    $data = $query->fetch();

    if ($data === false) {
        balas(404, ['pesan' => 'Konsumen tidak ditemukan.']);
    }

    balas(200, ['data' => $data]);
} catch (Throwable $error) {
    error_log($error->getMessage());
    balas(500, ['pesan' => 'Kesalahan server. Periksa log PHP.']);
}