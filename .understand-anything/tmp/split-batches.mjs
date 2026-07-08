import fs from 'fs';
import path from 'path';

const root = process.argv[2];
const batchesPath = path.join(root, '.understand-anything/intermediate/batches.json');
const data = JSON.parse(fs.readFileSync(batchesPath, 'utf8'));

const outDir = path.join(root, '.understand-anything/intermediate/batch-inputs');
fs.mkdirSync(outDir, { recursive: true });

for (const batch of data.batches) {
  const idx = batch.batchIndex;
  const files = batch.files;
  const batchImportData = {};
  for (const f of files) {
    batchImportData[f.path] = batch.batchImportData[f.path] || [];
  }
  // neighborMap: for each import target not in this batch, find its exports
  const neighborMap = {};
  const inBatch = new Set(files.map(f => f.path));
  for (const f of files) {
    for (const dep of (batch.batchImportData[f.path] || [])) {
      if (!inBatch.has(dep) && data.exportsByPath[dep] !== undefined) {
        neighborMap[dep] = data.exportsByPath[dep];
      }
    }
  }
  const out = {
    batchIndex: idx,
    totalBatches: data.totalBatches,
    files,
    batchImportData,
    neighborMap
  };
  fs.writeFileSync(path.join(outDir, `batch-${idx}-input.json`), JSON.stringify(out, null, 2));
}
console.log(`Wrote ${data.batches.length} batch input files to ${outDir}`);
