package com.adobe.printservice.render;

import com.adobe.printservice.model.Job;

public interface Renderer {

    String render(Job job);
}
