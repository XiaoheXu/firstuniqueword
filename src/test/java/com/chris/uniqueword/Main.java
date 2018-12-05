package com.chris.uniqueword;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.chris.uniqueword.info.StartCommand;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chris xu 2018/08/09.
 */
public class Main {

    @Test
    public void main() {
        ActorSystem system = ActorSystem.create("bigFile");
        Map<String, ActorRef> workers = new HashMap<>();

        try {
            // 创建主节点
            ActorRef mainServer = system.actorOf(MainServer.props(workers), "mainServer");
            // 创建worker节点
            for (int i = 0; i < 100; i++) {
                ActorRef worker = system.actorOf(Worker.props(mainServer));
                workers.put(worker.toString(), worker);
            }
            // 开始
            mainServer.tell(new StartCommand(), ActorRef.noSender());

            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            system.terminate();
        }
    }
}
