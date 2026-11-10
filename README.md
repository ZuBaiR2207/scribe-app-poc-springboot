# scribe-app-poc
# Wellness Scribe (POC)

Real-time multilingual speech-to-documentation for wellness practitioners.

## Goal
Practitioner speaks (English / Malay / Mandarin / mixed) → live transcript → structured wellness note → review and approve.

## Architecture
Browser mic → Speech streaming API → live text → LLM + template → note → review/approve

## Status
- [x] Milestone 0: Spring Boot + WebSocket skeleton
- [ ] Milestone 1: Browser mic streaming
- [ ] Milestone 2: Live transcript
- [ ] Milestone 3: Note generation
- [ ] Milestone 4: Review and approve UI
- [ ] Milestone 5: Language and accent comparison

## Run locally
./mvnw spring-boot:run   (Windows: .\mvnw spring-boot:run)
