// 使用 CDN 引入 fflate，避免 npm 依赖问题
let fflate;

// 动态加载 fflate
async function loadFflate() {
    if (!fflate) {
        // 使用 jsDelivr CDN
        const fflateUrl = 'https://cdn.jsdelivr.net/npm/fflate@0.8.2/+esm';
        const module = await import(fflateUrl);
        fflate = module;
    }
    return fflate;
}

const storageKeyPrefix = "transai.browser.file.";
const tempPathPrefix = "browser://temp/";
const bookPathPrefix = "browser://book/";
const archiveCache = new Map();

export function createTempPath(name) {
    return `${tempPathPrefix}${randomId()}/${sanitizeFileName(name)}`;
}

export function createBookPath(name) {
    return `${bookPathPrefix}${randomId()}/${sanitizeFileName(name)}`;
}

export function saveFile(path, base64) {
    localStorage.setItem(storageKey(path), base64);
    archiveCache.delete(path);
}

export function readFile(path) {
    return localStorage.getItem(storageKey(path));
}

export function deleteStoredFile(path) {
    const key = storageKey(path);
    const exists = localStorage.getItem(key) !== null;
    if (exists) {
        localStorage.removeItem(key);
    }
    archiveCache.delete(path);
    return exists;
}

export async function zipEntryNames(path) {
    const archive = await getArchive(path);
    return JSON.stringify(Object.keys(archive));
}

export async function zipEntryBase64(path, name) {
    const archive = await getArchive(path);
    const entry = archive[name];
    if (!entry) {
        return null;
    }
    return bytesToBase64(entry);
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
            if (!file) {
                cleanup();
                resolve(null);
                return;
            }

            try {
                const base64 = await readFileAsBase64(file);
                resolve(JSON.stringify({ name: file.name, base64 }));
            } catch (error) {
                console.error("Failed to read selected file", error);
                resolve(null);
            } finally {
                cleanup();
            }
        };

        document.body?.appendChild(input);
        input.click();
    });
}

async function getArchive(path) {
    if (archiveCache.has(path)) {
        return archiveCache.get(path);
    }

    const base64 = readFile(path);
    if (!base64) {
        return {};
    }

    const fflateModule = await loadFflate();
    const archive = fflateModule.unzipSync(base64ToBytes(base64));
    archiveCache.set(path, archive);
    return archive;
}

function storageKey(path) {
    return storageKeyPrefix + path;
}

function randomId() {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
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
    let binary = "";
    const chunkSize = 0x8000;
    for (let offset = 0; offset < bytes.length; offset += chunkSize) {
        const chunk = bytes.subarray(offset, Math.min(offset + chunkSize, bytes.length));
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
