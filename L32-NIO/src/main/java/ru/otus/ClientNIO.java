package ru.otus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class ClientNIO {
    private static final Logger logger = LoggerFactory.getLogger(ClientNIO.class);

    private static final int PORT = 8080;
    private static final String HOST = "localhost";

    public static void main(String[] args) throws InterruptedException {
       // new Thread(() -> new ClientNIO().go("wait")).start();
        Thread.sleep(10);
        new Thread(() -> new ClientNIO().go("testData_1")).start();
        new Thread(() -> new ClientNIO().go("testData_2")).start();
    }

    private void go(String request) {
        try {
            try (SocketChannel socketChannel = SocketChannel.open()) {
                socketChannel.configureBlocking(false);

                socketChannel.connect(new InetSocketAddress(HOST, PORT));

                logger.info("connecting to server");
                while (!socketChannel.finishConnect()) {
                    logger.info("connection established");
                }
                send(socketChannel, request);
                handleResponse(socketChannel);
                sleep();
                logger.info("stop communication");
                send(socketChannel, "stop");
            }
        } catch (Exception ex) {
            logger.error("error", ex);
        }
    }

    private void send(SocketChannel socketChannel, String request) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1000);
        buffer.put(request.getBytes());
        buffer.flip();
        logger.info("sending to server");
        socketChannel.write(buffer);
    }

    private void handleResponse(SocketChannel socketChannel) throws IOException {
        Selector selectorOpen = Selector.open();
        socketChannel.register(selectorOpen, SelectionKey.OP_READ);
        while (true) {
            logger.info("waiting for response");
            if (selectorOpen.select() > 0) { //This method performs a blocking
                Iterator<SelectionKey> selectedKeys = selectorOpen.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    if (key.isReadable()) {
                        processServerResponse((SocketChannel) key.channel());
                        selectedKeys.remove();
                    }
                }
                return;
            }
        }
    }

    private void processServerResponse(SocketChannel socketChannel) throws IOException {
        logger.info("something happened");
        ByteBuffer buffer = ByteBuffer.allocate(2);

        StringBuilder response = new StringBuilder();
        while (socketChannel.read(buffer) > 0) {
            buffer.flip();
            String responsePart = StandardCharsets.UTF_8.decode(buffer).toString();
            logger.info("responsePart: {}", responsePart);
            response.append(responsePart);
            buffer.flip();
        }
        logger.info("response: {}", response);
    }

    private static void sleep() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

