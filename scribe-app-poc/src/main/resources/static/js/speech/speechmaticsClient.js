export class SpeechmaticsClient {
    #ws = null;
    #seqNo = 0;
    #ready = false;
    #callbacks = {};

    constructor(callbacks = {}) {
        this.#callbacks = callbacks;
    }

    connect(token, language, wsHost = "global.rt.speechmatics.com") {
        return new Promise((resolve, reject) => {
            this.#seqNo = 0;
            this.#ready = false;

            const url = `wss://${wsHost}/v2?jwt=${encodeURIComponent(token)}`;
            this.#ws = new WebSocket(url);
            this.#ws.binaryType = "arraybuffer";

            this.#ws.onopen = () => {
                this.#ws.send(JSON.stringify({
                    message: "StartRecognition",
                    audio_format: {
                        type: "raw",
                        encoding: "pcm_f32le",
                        sample_rate: 16000
                    },
                    transcription_config: {
                        language: language,
                        enable_partials: true,
                        max_delay: 2
                    }
                }));
                resolve();
            };

            this.#ws.onmessage = (event) => {
                try {
                    const msg = JSON.parse(event.data);
                    this.#handleMessage(msg);
                } catch (err) {
                    console.error("Failed to parse Speechmatics WS message:", err);
                }
            };

            this.#ws.onerror = (evt) => {
                const err = "Speechmatics WebSocket connection error";
                if (this.#callbacks.onError) this.#callbacks.onError(err);
                reject(new Error(err));
            };

            this.#ws.onclose = () => {
                this.#ready = false;
                if (this.#callbacks.onClose) this.#callbacks.onClose();
            };
        });
    }

    #handleMessage(msg) {
        switch (msg.message) {
            case "RecognitionStarted":
                this.#ready = true;
                if (this.#callbacks.onRecognitionStarted) {
                    this.#callbacks.onRecognitionStarted();
                }
                break;
            case "AddPartialTranscript":
                if (this.#callbacks.onPartialTranscript && msg.metadata?.transcript) {
                    this.#callbacks.onPartialTranscript(msg.metadata.transcript);
                }
                break;
            case "AddTranscript":
                if (this.#callbacks.onFinalTranscript && msg.metadata?.transcript) {
                    this.#callbacks.onFinalTranscript(msg.metadata.transcript);
                }
                break;
            case "EndOfTranscript":
                this.#ready = false;
                if (this.#callbacks.onEndOfTranscript) {
                    this.#callbacks.onEndOfTranscript();
                }
                break;
            case "Error":
            case "Warning":
                const err = `${msg.message}: ${msg.reason || msg.type || "Unknown"}`;
                if (this.#callbacks.onError) {
                    this.#callbacks.onError(err);
                }
                break;
        }
    }

    sendAudioChunk(chunkBuffer) {
        if (this.#ready && this.#ws && this.#ws.readyState === WebSocket.OPEN) {
            this.#ws.send(chunkBuffer);
            this.#seqNo++;
        }
    }

    stop() {
        if (this.#ws && this.#ws.readyState === WebSocket.OPEN) {
            this.#ws.send(JSON.stringify({
                message: "EndOfStream",
                last_seq_no: this.#seqNo
            }));
        }
        this.#ready = false;
    }

    close() {
        if (this.#ws) {
            this.#ws.close();
            this.#ws = null;
        }
        this.#ready = false;
    }
}

