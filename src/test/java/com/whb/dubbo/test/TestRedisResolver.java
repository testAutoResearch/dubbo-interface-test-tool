/*
 * Copyright (c) 2010-2020 Founder Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Founder. You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the agreements
 * you entered into with Founder.
 *
 */
package com.whb.dubbo.test;

import com.whb.dubbo.cache.RedisResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

/**
 * @author Joey
 * @date 2018/6/17 17:24
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestRedisResolver {

    @Autowired
    private RedisResolver redisResolver;

    @Test
    public void testString() {

        System.out.println("begin.");

        redisResolver.set("testKey", "testValue", 1, TimeUnit.HOURS);

        String value = (String) redisResolver.get("testKey");

        Assert.assertTrue("testValue".equals(value));


        System.out.println("done.");

    }
}
