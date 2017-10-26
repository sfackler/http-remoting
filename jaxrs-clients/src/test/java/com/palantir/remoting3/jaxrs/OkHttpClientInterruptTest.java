/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.remoting3.jaxrs;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;

public class OkHttpClientInterruptTest extends TestBase {
    @Rule
    public final MockWebServer server = new MockWebServer();

    @Test(timeout = 10_000)
    public void http_client_should_be_interruptible() throws InterruptedException {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                Thread.sleep(Duration.ofDays(1).toMillis());
                return null;
            }
        });

        InfiniteHangService infiniteHangService = JaxRsClient.create(InfiniteHangService.class, "foo",
                createTestConfig("http://localhost:" + server.getPort()));

        CountDownLatch expensiveCallStarted = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            System.out.println("Starting expensive call");
            expensiveCallStarted.countDown();
            infiniteHangService.hangForever();
            System.out.println("Finished call");
        });

        thread.start();

        expensiveCallStarted.await();

        System.out.println("Interrupting thread");
        thread.interrupt();

        thread.join();

    }

    @Path("/")
    public interface InfiniteHangService {
        @GET
        @Path("foo")
        String hangForever();
    }
}
