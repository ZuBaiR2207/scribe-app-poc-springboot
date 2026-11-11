export class NoteEditorUi {
    #aiDraftContainer;
    #aiDraftTextEl;
    #noteTextarea;
    #approveBtn;
    #retryDraftBtn;
    #statusBanner;

    constructor({ aiDraftContainer, aiDraftTextEl, noteTextarea, approveBtn, retryDraftBtn, statusBanner }) {
        this.#aiDraftContainer = aiDraftContainer;
        this.#aiDraftTextEl = aiDraftTextEl;
        this.#noteTextarea = noteTextarea;
        this.#approveBtn = approveBtn;
        this.#retryDraftBtn = retryDraftBtn;
        this.#statusBanner = statusBanner;
    }

    renderSession(session) {
        if (!session) {
            this.clear();
            return;
        }

        // Show AI draft (immutable original)
        if (session.aiDraft) {
            this.#aiDraftContainer.style.display = "block";
            this.#aiDraftTextEl.textContent = session.aiDraft;
        } else {
            this.#aiDraftContainer.style.display = "none";
            this.#aiDraftTextEl.textContent = "";
        }

        // If approved, display approved note in textarea and disable editing
        if (session.status === "APPROVED") {
            this.#noteTextarea.value = session.approvedNote || session.aiDraft || "";
            this.#noteTextarea.disabled = true;
            this.#statusBanner.textContent = "Status: APPROVED (Read-only)";
            this.#statusBanner.className = "banner approved";
        } else if (session.status === "DRAFTED") {
            this.#noteTextarea.value = session.approvedNote || session.aiDraft || "";
            this.#noteTextarea.disabled = false;
            this.#statusBanner.textContent = "Status: DRAFTED (Edit below and click Approve)";
            this.#statusBanner.className = "banner drafted";
        } else {
            this.#noteTextarea.value = "";
            this.#noteTextarea.disabled = false;
            this.#statusBanner.textContent = "Status: TRANSCRIBED (AI draft generation pending/retryable)";
            this.#statusBanner.className = "banner transcribed";
        }
    }

    getNoteContent() {
        return this.#noteTextarea.value.trim();
    }

    clear() {
        this.#aiDraftContainer.style.display = "none";
        this.#aiDraftTextEl.textContent = "";
        this.#noteTextarea.value = "";
        this.#noteTextarea.disabled = false;
        this.#statusBanner.textContent = "";
        this.#statusBanner.className = "banner";
    }

    updateControls(controls) {
        if (this.#approveBtn) {
            this.#approveBtn.disabled = !controls.approveEnabled;
        }
        if (this.#retryDraftBtn) {
            this.#retryDraftBtn.disabled = !controls.retryDraftEnabled;
        }
    }
}

