package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers() {
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @PostMapping("/login")
  public ResponseEntity<String> loginUser(@RequestBody UserPostDTO userPostDTO) {

    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // login the user, if exception is thrown return response with the exception's
    // msg and http status
    try {
      String token = userService.loginUser(userInput);

      // append token to response header using ResponseEntity
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.set("Token",
          token);

      return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
    } catch (ResponseStatusException e) {
      return new ResponseEntity<>(e.getMessage(), e.getStatus());
    }
  }

  @PostMapping("/users")
  public ResponseEntity<UserGetDTO> registerUser(@RequestBody UserPostDTO userPostDTO) {
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    User createdUser = userService.createUser(userInput);
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Token", createdUser.getToken());
    UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    return new ResponseEntity<>(userGetDTO, responseHeaders, HttpStatus.CREATED);
  }

}
