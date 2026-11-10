class PcmProcessor extends AudioWorkletProcessor {
    constructor() {
        super();
        this.chunks = [];
        this.size = 0;
    }
    process(inputs) {
        const input = inputs[0][0];
        if (input) {
            this.chunks.push(new Float32Array(input));
            this.size += input.length;
            if (this.size >= 4096) { // about 256 ms of audio at 16 kHz
                const out = new Float32Array(this.size);
                let offset = 0;
                for (const c of this.chunks) { out.set(c, offset); offset += c.length; }
                this.port.postMessage(out.buffer, [out.buffer]);
                this.chunks = [];
                this.size = 0;
            }
        }
        return true;
    }
}
registerProcessor("pcm-processor", PcmProcessor);