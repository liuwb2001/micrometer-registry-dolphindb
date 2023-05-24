# micrometer-registry-dolphindb
# Micrometer

1. ## Micrometer简介

   Micrometer 是用于检测基于 JVM 应用程序指标的检测库。它可以基于一些最流行的监控系统（如prometheus，influxDB），为用户在客户端检测提供一个简单的外观（facade），让用户可以检测基于 JVM 的应用程序的代码性能。micrometer旨在最大限度地提高指标检测的可移植性，同时为用户的指标收集活动增加很少的开销。

2. ## Meter

   Meter是一系列监控指标的合称，其中包括`Timer`、`Counter`、`Gauge`、`DistributionSummary`、`LongTaskTimer`、`FunctionCounter`、`FunctionTimer`和`TimeGauge`。不同的Meter类型会产生不同的时间序列指标。

   |          |                Counter                 |                        Gauge                         |                            Timer                             |
   | :------: | :------------------------------------: | :--------------------------------------------------: | :----------------------------------------------------------: |
   |   概述   |         跟踪某个事件的发生次数         |               用于度量某个数值的瞬时值               |                   用于度量操作的时间和频率                   |
   | 应用场景 | 请求计数、异常计数、任务计数、事件计数 | 实时指标监控、缓存和队列容量、状态跟踪、应用程序度量 | 方法执行时间、API调用时间、批处理任务时间、定时任务时间、系统响应时间 |
   |   特点   |    只增不减，适用于累积型的计数场景    |         瞬时值，可增减，可以根据需要随时更新         | 提供一系列统计信息，如最小值、最大值、平均值、百分位数等，以及调用次数和总的持续时间。 |

   

   1. ### Counter

      Counter 是一种度量类型，用于度量一个计数器的值。Counter 的作用是跟踪某个事件发生的次数，可以用于度量系统中的请求次数、错误次数、任务完成次数等。Counter接口允许用户确定一个大于零的数，然后micrometer根据监控的结果在这个数的基础上进行累加。

      Counter通常被用于计算某事件在一定时间内发生的频率。例如我们可以用Counter来计算一个队列中元素的插入删除频率。下面是一个Counter的例子：

      ```java
      Normal rand = ...; // a random generator
      
      MeterRegistry registry = ...
      Counter counter = registry.counter("counter"); (1)
      
      Flux.interval(Duration.ofMillis(10))
              .doOnEach(d -> {
                  if (rand.nextDouble() + 0.1 > 0) { (2)
                      counter.increment(); (3)
                  }
              })
              .blockLast();
      ```

      此外，用户还可以通过调用`counter.increment(n)`的方式来在单个操作中递增n个。

      Counter也可以通过如下方法构造：

      ```Java
      Counter counter = Counter
          .builder("counter")
          .baseUnit("beans") // optional
          .description("a description of what this counter does") // optional
          .tags("region", "test") // optional
          .register(registry);
      ```

      

   2. ### Gauges

      Gauge 是一种度量类型，用于度量某个数值的瞬时值。Gauge 的作用是记录和报告当前的数值，可以用于度量系统中的实时指标、状态和容量。Gauge应用的典型例子是一个监控集合或map的大小或一个运行状态下的线程数。Gauge适合用于检测有上限的变量。不建议使用gauge监测例如请求数这样的没有上限的变量。

      gauge只有在被查看、观测的时候才会发生改变，而不像其他指标一样会累积计数直到将结果发送到后端的监控平台。

      `MeterRegistry`接口包括构造用来观察数值变量、函数、集合、map的gauge。

      ```java
      List<String> list = registry.gauge("listGauge", Collections.emptyList(), new ArrayList<>(), List::size); (1)
      List<String> list2 = registry.gaugeCollectionSize("listSize2", Tags.empty(), new ArrayList<>()); (2)
      Map<String, Integer> map = registry.gaugeMapSize("mapGauge", Tags.empty(), new HashMap<>());
      ```

      gauge可以被用来监测任何`java.lang.Number`类型，例如 `AtomicInteger`

      ```Java
      // maintain a reference to myGauge
      AtomicInteger myGauge = registry.gauge("numberGauge", new AtomicInteger(0));
      
      // ... elsewhere you can update the value it holds using the object reference
      myGauge.set(27);
      myGauge.set(11);
      ```

      

   3. ### Timer

      Timer 是一种度量类型，用于度量操作的持续时间和频率。Timer 的作用是测量一段代码块的执行时间以及该代码块执行的频率。Timer不支持负数，并且支持记录的最大时长为292.3年。例如，考虑一个显示对典型 Web 服务器的请求延迟的图表。 服务器可以快速响应许多请求，因此计时器每秒更新多次。

      Timer的接口如下：

      ```Java
      public interface Timer extends Meter {
          ...
          void record(long amount, TimeUnit unit);
          void record(Duration duration);
          double totalTime(TimeUnit unit);
      }
      ```

      一个Timer的构造例子如下：

      ```java
      Timer timer = Timer
          .builder("my.timer")
          .description("a description of what this timer does") // optional
          .tags("region", "test") // optional
          .register(registry);
      ```

   4. ### LongTaskTimer

      LongTaskTimer 是一种度量类型，用于度量长时间运行的任务（Long Task）的执行情况。LongTaskTimer 的作用是提供一种方法来度量长时间运行任务的数量、持续时间以及任务执行的频率。

      LongTaskTimer 的应用场景包括：长时间任务监控，异步任务监控，阻塞任务监控（如网络请求、数据库查询），容器任务监控。LongTaskTimer 提供了一系列统计信息，如任务的持续时间、任务执行的次数等。

      

   5. ### FunctionTimer

      FunctionTimer 是一种度量类型，与Timer类似，用于度量函数或方法的执行时间和调用频率。FunctionTimer 的作用是提供一种方法来度量函数或方法的执行情况，包括执行时间、调用次数和调用频率。

      FunctionTimer的应用场景包括：函数执行时间监控，函数调用频率监控，应用程序逻辑性能监控，资源消耗监控。

      

   6. ### FunctionCounter

      FunctionCounter 用于度量函数或方法的计数器值。FunctionCounter 的作用是提供一种方法来度量函数或方法被调用的次数，并提供相关的统计信息。

      FunctionCounter的应用场景包括：函数调用次数监控，事件计数监控，错误计数监控，队列长度监控。

      

