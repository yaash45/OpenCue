
/*
 * Copyright Contributors to the OpenCue Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.imageworks.spcue.test.dispatcher;

import javax.annotation.Resource;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import com.imageworks.spcue.DispatchHost;
import com.imageworks.spcue.config.TestAppConfig;
import com.imageworks.spcue.dao.HostDao;
import com.imageworks.spcue.dispatcher.BookingQueue;
import com.imageworks.spcue.dispatcher.Dispatcher;
import com.imageworks.spcue.dispatcher.commands.DispatchBookHost;
import com.imageworks.spcue.grpc.host.HardwareState;
import com.imageworks.spcue.grpc.report.RenderHost;
import com.imageworks.spcue.service.HostManager;
import com.imageworks.spcue.util.CueUtil;

@Transactional
@ContextConfiguration(classes=TestAppConfig.class, loader=AnnotationConfigContextLoader.class)
public class TestBookingQueue extends AbstractTransactionalJUnit4SpringContextTests {

    @Resource
    HostDao hostDao;

    @Resource
    Dispatcher dispatcher;

    @Resource
    HostManager hostManager;

    @Resource
    BookingQueue bookingQueue;

    private static final String HOSTNAME = "beta";

    @Before
    public void create() {
        RenderHost host = RenderHost.newBuilder()
                .setName(HOSTNAME)
                .setBootTime(1192369572)
                .setFreeMcp(76020)
                .setFreeMem(53500)
                .setFreeSwap(20760)
                .setLoad(1)
                .setTotalMcp(195430)
                .setTotalMem(8173264)
                .setTotalSwap(20960)
                .setNimbyEnabled(false)
                .setNumProcs(1)
                .setCoresPerProc(100)
                .setState(HardwareState.UP)
                .setFacility("spi")
                .addAllTags(ImmutableList.of("mcore", "4core", "8g"))
                .setFreeGpuMem((int) CueUtil.MB512)
                .setTotalGpuMem((int) CueUtil.MB512)
                .build();

        hostManager.createHost(host);
    }

    @Test
    @Transactional
    @Rollback(true)
    public void testBookingQueue() {

        int healthThreshold = 10;
        int minUnhealthyPeriodMin = 3;
        int queueCapacity = 2000;
        int corePoolSize = 10;
        int maxPoolSize = 14;

        DispatchHost host1 = hostDao.findDispatchHost(HOSTNAME);
        host1.idleCores = 500;
        DispatchHost host2 = hostDao.findDispatchHost(HOSTNAME);
        DispatchHost host3 = hostDao.findDispatchHost(HOSTNAME);
        BookingQueue queue = new BookingQueue(healthThreshold, minUnhealthyPeriodMin, queueCapacity,
                corePoolSize, maxPoolSize);
        bookingQueue.execute(new DispatchBookHost(host2,dispatcher));
        bookingQueue.execute(new DispatchBookHost(host3,dispatcher));
        bookingQueue.execute(new DispatchBookHost(host1,dispatcher));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }




}

