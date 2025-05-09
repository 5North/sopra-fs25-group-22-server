package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPostResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

  DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

  @Mapping(source = "username", target = "username")
  @Mapping(source = "password", target = "password")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "token", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "winCount", ignore = true)
  @Mapping(target = "lossCount", ignore = true)
  @Mapping(target = "lobby", ignore = true)
  @Mapping(target = "tieCount", ignore = true)
  @Mapping(target = "lobbyJoined", ignore = true)
  User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "winCount", target = "winCount")
  @Mapping(source = "lossCount", target = "lossCount")
  @Mapping(target = "lobby", ignore = true)
  @Mapping(source = "tieCount", target = "tieCount")
  @Mapping(source = "lobbyJoined", target = "lobbyJoined")
  UserGetDTO convertEntityToUserGetDTO(User user);

  @Mapping(source="lobbyId", target = "lobbyId")
  LobbyPostResponseDTO convertEntityToLobbyPostResponseDTO(Lobby lobby);

    @Mapping(source = "lobbyId", target = "lobbyId")
    @Mapping(source = "user", target = "hostId", qualifiedByName = "getUserId")
    @Mapping(source = "users", target = "usersIds")
    @Mapping(target = "rematchersIds", ignore = true)
    LobbyDTO convertLobbyToLobbyDTO(Lobby lobby);

    @Mapping(source = "lobbyId", target = "lobbyId")
    @Mapping(source = "user", target = "hostId", qualifiedByName = "getUserId")
    @Mapping(source = "users", target = "usersIds")
    @Mapping(source = "rematchers", target = "rematchersIds")
    LobbyDTO convertLobbyToLobbyRematchDTO(Lobby lobby);

    @Named("getUserId")
    static Long getUserId(User user) {
        return user.getId();
    }
}
