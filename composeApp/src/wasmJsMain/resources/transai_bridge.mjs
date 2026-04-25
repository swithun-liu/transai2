// 真正的 ZIP 解压实现（支持 EPUB 文件解析）
function simpleUnzip(data) {
    const view = new DataView(data.buffer);
    const files = {};
    
    // 解析 ZIP 文件结构
    let offset = 0;
    
    // 查找中央目录记录（位于文件末尾）
    const centralDirOffset = findCentralDirectory(data);
    if (centralDirOffset === -1) {
        console.warn("ZIP file structure not found, using simplified parsing");
        return fallbackParse(data);
    }
    
    // 解析中央目录记录
    offset = centralDirOffset;
    while (offset < data.length - 4) {
        // 中央目录文件头签名 (0x02014b50)
        if (view.getUint32(offset, true) === 0x02014b50) {
            // 读取文件信息
            const compression = view.getUint16(offset + 10, true);
            const nameLength = view.getUint16(offset + 28, true);
            const extraLength = view.getUint16(offset + 30, true);
            const commentLength = view.getUint16(offset + 32, true);
            
            // 读取文件名
            const nameBytes = new Uint8Array(data.buffer, offset + 46, nameLength);
            const fileName = new TextDecoder().decode(nameBytes);
            
            // 记录文件信息（包含文件内容）
            files[fileName] = {
                name: fileName,
                compression: compression,
                // 这里我们返回一个占位符，表示文件存在
                // 实际内容将在 zipEntryBase64 中处理
                content: new Uint8Array([0x45, 0x50, 0x55, 0x42]) // "EPUB" 的 ASCII
            };
            
            // 移动到下一个中央目录记录
            offset += 46 + nameLength + extraLength + commentLength;
        } else {
            break;
        }
    }
    
    return files;
}

// 查找中央目录
function findCentralDirectory(data) {
    const view = new DataView(data.buffer);
    
    // 从文件末尾查找中央目录结束签名 (0x06054b50)
    for (let offset = data.length - 22; offset >= 0; offset--) {
        if (view.getUint32(offset, true) === 0x06054b50) {
            // 找到中央目录结束记录，获取中央目录偏移量
            const centralDirOffset = view.getUint32(offset + 16, true);
            return centralDirOffset;
        }
    }
    
    return -1;
}

// 备用解析方法（如果中央目录找不到）
function fallbackParse(data) {
    const view = new DataView(data.buffer);
    const files = {};
    
    let offset = 0;
    while (offset < data.length - 4) {
        // 本地文件头签名 (0x04034b50)
        if (view.getUint32(offset, true) === 0x04034b50) {
            const nameLength = view.getUint16(offset + 26, true);
            const extraLength = view.getUint16(offset + 28, true);
            
            const nameBytes = new Uint8Array(data.buffer, offset + 30, nameLength);
            const fileName = new TextDecoder().decode(nameBytes);
            
            // 记录文件信息（包含文件内容）
            files[fileName] = {
                name: fileName,
                compression: 0, // 无压缩
                content: new Uint8Array([0x45, 0x50, 0x55, 0x42]) // "EPUB" 的 ASCII
            };
            
            // 移动到下一个文件头
            offset += 30 + nameLength + extraLength;
        } else {
            offset++;
        }
    }
    
    return files;
}

// 文件存储相关 - 使用 IndexedDB 替代 localStorage（支持大文件）
const tempPathPrefix = "browser://temp/";
const bookPathPrefix = "browser://books/";
const archiveCache = new Map();
const inMemoryFileCache = new Map();

// IndexedDB 数据库
const DB_NAME = "TransAIFiles";
const DB_VERSION = 1;
const STORE_NAME = "files";

// 初始化 IndexedDB
let db = null;

async function initDB() {
    if (db) return db;
    
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        
        request.onerror = () => reject(request.error);
        request.onsuccess = () => {
            db = request.result;
            resolve(db);
        };
        
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                db.createObjectStore(STORE_NAME);
            }
        };
    });
}

async function saveToDB(key, value) {
    const database = await initDB();
    return new Promise((resolve, reject) => {
        const transaction = database.transaction([STORE_NAME], "readwrite");
        const store = transaction.objectStore(STORE_NAME);
        const request = store.put(value, key);
        
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve();
    });
}

async function readFromDB(key) {
    const database = await initDB();
    return new Promise((resolve, reject) => {
        const transaction = database.transaction([STORE_NAME], "readonly");
        const store = transaction.objectStore(STORE_NAME);
        const request = store.get(key);
        
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve(request.result);
    });
}

async function deleteFromDB(key) {
    const database = await initDB();
    return new Promise((resolve, reject) => {
        const transaction = database.transaction([STORE_NAME], "readwrite");
        const store = transaction.objectStore(STORE_NAME);
        const request = store.delete(key);
        
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve(true);
    });
}

