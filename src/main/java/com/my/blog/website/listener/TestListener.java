package com.my.blog.website.listener;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author 文希
 * @create 2019-11-07 21:56
 */
@Component
public class TestListener implements ApplicationListener {
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        Object source = applicationEvent.getSource();


    }
}
