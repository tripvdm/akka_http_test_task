package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.*;

public class UserRegistry extends AbstractBehavior<UserRegistry.Command>  {
    sealed interface Command {}
    public record User(String id, String name, String email, String created, String password, boolean login) {
        User(String email, boolean login) {
            this("", "", email, "", "", login);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return Objects.equals(email, user.email);
        }

        @Override
        public int hashCode() {
            return Objects.hash(email);
        }
    }
    public record RegistrationUser(String email, String password, String name) {}
    public record LoginUser(String email, String password) {}
    public record AuthorizationUser(String id, String email, String created, String name) {}
    public record Error(String error) {}
    public record CreateUser(User user, ActorRef<RegistrationUser> replyTo) implements Command {}
    public record LoginExistsUser(User user, ActorRef<LoginUser> replyTo) implements Command {}
    public record GetAuthorizationUser(User user, ActorRef<AuthorizationUser> replyTo) implements Command {}

    private UserRegistry(ActorContext<Command> context) {
      super(context);
    }

    public static Behavior<Command> create() {
      return Behaviors.setup(UserRegistry::new);
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
              .onMessage(CreateUser.class, this::onCreateUser)
              .onMessage(LoginExistsUser.class, this::onLoginUser)
              .onMessage(GetAuthorizationUser.class, this::onGetUser)
              .build();
    }

    private Behavior<Command> onCreateUser(CreateUser command) {
        User user = command.user();
        RegistrationUser registrationUser = new RegistrationUser(user.email, user.password, user.name);
        command.replyTo().tell(registrationUser);
        return this;
    }

    private Behavior<Command> onLoginUser(LoginExistsUser command) {
        User user = command.user();
        LoginUser loginUser = new LoginUser(user.email, user.password);
        command.replyTo().tell(loginUser);
        return this;
    }

    private Behavior<Command> onGetUser(GetAuthorizationUser command) {
        User user = command.user;
        command.replyTo().tell(new AuthorizationUser(user.id, user.email, user.created, user.name));
        return this;
    }
}