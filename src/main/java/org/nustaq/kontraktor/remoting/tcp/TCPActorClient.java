package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;

import java.io.*;
import java.net.Socket;

/**
 * Created by ruedi on 08.08.14.
 */
public class TCPActorClient extends RemoteRefRegistry {

    Actor facadeProxy;
    BackOffStrategy backOffStrategy = new BackOffStrategy();

    String host;
    int port;
    ActorClient client;

    public TCPActorClient(ActorProxy proxy, String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.facadeProxy = (Actor) proxy;
        registerRemoteRefDirect(facadeProxy);
        connect();
    }

    void connect() throws IOException {
        client = new ActorClient();
    }

    /**
     * FIXME: slowish (for a starting ..)
     */
    public class ActorClient {

        Socket clientSocket;
        OutputStream outputStream;
        InputStream inputStream;

        public ActorClient() throws IOException {
            clientSocket = new Socket(host, port);
            outputStream = new BufferedOutputStream(clientSocket.getOutputStream(),64000);
            inputStream  = new BufferedInputStream(clientSocket.getInputStream(), 64000);
//            outputStream = new DataOutputStream(clientSocket.getOutputStream());
//            inputStream  = new DataInputStream(clientSocket.getInputStream());
            new Thread(
                () -> {
                    currentOutput.set(outputStream);
                    sendLoop(outputStream);
                },
                "sender"
            ).start();
            new Thread(
                () -> {
                    currentOutput.set(outputStream);
                    receiveLoop(inputStream,outputStream);
                },
                "receiver"
            ).start();
        }

        public void close() throws IOException {
            clientSocket.close();
        }
    }

    public static class TA extends Actor<TA> {
        public void $run(ServerTestFacade test, ClientSideActor csa) {
            delayed(1000, () -> {
                test.$testCall("Hello", csa);
            });
            delayed(1000, () -> {
                test.$testCallWithCB(System.currentTimeMillis(), (r, e) -> {
                    System.out.println(r+" "+Thread.currentThread().getName());
                });
            });
            delayed(1000, () -> {
                test.$doubleMe("ToBeDoubled").then( (r,e) -> {
                    System.out.println(r+" "+Thread.currentThread().getName());
                    self().$run(test,csa);
                });
            });
        }
    }

    public static void main( String arg[] ) throws IOException, InterruptedException {
        ServerTestFacade test = Actors.AsActor(ServerTestFacade.class, new RemoteScheduler());
        test.__remoteId = 1; // facade is always 1

        ClientSideActor csa = Actors.AsActor(ClientSideActor.class);

        TCPActorClient client = new TCPActorClient((ActorProxy) test,"localhost",7777);
        boolean bench = false;
        if ( bench ) {
            while( true ) {
//                test.$benchMark(13, "this is a longish string");
                test.$benchMark(13, null);
            }
        } else {
            TA t = Actors.AsActor(TA.class);
            t.$run(test,csa);
        }
    }
}