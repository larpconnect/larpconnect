package com.larpconnect.njall.queue.publisher;

import com.google.common.util.concurrent.Service;

/** Service managing AMQP 1.0 connection lifecycle, queue topology, and publisher resources. */
public interface AmqpQueueService extends AmqpClient, Service {}
