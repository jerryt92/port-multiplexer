package io.github.jerryt92.multiplexer.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ForwardTarget {
    private String host;
    private int port;
    private boolean reject = false;
}
