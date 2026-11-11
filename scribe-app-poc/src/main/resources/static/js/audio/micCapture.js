export class MicCapture {
    #audioCtx = null;
    #micStream = null;
    #workletNode = null;
    #onChunkCallback = null;

    constructor(onChunkCallback) {
        this.#onChunkCallback = onChunkCallback;
    }

    async start() {
        this.#micStream = await navigator.mediaDevices.getUserMedia({
            audio: {
                channelCount: 1,
                echoCancellation: true,
                noiseSuppression: true
            }
        });

        this.#audioCtx = new AudioContext({ sampleRate: 16000 });
        await this.#audioCtx.audioWorklet.addModule("pcm-worklet.js");

        const source = this.#audioCtx.createMediaStreamSource(this.#micStream);
        this.#workletNode = new AudioWorkletNode(this.#audioCtx, "pcm-processor");

        this.#workletNode.port.onmessage = (event) => {
            if (this.#onChunkCallback) {
                this.#onChunkCallback(event.data);
            }
        };

        source.connect(this.#workletNode);
        this.#workletNode.connect(this.#audioCtx.destination);
    }

    stop() {
        if (this.#workletNode) {
            this.#workletNode.disconnect();
            this.#workletNode = null;
        }
        if (this.#micStream) {
            this.#micStream.getTracks().forEach(t => t.stop());
            this.#micStream = null;
        }
        if (this.#audioCtx) {
            this.#audioCtx.close();
            this.#audioCtx = null;
        }
    }
}

