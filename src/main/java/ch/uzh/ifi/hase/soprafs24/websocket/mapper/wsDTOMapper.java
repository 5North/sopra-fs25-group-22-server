package ch.uzh.ifi.hase.soprafs24.websocket.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.wsLobbyDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface wsDTOMapper {

    ch.uzh.ifi.hase.soprafs24.websocket.mapper.wsDTOMapper INSTANCE = Mappers.getMapper(ch.uzh.ifi.hase.soprafs24.websocket.mapper.wsDTOMapper.class);

    @Mapping(source = "lobbyId", target = "lobbyId")
    @Mapping(source = "user", target = "hostId", qualifiedByName = "getUserId")
    @Mapping(source = "users", target = "usersIds")
    @Mapping(target = "rematchersIds", ignore = true)
    wsLobbyDTO convertLobbyTowsLobbyDTO(Lobby lobby);

    @Mapping(source = "lobbyId", target = "lobbyId")
    @Mapping(source = "user", target = "hostId", qualifiedByName = "getUserId")
    @Mapping(source = "users", target = "usersIds")
    @Mapping(source = "rematchers", target = "rematchersIds")
    wsLobbyDTO convertLobbyTowsLobbyRematchDTO(Lobby lobby);

    @Named("getUserId")
    static Long getUserId(User user) {
        return user.getId();
    }
}

