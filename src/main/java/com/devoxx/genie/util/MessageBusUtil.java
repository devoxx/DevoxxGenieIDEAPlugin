package com.devoxx.genie.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MessageBusUtil {

    public static <T> void subscribe(@NotNull MessageBusConnection connection,
                                     @NotNull Topic<T> topic,
                                     @NotNull T listener) {
        connection.subscribe(topic, listener);
    }

    public static <T> void publish(@NotNull Project project, @NotNull Topic<T> topic, @NotNull Consumer<T> publisher) {
        T publisherInstance = project.getMessageBus().syncPublisher(topic);
        publisher.accept(publisherInstance);
    }

    public static Disposable connect(@NotNull Project project, @NotNull Consumer<MessageBusConnection> consumer) {
        MessageBusConnection connection = project.getMessageBus().connect();
        consumer.accept(connection);
        return connection;
    }
}
