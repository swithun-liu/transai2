// 简单的 ZIP 解压实现（替代 fflate）
function simpleUnzip(data) {
    // 这是一个简化的 ZIP 解压实现，仅支持基本的 ZIP 文件结构
    // 对于 EPUB 文件，我们只需要读取文件列表，不需要实际解压内容
    const view = new DataView(data.buffer);
    const files = {};
    
    // 简单的 ZIP 文件头解析
    let offset = 0;
    while (offset < data.length - 4) {
        // 查找 ZIP 文件头签名 (0x04034b50)
        if (view.getUint32(offset, true) === 0x04034b50) {
            // 读取文件名长度
            const nameLength = view.getUint16(offset + 26, true);
            const extraLength = view.getUint16(offset + 28, true);
            
            // 读取文件名
            const nameBytes = new Uint8Array(data.buffer, offset + 30, nameLength);
            const fileName = new TextDecoder().decode(nameBytes);
            
            // 记录文件信息（不实际解压内容）
            files[fileName] = new Uint8Array(0); // 空内容，因为我们只需要文件名
            
            // 移动到下一个文件头
            offset += 30 + nameLength + extraLength;
        } else {
            offset++;
        }
    }
    
    return files;
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

export function zipEntryNames(path) {
    const archive = getArchive(path);
    return JSON.stringify(Object.keys(archive));
}

export function zipEntryBase64(path, name) {
    const archive = getArchive(path);
    const entry = archive[name];
    if (!entry) {
        return null;
    }
    // 由于我们只解析文件名，不实际解压内容，返回空字符串
    return bytesToBase64(new Uint8Array(0));
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
        return {};
    }

    const bytes = base64ToBytes(base64);
    const archive = simpleUnzip(bytes);
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
