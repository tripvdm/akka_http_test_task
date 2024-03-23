package com.example;


import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.typed.ActorRef;
import akka.http.javadsl.model.DateTime;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.example.UserRoutes.users;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserRoutesTest extends JUnitRouteTest {

    @ClassRule
    public static TestKitJunitResource testkit = new TestKitJunitResource();

    private static ActorRef<UserRegistry.Command> userRegistry;
    private TestRoute appRoute;

    @BeforeClass
    public static void beforeClass() {
        userRegistry = testkit.spawn(UserRegistry.create());
    }

    @Before
    public void before() {
        UserRoutes userRoutes = new UserRoutes(testkit.system(), userRegistry);
        appRoute = testRoute(userRoutes.userRoutes());
        // (String id, String name, String email, String created, String password, boolean login)
        users.add(new UserRegistry.User("42b9a471-5d70-489f-ae4f-7702411e527b",
                "sergey",
                "sergey@mail.ru",
                "12-12-12",
                "erere",
                true));
    }

    @AfterClass
    public static void afterClass() {
        testkit.stop(userRegistry);
    }

    @Test
    public void testIfUserNotExistForRegistration() {
        appRoute.run(HttpRequest.POST("/api_v1/registrate")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), "{\n" +
                                "  \"email\": \"sfds@mail.ru\",\n" +
                                "  \"password\": \"wewq\",\n" +
                                "   \"name\": \"vadim\"\n" +
                                "}"))
                .assertStatusCode(StatusCodes.OK)
                .assertMediaType("application/json")
                .assertEntity("\"\"");
    }

    @Test
    public void testIfUserExistForRegistration() {
        appRoute.run(HttpRequest.POST("/api_v1/registrate")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), "{\n" +
                                "  \"email\": \"sergey@mail.ru\",\n" +
                                "  \"password\": \"erere\",\n" +
                                "   \"name\": \"sergey\"\n" +
                                "}"))
                .assertStatusCode(StatusCodes.UNPROCESSABLE_CONTENT)
                .assertMediaType("application/json")
                .assertEntity("{\"error\":\"session.errors.emailAlreadyRegistered\"}");
    }

    @Test
    public void testLoginExistsUser() {
        appRoute.run(HttpRequest.POST("/api_v1/login")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), "{\n" +
                                " \"email\":\"sergey@mail.ru\",\n" +
                                " \"password\":\"erere\"\n" +
                                "}"))
                .assertStatusCode(StatusCodes.OK)
                .assertMediaType("application/json")
                .assertEntity("\"\"");
    }

    @Test
    public void testLoginNotExistsUser() {
        appRoute.run(HttpRequest.POST("/api_v1/login")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), "{\n" +
                                "  \"email\": \"sfds@mail.ru\",\n" +
                                "  \"password\": \"wewq\"\n" +
                                "}"))
                .assertStatusCode(StatusCodes.UNPROCESSABLE_CONTENT)
                .assertMediaType("application/json")
                .assertEntity("{\"error\":\"session.errors.emailAlreadyRegistered\"}");
    }

    @Test
    public void testLoginAfterRegistration() {
        appRoute.run(HttpRequest.POST("/api_v1/login")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), "{\n" +
                                " \"email\":\"sergey@mail.ru\",\n" +
                                " \"password\":\"erere\"\n" +
                                "}"))
                .assertStatusCode(StatusCodes.OK)
                .assertMediaType("application/json")
                .assertEntity("\"\"");
    }

    @Test
    public void testGetAuthorizationUser() {
        appRoute.run(HttpRequest.GET("/api_v1/me"))
                .assertStatusCode(StatusCodes.OK)
                .assertEntity("{\n" +
                        "\"created\": \"Sat, 23 Mar 2024 14:49:13 GMT\",\n" +
                        "\"email\": \"sergey@mail.ru\",\n" +
                        "\"id\": \"11162b7f-7fa1-4981-b894-f020a6f77e6a\",\n" +
                        "\"name\": \"sergey\"\n" +
                        "}");
    }

    @Test
    public void testGetNonAuthorizationUser() {
        appRoute.run(HttpRequest.GET("/api_v1/me")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
                                "{\"name\":\"Kapik\",\"age\":42,\"countryOfResidence\":\"jp\"}"))
                .assertStatusCode(StatusCodes.UNAUTHORIZED)
                .assertMediaType("application/json")
                .assertEntity("\"\"");
    }

    @Test
    public void testLogoutAuthorizationUser() {
        appRoute.run(HttpRequest.PUT("/api_v1/logout"))
                .assertStatusCode(StatusCodes.OK);
    }

    @Test
    public void testGetNotAuthorizationUser() {
        appRoute.run(HttpRequest.GET("/api_v1/me")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
                                "{\"name\":\"Kapik\",\"age\":42,\"countryOfResidence\":\"jp\"}"))
                .assertStatusCode(StatusCodes.UNAUTHORIZED)
                .assertMediaType("application/json")
                .assertEntity("\"\"");
    }
}
