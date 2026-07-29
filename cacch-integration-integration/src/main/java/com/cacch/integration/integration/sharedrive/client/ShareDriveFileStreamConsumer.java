package com.cacch.integration.integration.sharedrive.client;

import java.io.IOException;
import java.io.InputStream;

/**
 * 共享盘文件流消费回调（调用方须在 try-with-resources 内消费 InputStream）
 *
 * @author hongfu_zhou@cacch.com
 */
@FunctionalInterface
public interface ShareDriveFileStreamConsumer {

    /**
     * 消费共享盘文件输入流
     *
     * @param inputStream 文件流；由客户端打开并在回调返回后关闭
     * @throws IOException 读写失败时抛出
     */
    void accept(InputStream inputStream) throws IOException;
}
