package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GameService {

    public GameSession startGame(Lobby lobby){
        return new GameSession(lobby.getGameSession().getGameId(), lobby.getUsers());
    }
    //webSocketService.lobbyNotifications(userId, CardDTO);

}
