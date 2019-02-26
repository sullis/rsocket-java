package io.rsocket.resume;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionManager {
  private volatile boolean disposed;
  private final ConcurrentMap<ResumeToken, ServerRSocketSession> sessions =
      new ConcurrentHashMap<>();

  public ServerRSocketSession save(ServerRSocketSession session) {
    ResumeToken token = session.token();
    ServerRSocketSession prev = sessions.putIfAbsent(token, session);
    if (disposed) {
      sessions.remove(token);
      session.dispose();
    } else if (prev != null) {
      session.dispose();
    } else {
      session.onClose().doOnSuccess(v -> sessions.remove(token)).subscribe();
    }
    return session;
  }

  public Optional<ServerRSocketSession> get(ResumeToken resumeToken) {
    return Optional.ofNullable(sessions.get(resumeToken));
  }

  public void dispose() {
    disposed = true;
    sessions.values().forEach(ServerRSocketSession::dispose);
    sessions.clear();
  }
}
