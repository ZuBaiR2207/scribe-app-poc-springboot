export const RecorderStateEnum = Object.freeze({
    IDLE: "IDLE",
    CONNECTING: "CONNECTING",
    LISTENING: "LISTENING",
    FINISHING: "FINISHING",
    GENERATING: "GENERATING",
    REVIEW: "REVIEW",
    ERROR: "ERROR"
});

export class RecorderStateManager {
    #state = RecorderStateEnum.IDLE;
    #consentGiven = false;
    #currentSession = null;
    #errorMessage = null;
    #listeners = [];

    constructor() {
    }

    getState() {
        return this.#state;
    }

    isConsentGiven() {
        return this.#consentGiven;
    }

    getCurrentSession() {
        return this.#currentSession;
    }

    getErrorMessage() {
        return this.#errorMessage;
    }

    setConsent(given) {
        this.#consentGiven = Boolean(given);
        this.#notify();
    }

    transitionTo(newState, options = {}) {
        if (!Object.values(RecorderStateEnum).includes(newState)) {
            throw new Error(`Invalid state: ${newState}`);
        }
        this.#state = newState;
        if (options.session !== undefined) {
            this.#currentSession = options.session;
        }
        if (options.error !== undefined) {
            this.#errorMessage = options.error;
        } else if (newState !== RecorderStateEnum.ERROR) {
            this.#errorMessage = null;
        }
        this.#notify();
    }

    getControlsState() {
        const isIdle = this.#state === RecorderStateEnum.IDLE;
        const isListening = this.#state === RecorderStateEnum.LISTENING;
        const isReview = this.#state === RecorderStateEnum.REVIEW;
        const isGenerating = this.#state === RecorderStateEnum.GENERATING;
        const isFinishing = this.#state === RecorderStateEnum.FINISHING;

        const isApproved = this.#currentSession?.status === "APPROVED";
        const hasDraft = Boolean(this.#currentSession?.aiDraft);

        return {
            startEnabled: isIdle && this.#consentGiven,
            stopEnabled: isListening,
            consentCheckboxEnabled: isIdle,
            languageSelectEnabled: isIdle,
            approveEnabled: isReview && hasDraft && !isApproved,
            retryDraftEnabled: (this.#state === RecorderStateEnum.REVIEW || this.#state === RecorderStateEnum.ERROR)
                && Boolean(this.#currentSession?.id)
                && !isApproved,
            statusText: this.#getStatusText()
        };
    }

    #getStatusText() {
        switch (this.#state) {
            case RecorderStateEnum.IDLE:
                return this.#consentGiven ? "Ready to start" : "Please confirm practitioner consent";
            case RecorderStateEnum.CONNECTING:
                return "Connecting to Speechmatics...";
            case RecorderStateEnum.LISTENING:
                return "Listening & transcribing...";
            case RecorderStateEnum.FINISHING:
                return "Finalizing transcript...";
            case RecorderStateEnum.GENERATING:
                return "Saving session & generating AI draft note...";
            case RecorderStateEnum.REVIEW:
                return this.#currentSession?.status === "APPROVED"
                    ? "Session Approved"
                    : "Draft ready for practitioner review";
            case RecorderStateEnum.ERROR:
                return this.#errorMessage ? `Error: ${this.#errorMessage}` : "An error occurred";
            default:
                return "Idle";
        }
    }

    subscribe(listener) {
        this.#listeners.push(listener);
        listener(this.getControlsState(), this);
        return () => {
            this.#listeners = this.#listeners.filter(l => l !== listener);
        };
    }

    #notify() {
        const controls = this.getControlsState();
        for (const listener of this.#listeners) {
            try {
                listener(controls, this);
            } catch (err) {
                console.error("State listener error:", err);
            }
        }
    }
}

