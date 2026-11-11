export class SessionListUi {
    #containerEl;
    #onSelectCallback;
    #onDeleteCallback;

    constructor(containerElement, onSelectCallback, onDeleteCallback) {
        this.#containerEl = containerElement;
        this.#onSelectCallback = onSelectCallback;
        this.#onDeleteCallback = onDeleteCallback;
    }

    render(sessions, activeSessionId = null) {
        this.#containerEl.replaceChildren();

        if (!sessions || sessions.length === 0) {
            const emptyEl = document.createElement("p");
            emptyEl.className = "empty-list";
            emptyEl.textContent = "No past sessions found.";
            this.#containerEl.appendChild(emptyEl);
            return;
        }

        const listEl = document.createElement("ul");
        listEl.className = "session-list";

        for (const session of sessions) {
            const itemEl = document.createElement("li");
            itemEl.className = "session-item" + (session.id === activeSessionId ? " active" : "");

            const infoDiv = document.createElement("div");
            infoDiv.className = "session-info";
            infoDiv.onclick = () => {
                if (this.#onSelectCallback) this.#onSelectCallback(session.id);
            };

            const titleSpan = document.createElement("span");
            titleSpan.className = "session-title";
            const dateStr = session.createdAt ? new Date(session.createdAt).toLocaleString() : "Unknown date";
            titleSpan.textContent = `${dateStr} (${session.language})`;

            const badgeSpan = document.createElement("span");
            badgeSpan.className = `badge status-${session.status.toLowerCase()}`;
            badgeSpan.textContent = session.status;

            infoDiv.appendChild(titleSpan);
            infoDiv.appendChild(badgeSpan);

            const deleteBtn = document.createElement("button");
            deleteBtn.className = "btn-delete";
            deleteBtn.textContent = "Delete";
            deleteBtn.onclick = (e) => {
                e.stopPropagation();
                if (confirm("Delete this session permanently?")) {
                    if (this.#onDeleteCallback) this.#onDeleteCallback(session.id);
                }
            };

            itemEl.appendChild(infoDiv);
            itemEl.appendChild(deleteBtn);
            listEl.appendChild(itemEl);
        }

        this.#containerEl.appendChild(listEl);
    }
}

