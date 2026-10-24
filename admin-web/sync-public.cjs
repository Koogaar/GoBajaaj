const fs = require('fs');
const path = require('path');

const distDir = path.resolve(__dirname, 'dist');
const publicDir = path.resolve(__dirname, '..', 'public');

if (!fs.existsSync(distDir)) {
  console.error('dist directory does not exist, run build first.');
  process.exit(1);
}

if (!fs.existsSync(publicDir)) fs.mkdirSync(publicDir, { recursive: true });
const publicAssetsDir = path.join(publicDir, 'assets');
if (!fs.existsSync(publicAssetsDir)) fs.mkdirSync(publicAssetsDir, { recursive: true });

// Clean old js/css bundles in public/assets to prevent clutter
const oldAssets = fs.readdirSync(publicAssetsDir);
for (const file of oldAssets) {
  if (file.endsWith('.js') || file.endsWith('.css') || file.endsWith('.map')) {
    fs.unlinkSync(path.join(publicAssetsDir, file));
  }
}

// Copy dist root files (skip heavy binaries)
for (const file of fs.readdirSync(distDir)) {
  const src = path.join(distDir, file);
  const dest = path.join(publicDir, file);
  if (fs.statSync(src).isDirectory()) continue;
  if (file.endsWith('.apk') || file.endsWith('.aab')) continue;
  fs.copyFileSync(src, dest);
}

// Copy dist/assets
const distAssetsDir = path.join(distDir, 'assets');
if (fs.existsSync(distAssetsDir)) {
  for (const file of fs.readdirSync(distAssetsDir)) {
    fs.copyFileSync(path.join(distAssetsDir, file), path.join(publicAssetsDir, file));
  }
}

console.log('Successfully synced admin-web build to root public/ directory!');
