onmessage = function(event) {
    const data = event.data;
    if (data && data instanceof FileList && data.length > 0) {
        const reader = new FileReaderSync();
        let items = [];
        for (const file of data) {
            const buffer = reader.readAsArrayBuffer(file);
            items.push(buffer);
        }
        postMessage(items);
    }
    close()
};