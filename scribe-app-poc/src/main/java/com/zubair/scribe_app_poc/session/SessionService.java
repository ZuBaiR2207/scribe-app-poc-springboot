package com.zubair.scribe_app_poc.session;

import com.zubair.scribe_app_poc.session.dto.ApproveSessionRequest;
import com.zubair.scribe_app_poc.session.dto.CreateSessionRequest;
import com.zubair.scribe_app_poc.session.dto.SessionResponse;
import com.zubair.scribe_app_poc.session.dto.SessionSummary;

import java.util.List;
import java.util.UUID;

public interface SessionService {

    SessionResponse createSession(CreateSessionRequest request);

    SessionResponse getSession(UUID id);

    List<SessionSummary> listSessions();

    SessionResponse regenerateDraft(UUID id);

    SessionResponse approveSession(UUID id, ApproveSessionRequest request);

    void deleteSession(UUID id);
}

