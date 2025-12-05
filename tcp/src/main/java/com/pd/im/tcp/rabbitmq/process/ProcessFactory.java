package com.pd.im.tcp.rabbitmq.process;

/**
 * @author Parker
 * @date 12/5/25
 */
public class ProcessFactory {
    private static BaseProcess defaultProcess;

    static {
        defaultProcess = new BaseProcess() {
            @Override
            public void processBefore() {

            }

            @Override
            public void processAfter() {

            }
        };
    }

    public static BaseProcess getMessageProcess(Integer command) {
        return defaultProcess;
    }
}
