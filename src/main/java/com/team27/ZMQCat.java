package com.team27;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.ZError;

public class ZMQCat {

  private static SocketType parse_socket_type(String socket_type) throws IOException {
    switch (socket_type) {
      case "PAIR":
        return SocketType.PAIR;
      case "PUB":
        return SocketType.PUB;
      case "SUB":
        return SocketType.SUB;
      case "REQ":
        return SocketType.REQ;
      case "REP":
        return SocketType.REP;
      case "DEALER":
        return SocketType.DEALER;
      case "ROUTER":
        return SocketType.ROUTER;
      case "PULL":
        return SocketType.PULL;
      case "PUSH":
        return SocketType.PUSH;
      case "XPUB":
        return SocketType.XPUB;
      case "XSUB":
        return SocketType.XSUB;
      case "STREAM":
        return SocketType.STREAM;
      default:
        throw new IOException(String.format("Unknown Socket Type %s", socket_type));
    }
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("Usage: java -jar ZMQCat.jar <Socket Type> <Pub Socket Address>");
      System.exit(1);
    }

    String address = args[1];

    try (ZContext context = new ZContext();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {

      SocketType socket_type = parse_socket_type(args[0]);
      ZMQ.Socket subscriber = context.createSocket(socket_type);
      if (!subscriber.connect(address)) {
        System.err.println(String.format("Failed to connect to %s. Reason: %s", address, ZError.toString(subscriber.errno())));
        return;
      }
      subscriber.subscribe(ZMQ.SUBSCRIPTION_ALL);

      while (!Thread.currentThread().isInterrupted()) {
        String msg = subscriber.recvStr(ZMQ.DONTWAIT);

        if (msg != null) {
          writer.write(msg);
          writer.newLine();
        }
      }

      writer.close();

      subscriber.close();
    } catch (ZMQException e) {
      System.err.println(String.format("ERR ZMQException: %s", e.getMessage()));
      System.exit(1);
    } catch (IllegalArgumentException e) {
      System.err.println(String.format("ERR IllegalArgumentException: %s", e.getMessage()));
      System.exit(1);
    } catch (IOException e) {
      System.err.println(String.format("ERR IOException: %s", e.getMessage()));
      System.exit(1);
    }
  }
}
