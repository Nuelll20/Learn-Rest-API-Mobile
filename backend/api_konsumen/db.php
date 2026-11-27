<?php

declare(strict_types=1);

function bukaDatabase(): PDO
{
    $config = [
        'host' => '127.0.0.1',
        'port' => 3306,
        'user' => 'root',
        'password' => '',
    ];

    $fileLokal = __DIR__ . '/db.local.php';
    if (is_file($fileLokal)) {
        $config = array_replace($config, require $fileLokal);
    }

    $dsn = "mysql:host={$config['host']};port={$config['port']};"
         . "dbname=praktikum_android;charset=utf8mb4";

    return new PDO($dsn, $config['user'], $config['password'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
}