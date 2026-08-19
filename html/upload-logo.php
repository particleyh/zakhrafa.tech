<?php
$uploadDir = __DIR__ . '/uploads/logos';
$publicDir = '/uploads/logos';
$maxBytes = 8 * 1024 * 1024;
$allowedTypes = [
    'image/png' => 'png',
    'image/jpeg' => 'jpg',
    'image/webp' => 'webp',
    'image/svg+xml' => 'svg',
];
$messages = [];
$saved = [];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }

    foreach (['logo_one', 'logo_two'] as $field) {
        if (empty($_FILES[$field]) || $_FILES[$field]['error'] === UPLOAD_ERR_NO_FILE) {
            continue;
        }

        $file = $_FILES[$field];
        if ($file['error'] !== UPLOAD_ERR_OK) {
            $messages[] = 'تعذر رفع أحد الملفات. جرّب مرة أخرى.';
            continue;
        }

        if ($file['size'] > $maxBytes) {
            $messages[] = 'حجم الملف كبير. الحد الأقصى 8MB لكل صورة.';
            continue;
        }

        $detectedType = mime_content_type($file['tmp_name']);
        if (!isset($allowedTypes[$detectedType])) {
            $messages[] = 'ارفع صورة فقط: PNG أو JPG أو WEBP أو SVG.';
            continue;
        }

        $safeName = preg_replace('/[^a-zA-Z0-9._-]+/', '-', pathinfo($file['name'], PATHINFO_FILENAME));
        $safeName = trim($safeName, '-_.') ?: 'logo';
        $filename = date('Ymd-His') . '-' . $field . '-' . $safeName . '.' . $allowedTypes[$detectedType];
        $target = $uploadDir . '/' . $filename;

        if (move_uploaded_file($file['tmp_name'], $target)) {
            $saved[] = $publicDir . '/' . $filename;
        } else {
            $messages[] = 'لم يتم حفظ الملف على السيرفر.';
        }
    }

    if (!$saved && !$messages) {
        $messages[] = 'اختر صورة واحدة على الأقل.';
    }
}
?>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>رفع شعار زخرفة</title>
  <meta name="robots" content="noindex,nofollow">
  <link rel="stylesheet" href="/css/style.css">
  <style>
    .upload-page { min-height: 70vh; display: grid; place-items: center; padding: 32px 16px; }
    .upload-box { width: min(560px, 100%); background: #fff; border: 1px solid #dde3ea; border-radius: 10px; padding: 22px; box-shadow: 0 12px 30px rgba(16, 24, 40, .08); }
    .upload-box h1 { margin: 0 0 8px; font-size: 26px; }
    .upload-box p { color: #667085; margin: 0 0 18px; }
    .upload-field { display: grid; gap: 8px; margin: 14px 0; }
    .upload-field label { font-weight: 700; color: #101828; }
    .upload-field input { border: 1px solid #ccd5df; border-radius: 8px; padding: 12px; background: #f8fafc; }
    .upload-actions button { width: 100%; border: 0; border-radius: 8px; padding: 14px; background: #136f63; color: white; font-size: 17px; font-weight: 800; cursor: pointer; }
    .upload-result { margin-top: 16px; padding: 12px; border-radius: 8px; background: #e7f4f1; color: #0f5f56; }
    .upload-error { margin-top: 16px; padding: 12px; border-radius: 8px; background: #fff1f0; color: #b42318; }
    .upload-result a { display: block; direction: ltr; text-align: left; overflow-wrap: anywhere; margin-top: 6px; }
  </style>
</head>
<body>
<header class="site-header"><div class="hdr"><a href="/" class="brand"><img src="/logo.png" alt="زخرفة" class="site-logo"><span>زخرفة</span></a></div></header>
<main class="upload-page">
  <form class="upload-box" method="post" enctype="multipart/form-data">
    <h1>رفع الشعارات</h1>
    <p>ارفع شعارين هنا، وسأجدهم داخل مجلد <strong>html/uploads/logos</strong>.</p>

    <div class="upload-field">
      <label for="logo_one">الشعار الأول</label>
      <input id="logo_one" name="logo_one" type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml">
    </div>

    <div class="upload-field">
      <label for="logo_two">الشعار الثاني</label>
      <input id="logo_two" name="logo_two" type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml">
    </div>

    <div class="upload-actions">
      <button type="submit">رفع الصور</button>
    </div>

    <?php if ($saved): ?>
      <div class="upload-result">
        تم رفع الملفات:
        <?php foreach ($saved as $path): ?>
          <a href="<?= htmlspecialchars($path, ENT_QUOTES, 'UTF-8') ?>" target="_blank" rel="noopener">https://zakhrafa.tech<?= htmlspecialchars($path, ENT_QUOTES, 'UTF-8') ?></a>
        <?php endforeach; ?>
      </div>
    <?php endif; ?>

    <?php if ($messages): ?>
      <div class="upload-error">
        <?php foreach ($messages as $message): ?>
          <div><?= htmlspecialchars($message, ENT_QUOTES, 'UTF-8') ?></div>
        <?php endforeach; ?>
      </div>
    <?php endif; ?>
  </form>
</main>
</body>
</html>
