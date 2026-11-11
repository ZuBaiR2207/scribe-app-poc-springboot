export class ApiClient {

    static async #handleResponse(res) {
        if (!res.ok) {
            let errorDetail = `HTTP ${res.status}`;
            try {
                const problem = await res.json();
                errorDetail = problem.detail || problem.title || errorDetail;
            } catch {
                // Not JSON problem detail
            }
            throw new Error(errorDetail);
        }
        if (res.status === 204) {
            return null;
        }
        return res.json();
    }

    static async fetchToken() {
        const res = await fetch("/api/token", { method: "POST" });
        return this.#handleResponse(res);
    }

    static async createSession(language, transcript, consent) {
        const res = await fetch("/api/sessions", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ language, transcript, consent })
        });
        return this.#handleResponse(res);
    }

    static async listSessions() {
        const res = await fetch("/api/sessions");
        return this.#handleResponse(res);
    }

    static async getSession(id) {
        const res = await fetch(`/api/sessions/${id}`);
        return this.#handleResponse(res);
    }

    static async regenerateDraft(id) {
        const res = await fetch(`/api/sessions/${id}/draft`, { method: "POST" });
        return this.#handleResponse(res);
    }

    static async approveSession(id, approvedNote) {
        const res = await fetch(`/api/sessions/${id}/approve`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ approvedNote })
        });
        return this.#handleResponse(res);
    }

    static async deleteSession(id) {
        const res = await fetch(`/api/sessions/${id}`, { method: "DELETE" });
        return this.#handleResponse(res);
    }
}

