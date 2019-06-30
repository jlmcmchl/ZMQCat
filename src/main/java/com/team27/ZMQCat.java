package com.team27;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

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
        System.err.println(
            "Usage: java -jar ZMQCat.jar <Socket Type> [--bind | --connect] <Pub Socket Address>");
        System.exit(1);
    }

    return SocketType.SUB;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println(
          "Usage: java -jar ZMQCat.jar <Socket Type> [--bind | --connect] <Pub Socket Address>");
      System.exit(1);
    }

    boolean bind = false;
    String address;
    if (args[1].equals("--bind")) {
      address = args[2];
      bind = true;
    } else if (args[1].equals("--connect")) {
      address = args[2];
    } else {
      address = args[1];
    }

    ZMQ.Socket socket = null;
    try (ZContext context = new ZContext();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

      SocketType socket_type = parse_socket_type(args[0]);
      socket = context.createSocket(socket_type);

      boolean success;
      if (bind) {
        if (!socket.bind(address)) {
          System.err.println(
              String.format(
                  "Failed to bind to %s. Reason: %s",
                  address, ZMQ.Error.findByCode(socket.errno()).getMessage()));
          return;
        }
        System.out.println(String.format("Bound to %s", address));
      } else {
        if (!socket.connect(address)) {
          System.err.println(
              String.format(
                  "Failed to connect to %s. Reason: %s",
                  address, ZMQ.Error.findByCode(socket.errno()).getMessage()));
          return;
        }
        System.err.println(String.format("Connected to %s", address));
      }

      if (socket_type.equals(SocketType.SUB)) {
        socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
      }

      boolean can_send = true;
      boolean can_recv = true;

      while (!Thread.currentThread().isInterrupted()) {
        String incoming = null;
        String outgoing = null;

        if (can_send) {
          try {
            if (reader.ready()) {
              outgoing = reader.readLine();
              if (outgoing != null && !socket.send(outgoing, ZMQ.DONTWAIT)) {
                System.err.println(
                    String.format(
                        "SEND on %s @ %s failed: %s",
                        args[0], address, ZMQ.Error.findByCode(socket.errno()).getMessage()));
              }
            }
          } catch (UnsupportedOperationException e) {
            System.err.println(String.format("%s does not support sending.", args[0]));
            can_send = false;
          } catch (ZMQException e) {
            System.err.println(
                String.format(
                    "SEND on %s @ %s failed: %s",
                    args[0], address, ZMQ.Error.findByCode(socket.errno()).getMessage()));
          }
        }

        try {
          if ((incoming = socket.recvStr(ZMQ.DONTWAIT)) != null) {
            can_recv = true;
            writer.write(incoming);
            writer.newLine();
          }
        } catch (UnsupportedOperationException e) {
          if (can_recv) {
            System.err.println(String.format("%s does not support receiving.", args[0]));
            can_recv = false;
          }
        } catch (ZMQException e) {
          if (can_recv) {
            System.err.println(
                String.format(
                    "RECV on %s @ %s failed: %s",
                    args[0], address, ZMQ.Error.findByCode(socket.errno()).getMessage()));
            can_recv = false;
          }
        }

        Thread.sleep(1);
      }
    } catch (ZMQException e) {
      e.printStackTrace(System.err);
    } catch (IllegalArgumentException e) {
      e.printStackTrace(System.err);
    } catch (IOException e) {
      e.printStackTrace(System.err);
    } catch (InterruptedException e) {
      e.printStackTrace(System.err);
    } finally {
      if (socket != null) {
        socket.close();
      }
    }
  }
}