3. ## micrometer-registry-dolphindb插件

   在`pom.xml`中通过以下代码引入`micrometer-registry-dolphindb`插件：

   ```xml
   	<dependency>
           <groupId>io.micrometer</groupId>
           <artifactId>micrometer-registry-dolphindb</artifactId>
           <version>1.0.0</version>
           <exclusions>
               <exclusion>
                   <artifactId>micrometer-core</artifactId>
                   <groupId>io.micrometer</groupId>
               </exclusion>
           </exclusions>
   	</dependency>
   ```

   在Java文件中通过`import io.micrometer.dolphindb.DolphinDBMeterRegistry;`来引入`micrometer-registry-dolphindb`插件。

4. ## 如何使用DolphinDB作为监视器

   在引入`micrometer-registry-dolphindb`插件后，我们可以在Java程序中使用DolphinDB数据库作为监视器，存储监视数据。下面是一个例子：

   ```Java
   public class Main {
       public static void main(String[] args) {
           Main app = new Main();
           MeterRegistry registry = new DolphinDBMeterRegistry(key -> null, Clock.SYSTEM);
   
           ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
           scheduledExecutorService.scheduleAtFixedRate(() -> app.flushMetric(registry), 1000, 1000, TimeUnit.MILLISECONDS);
       }
   
       private void flushMetric(MeterRegistry registry) {
           Counter counter = Counter.builder("demo.counter")
                   .tag("app", "java")
                   .description("123")
                   .register(registry);
           counter.increment();
           Timer timer = Timer.builder("demo.timer")
                   .tag("app", "python")
                   .register(registry);
           timer.record(() -> {
               try {
                   TimeUnit.MILLISECONDS.sleep(500);  //用sleep模拟被监控的任务延迟
               } catch (InterruptedException ignored) {
               }
           });
           System.out.println(timer.count());
       }
   }
   ```

   我们构建了一个`Counter`和一个`Timer`，名称分别是“demo.counter”和“demo.timer”。`tag`标签分别是`app=java`和`app=python`。数据在DolphinDB中的存储格式是[metric, timestamp, value, tags]，如下图所示：

   其中，metric为监控的指标，timestamp是监控时的时间戳，value为检测属性的数值，tags为标签（可以在Java程序中设定）。

   这些信息可以与监控系统或度量指标的展示工具结合使用，例如 Grafana。

   在这里，我们选择在DolphinDB特有的GUI内通过调用`plot`函数来绘图分析监测数据。

   上述代码的DolphinDB分析脚本如下：

   ```sql
   pt = loadTable("dfs://mydb", `pt)
   select * from pt
   
   t0 = select * from pt where metric=`demo_counter
   t1 = select * from pt where metric=`demo_timer.count
   t2 = select * from pt where metric=`demo_timer.max
   t3 = select * from pt where metric=`demo_timer.sum
   
   plot(t0.value)
   plot(t1.value)
   plot(t2.value)
   plot(t3.value)
   ```

   `micrometer-registry-dolphindb`默认的数据库路径是“dfs://mydb”，默认的存储表是`pt`。

   

   依次绘图结果如下：
![t0](https://github.com/liuwb2001/micrometer-registry-dolphindb/assets/79987900/3ab02ac4-760b-4ce4-9e76-772de43ddea4)
![t1](https://github.com/liuwb2001/micrometer-registry-dolphindb/assets/79987900/55e26f70-4841-45c6-b6db-60b867517c68)
![t3](https://github.com/liuwb2001/micrometer-registry-dolphindb/assets/79987900/bc5f020d-49a4-4d42-9fd7-44e7bb00db52)
![t3](https://github.com/liuwb2001/micrometer-registry-dolphindb/assets/79987900/c6201ebe-8471-41e0-98d1-eaeed97e6642)

   表pt的存储快照如下：
![all](https://github.com/liuwb2001/micrometer-registry-dolphindb/assets/79987900/9dab67f6-1271-4def-9673-bd61ccda46d4)



