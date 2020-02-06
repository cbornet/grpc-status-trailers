package com.example.grpc.server;

import com.example.grpc.GreetingServiceGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import com.example.grpc.HelloResponseOrBuilder;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class MyGrpcServer {

  static final Metadata.Key<String> ERROR = Metadata.Key.of("test-error", ASCII_STRING_MARSHALLER);

  static public void main(String[] args) throws IOException, InterruptedException, ExecutionException {

    String serverName = InProcessServerBuilder.generateName();

    Server server = InProcessServerBuilder.forName(serverName)
        .addService(new GreetingServiceImpl()).build();

    server.start();

    ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    GreetingServiceGrpc.GreetingServiceStub stub = GreetingServiceGrpc.newStub(channel);

    CompletableFuture<Throwable> error = new CompletableFuture<>();

    StreamObserver<HelloResponse> responseObserver = new StreamObserver<HelloResponse>() {
      @Override
      public void onNext(HelloResponse helloResponse) {}

      @Override
      public void onError(Throwable throwable) {
        error.complete(throwable);
      }

      @Override
      public void onCompleted() {}
    };

    stub.greeting(HelloRequest.newBuilder().build(), responseObserver);
    Throwable t = error.get();
    Metadata metadata = ((StatusRuntimeException) t).getTrailers();
    System.out.println(metadata.get(ERROR));

    server.shutdownNow();
  }

  public static class GreetingServiceImpl extends GreetingServiceGrpc.GreetingServiceImplBase {
    @Override
    public void greeting(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
      Metadata metadata = new Metadata();
      metadata.put(ERROR, "test");
      throw Status.INTERNAL.asRuntimeException(metadata);
      //responseObserver.onError(Status.INTERNAL.asRuntimeException(metadata));
    }
  }
}
