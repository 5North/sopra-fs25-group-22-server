# SoPra RESTful Service Template FS25

## REST Specs

| Mapping                | Method     | Parameter(s)                                                      | Parameter Type     | Status Code | Response                                            | Response Type | Description                                                       |
|------------------------|------------|-------------------------------------------------------------------|--------------------|-------------|-----------------------------------------------------|---------------|-------------------------------------------------------------------|
| **/login**             | **POST**   | username &lt;string&gt;, password &lt;string&gt;                  | Body               | 200         | Token &lt;string&gt;                                | Header        | Log in user and return an authentication token                    |
| **/login**             | **POST**   | username &lt;string&gt;, password &lt;string&gt;                  | Body               | 403         | Error: reason &lt;string&gt;                        | Body          | Login failed due to invalid credentials                           |
| **/logout**            | **POST**   | Token &lt;string&gt;                                              | Header             | 204         | --                                                  | Header        | Log out the user (invalidate token)                               |
| **/logout**            | **POST**   | Token &lt;string&gt;                                              | Header             | 401         | Error: reason &lt;string&gt;                        | Body          | Logout failed due to unauthenticated request                      |
| **/users**             | **POST**   | username &lt;string&gt;, password &lt;string&gt;                  | Body               | 201         | Token &lt;string&gt;; User(*)                       | Header; Body  | Create new user and auto-login                                    |
| **/users**             | **POST**   | username &lt;string&gt;, password &lt;string&gt;                  | Body               | 409         | Error: reason &lt;string&gt;                        | Body          | User creation failed because username already exists              |
| **/users**             | **GET**    | Token &lt;string&gt;                                              | Header             | 200         | list&lt;User(*)&gt;                                 | Body          | Retrieve all users (for scoreboard)                               |
| **/users**             | **GET**    | Token &lt;string&gt;                                              | Header             | 401         | Error: reason &lt;string&gt;                        | Body          | Unauthenticated request for users list                            |
| **/users/{userId}**    | **GET**    | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Path       | 200         | User(*)                                             | Body          | Retrieve specific user profile                                    |
| **/users/{userId}**    | **GET**    | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Path       | 401         | Error: reason &lt;string&gt;                        | Body          | Unauthenticated request for user profile                          |
| **/users/{userId}**    | **GET**    | Token &lt;string&gt;; userId &lt;long&gt;                         | Header; Path       | 404         | Error: reason &lt;string&gt;                        | Body          | User with userId not found                                        |
| **/users/{userId}**    | **PUT**    | Token &lt;string&gt;; User(*) (profile data); userId &lt;long&gt; | Header; Body; Path | 204         | --                                                  | --            | Update user profile                                               |
| **/users/{userId}**    | **PUT**    | Token &lt;string&gt;; User(*) (profile data); userId &lt;long&gt; | Header; Body; Path | 404         | Error: reason &lt;string&gt;                        | Body          | User with userId not found                                        |
| **/lobbies**           | **POST**   | Token &lt;string&gt;                                              | Header; Body       | 201         | Lobby(*) (includes lobbyId, PIN, roomName, players) | Body          | Create new lobby; persist via LobbyRepository ensuring unique PIN |
| **/lobbies**           | **POST**   | Token &lt;string&gt;                                              | Header; Body       | 401         | Error: reason &lt;string&gt;                        | Body          | Lobby creation failed because user is not authenticated           |


## WebSocket Specs

| Mapping                    | Method          | Parameter(s)                                                                                         | Parameter Type | Description                                                                                         |
|----------------------------|-----------------|------------------------------------------------------------------------------------------------------|----------------|-----------------------------------------------------------------------------------------------------|
| **/lobby**                 | **CONNECT**     | Token &lt;string&gt;                                                                                 | Header         | Upgrade connection to WebSocket for lobby operations                                                |
| **/lobby**                 | **DISCONNECT**  | --                                                                                                   | --             | Terminates the WebSocket connection                                                                 |
| **/topic/lobby/{lobbyId}** | **SUBSCRIBE**   | lobbyId &lt;string&gt;                                                                               | Path           | Subscribe to real-time lobby updates (player joins/leaves, notifications)                           |
| **/topic/lobby/{lobbyId}** | **UNSUBSCRIBE** | lobbyId &lt;string&gt;                                                                               | Path           | Unsubscribe from lobby updates                                                                      |
| **/app/playcard**          | **SEND**        | gameId &lt;string&gt;, card &lt;Card&gt;                                                             | Body (JSON)    | Send played card event to server for in-game processing                                             |
| **/app/chooseCapture**     | **SEND**        | gameId &lt;string&gt;, userId &lt;long&gt;, chosenOption &lt;List{Card}&gt;, playedCard &lt;Card&gt; | Body (JSON)    | Send chosen capture option when multiple options exist                                              |
| **/app/ai**                | **SEND**        | gameId &lt;string&gt;, userId &lt;long&gt;, requestFlag &lt;string&gt;                               | Body (JSON)    | Send request for AI assistance (hint) to the server                                                 |
| **/app/rematch**           | **SEND**        | gameId &lt;string&gt;, userId &lt;long&gt;, confirmRematch &lt;boolean&gt;                           | Body (JSON)    | Send rematch confirmation from the player to the server                                             |
| **/app/quitGame**          | **SEND**        | gameId &lt;string&gt;, userId &lt;long&gt;                                                           | Body (JSON)    | Send quit game request to the server                                                                |
| **/game/{gameId}/results** | **SUBSCRIBE**   | gameId &lt;string&gt;                                                                                | Path           | Subscribe to final game results broadcast when the match ends                                       |
| **/user/queue/reply**      | **SUBSCRIBE**   | --                                                                                                   | --             | Subscribe to private channel for receiving personal notifications (capture options, AI hints, etc.) |
| **/user/queue/reply**      | **UNSUBSCRIBE** | --                                                                                                   | --             | Unsubscribe from the private channel                                                                |