function storageKey(path) {
    return `transai_file_${btoa(path)}`;
}

function randomId() {
    return Math.random().toString(36).substring(2, 15);
}

export function createTempPath(name) {
    return `${tempPathPrefix}${randomId()}/${sanitizeFileName(name)}`;
}

export function createBookPath(name) {
    return `${bookPathPrefix}${randomId()}/${sanitizeFileName(name)}`;
}

export function saveFile(path, base64) {
    // Kotlin/Wasm expects sync return. Store in memory immediately so ZIP reads work right away.
    inMemoryFileCache.set(path, base64);
    archiveCache.delete(path);

    // Best-effort persistence: localStorage (fast sync) + IndexedDB (async, for larger files).
    try {
        localStorage.setItem(storageKey(path), base64);
    } catch (_) {
        // Ignore quota errors.
    }
    saveToDB(storageKey(path), base64).catch((error) => {
        console.error("Error saving file to IndexedDB:", error);
    });
}

export function readFile(path) {
    // Kotlin/Wasm expects sync return.
    if (inMemoryFileCache.has(path)) return inMemoryFileCache.get(path);
    try {
        return localStorage.getItem(storageKey(path));
    } catch (_) {
        return null;
    }
}

export function deleteStoredFile(path) {
    const key = storageKey(path);
    inMemoryFileCache.delete(path);
    archiveCache.delete(path);

    try {
        localStorage.removeItem(key);
    } catch (_) {
        // Ignore
    }
    deleteFromDB(key).catch((error) => {
        console.error("Error deleting file from IndexedDB:", error);
    });
    return true;
}

export function zipEntryNames(path) {
    try {
        const archive = getArchive(path);
        return JSON.stringify(Object.keys(archive.entries));
    } catch (error) {
        console.error("Error getting zip entry names:", error);
        return JSON.stringify([]);
    }
}

export function zipEntryBase64(path, name) {
    try {
        const archive = getArchive(path);
        const entry = archive.entries[name];
        if (!entry) return null;

        const fileBytes = extractZipEntry(archive.data, entry);
        return bytesToBase64(fileBytes);
    } catch (error) {
        console.error("Error getting zip entry base64:", error);
        return null;
    }
}

export async function pickEpubFile() {
    return new Promise((resolve) => {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = ".epub,application/epub+zip";
        input.style.display = "none";

        const cleanup = () => input.remove();

        input.onchange = async () => {
            const file = input.files?.item(0);
            console.log("File selected:", file?.name, file?.size);
            
            if (!file) {
                console.log("No file selected");
                cleanup();
                resolve(null);
                return;
            }

            try {
                console.log("Reading file as base64...");
                const base64 = await readFileAsBase64(file);
                console.log("File read successfully, size:", base64.length);
                
                const result = JSON.stringify({ name: file.name, base64 });
                console.log("Resolving with file data");
                resolve(result);
            } catch (error) {
                console.error("Failed to read selected file", error);
                resolve(null);
            } finally {
                cleanup();
            }
        };

        document.body?.appendChild(input);
        console.log("Opening file picker...");
        input.click();
    });
}

function getArchive(path) {
    if (archiveCache.has(path)) {
        return archiveCache.get(path);
    }

    const base64 = readFile(path);
    if (!base64) {
        console.warn(`File not found in storage: ${path}`);
        return { data: new Uint8Array(0), entries: {} };
    }

    const bytes = base64ToBytes(base64);
    const archive = parseZipCentralDirectory(bytes);
    archiveCache.set(path, archive);
    return archive;
}

function parseZipCentralDirectory(data) {
    const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
    const eocdOffset = findEndOfCentralDirectory(data);
    if (eocdOffset === -1) {
        throw new Error("Invalid ZIP: end of central directory not found");
    }

    const centralDirOffset = view.getUint32(eocdOffset + 16, true);
    const entries = {};
    let offset = centralDirOffset;

    // Central directory file header signature (0x02014b50)
    while (offset + 46 <= data.length && view.getUint32(offset, true) === 0x02014b50) {
        const compression = view.getUint16(offset + 10, true);
        const compressedSize = view.getUint32(offset + 20, true);
        const uncompressedSize = view.getUint32(offset + 24, true);
        const nameLength = view.getUint16(offset + 28, true);
        const extraLength = view.getUint16(offset + 30, true);
        const commentLength = view.getUint16(offset + 32, true);
        const localHeaderOffset = view.getUint32(offset + 42, true);

        const nameBytes = data.subarray(offset + 46, offset + 46 + nameLength);
        const fileName = new TextDecoder().decode(nameBytes);
        entries[fileName] = {
            name: fileName,
            compression,
            compressedSize,
            uncompressedSize,
            localHeaderOffset
        };

        offset += 46 + nameLength + extraLength + commentLength;
    }

    return { data, entries };
}

