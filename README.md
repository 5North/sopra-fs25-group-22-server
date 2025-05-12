```
  █████████    █████████     ███████    ███████████    █████████      ███████████    ███████    ███████████   
 ███░░░░░███  ███░░░░░███  ███░░░░░███ ░░███░░░░░███  ███░░░░░███    ░░███░░░░░░█  ███░░░░░███ ░░███░░░░░███  
░███    ░░░  ███     ░░░  ███     ░░███ ░███    ░███ ░███    ░███     ░███   █ ░  ███     ░░███ ░███    ░███  
░░█████████ ░███         ░███      ░███ ░██████████  ░███████████     ░███████   ░███      ░███ ░██████████   
 ░░░░░░░░███░███         ░███      ░███ ░███░░░░░░   ░███░░░░░███     ░███░░░█   ░███      ░███ ░███░░░░░███  
 ███    ░███░░███     ███░░███     ███  ░███         ░███    ░███     ░███  ░    ░░███     ███  ░███    ░███  
░░█████████  ░░█████████  ░░░███████░   █████        █████   █████    █████       ░░░███████░   █████   █████ 
 ░░░░░░░░░    ░░░░░░░░░     ░░░░░░░    ░░░░░        ░░░░░   ░░░░░    ░░░░░          ░░░░░░░    ░░░░░   ░░░░░  
                                                                                                              
                                                                                                              
                                                                                                              
 ███████████  ██████████   █████████  █████ ██████   █████ ██████   █████ ██████████ ███████████    █████████ 
░░███░░░░░███░░███░░░░░█  ███░░░░░███░░███ ░░██████ ░░███ ░░██████ ░░███ ░░███░░░░░█░░███░░░░░███  ███░░░░░███
 ░███    ░███ ░███  █ ░  ███     ░░░  ░███  ░███░███ ░███  ░███░███ ░███  ░███  █ ░  ░███    ░███ ░███    ░░░ 
 ░██████████  ░██████   ░███          ░███  ░███░░███░███  ░███░░███░███  ░██████    ░██████████  ░░█████████ 
 ░███░░░░░███ ░███░░█   ░███    █████ ░███  ░███ ░░██████  ░███ ░░██████  ░███░░█    ░███░░░░░███  ░░░░░░░░███
 ░███    ░███ ░███ ░   █░░███  ░░███  ░███  ░███  ░░█████  ░███  ░░█████  ░███ ░   █ ░███    ░███  ███    ░███
 ███████████  ██████████ ░░█████████  █████ █████  ░░█████ █████  ░░█████ ██████████ █████   █████░░█████████ 
░░░░░░░░░░░  ░░░░░░░░░░   ░░░░░░░░░  ░░░░░ ░░░░░    ░░░░░ ░░░░░    ░░░░░ ░░░░░░░░░░ ░░░░░   ░░░░░  ░░░░░░░░░                                                                                                                                                                

```
#

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=5North_sopra-fs25-group-22-server&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=5North_sopra-fs25-group-22-server)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=5North_sopra-fs25-group-22-server&metric=coverage)](https://sonarcloud.io/summary/new_code?id=5North_sopra-fs25-group-22-server)
![CI](https://img.shields.io/github/actions/workflow/status/5north/sopra-fs25-group-22-server/main.yml?label=CI)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# 🧹Scopa for Beginners🧹

This repository only contains the code of the backend. For the frontend implementation, check out this 
[repo](https://github.com/5North/sopra-fs25-group-22-client).

## Table of Contents

* [Introduction](#introduction)
* [Illustrations](#illustrations)
* [Technologies](#technologies)
* [High-Level Components](#high-level-components)
    * [Rest](#rest-specs)
    * [Websocket](#websocket-specs)
    * [Database](#database)
    * [Game Logic](#game-logic)
    * [Ai Assistance](#ai-assistance)
* [Launch & Deployment](#launch--deployment)
* [Roadmap](#roadmap)
* [Authors & Acknowledgments](#authors--acknowledgements)
* [License](#license)

## Introduction

🧹**Scopa for Beginners**🧹 aims to bring the beloved traditional italian cards game [Scopa](https://en.wikipedia.org/wiki/Scopa) 
to an international audience by creating an accessible digital version of the 2 versus 2 variant. The goal of this project is to create a user and 
beginner-friendly application client-server to allow both newcomers and hardcore fans of the game to conveniently play 
Scopa in their browser. To make it easier for beginners to learn the rules and achieve some flow, an integrated LLM 
assistant can suggest some possible options to play if help requested by the player. Real time communication via Websockets
allows to play this timeless classic with your friends without noticeable hiccups on a simple and no-fuss application,
that propose itself as an open source alternative to the usually paywalled or ads-filled commercial versions. 


## Illustrations

// TODO once application is final

## Technologies

* [Spring Boot](https://spring.io/projects/spring-boot) - Open-source Java framework to create Spring-based applications.
* [Gradle](https://gradle.org/) - a fast, dependable, and adaptable open-source build automation tool.
* [JPA](https://spring.io/projects/spring-data-jpa) - a persistence API used to map the application's *entities* to the database tables.
* [H2 Database](https://h2database.com/html/main.html) - a Java SQL database.
* [Websocket](https://docs.spring.io/spring-framework/reference/web/websocket.html) - To enable a two-way interactive communication session between the server and the client, for a realtime 
update of the game state among the participants during a match. 
* [STOMP](https://stomp.github.io/) protocol - To easily send and forward messages over websockets using Spring’s integrated STOMP support.
* [Google App Engine](https://cloud.google.com/) - cloud computing platform used to deploy our server.
* [SonarQube](https://www.sonarsource.com/products/sonarqube/) - open-source platform for continuous inspection of code quality.
* [OpenAi API](https://openai.com/) - API used for the game helping assistant.

## High-level components

### REST Specs

<details>
<summary>See table...</summary>

| Mapping                      | Method   | Parameter(s)                                                      | Parameter Type          | Status Code | Response                                            | Response Type | Description                                                                                                  |
|------------------------------|----------|-------------------------------------------------------------------|-------------------------|-------------|-----------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------------------|
| **/login**                   | **POST** | username &lt;string&gt;, password &lt;string&gt;                  | Body                    | 200         | Token &lt;string&gt;                                | Header        | Log in user and return an authentication token                                                               |
| **/login**                   | **POST** | username &lt;string&gt;, password &lt;string&gt;                  | Body                    | 403         | Error: reason &lt;string&gt;                        | Body          | Login failed due to invalid credentials                                                                      |
| **/logout**                  | **POST** | Token &lt;string&gt;                                              | Header                  | 204         | --                                                  | Header        | Log out the user (invalidate token)                                                                          |
| **/logout**                  | **POST** | Token &lt;string&gt;                                              | Header                  | 401         | Error: reason &lt;string&gt;                        | Body          | Logout failed due to unauthenticated request                                                                 |
| **/users**                   | **POST** | username &lt;string&gt;, password &lt;string&gt;                  | Body                    | 201         | Token &lt;string&gt;; User(*)                       | Header; Body  | Create new user and auto-login                                                                               |
| **/users**                   | **POST** | username &lt;string&gt;, password &lt;string&gt;                  | Body                    | 409         | Error: reason &lt;string&gt;                        | Body          | User creation failed because username already exists                                                         |
| **/users**                   | **GET**  | Token &lt;string&gt;                                              | Header                  | 200         | list&lt;User(*)&gt;                                 | Body          | Retrieve all users (for scoreboard)                                                                          |
| **/users**                   | **GET**  | Token &lt;string&gt;                                              | Header                  | 401         | Error: reason &lt;string&gt;                        | Body          | Unauthenticated request for users list                                                                       |
| **/users/{userId}**          | **GET**  | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Path            | 200         | User(*)                                             | Body          | Retrieve specific user profile                                                                               |
| **/users/{userId}**          | **GET**  | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Path            | 401         | Error: reason &lt;string&gt;                        | Body          | Unauthenticated request for user profile                                                                     |
| **/users/{userId}**          | **GET**  | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Path            | 404         | Error: reason &lt;string&gt;                        | Body          | User with userId not found                                                                                   |
| **/users/{userId}**          | **PUT**  | Token &lt;string&gt;; User(*) (profile data); userId &lt;long&gt; | Header; Body; Path      | 204         | --                                                  | --            | Update user profile                                                                                          |
| **/users/{userId}**          | **PUT**  | Token &lt;string&gt;; User(*) (profile data); userId &lt;long&gt; | Header; Body; Path      | 404         | Error: reason &lt;string&gt;                        | Body          | User with userId not found                                                                                   |
| **/lobbies**                 | **POST** | Token &lt;string&gt;                                              | Header; Body            | 201         | Lobby(*) (includes lobbyId, PIN, roomName, players) | Body          | Create new lobby; persist via LobbyRepository ensuring unique PIN                                            |
| **/lobbies**                 | **POST** | Token &lt;string&gt;                                              | Header; Body            | 401         | Error: reason &lt;string&gt;                        | Body          | Lobby creation failed because user is not authenticated                                                      |
| **/lobbies**                 | **POST** | Token &lt;string&gt;                                              | Header; Body            | 409         | Error: reason and id of lobby joined &lt;string&gt; | Body          | Lobby creation failed because user already joined a lobby                                                    |
| **/lobbies**                 | **POST** | Token &lt;string&gt;                                              | Header; Body            | 409         | Error: reason &lt;string&gt;                        | Body          | Lobby creation failed because user already has a lobby                                                       |
| **/lobbies?userId={userId}** | **GET**  | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Query Parameter | 200         | Lobby(*)                                            | Body          | Retrieve the lobby associated with the user                                                                  |
| **/lobbies?userId={userId}** | **GET**  | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Query Parameter | 404         | Lobby(*)                                            | Body          | The user doesn't have a lobby associated with it                                                             |
| **/lobbies?userId={userId}** | **GET**  | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Query Parameter | 401         | Lobby(*)                                            | Body          | Unauthenticated request                                                                                      |
| **/lobbies?userId={userId}** | **GET**  | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Query Parameter | 403         | Lobby(*)                                            | Body          | The user is neither the owner or a participant of the lobby and is not allowed to access it's representation |

</details>

### WebSocket Specs

<details>
<summary>See table...</summary>

| Supported | Mapping                    | Method          | Parameter(s)                                                                                         | Parameter Type | Description                                                                                         |
|-----------|----------------------------|-----------------|------------------------------------------------------------------------------------------------------|----------------|-----------------------------------------------------------------------------------------------------|
| ✅         | **/lobby**                 | **CONNECT**     | Token &lt;string&gt;                                                                                 | Query          | Upgrade connection to WebSocket for lobby operations                                                |
| ✅         | **/lobby**                 | **DISCONNECT**  | --                                                                                                   | --             | Terminates the WebSocket connection                                                                 |
| ✅         | **/topic/lobby/{lobbyId}** | **SUBSCRIBE**   | lobbyId &lt;string&gt;                                                                               | Path           | Subscribe to real-time lobby updates (player joins/leaves, notifications)                           |
| ✅         | **/topic/lobby/{lobbyId}** | **UNSUBSCRIBE** | lobbyId &lt;string&gt;                                                                               | Path           | Unsubscribe from lobby updates                                                                      |
| ✅         | **/startGame/{lobbyId}**   | **SEND**        | lobbyId &lt;string&gt;                                                                               | Path           | Start new game session                                                                              |
| ✅         | **/updateGame/{gameId}**   | **SEND**        | lobbyId &lt;string&gt;                                                                               | Path           | Request new game representation                                                                     |
| ✅         | **/app/playcard**          | **SEND**        | gameId &lt;string&gt;, card &lt;Card&gt;                                                             | Body (JSON)    | Send played card event to server for in-game processing                                             |
| ✅         | **/app/chooseCapture**     | **SEND**        | gameId &lt;string&gt;, userId &lt;long&gt;, chosenOption &lt;List{Card}&gt;, playedCard &lt;Card&gt; | Body (JSON)    | Send chosen capture option when multiple options exist                                              |
| ✅         | **/app/ai**                | **SEND**        | gameId &lt;string&gt;, userId &lt;long&gt;, requestFlag &lt;string&gt;                               | Body (JSON)    | Send request for AI assistance (hint) to the server                                                 |
| ✅         | **/app/rematch**           | **SEND**        | gameId &lt;string&gt;, userId &lt;long&gt;, confirmRematch &lt;boolean&gt;                           | Body (JSON)    | Send rematch confirmation from the player to the server                                             |
| ✅         | **/app/quitGame**          | **SEND**        | gameId &lt;string&gt;, userId &lt;long&gt;                                                           | Body (JSON)    | Send quit game request to the server                                                                |
| ✅         | **/user/queue/reply**      | **SUBSCRIBE**   | --                                                                                                   | --             | Subscribe to private channel for receiving personal notifications (capture options, AI hints, etc.) |
| ✅         | **/user/queue/reply**      | **UNSUBSCRIBE** | --                                                                                                   | --             | Unsubscribe from the private channel                                                                |

</details>

<details>
<summary>STOMP notifications...</summary>

#### Lobby join/leave

A client user does join a lobby by subscribing to the `topic/lobby/{lobbyId}` of the lobby he wants to join, and he leaves a lobby by unsubscribing from it.

##### Broadcast to all users in a lobby

When a new user join or leave a lobby the following notification will be broadcast to all the subscribers of 
`topic/lobby/{lobbyId}`.

        {
         "user": username <string>,
         "status": status <string>
         "lobby": {
                   "lobbyId": id <Long>,
                   "hostId": id <Long>,
                   "usersIds": ids List<Long>
                   }
        }

`status` can be either `subscribed` or `unsubscribed`.

##### Sent to a specific user

###### General notification

The user who tries to join will receive back the following notification: 

        {
         "success": success <bool>,
         "msg": msg <string>
        }

`success` describe the success of the operation, while `msg` is a short message describing the success or the reason of failure of the 
action.

###### What if the user is already in a lobby?

If the user is already in a lobby and they are trying to join again through the client ui, they will not be able to join a new lobby and the following message will be sent, 
so that the client can redirect the user to the right lobby.

        {
         "success": "false",
         "msg": "User with id {userId} already joined lobby {lobbyId}"
        }

#### Lobby deletion

When the host leave the lobby by explicitly sending an `unsubscribe` request, their lobby is deleted.

##### Broadcast to all users in a lobby

The following message is broadcast to all the participants of this lobby.

        {
        "msg": "Lobby with id {lobbyId} has been deleted"
        }

##### Sent to a specific user

The following message is sent to the host of the lobby.

        {
        "success": success <bool>,
        "msg": msg <string>
        }

#### Start game

The following message is broadcast to all the participants of this lobby.

        {
        "success": success <bool>,
        "msg": msg <string>
        }

`msg` can be either `"Starting game"` or a string describing the error, e.g. `"Lobby <lobbyId> is not full yet"` or
`"lobby <lobbyId>: not everyone wants a rematch yet"`

##### Sent to a specific user

The following message is sent to the client who requested a rematch.

        {
        "success": success <bool>,
        "msg": msg <string>
        }

`msg` can be either `"Rematcher has been added to the lobby"` or a string describing the error.

#### Rematch

When a user clicks on the rematch button the following messages are sent.

##### Broadcast to all users in a lobby

The following message is broadcast to all the participants of this lobby.

        {
        "lobbyId": lobbyId <Long>,
        "hostId": hostId <Long>,
        "usersIds": List<Long>,
        "rematchersIds": List<Long>
        }

`rematchersIds` contains all the user that have selected a rematch.

##### Sent to a specific user

The following message is sent to the client who requested a rematch.

        {
        "success": success <bool>,
        "msg": msg <string>
        }

`msg` can be either `"Rematcher has been added to the lobby"` or a string describing the error.

</details>

### Database

#### User

#### Lobby

### Game logic

### Ai Assistance

## Launch & Deployment

Getting started with Spring Boot

    Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/index.html
    Guides: http://spring.io/guides
        Building a RESTful Web Service: http://spring.io/guides/gs/rest-service/
        Building REST services with Spring: https://spring.io/guides/tutorials/rest/

Setup this Template with your IDE of choice

Download your IDE of choice (e.g., IntelliJ, Visual Studio Code, or Eclipse). Make sure Java 17 is installed on your
system (for Windows, please make sure your JAVA_HOME environment variable is set to the correct version of Java).
IntelliJ

    File -> Open... -> SoPra server template
    Accept to import the project as a gradle project
    To build right click the build.gradle file and choose Run Build

VS Code

The following extensions can help you get started more easily:

    vmware.vscode-spring-boot
    vscjava.vscode-spring-initializr
    vscjava.vscode-spring-boot-dashboard
    vscjava.vscode-java-pack

Note: You'll need to build the project first with Gradle, just click on the build command in the Gradle Tasks extension.
Then check the Spring Boot Dashboard extension if it already shows soprafs24 and hit the play button to start the
server. If it doesn't show up, restart VS Code and check again.
Building with Gradle

You can use the local Gradle Wrapper to build the application.

    macOS: ./gradlew
    Linux: ./gradlew
    Windows: ./gradlew.bat

<details>
<summary>More Information about Gradle Wrapper and Gradle.</summary>

Build

```./gradlew build```

Run

```./gradlew bootRun```

You can verify that the server is running by visiting localhost:8080 in your browser.
Test

```./gradlew test```

Development Mode

You can start the backend in development mode, this will automatically trigger a new build and reload the application
once the content of a file has been changed.

Start two terminal windows and run:

```./gradlew build --continuous```

and in the other one:

```./gradlew bootRun```

If you want to avoid running all tests with every change, use the following command instead:

```./gradlew build --continuous -xtest```

To configure a debugger for SpringBoot's Tomcat servlet (i.e. the process you start with ./gradlew bootRun command), do
the following:

    Open Tab: Run/Edit Configurations
    Add a new Remote Configuration and name it properly
    Start the Server in Debug mode: ./gradlew bootRun --debug-jvm
    Press Shift + F9 or the use Run/Debug "Name of your task"
    Set breakpoints in the application where you need it
    Step through the process one step at a time

</details>

Testing

Have a look here: https://www.baeldung.com/spring-boot-testing

### Release

New contributions to the main branch are continuously build, tested, analyzed and deployed to Google Engine using 
GitHub actions.

Code releases are done at the end of each sprint by manually creating a GitHub release with a new tag.

## Roadmap

## Authors & Acknowledgements

### Authors

* [5North](https://github.com/5North)
* Author 2
* Author 3
* Author 4

### Acknowledgements

We would like to thank our tutor [] for his support and guidance during this course.

## License

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This work is licensed under the MIT License - see the LICENSE.md file for details.

The project is based on the [sopra-fs25-template-server](https://github.com/HASEL-UZH/sopra-fs25-template-server)
, which is licensed under the Apache 2.0 license.