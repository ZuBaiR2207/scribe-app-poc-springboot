# Wellness Scribe: User Manual

**Version:** POC 0.1
**Audience:** Wellness practitioners (users) and the person who runs the system (operator)

> **Proof of concept, test data only.**
> This version is for demonstration and evaluation. Do not use real client or patient recordings with it. See [Privacy and data handling](#7-privacy-and-data-handling).

---

## 1. What is Wellness Scribe?

Wellness Scribe listens to a wellness session (for example a nutrition, fitness or counselling consultation) and helps you write the session note.

1. You speak. The app shows live text as you talk.
2. When you stop, the app sends the text to an AI, which writes a **draft** note from a template.
3. You **review and correct** the draft.
4. You **approve** it, and the note is saved.

**The AI only writes a draft. You are responsible for the final note.** Nothing is final until you approve it.

It supports English, Malay, Mandarin and Tamil, including sessions where people mix languages.

---

## 2. What you need

| Item | Details |
|---|---|
| Computer | A laptop or desktop with a working microphone |
| Browser | **Google Chrome or Microsoft Edge** (other browsers may not work) |
| Internet | A stable connection. Speech and notes are processed online |
| Microphone | A built-in mic works. A USB or headset microphone usually gives better accuracy |
| Address | The app opens at `http://localhost:8080` when run on your own computer |

The first time you click Start, the browser asks permission to use the microphone. Choose **Allow**.

---

## 3. Quick start (5 steps)

1. Open the app in Chrome or Edge.
2. Tick the **consent** checkbox.
3. Choose the **language** option.
4. Click **Start**, then speak.
5. Click **Stop**, review the note, and click **Approve note**.

---

## 4. Step-by-step guide

### 4.1 Confirm consent

At the top of the page is a checkbox confirming that the client has been told about, and agreed to, the session being recorded and transcribed.

- The **Start** button stays disabled until you tick it.
- Only tick it **after** the client has actually agreed.
- The server also refuses to save a session without consent.

### 4.2 Choose the language

| Option | Use it when |
|---|---|
| English + Mandarin + Malay + Tamil (mixed) | People switch between several of these languages |
| English + Malay | Conversation mixes English and Malay |
| English + Mandarin | Conversation mixes English and Mandarin |
| English + Tamil | Conversation mixes English and Tamil |
| English only | Only English is spoken |
| Malay only | Only Malay is spoken |

**Tip:** The more specific the option, the better it usually performs. Use "English only" for an English-only session rather than the 4-language mix. Which options work depends on the speech service account, so if one is rejected, try another or ask your operator.

### 4.3 Record the session

1. Click **Start**. The status shows "Getting token...", then **"Listening..."**.
2. Speak normally. Text appears in the live box:
   - **Grey text** is a draft that may still change as more speech arrives.
   - **Black text** is confirmed.
3. When the session is finished, click **Stop**.

The status changes to "Finishing...", then "Generating note...". This can take a few seconds.

### 4.4 Review and edit the note

When the status says **"Draft ready. Review, edit, then approve."**, the AI note appears in the **Wellness note** box. The box is editable.

Read the whole note against what was actually said. Look out for:

- **"Not mentioned"**: the AI found nothing about that section. This is normal and correct, and you can fill it in yourself if needed.
- **[unclear]**: the AI was not sure about a word, usually because of a speech recognition error. Check and correct these.
- **Names, amounts and times**: dosages, quantities, product names and dates are where mistakes matter most. Always check them.
- **Anything that was not said**: the AI is told not to add facts or advice, but it can still make mistakes. Delete anything that is wrong.

### 4.5 Approve the note

When you are happy with the note, click **Approve note**. The status shows **"Approved and saved"**.

- The AI's original draft is kept separately from your approved version.
- After approval, the session cannot be approved again.

### 4.6 Past sessions

The **Past sessions** list shows previous sessions, newest first, with date, language and status.

- **Click a session** to load its transcript and note.
- **Delete** removes the session permanently. This cannot be undone.

Sessions are saved on the computer running the system and are still there after the app is restarted.

---

## 5. Tips for better accuracy

- **Use a good microphone** and keep it close to whoever is speaking. Audio quality matters as much as the AI.
- **Choose a quiet room.** Fans, traffic, music and TV reduce accuracy.
- **Speak clearly and at a normal pace.** Avoid talking over each other.
- **Say product, supplement and food names clearly.** Specialised words are the most likely to be misheard.
- **Pick the language option that matches the session**, as described in section 4.2.
- **Do a 20-second test** before a real session to check the microphone and text.

---

## 6. Troubleshooting

| Problem | What to try |
|---|---|
| **Start is greyed out** | Tick the consent checkbox |
| **Browser did not ask for the microphone, or says it is blocked** | Click the lock icon next to the address, allow the microphone, and reload the page |
| **"Token request failed"** | The speech service key is missing or wrong. Tell your operator (section 8) |
| **"Error" or "Warning" message after Start** | Try a different language option. If it persists, note the exact message and tell your operator |
| **No text appears while speaking** | Check the correct microphone is selected in the computer settings, that it is not muted, and that you are using Chrome or Edge |
| **Text is very inaccurate** | Check background noise and microphone distance, and choose a more specific language option |
| **"No speech captured"** | Nothing was recognised. Speak closer to the microphone and try again |
| **"Note generation failed"** | The AI service may be busy or over its free limit. Wait a minute and try a new session. The operator should check the logs |
| **Page looks broken after an update** | Hard refresh with `Ctrl + Shift + R` |

---

## 7. Privacy and data handling

This is a proof of concept. Please read this section.

- **Use test data only.** Do not record real clients or patients with this version.
- **Where your audio and text go.** Speech is sent to the speech-recognition provider (Speechmatics) and the transcript is sent to an AI provider (currently Google's Gemini through a free tier) to draft the note. Both are cloud services, so data leaves the computer and may leave Malaysia.
- **Free-tier terms.** Free AI tiers may allow the provider to use submitted content to improve their products. This is another reason to use only test data.
- **What is stored locally.** Transcripts, AI drafts and approved notes are stored in a database file in the `data/` folder on the operator's computer. Audio itself is **not** stored by this app.
- **No login.** Anyone who can open the app can see all saved sessions.
- **Not encrypted at rest.** Stored data is not encrypted in this version.
- **Consent.** Always tell the client that the session is being recorded and transcribed, and get their agreement before you start.

Health and wellness information is sensitive personal data under Malaysia's Personal Data Protection Act (PDPA). Before any real-world use, the clinic should confirm requirements with its legal or compliance adviser, and the system will need production-level safeguards (see section 9).

---

## 8. For the operator (setup)

You only need this section if you are the person running the system.

**Requirements:** JDK 21 or newer, Git, and two API keys (speech and AI).

**Get the code and set the keys** (PowerShell, in the project folder):

```powershell
git clone https://github.com/YOUR-USERNAME/wellness-scribe.git
cd wellness-scribe
$env:SPEECHMATICS_API_KEY="your-speech-key"
$env:LLM_API_KEY="your-ai-key"
```

**Check `src/main/resources/application.properties` contains:**

```properties
speechmatics.api-key=${SPEECHMATICS_API_KEY}
llm.base-url=https://generativelanguage.googleapis.com/v1beta/openai
llm.api-key=${LLM_API_KEY}
llm.model=<a current model ID from Google AI Studio>
```

**Run:**

```powershell
.\mvnw spring-boot:run
```

Then open `http://localhost:8080` in Chrome or Edge.

**Operator notes**

- Never commit API keys or the `data/` folder to Git, and never share keys in chat or email. If a key leaks, delete it in the provider's console and create a new one.
- Free-tier limits and model names change. If notes stop generating, check the model ID and your limits in Google AI Studio.
- To switch AI provider, change the three `llm.*` settings. No code change is needed for providers that support the OpenAI-compatible chat format.
- To see why something failed, read the console output of the running app. Transcripts and notes are deliberately not written to logs.

---

## 9. Current limitations

This version does **not** include:

- User accounts, login or role-based access
- Encryption of stored data
- Speaker labels (who said what)
- Choosing between several microphones inside the app
- Multiple note templates or template editing
- Exporting notes to PDF or other systems
- Support for phones or tablets (microphone access needs a secure connection)
- Offline use
- Guaranteed accuracy for every accent, mixed-language sentence or specialised term. Accuracy should be measured on your own recordings before relying on it

The AI can make mistakes: it can mishear words, miss details, or occasionally state something that was not said. **Every note must be reviewed by the practitioner before approval.**

---

## 10. FAQ

**Does the AI make decisions about the client?**
No. It only turns what was said into a draft note. It is instructed not to give advice or diagnoses. The practitioner is responsible for the final note.

**Can I edit a note after approving it?**
Not in this version. Approval is final, so check carefully first.

**Why is there both a draft and an approved note?**
The AI draft is kept as it was written, and your approved version is saved separately. This lets the team measure how much the AI needs correcting, which helps improve quality over time.

**Can it handle people switching languages mid-sentence?**
It is designed to, using the mixed-language options, but accuracy varies. Test with your own recordings.

**Is the audio saved?**
No. Only the text, the AI draft and the approved note are stored.

**Who can see my sessions?**
Anyone who can open the app on that computer, because there is no login in this version.

---

## 11. Getting help

Contact: **[name / email of the project owner]**

When reporting a problem, include:

1. What you were doing when it happened
2. The exact message shown on the screen
3. Which language option you selected
4. The browser you used (Chrome or Edge)

**Do not include client information or API keys in problem reports.**
