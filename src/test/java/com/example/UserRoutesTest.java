package com.example;



import akka.actor.typed.ActorRef;
import akka.http.javadsl.model.*;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import org.junit.*;
import org.junit.runners.MethodSorters;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;
import akka.actor.testkit.typed.javadsl.TestKitJunitResource;

import static com.example.UserRegistry.users;


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
        users.add(new UserRegistry.User("11", "vadim", "sfds@mail.ru", "11.11.11", "wewq"));
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
                                "  \"email\": \"sfds@mail.ru\",\n" +
                                "  \"password\": \"wewq\",\n" +
                                "   \"name\": \"vadim\"\n" +
                                "}"))
                .assertStatusCode(StatusCodes.UNPROCESSABLE_CONTENT)
                .assertMediaType("application/json")
                .assertEntity("{\"error\":\"session.errors.emailAlreadyRegistered\"}");
    }

    @Test
    public void testLoginExistsUser() {
        appRoute.run(HttpRequest.POST("/api_v1/login")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), "{\n" +
                                "  \"email\": \"sfds@mail.ru\",\n" +
                                "  \"password\": \"wewq\"\n" +
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
                                "  \"email\": \"sfds@mail.ru\",\n" +
                                "  \"password\": \"wewq\"\n" +
                                "}"))
                .assertStatusCode(StatusCodes.OK)
                .assertMediaType("application/json")
                .assertEntity("\"\"");
    }

    @Test
    public void testGetAuthorizationUser() {
        appRoute.run(HttpRequest.GET("/api_v1/me")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
                                "{\"name\":\"Kapi\",\"age\":42,\"countryOfResidence\":\"jp\"}"))
                .assertStatusCode(StatusCodes.OK)
                .assertMediaType("application/json")
                .assertEntity("{\"name\":\"Kapi\",\"age\":42,\"countryOfResidence\":\"jp\"}");
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
                .assertStatusCode(StatusCodes.OK)
                .assertEntity("\"\"");
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
