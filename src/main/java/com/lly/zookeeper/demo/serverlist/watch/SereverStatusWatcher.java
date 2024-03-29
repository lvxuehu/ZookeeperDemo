package com.lly.zookeeper.demo.serverlist.watch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lly on 14-7-16.
 */
public class SereverStatusWatcher {
    private String groupNode = "sgroup";
    private ZooKeeper zk;
    private Stat stat = new Stat();
    private volatile List<String> serverList;

    /**
     * 连接zookeeper
     */
    public void connectZookeeper() throws Exception {
        zk = new ZooKeeper("master:2181,node1:2181,node2:2181", 5000, new Watcher() {
            public void process(WatchedEvent event) {
                // 如果发生了"/sgroup"节点下的子节点变化事件, 更新server列表, 并重新注册监听
                if (event.getType() == Event.EventType.NodeChildrenChanged
                        && ("/" + groupNode).equals(event.getPath())) {
                    try {
                        updateServerList();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        updateServerList();
    }

    /**
     * 更新server列表
     */
    private void updateServerList() throws Exception {
        List<String> newServerList = new ArrayList<String>();

        // 获取并监听groupNode的子节点变化
        // watch参数为true, 表示监听子节点变化事件.
        // 每次都需要重新注册监听, 因为一次注册, 只能监听一次事件, 如果还想继续保持监听, 必须重新注册
        List<String> subList = zk.getChildren("/" + groupNode, true);
        for (String subNode : subList) {
            // 获取每个子节点下关联的server地址
            byte[] data = zk.getData("/" + groupNode + "/" + subNode, false, stat);
            newServerList.add(new String(data, "utf-8"));
        }

        // 替换server列表
        serverList = newServerList;

        System.out.println("server list updated: " + serverList);
    }

    /**
     * client的工作逻辑写在这个方法中
     * 此处不做任何处理, 只让client sleep
     */
    public void handle() throws InterruptedException {
        Thread.sleep(Long.MAX_VALUE);
    }

    public static void main(String[] args) throws Exception {
        SereverStatusWatcher ac = new SereverStatusWatcher();
        ac.connectZookeeper();

        ac.handle();
    }
}
