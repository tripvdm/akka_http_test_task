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

  public static final record GetUsers(ActorRef<Users> replyTo) implements Command {}

  public static final record CreateUser(User user, ActorRef<ActionPerformed> replyTo) implements Command {}

  public static final record GetUserResponse(Optional<User> maybeUser) {}

  public static final record GetUser(String name, ActorRef<GetUserResponse> replyTo) implements Command {}

  public static final record ActionPerformed(String description) implements Command {}

  public static final record User(String name, int age, String countryOfResidence) {}

  public static final record Users(List<User> users) {}

  private final List<User> users = new ArrayList<>();

  private UserRegistry(ActorContext<Command> context) {
    super(context);
  }

  public static Behavior<Command> create() {
    return Behaviors.setup(UserRegistry::new);
  }

  @Override
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
            .onMessage(GetUsers.class, this::onGetUsers)
            .onMessage(CreateUser.class, this::onCreateUser)
            .onMessage(GetUser.class, this::onGetUser)
            .build();
  }

  private Behavior<Command> onGetUsers(GetUsers command) {
    command.replyTo().tell(new Users(Collections.unmodifiableList(new ArrayList<>(users))));
    return this;
  }

  private Behavior<Command> onCreateUser(CreateUser command) {
    users.add(command.user());
    command.replyTo().tell(new ActionPerformed(String.format("User %s created.", command.user().name())));
    return this;
  }

  private Behavior<Command> onGetUser(GetUser command) {
    Optional<User> maybeUser = users.stream()
            .filter(user -> user.name().equals(command.name()))
            .findFirst();
    command.replyTo().tell(new GetUserResponse(maybeUser));
    return this;
  }
}