package V1;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Server {
    public static void main(String[] args){
//        System.out.println("启动");
//        for(int i = 0; i < args.length; i++){
//            System.out.println(args[i]);
//        }
//        if(args.length == 0){
//            System.out.println("请提供配置文件目录");
//            System.exit(0);
//        }
        String[] configDirArr = args[0].split("=");
        if(!configDirArr[0].equals("-c")){
            System.out.println("请提供配置文件目录");
            System.exit(0);
        }
        String configDir = configDirArr[1];
//        String configDir = "/Users/cg/data/code/wheel/java/raft/test/conf3";
        String serverFile = configDir + "/" + "server";
        String configFile = configDir + "/" + "config.cf";
        try{
            FileInputStream fileInputStream = new FileInputStream(serverFile);
            FileInputStream fileInputStream1 = new FileInputStream(configFile);
            byte[] server = new byte[1];
            fileInputStream.read(server);
//            String serverId = server.toString();
            String serverId = new String(server);

            byte[] serverListTmp = new byte[1];
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            while (fileInputStream1.read(serverListTmp) != -1){
                byteBuffer.put(serverListTmp);
            }

            byteBuffer.flip();
            byte[] serverList = new byte[byteBuffer.remaining()];
            byteBuffer.get(serverList);
//            String serverListStr = serverList.toString();
            String serverListStr = new String(serverList);
            String[] serverListArr = serverListStr.split("\n");
            ArrayList<String> serverListWithoutSelf = new ArrayList<String>();
            Worker worker = new Worker();
            for(int i = 0; i < serverListArr.length; i++){
                String[] serverArr = serverListArr[i].split("=");
                String serverIdStr = serverArr[0];
                String[] serverIdArr = serverIdStr.split("\\.");
                String id = serverIdArr[1];
                if(id.equals(serverId)){
                    String oneServerStr = serverArr[1];
                    String portStr = oneServerStr.split(":")[1];
//                    System.out.println(oneServerStr);
                    worker.setPort(Integer.parseInt(portStr));

                   continue;
                }
                serverListWithoutSelf.add(serverArr[1]);
            }

            worker.setServerList(serverListWithoutSelf);
            worker.start();

        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

//        Worker worker = new Worker();
//        worker.start();
    }
}
