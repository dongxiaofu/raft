创建发送数据的socket、创建接收数据的socket、接收数据、发送数据，每个工作单独使用一个线程。

每个node，即每个进程，在三种状态间转换，不能通过为每个状态创建一个进程来完成（例如服务端和客户端）。

接收数据，影响进程状态，进而影响发送数据，这涉及线程通信，如何实现？

candidate接收到了过半ACK，接收线程应该进入休眠，让渡资源给发送线程吗？
若让渡了，剩余的数据，是否接收，后面继续接收，会导致未知错误码？

难点，我不知道用什么方式实现Raft算法中的一系列操作。

每个线程，都是一个死循环。

需要手工控制每个线程的唤醒与休眠吗？

创建接收数据的socket线程 与 接收数据线程，存在先后关系；创建发送数据的socket线程 与 发送数据，也是如此。
A.创建接收数据的socket 与 接收数据，放在同一个线程。
办不到，因为前者有死循环。
Aa.创建一个socket,马上启动一个接收数据的thread，此线程接收数据，需要while(true){}运行吗？
===因为是长连接，需要。
Ab.接收线程，while(true){}，没有数据可接收的时候，仍然运行，占用系统资源吗？
===回答不了。假设是的，我这样解决：发现接收到的数据为空时，让接收线程sleep
B.分两个线程
接收数据的socket全部创建完毕后，才开始接收数据？
如何判断是否创建完毕？===不好判断
如何安排两个线程的先后顺序？
====放弃此方案

创建发送socket 与 发送数据
A.两个线程还是一个线程？
Aa.两个线程===可以
Aaa.创建socket完毕后，发送数据线程，才启动。
发送数据事件，有两大类：
一、拉选票
二、heartbeat
区分二者的原因是，发送数据的间隔时间不一样。
这两类事件，都打算使用timer，据说，timer会启动新线程。
Aab.状态转换后，拉选票timer需要死亡，heartbeat timer需要启动。再次回到candidate时，拉选票线程如何启动？
Aac.创建socket线程，和拉选票timer线程，需要控制好先后顺序。
打算使用 notify和wait 来控制先后顺序，但是，一直构思不出具体该怎么写代码。
Node，根据state是否变化flag，决定执行哪个动作？
什么动作？
管理timer的动作，动作启动后，flag设置为false，本动作睡眠
具体timer，timer启动后，flag设置为true，何时睡眠？是由这个动作，来唤醒管理timer的动作吗？
===不是。此动作启动后，一直运行吗？拉选票timer死亡于node state变为非candidate；heartbeat timer死亡于node state变为非leader
===不对。wait机制管理的不是【管理timer的动作】和【具体timer】，而是【管理timer的动作】和【接收消息的过程中】。
===非典型notify和wait，只有【管理timer的动作】的wait，而无【接收消息的过程中】的wait。
===当state变化时，该线程wait
===node state监控线程 与 具体的timer 能够用 notify 机制处理吗？
===timer启动，监控线程需要sleep；何时唤醒呢？当node state发生变化时，需要再次唤醒 监控线程。timer停止时，唤醒

解决了大难题。为什么会这样困难？我对照着notify例程，不能快速找出具体场景中的对应部分。
当state变化时，唤醒管理timer的动作;state的变化，产生于接收消息的过程中
Aad.单独用一个线程，根据node state管理timer。此线程，平时睡眠，当state有变化时运行。
Aada.该线程，需要while(true){}吗？需要

Ab.一个线程===不可以，或者说，不方便。不能控制何时发送数据

==============
candidate、leader发生数据，使用timer。
follower发送ack，不使用timer，还没有实现。
follower探测leader是否正常，使用timer，timeout是heartTimout。用单独线程C实现。
===follower怎么检查是否收到了heartbeat？暂时不知道。
===每次收到heartbeat设置isReceived为true，并记录接收时间；另外用一个线程，定时检查这个值，过期就设置为false.
再用一个线程，检查isReceived检查这个值，若为false，就认为没有收到。此种方法，三个线程，时间间隔不好把握。
===每次收到heartbeat，计算两个hearbeat的时间差。有问题，leader不发送，没有接受到，如何计算时间差？
===改进。另用一个线程，计算两次heartbeat的时间差，若没有接受到数据，用当前时间做现在heartbeat的时间。
非follower，线程C休眠，follower，线程C运行，如何实现？仍取决于node state。