### STOMP notifications

#### Broadcast to all users in a lobby

When a new user join or leave a lobby the following notification will be broadcast to all the subscribers of 
`topic/lobby/{lobbyId}`.

        {
         "user": username,
         "status": status
        }

`status` can be either `subscribed`, `unsubscribed` or `disconnected`.
Keep in mind that `disconnected` does refer to a disconnection event of the STOMP protocol, and not to a disconnection 
of the Web Socket session.

#### Sent to a specific user

The user who tries to join will receive back the following notification: 

        {
         "join": success,
         "msg": msg
        }

`success` is a boolean value, while `msg` is a short message describing the success or the reason of failure of the 
action.

## Getting started with Spring Boot
-   Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/index.html
-   Guides: http://spring.io/guides
    -   Building a RESTful Web Service: http://spring.io/guides/gs/rest-service/
    -   Building REST services with Spring: https://spring.io/guides/tutorials/rest/

## Setup this Template with your IDE of choice
Download your IDE of choice (e.g., [IntelliJ](https://www.jetbrains.com/idea/download/), [Visual Studio Code](https://code.visualstudio.com/), or [Eclipse](http://www.eclipse.org/downloads/)). Make sure Java 17 is installed on your system (for Windows, please make sure your `JAVA_HOME` environment variable is set to the correct version of Java).

### IntelliJ
If you consider to use IntelliJ as your IDE of choice, you can make use of your free educational license [here](https://www.jetbrains.com/community/education/#students).
1. File -> Open... -> SoPra server template
2. Accept to import the project as a `gradle project`
3. To build right click the `build.gradle` file and choose `Run Build`

### VS Code
The following extensions can help you get started more easily:
-   `vmware.vscode-spring-boot`
-   `vscjava.vscode-spring-initializr`
-   `vscjava.vscode-spring-boot-dashboard`
-   `vscjava.vscode-java-pack`

**Note:** You'll need to build the project first with Gradle, just click on the `build` command in the _Gradle Tasks_ extension. Then check the _Spring Boot Dashboard_ extension if it already shows `soprafs24` and hit the play button to start the server. If it doesn't show up, restart VS Code and check again.

## Building with Gradle
You can use the local Gradle Wrapper to build the application.
-   macOS: `./gradlew`
-   Linux: `./gradlew`
-   Windows: `./gradlew.bat`

More Information about [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) and [Gradle](https://gradle.org/docs/).

### Build

```bash
./gradlew build
```

### Run

```bash
./gradlew bootRun
```

You can verify that the server is running by visiting `localhost:8080` in your browser.

### Test

```bash
./gradlew test
```

### Development Mode
You can start the backend in development mode, this will automatically trigger a new build and reload the application
once the content of a file has been changed.

Start two terminal windows and run:

`./gradlew build --continuous`

and in the other one:

`./gradlew bootRun`

If you want to avoid running all tests with every change, use the following command instead:

`./gradlew build --continuous -xtest`

## API Endpoint Testing with Postman
We recommend using [Postman](https://www.getpostman.com) to test your API Endpoints.

## Debugging
If something is not working and/or you don't know what is going on. We recommend using a debugger and step-through the process step-by-step.

To configure a debugger for SpringBoot's Tomcat servlet (i.e. the process you start with `./gradlew bootRun` command), do the following:

1. Open Tab: **Run**/Edit Configurations
2. Add a new Remote Configuration and name it properly
3. Start the Server in Debug mode: `./gradlew bootRun --debug-jvm`
4. Press `Shift + F9` or the use **Run**/Debug "Name of your task"
5. Set breakpoints in the application where you need it
6. Step through the process one step at a time

## Testing
Have a look here: https://www.baeldung.com/spring-boot-testing

<br>
<br>
<br>

## Docker

### Introduction
This year, for the first time, Docker will be used to ease the process of deployment.\
Docker is a tool that uses containers as isolated environments, ensuring that the application runs consistently and uniformly across different devices.\
Everything in this repository is already set up to minimize your effort for deployment.\
All changes to the main branch will automatically be pushed to dockerhub and optimized for production.

### Setup
1. **One** member of the team should create an account on [dockerhub](https://hub.docker.com/), _incorporating the group number into the account name_, for example, `SoPra_group_XX`.\
2. This account then creates a repository on dockerhub with the _same name as the group's Github repository name_.\
3. Finally, the person's account details need to be added as [secrets](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions#creating-secrets-for-a-repository) to the group's repository:
    - dockerhub_username (the username of the dockerhub account from step 1, for example, `SoPra_group_XX`)
    - dockerhub_password (a generated PAT([personal access token](https://docs.docker.com/docker-hub/access-tokens/)) of the account with read and write access)
    - dockerhub_repo_name (the name of the dockerhub repository from step 2)

### Pull and run
Once the image is created and has been successfully pushed to dockerhub, the image can be run on any machine.\
Ensure that [Docker](https://www.docker.com/) is installed on the machine you wish to run the container.\
First, pull (download) the image with the following command, replacing your username and repository name accordingly.

```docker pull <dockerhub_username>/<dockerhub_repo_name>```

Then, run the image in a container with the following command, again replacing _<dockerhub_username>_ and _<dockerhub_repo_name>_ accordingly.

```docker run -p 3000:3000 <dockerhub_username>/<dockerhub_repo_name>```
