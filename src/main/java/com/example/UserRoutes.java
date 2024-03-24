package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.DateTime;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;
import static com.example.UserRegistry.*;

public class UserRoutes {
    public static final List<User> users = new ArrayList<>();
    private final ActorRef<Command> userRegistryActor;
    private final Duration askTimeout;
    private final Scheduler scheduler;
    private final UserRegistry.Error errorUnProccessableContent;
    private User userAuth;
    public UserRoutes(ActorSystem<?> system, ActorRef<Command> userRegistryActor) {
        this.userRegistryActor = userRegistryActor;
        scheduler = system.scheduler();
        askTimeout = system.settings().config().getDuration("my-app.routes.ask-timeout");
        errorUnProccessableContent = new UserRegistry.Error("session.errors.emailAlreadyRegistered");
        userAuth = new User("", false);
    }

    private CompletionStage<RegistrationUser> createUser(User user) {
        return AskPattern.ask(userRegistryActor, ref -> new CreateUser(user, ref), askTimeout, scheduler);
    }

    private CompletionStage<LoginUser> authorizeUsers(User user) {
        return AskPattern.ask(userRegistryActor, ref -> new LoginExistsUser(user, ref), askTimeout, scheduler);
    }

    private CompletionStage<AuthorizationUser> getAuthorizationUser(User user) {
        return AskPattern.ask(userRegistryActor, ref ->
                new GetAuthorizationUser(Objects.requireNonNullElse(user, new User("", false)), ref),
                askTimeout, scheduler);
    }

    public Route userRoutes() {
        return pathPrefix("api_v1", () ->
                concat(path("registrate", this::registrateUser))
                        .orElse(path("login", this::loginUser))
                        .orElse(path("me", this::getUser))
                        .orElse(path("logout", this::logoutUser)));
    }

    private Route registrateUser() {
        return post(() ->
                entity(Jackson.unmarshaller(RegistrationUser.class),
                        regUser -> onSuccess(createUser(new User(regUser.email(), false)), performed -> {
                            User newUser = new User(UUID.randomUUID().toString(),
                                    regUser.name(),
                                    regUser.email(),
                                    DateTime.now().toRfc1123DateTimeString(),
                                    regUser.password(),
                                    false);

                            if (users.contains(newUser)) {
                                return complete(StatusCodes.UNPROCESSABLE_CONTENT, errorUnProccessableContent, Jackson.marshaller());
                            } else {
                                users.add(newUser);

                                return complete(StatusCodes.OK, "", Jackson.marshaller());
                            }
                        })
                )
        );
    }

    private Route loginUser() {
        return post(() ->
                entity(
                        Jackson.unmarshaller(LoginUser.class),
                        loginUser -> {
                            User user = new User(loginUser.email(), false);
                            return onSuccess(authorizeUsers(user), performed -> {
                                        if (users.contains(user)) {
                                            User authUser = users.stream()
                                                    .filter(users::contains)
                                                    .findFirst()
                                                    .get();

                                            userAuth = new User(authUser.id(),
                                                    authUser.name(),
                                                    authUser.email(),
                                                    authUser.created(),
                                                    authUser.password(),
                                                    true);

                                            return complete(StatusCodes.OK, "", Jackson.marshaller());
                                        } else {
                                            return complete(StatusCodes.UNPROCESSABLE_CONTENT, errorUnProccessableContent, Jackson.marshaller());
                                        }
                                    }
                            );
                        }
                ));
    }

    private Route getUser() {
        return get(() ->
             onSuccess(getAuthorizationUser(userAuth), performed -> {
                if (userAuth.login()) {
                    return complete(StatusCodes.OK, performed, Jackson.marshaller());
                } else {
                    return complete(StatusCodes.UNAUTHORIZED, "", Jackson.marshaller());
                }
        }));
    }

    private Route logoutUser() {
        return put(() -> {
            userAuth = new User("", false);

            return complete(StatusCodes.OK);
        });
    }
}
