package com.eumji.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RPCServer {

    private static final String RPC_QUEUE_NAME = "rpc_queue";

    private static String fib(String  request) {
        return "hello client, i got you request message: "+request;
    }

    public static void main(String[] argv) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = null;
        try {
            connection      = factory.newConnection();
            final Channel channel = connection.createChannel();

            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

            channel.basicQos(1);

            System.out.println("RPC Server Awaiting Client requests");

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                            .Builder()
                            .correlationId(properties.getCorrelationId())
                            .build();

                    String response = "";

                    try {
                        String message = new String(body,"UTF-8");

                        System.out.println(" client request message: " + message );
                        response = fib(message);
                    }
                    catch (RuntimeException e){
                        System.out.println(" server has exception: " + e.toString());
                    }
                    finally {
                        channel.basicPublish( "", properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));

                        channel.basicAck(envelope.getDeliveryTag(), false);
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, consumer);

            //loop to prevent reaching finally block
            while(true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException _ignore) {}
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
        finally {
            if (connection != null)
                try {
                    connection.close();
                } catch (IOException _ignore) {}
        }
    }
}