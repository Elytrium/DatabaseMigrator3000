package net.elytrium.dbm;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.sql.SQLException;

public class Main {

  public static void main(String[] args) {
    Config.INSTANCE.reload(Paths.get("config.yml").toAbsolutePath());

    DiscordClient.create(Config.INSTANCE.token)
        .withGateway(client -> {
          client.getEventDispatcher().on(ReadyEvent.class)
              .subscribe(ready -> System.out.println("Logged in as " + ready.getSelf().getUsername()));

          client.getEventDispatcher().on(MessageCreateEvent.class)
              .map(MessageCreateEvent::getMessage)
              .filter(message -> message.getContent().startsWith("!migrate"))
              .publishOn(Schedulers.boundedElastic())
              .flatMap(message -> message.getChannel().flatMap(channel -> {
                // Monke Solutions
                if (!message.getContent().contains(" ")) {
                  return channel.createMessage(
                      MessageCreateSpec.builder()
                          .messageReference(message.getId())
                          .addEmbed(
                              EmbedCreateSpec.builder()
                                  .color(Color.RED)
                                  .description(
                                      "**Использование:** !migrate (ссылка на бд | или вложение) [тип бд до миграции]->[тип бд после миграции] [запрос | пресет]\n" +
                                          "**Пример:** !migrate sqlite->h2 authme\n\n" +
                                          "**Доступные типы бд:** " + String.join(", ", DatabaseMigrator.DATABASE_CONNECTORS.keySet()) + "\n" +
                                          "**Доступные пресеты:** " + String.join(", ", Config.INSTANCE.presets.keySet()))
                                  .build()
                          )
                          .build()
                  );
                }

                String[] commandArgs;
                String databaseUrl;
                int offset;
                if (message.getAttachments().size() > 0) {
                  offset = 0;
                  commandArgs = message.getContent().split(" ", 3);
                  databaseUrl = message.getAttachments().get(0).getUrl();
                } else {
                  offset = 1;
                  commandArgs = message.getContent().split(" ", 4);
                  databaseUrl = commandArgs[1];
                }

                if (commandArgs.length < 3 + offset) {
                  return channel.createMessage(
                      MessageCreateSpec.builder()
                          .messageReference(message.getId())
                          .addEmbed(
                              EmbedCreateSpec.builder()
                                  .color(Color.RED)
                                  .description("Недостаточно аргументов.")
                                  .build()
                          )
                          .build()
                  );
                }

                if (databaseUrl == null || (!databaseUrl.startsWith("http://") && !databaseUrl.startsWith("https://"))) {
                  return channel.createMessage(
                      MessageCreateSpec.builder()
                          .messageReference(message.getId())
                          .addEmbed(
                              EmbedCreateSpec.builder()
                                  .color(Color.RED)
                                  .description("Необходимо указать ссылку на базу данных или прикрепить файл с ней к сообщению.")
                                  .build()
                          )
                          .build()
                  );
                }

                String[] migrationTypes = commandArgs[1 + offset].split("->");
                if (migrationTypes.length != 2) {
                  return channel.createMessage(
                      MessageCreateSpec.builder()
                          .messageReference(message.getId())
                          .addEmbed(
                              EmbedCreateSpec.builder()
                                  .color(Color.RED)
                                  .description("Необходимо указать из какого и в какой тип бд нужно преобразовать данные, например !migrate h2->mysql.")
                                  .build()
                          )
                          .build()
                  );
                }

                String query = commandArgs[2 + offset];

                File inputFile = null;
                try {
                  inputFile = File.createTempFile("dbm", ".mv.db");

                  ReadableByteChannel urlChannel = Channels.newChannel(new URL(databaseUrl).openStream());
                  FileOutputStream fileOutputStream = new FileOutputStream(inputFile);
                  FileChannel fileChannel = fileOutputStream.getChannel();
                  fileChannel.transferFrom(urlChannel, 0, Long.MAX_VALUE);

                  if (message.getAuthor().isPresent()) {
                    System.out.println(message.getAuthor().get().getUsername() + " issued migrate command from " +
                        migrationTypes[0] + " to " + migrationTypes[1] + " with query: " + query);
                  }

                  NamedDatabaseBinary migrated = DatabaseMigrator.migrate(inputFile, migrationTypes[0], migrationTypes[1], query);

                  return channel.createMessage(
                      MessageCreateSpec.builder()
                          .messageReference(message.getId())
                          .addEmbed(
                              EmbedCreateSpec.builder()
                                  .color(Color.GREEN)
                                  .description(
                                      "База данных мигрирована из " + migrated.getInputType() + " / " +
                                          migrated.getQueryType() + " в " + migrated.getOutputType() + " / LimboAuth.")
                                  .build()
                          )
                          .addFile(migrated.getFilename(), new ByteArrayInputStream(migrated.getData()))
                          .build()
                  );
                } catch (IOException | IllegalArgumentException e) {
                  e.printStackTrace();
                  return channel.createMessage(
                      MessageCreateSpec.builder()
                          .messageReference(message.getId())
                          .addEmbed(
                              EmbedCreateSpec.builder()
                                  .color(Color.RED)
                                  .description(
                                      "**Во время миграции бд произошла ошибка:**\n" +
                                          e.getMessage())
                                  .build()
                          )
                          .build()
                  );
                } catch (SQLException e) {
                  return channel.createMessage(
                      MessageCreateSpec.builder()
                          .messageReference(message.getId())
                          .addEmbed(
                              EmbedCreateSpec.builder()
                                  .color(Color.RED)
                                  .description(
                                      "**Во время выполнения запроса произошла ошибка:**\n" +
                                          e.getMessage())
                                  .build()
                          )
                          .build()
                  );
                } finally {
                  if (inputFile != null) {
                    if (!inputFile.delete()) {
                      inputFile.deleteOnExit();
                    }
                  }
                }
              }))
              .subscribe();

          return client.onDisconnect();
        })
        .block();
  }
}
