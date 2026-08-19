<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Cache-Control: public, max-age=86400');

// This JSON mirrors the Kotlin engine styles
// Sync endpoint for both Android apps

$arabicStyles = [];
// Include the same style data
// (In production, this would be auto-generated from the engine)

echo json_encode([
    'version' => 1,
    'updated' => date('Y-m-d'),
    'totalStyles' => 164,
    'arabicCount' => 56,
    'englishCount' => 49,
    'styles' => []
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
