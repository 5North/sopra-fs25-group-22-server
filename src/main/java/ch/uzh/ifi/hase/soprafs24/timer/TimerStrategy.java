// src/main/java/ch/uzh/ifi/hase/soprafs24/timer/TimerStrategy.java
package ch.uzh.ifi.hase.soprafs24.timer;

/**
 * Interfaccia per le due strategie di timeout.
 */
public interface TimerStrategy {
    /**
     * @return durata del timer (in secondi)
     */
    long getTimeoutSeconds();

    /**
     * Invocato al timeout: contiene il fallback specifico.
     *
     * @param gameId    id della partita
     * @param forUserId opzionale: id del giocatore (solo per ChoiceTimer)
     */
    void onTimeout(Long gameId, Long forUserId);
}
