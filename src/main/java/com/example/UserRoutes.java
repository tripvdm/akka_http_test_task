package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import com.example.UserRegistry.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;
import static com.example.UserRegistry.*;

public class UserRoutes {
    private static final Logger log = LoggerFactory.getLogger(UserRoutes.class);
    private final ActorRef<Command> userRegistryActor;
    private final Duration askTimeout;
    private final Scheduler scheduler;

    public UserRoutes(ActorSystem<?> system, ActorRef<Command> userRegistryActor) {
        this.userRegistryActor = userRegistryActor;
        scheduler = system.scheduler();
        askTimeout = system.settings().config().getDuration("my-app.routes.ask-timeout");
    }

    private CompletionStage<GetUserResponse> getUser(String name) {
        return AskPattern.ask(userRegistryActor, ref -> new GetUser(name, ref), askTimeout, scheduler);
    }

    private CompletionStage<Users> getUsers() {
        return AskPattern.ask(userRegistryActor, GetUsers::new, askTimeout, scheduler);
    }

    private CompletionStage<ActionPerformed> createUser(User user) {
        return AskPattern.ask(userRegistryActor, ref -> new CreateUser(user, ref), askTimeout, scheduler);
    }

    public Route userRoutes() {
        return pathPrefix("api_v1", () ->
                concat(path("registrate", this::registrateUser))
                        .orElse(path("login", this::loginUser))
                        .orElse(path("logout", this::logoutUser)));
    }

    private Route registrateUser() {
        return post(() ->
                entity(
                        Jackson.unmarshaller(User.class),
                        user ->
                                onSuccess(createUser(user), performed -> complete(StatusCodes.OK, "", Jackson.marshaller()))
                )
        );
    }

    private Route loginUser() {
        return post(() ->
                entity(
                        Jackson.unmarshaller(User.class),
                        user -> onSuccess(createUser(user), performed -> complete(StatusCodes.OK, "", Jackson.marshaller()))
                )
        );
    }

    private Route logoutUser() {
        return put(() ->
                entity(
                        Jackson.unmarshaller(User.class),
                        user -> onSuccess(createUser(user), performed -> complete(StatusCodes.OK, "", Jackson.marshaller()))
                )
        );
    }
}