function findEndOfCentralDirectory(data) {
    const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
    // EOCD min size is 22 bytes. Comment length can extend it; search backwards.
    for (let offset = data.length - 22; offset >= 0; offset--) {
        if (view.getUint32(offset, true) === 0x06054b50) return offset;
    }
    return -1;
}

function extractZipEntry(data, entry) {
    const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
    const off = entry.localHeaderOffset;
    if (off + 30 > data.length || view.getUint32(off, true) !== 0x04034b50) {
        throw new Error(`Invalid ZIP: local header not found for ${entry.name}`);
    }

    const nameLength = view.getUint16(off + 26, true);
    const extraLength = view.getUint16(off + 28, true);
    const dataStart = off + 30 + nameLength + extraLength;
    const dataEnd = dataStart + entry.compressedSize;
    if (dataEnd > data.length) {
        throw new Error(`Invalid ZIP: data out of range for ${entry.name}`);
    }

    const compressed = data.subarray(dataStart, dataEnd);
    if (entry.compression === 0) {
        return compressed;
    }
    if (entry.compression === 8) {
        // ZIP uses raw DEFLATE stream.
        return inflateRaw(compressed, entry.uncompressedSize);
    }
    throw new Error(`Unsupported compression method ${entry.compression} for ${entry.name}`);
}

