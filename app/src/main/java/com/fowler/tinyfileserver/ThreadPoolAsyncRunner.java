package com.fowler.tinyfileserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

public class ThreadPoolAsyncRunner implements NanoHTTPD.AsyncRunner {

    private ExecutorService executor;
    private final List<NanoHTTPD.ClientHandler> running =
            Collections.synchronizedList(new ArrayList<NanoHTTPD.ClientHandler>());


    public ThreadPoolAsyncRunner() {
        int nProcessors = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(nProcessors);
    }

    @Override
    public void closeAll() {
        // copy of the list for concurrency
        for (NanoHTTPD.ClientHandler clientHandler : new ArrayList<>(this.running)) {
            clientHandler.close();
        }
    }

    @Override
    public void closed(NanoHTTPD.ClientHandler clientHandler) {
        running.remove(clientHandler);
    }

    @Override
    public void exec(NanoHTTPD.ClientHandler clientHandler) {
        executor.submit(clientHandler);
        running.add(clientHandler);
    }
}
