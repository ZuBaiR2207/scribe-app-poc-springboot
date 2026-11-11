export class LiveTranscriptUi {
    #finalEl;
    #partialEl;

    constructor(finalElement, partialElement) {
        this.#finalEl = finalElement;
        this.#partialEl = partialElement;
    }

    appendFinal(text) {
        this.#finalEl.textContent += text;
        this.#partialEl.textContent = "";
    }

    setPartial(text) {
        this.#partialEl.textContent = text;
    }

    clear() {
        this.#finalEl.textContent = "";
        this.#partialEl.textContent = "";
    }

    getTranscript() {
        return this.#finalEl.textContent.trim();
    }

    setTranscript(text) {
        this.#finalEl.textContent = text || "";
        this.#partialEl.textContent = "";
    }
}