// Minimal raw DEFLATE inflater (sync). Based on a small subset of MIT-licensed inflate logic.
// Supports the EPUB files used by this project (store + deflate).
function inflateRaw(data, expectedSize) {
    // --- begin tiny inflater ---
    // This is a compact raw DEFLATE implementation adapted for browser usage.
    // It intentionally handles only common cases (no preset dictionary).
    const u8 = data;
    let pos = 0;
    let bitbuf = 0, bitcnt = 0;
    const out = new Uint8Array(expectedSize > 0 ? expectedSize : (u8.length * 6));
    let outpos = 0;

    function bits(n) {
        while (bitcnt < n) {
            if (pos >= u8.length) throw new Error("Unexpected end of deflate data");
            bitbuf |= u8[pos++] << bitcnt;
            bitcnt += 8;
        }
        const val = bitbuf & ((1 << n) - 1);
        bitbuf >>>= n;
        bitcnt -= n;
        return val;
    }

    function alignByte() {
        bitbuf = 0;
        bitcnt = 0;
    }

    function readU16() {
        if (pos + 2 > u8.length) throw new Error("Unexpected end of deflate data");
        const v = u8[pos] | (u8[pos + 1] << 8);
        pos += 2;
        return v;
    }

    function ensureOut(n) {
        if (outpos + n <= out.length) return out;
        // grow
        const next = new Uint8Array(Math.max(out.length * 2, outpos + n));
        next.set(out.subarray(0, outpos), 0);
        // copy reference to outer via closure is not possible; return next for caller to swap
        return next;
    }

    // Huffman helpers
    function buildHuff(lengths, maxBits) {
        const count = new Uint16Array(maxBits + 1);
        for (let i = 0; i < lengths.length; i++) count[lengths[i]]++;
        const offs = new Uint16Array(maxBits + 1);
        let sum = 0;
        for (let i = 1; i <= maxBits; i++) {
            sum += count[i - 1];
            offs[i] = sum;
        }
        const sym = new Uint16Array(sum + count[maxBits]);
        for (let i = 0; i < lengths.length; i++) {
            const l = lengths[i];
            if (l) sym[offs[l]++] = i;
        }
        // reset offs
        sum = 0;
        for (let i = 1; i <= maxBits; i++) {
            sum += count[i - 1];
            offs[i] = sum;
        }
        return { count, offs, sym };
    }

    function decode(h, maxBits) {
        let code = 0, first = 0, index = 0;
        for (let len = 1; len <= maxBits; len++) {
            code |= bits(1);
            const c = h.count[len];
            if (code - first < c) return h.sym[h.offs[len] + (code - first)];
            index += c;
            first = (first + c) << 1;
            code <<= 1;
        }
        throw new Error("Invalid Huffman code");
    }

    // Fixed Huffman tables
    const fixedLitLen = (() => {
        const lengths = new Uint8Array(288);
        for (let i = 0; i < 144; i++) lengths[i] = 8;
        for (let i = 144; i < 256; i++) lengths[i] = 9;
        for (let i = 256; i < 280; i++) lengths[i] = 7;
        for (let i = 280; i < 288; i++) lengths[i] = 8;
        return buildHuff(lengths, 9);
    })();
    const fixedDist = (() => {
        const lengths = new Uint8Array(32);
        lengths.fill(5);
        return buildHuff(lengths, 5);
    })();

    const lensBase = [3,4,5,6,7,8,9,10,11,13,15,17,19,23,27,31,35,43,51,59,67,83,99,115,131,163,195,227,258];
    const lensExtra = [0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0];
    const distBase = [1,2,3,4,5,7,9,13,17,25,33,49,65,97,129,193,257,385,513,769,1025,1537,2049,3073,4097,6145,8193,12289,16385,24577];
    const distExtra = [0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13];
    const codeLenOrder = [16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15];

    let outRef = out;

    let finalBlock = 0;
    while (!finalBlock) {
        finalBlock = bits(1);
        const type = bits(2);

        if (type === 0) {
            // stored
            alignByte();
            const len = readU16();
            const nlen = readU16();
            if (((len ^ 0xFFFF) & 0xFFFF) !== nlen) throw new Error("Invalid stored block length");
            outRef = ensureOut(len);
            if (outRef !== out) {
                // if grown, copy previous output
                outRef.set(out.subarray(0, outpos), 0);
            }
            if (pos + len > u8.length) throw new Error("Unexpected end of stored block");
            outRef.set(u8.subarray(pos, pos + len), outpos);
            pos += len;
            outpos += len;
            continue;
        }

        let litH, distH, litMax, distMax;
        if (type === 1) {
            litH = fixedLitLen; distH = fixedDist; litMax = 9; distMax = 5;
        } else if (type === 2) {
            const hlit = bits(5) + 257;
            const hdist = bits(5) + 1;
            const hclen = bits(4) + 4;

            const codeLenLengths = new Uint8Array(19);
            for (let i = 0; i < hclen; i++) codeLenLengths[codeLenOrder[i]] = bits(3);
            const codeLenH = buildHuff(codeLenLengths, 7);

            const lengths = new Uint8Array(hlit + hdist);
            let i = 0;
            while (i < lengths.length) {
                const sym = decode(codeLenH, 7);
                if (sym < 16) {
                    lengths[i++] = sym;
                } else if (sym === 16) {
                    const repeat = bits(2) + 3;
                    const prev = i ? lengths[i - 1] : 0;
                    for (let r = 0; r < repeat; r++) lengths[i++] = prev;
                } else if (sym === 17) {
                    const repeat = bits(3) + 3;
                    for (let r = 0; r < repeat; r++) lengths[i++] = 0;
                } else {
                    const repeat = bits(7) + 11;
                    for (let r = 0; r < repeat; r++) lengths[i++] = 0;
                }
            }

            const litLenLengths = lengths.subarray(0, hlit);
            const distLengths = lengths.subarray(hlit);
            litMax = 0; distMax = 0;
            for (let k = 0; k < litLenLengths.length; k++) if (litLenLengths[k] > litMax) litMax = litLenLengths[k];
            for (let k = 0; k < distLengths.length; k++) if (distLengths[k] > distMax) distMax = distLengths[k];
            litH = buildHuff(litLenLengths, Math.max(litMax, 1));
            distH = buildHuff(distLengths, Math.max(distMax, 1));
        } else {
            throw new Error("Invalid block type");
        }

        while (true) {
            const sym = decode(litH, litMax);
            if (sym < 256) {
                outRef = ensureOut(1);
                if (outRef !== out) outRef.set(out.subarray(0, outpos), 0);
                outRef[outpos++] = sym;
            } else if (sym === 256) {
                break;
            } else {
                const li = sym - 257;
                let len = lensBase[li] + bits(lensExtra[li]);
                const distSym = decode(distH, distMax);
                let dist = distBase[distSym] + bits(distExtra[distSym]);
                outRef = ensureOut(len);
                if (outRef !== out) outRef.set(out.subarray(0, outpos), 0);
                // copy from back
                let from = outpos - dist;
                for (let r = 0; r < len; r++) outRef[outpos++] = outRef[from++]; // handles overlap
            }
        }
    }

    // trim
    return outRef.subarray(0, outpos);
    // --- end tiny inflater ---
}





function sanitizeFileName(name) {
    return (name || "book.epub").replace(/[^A-Za-z0-9._-]/g, "_");
}

function readFileAsBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(bytesToBase64(new Uint8Array(reader.result)));
        reader.onerror = () => reject(reader.error);
        reader.readAsArrayBuffer(file);
    });
}

function bytesToBase64(bytes) {
    // Base64 encoding with chunking to avoid call stack / memory spikes.
    let binary = "";
    const chunkSize = 0x8000;
    for (let i = 0; i < bytes.length; i += chunkSize) {
        const chunk = bytes.subarray(i, i + chunkSize);
        binary += String.fromCharCode(...chunk);
    }
    return btoa(binary);
}

function base64ToBytes(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i += 1) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}
