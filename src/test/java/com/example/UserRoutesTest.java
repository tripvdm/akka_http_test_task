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
    }

    @AfterClass
    public static void afterClass() {
        testkit.stop(userRegistry);
    }

    @Test
    public void testIfUserNotExistForRegistration() {
        appRoute.run(HttpRequest.POST("/api_v1/registrate")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
                                "{\"name\": \"Kapi\", \"age\": 42, \"countryOfResidence\": \"jp\"}"))
                .assertStatusCode(StatusCodes.OK)
                .assertMediaType("application/json")
                .assertEntity("\"\"");
    }

    @Test
    public void testIfUserExistForRegistration() {
        appRoute.run(HttpRequest.POST("/users")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
                                "{\"name\": \"Kapi\", \"age\": 42, \"countryOfResidence\": \"jp\"}"))
                .assertStatusCode(StatusCodes.UNPROCESSABLE_ENTITY)
                .assertMediaType("application/json")
                .assertEntity("{\"error\": \"session.errors.emailAlreadyRegistered\"}");
    }

}
