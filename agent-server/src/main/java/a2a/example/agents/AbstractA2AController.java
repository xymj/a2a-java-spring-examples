package a2a.example.agents;

import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.*;

public abstract class AbstractA2AController {
  private final ObjectMapper objectMapper = new ObjectMapper();

  protected abstract JSONRPCHandler getJSONRPCHandler();

  public Object handleRequest(@RequestBody String requestBody) {
    try {
      JSONRPCHandler jsonRpcHandler = getJSONRPCHandler();
      // Parse the request to determine its type
      if (isStreamingRequest(requestBody)) {
        StreamingJSONRPCRequest<?> request =
            objectMapper.readValue(requestBody, StreamingJSONRPCRequest.class);
        if (request instanceof SendStreamingMessageRequest) {
          return jsonRpcHandler.onMessageSendStream((SendStreamingMessageRequest) request);
        } else if (request instanceof TaskResubscriptionRequest) {
          return jsonRpcHandler.onResubscribeToTask((TaskResubscriptionRequest) request);
        }
      } else {
        NonStreamingJSONRPCRequest<?> request =
            objectMapper.readValue(requestBody, NonStreamingJSONRPCRequest.class);
        if (request instanceof GetTaskRequest) {
          return jsonRpcHandler.onGetTask((GetTaskRequest) request);
        } else if (request instanceof CancelTaskRequest) {
          return jsonRpcHandler.onCancelTask((CancelTaskRequest) request);
        } else if (request instanceof SetTaskPushNotificationConfigRequest) {
          return jsonRpcHandler
              .setPushNotificationConfig((SetTaskPushNotificationConfigRequest) request);
        } else if (request instanceof GetTaskPushNotificationConfigRequest) {
          return jsonRpcHandler
              .getPushNotificationConfig((GetTaskPushNotificationConfigRequest) request);
        } else if (request instanceof SendMessageRequest) {
          return jsonRpcHandler.onMessageSend((SendMessageRequest) request);
        } else if (request instanceof ListTaskPushNotificationConfigRequest) {
          return jsonRpcHandler
              .listPushNotificationConfig((ListTaskPushNotificationConfigRequest) request);
        } else if (request instanceof DeleteTaskPushNotificationConfigRequest) {
          return jsonRpcHandler
              .deletePushNotificationConfig((DeleteTaskPushNotificationConfigRequest) request);
        }
      }
    } catch (Exception e) {
      JSONRPCError jsonRpcError = new InvalidRequestError();
      return new JSONRPCErrorResponse(null, jsonRpcError);
    }

    JSONRPCError error = new UnsupportedOperationError();
    return new JSONRPCErrorResponse(null, error);
  }



  private static boolean isStreamingRequest(String requestBody) {
    try {
      JsonNode node = new ObjectMapper().readTree(requestBody);
      JsonNode method = node != null ? node.get("method") : null;
      return method != null && (SendStreamingMessageRequest.METHOD.equals(method.asText())
          || TaskResubscriptionRequest.METHOD.equals(method.asText()));
    } catch (Exception e) {
      return false;
    }
  }
}
