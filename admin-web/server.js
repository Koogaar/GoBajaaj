import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DIST_DIR = path.join(__dirname, 'dist');
const PORT = 3000;

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.aab': 'application/octet-stream',
  '.apk': 'application/vnd.android.package-archive'
};

const server = http.createServer((req, res) => {
  // Parse URL path
  let reqPath = decodeURI(req.url.split('?')[0]);
  if (reqPath === '/' || reqPath === '') {
    reqPath = '/index.html';
  }

  let filePath = path.join(DIST_DIR, reqPath);

  // Security: prevent directory traversal
  if (!filePath.startsWith(DIST_DIR)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  // If path doesn't exist or is a directory, fallback to index.html for SPA,
  // unless requesting a specific file with extension like .aab or .apk
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    if (path.extname(reqPath)) {
      res.writeHead(404);
      res.end('File Not Found');
      return;
    }
    filePath = path.join(DIST_DIR, 'index.html');
  }

  try {
    const stat = fs.statSync(filePath);
    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    const fileName = path.basename(filePath);

    const headers = {
      'Content-Type': contentType,
      'Accept-Ranges': 'bytes',
      'Access-Control-Allow-Origin': '*'
    };

    // For downloadable binaries (aab, apk), force proper attachment headers
    if (ext === '.aab' || ext === '.apk') {
      headers['Content-Disposition'] = `attachment; filename="${fileName}"`;
      headers['Cache-Control'] = 'public, max-age=86400';
    }

    // Handle Range Requests for mobile downloads
    const range = req.headers.range;
    if (range) {
      const parts = range.replace(/bytes=/, '').split('-');
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : stat.size - 1;

      if (start >= stat.size || end >= stat.size) {
        res.writeHead(416, { 'Content-Range': `bytes */${stat.size}` });
        res.end();
        return;
      }

      headers['Content-Range'] = `bytes ${start}-${end}/${stat.size}`;
      headers['Content-Length'] = end - start + 1;
      res.writeHead(206, headers);
      const stream = fs.createReadStream(filePath, { start, end });
      stream.on('error', () => { if (!res.writableEnded) res.end(); });
      res.on('close', () => { stream.destroy(); });
      res.on('error', () => { stream.destroy(); });
      stream.pipe(res);
    } else {
      headers['Content-Length'] = stat.size;
      res.writeHead(200, headers);
      const stream = fs.createReadStream(filePath);
      stream.on('error', () => { if (!res.writableEnded) res.end(); });
      res.on('close', () => { stream.destroy(); });
      res.on('error', () => { stream.destroy(); });
      stream.pipe(res);
    }
  } catch (err) {
    console.error('Server error:', err);
    if (!res.headersSent) {
      res.writeHead(500);
    }
    res.end('Internal Server Error');
  }
});

process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:', err?.message || err);
});

process.on('unhandledRejection', (reason) => {
  console.error('Unhandled Rejection:', reason);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`GoBajaaj Web Admin running on http://0.0.0.0:${PORT}`);
});
