const startBtn = document.getElementById("start");
const stopBtn = document.getElementById("stop");
const statusEl = document.getElementById("status");
const finalEl = document.getElementById("final");
const partialEl = document.getElementById("partial");
const langEl = document.getElementById("lang");

let ws, audioCtx, micStream, workletNode;
let seqNo = 0;
let ready = false;

startBtn.onclick = async () => {
    finalEl.textContent = "";
    partialEl.textContent = "";
    seqNo = 0;
    ready = false;
    statusEl.textContent = "Getting token...";

    // 1. Ask our Spring Boot server for a temporary key
    const res = await fetch("/api/token", { method: "POST" });
    if (!res.ok) {
        statusEl.textContent = "Token request failed: " + res.status;
        return;
    }
    const { token } = await res.json();

    // 2. Open the microphone (16 kHz audio)
    micStream = await navigator.mediaDevices.getUserMedia({
        audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true },
    });
    audioCtx = new AudioContext({ sampleRate: 16000 });
    await audioCtx.audioWorklet.addModule("pcm-worklet.js");
    const source = audioCtx.createMediaStreamSource(micStream);
    workletNode = new AudioWorkletNode(audioCtx, "pcm-processor");
    source.connect(workletNode);
    workletNode.connect(audioCtx.destination); // processor outputs silence, so no echo

    // 3. Connect straight to Speechmatics with the temporary key
    ws = new WebSocket("wss://global.rt.speechmatics.com/v2?jwt=" + token);
    ws.binaryType = "arraybuffer";

    ws.onopen = () => {
        ws.send(JSON.stringify({
            message: "StartRecognition",
            audio_format: { type: "raw", encoding: "pcm_f32le", sample_rate: 16000 },
            transcription_config: {
                language: langEl.value,
                enable_partials: true,
                max_delay: 2,
            },
        }));
    };

    ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        switch (msg.message) {
            case "RecognitionStarted":
                ready = true;
                statusEl.textContent = "Listening...";
                break;
            case "AddPartialTranscript":
                partialEl.textContent = msg.metadata.transcript;
                break;
            case "AddTranscript":
                finalEl.textContent += msg.metadata.transcript;
                partialEl.textContent = "";
                break;
            case "EndOfTranscript":
                statusEl.textContent = "Done";
                cleanup();
                break;
            case "Error":
            case "Warning":
                statusEl.textContent = msg.message + ": " + (msg.reason || msg.type);
                console.log(msg);
                break;
        }
    };

    ws.onerror = () => { statusEl.textContent = "Connection error"; };

    // 4. Send each audio chunk as it arrives
    workletNode.port.onmessage = (e) => {
        if (ready && ws.readyState === WebSocket.OPEN) {
            ws.send(e.data);
            seqNo++;
        }
    };

    startBtn.disabled = true;
    stopBtn.disabled = false;
};

stopBtn.onclick = () => {
    workletNode?.disconnect();
    micStream?.getTracks().forEach((t) => t.stop());
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ message: "EndOfStream", last_seq_no: seqNo }));
    }
    stopBtn.disabled = true;
    statusEl.textContent = "Finishing...";
};

function cleanup() {
    ws?.close();
    audioCtx?.close();
    startBtn.disabled = false;
    stopBtn.disabled = true;
}