import { ApiClient } from "./api/apiClient.js";
import { RecorderStateManager, RecorderStateEnum } from "./state/recorderState.js";
import { MicCapture } from "./audio/micCapture.js";
import { SpeechmaticsClient } from "./speech/speechmaticsClient.js";
import { LiveTranscriptUi } from "./ui/liveTranscriptUi.js";
import { NoteEditorUi } from "./ui/noteEditorUi.js";
import { SessionListUi } from "./ui/sessionListUi.js";

document.addEventListener("DOMContentLoaded", () => {
    // DOM Elements
    const consentCheckbox = document.getElementById("consentCheckbox");
    const langSelect = document.getElementById("lang");
    const startBtn = document.getElementById("start");
    const stopBtn = document.getElementById("stop");
    const statusEl = document.getElementById("status");

    const finalEl = document.getElementById("final");
    const partialEl = document.getElementById("partial");

    const aiDraftContainer = document.getElementById("aiDraftContainer");
    const aiDraftTextEl = document.getElementById("aiDraftText");
    const noteTextarea = document.getElementById("note");
    const approveBtn = document.getElementById("approveBtn");
    const retryDraftBtn = document.getElementById("retryDraftBtn");
    const statusBanner = document.getElementById("statusBanner");
    const sessionsListContainer = document.getElementById("sessionsListContainer");

    // Managers & Clients
    const stateManager = new RecorderStateManager();
    const transcriptUi = new LiveTranscriptUi(finalEl, partialEl);
    const noteEditorUi = new NoteEditorUi({
        aiDraftContainer,
        aiDraftTextEl,
        noteTextarea,
        approveBtn,
        retryDraftBtn,
        statusBanner
    });

    let speechmaticsClient = null;
    let micCapture = null;

    // Past sessions UI
    const sessionListUi = new SessionListUi(
        sessionsListContainer,
        async (selectedId) => {
            try {
                const session = await ApiClient.getSession(selectedId);
                stateManager.transitionTo(RecorderStateEnum.REVIEW, { session });
                transcriptUi.setTranscript(session.transcript);
                noteEditorUi.renderSession(session);
                refreshSessionsList(session.id);
            } catch (err) {
                alert("Failed to load session: " + err.message);
            }
        },
        async (deleteId) => {
            try {
                await ApiClient.deleteSession(deleteId);
                if (stateManager.getCurrentSession()?.id === deleteId) {
                    stateManager.transitionTo(RecorderStateEnum.IDLE, { session: null });
                    transcriptUi.clear();
                    noteEditorUi.clear();
                }
                await refreshSessionsList();
            } catch (err) {
                alert("Failed to delete session: " + err.message);
            }
        }
    );

    async function refreshSessionsList(activeId = null) {
        try {
            const list = await ApiClient.listSessions();
            sessionListUi.render(list, activeId);
        } catch (err) {
            console.error("Failed to fetch sessions list:", err);
        }
    }

    // State changes drive UI controls
    stateManager.subscribe((controls) => {
        startBtn.disabled = !controls.startEnabled;
        stopBtn.disabled = !controls.stopEnabled;
        consentCheckbox.disabled = !controls.consentCheckboxEnabled;
        langSelect.disabled = !controls.languageSelectEnabled;
        statusEl.textContent = controls.statusText;
        noteEditorUi.updateControls(controls);
    });

    // Consent checkbox listener
    consentCheckbox.addEventListener("change", () => {
        stateManager.setConsent(consentCheckbox.checked);
    });

    // START Flow
    startBtn.addEventListener("click", async () => {
        try {
            transcriptUi.clear();
            noteEditorUi.clear();
            stateManager.transitionTo(RecorderStateEnum.CONNECTING);

            // 1. Fetch short-lived token
            const tokenResponse = await ApiClient.fetchToken();
            const token = tokenResponse.token;

            // 2. Set up Speechmatics client
            speechmaticsClient = new SpeechmaticsClient({
                onRecognitionStarted: () => {
                    stateManager.transitionTo(RecorderStateEnum.LISTENING);
                },
                onPartialTranscript: (text) => {
                    transcriptUi.setPartial(text);
                },
                onFinalTranscript: (text) => {
                    transcriptUi.appendFinal(text);
                },
                onEndOfTranscript: async () => {
                    await handleEndOfTranscript();
                },
                onError: (err) => {
                    console.error("Speechmatics error:", err);
                    stateManager.transitionTo(RecorderStateEnum.ERROR, { error: err });
                    cleanupAudio();
                }
            });

            // 3. Set up microphone capture
            micCapture = new MicCapture((chunk) => {
                if (speechmaticsClient) {
                    speechmaticsClient.sendAudioChunk(chunk);
                }
            });

            // 4. Connect Speechmatics and start mic
            await speechmaticsClient.connect(token, langSelect.value);
            await micCapture.start();

        } catch (err) {
            console.error("Failed to start session:", err);
            stateManager.transitionTo(RecorderStateEnum.ERROR, { error: err.message });
            cleanupAudio();
        }
    });

    // STOP Flow
    stopBtn.addEventListener("click", () => {
        stateManager.transitionTo(RecorderStateEnum.FINISHING);
        if (micCapture) {
            micCapture.stop();
            micCapture = null;
        }
        if (speechmaticsClient) {
            speechmaticsClient.stop();
        }
    });

    // When transcription stream completes
    async function handleEndOfTranscript() {
        cleanupAudio();
        const transcript = transcriptUi.getTranscript();

        if (!transcript) {
            stateManager.transitionTo(RecorderStateEnum.IDLE, {
                error: "No audio was transcribed"
            });
            return;
        }

        stateManager.transitionTo(RecorderStateEnum.GENERATING);

        try {
            const session = await ApiClient.createSession(langSelect.value, transcript, true);
            stateManager.transitionTo(RecorderStateEnum.REVIEW, { session });
            noteEditorUi.renderSession(session);
            await refreshSessionsList(session.id);
        } catch (err) {
            console.error("Session creation or note generation error:", err);
            stateManager.transitionTo(RecorderStateEnum.ERROR, {
                error: "Saved session, but AI draft failed: " + err.message
            });
            await refreshSessionsList();
        }
    }

    // APPROVE Note
    approveBtn.addEventListener("click", async () => {
        const session = stateManager.getCurrentSession();
        if (!session) return;

        const approvedText = noteEditorUi.getNoteContent();
        if (!approvedText) {
            alert("Approved note must not be blank.");
            return;
        }

        try {
            const updated = await ApiClient.approveSession(session.id, approvedText);
            stateManager.transitionTo(RecorderStateEnum.REVIEW, { session: updated });
            noteEditorUi.renderSession(updated);
            await refreshSessionsList(updated.id);
            alert("Session approved and locked!");
        } catch (err) {
            alert("Approval failed: " + err.message);
        }
    });

    // RETRY / Regenerate Draft
    retryDraftBtn.addEventListener("click", async () => {
        const session = stateManager.getCurrentSession();
        if (!session) return;

        try {
            stateManager.transitionTo(RecorderStateEnum.GENERATING);
            const updated = await ApiClient.regenerateDraft(session.id);
            stateManager.transitionTo(RecorderStateEnum.REVIEW, { session: updated });
            noteEditorUi.renderSession(updated);
            await refreshSessionsList(updated.id);
        } catch (err) {
            stateManager.transitionTo(RecorderStateEnum.ERROR, { error: err.message });
        }
    });

    function cleanupAudio() {
        if (micCapture) {
            micCapture.stop();
            micCapture = null;
        }
        if (speechmaticsClient) {
            speechmaticsClient.close();
            speechmaticsClient = null;
        }
    }

    // Initial load
    refreshSessionsList();
});

